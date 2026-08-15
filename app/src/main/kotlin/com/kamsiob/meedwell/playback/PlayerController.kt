package com.kamsiob.meedwell.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.kamsiob.meedwell.AppContainer
import com.kamsiob.meedwell.core.model.Track
import com.kamsiob.meedwell.core.subsonic.SubsonicClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What the interface talks to about playback.
 *
 * Holds a `MediaController` bound to the service, so the notification, the lock
 * screen and the app are all driving one player rather than two that can
 * disagree.
 */
class PlayerController(
    private val context: Context,
    private val container: AppContainer,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    /**
     * A position ticker.
     *
     * The player only emits events on state changes, so without this the
     * waveform and the clock would sit still through a whole track. One second
     * is enough for a clock and for a scrubber, and cheap enough to run only
     * while something is actually playing.
     */
    private fun startTicker() {
        if (ticker != null) return
        ticker = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(1_000)
                val c = controller ?: continue
                if (c.isPlaying) pushState(c)
            }
        }
    }

    private var ticker: kotlinx.coroutines.Job? = null

    fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val c = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = c
                c.addListener(StateListener())
                restoreQueue(c)
                pushState(c)
                startTicker()
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun release() {
        ticker?.cancel()
        ticker = null
        controller?.release()
        controller = null
    }

    /**
     * Puts the saved queue back without playing it.
     *
     * `prepare()` but never `play()`. Reopening the app lands on the same
     * queue, paused where it left off. An app that starts making noise because
     * you opened it is exactly the kind of thing this one does not do.
     */
    private fun restoreQueue(c: MediaController) {
        if (c.mediaItemCount > 0) return
        scope.launch {
            val saved = container.database.queue().all().map { it.trackId }
            if (saved.isEmpty()) return@launch
            val tracks = container.library.tracks(saved)
            if (tracks.isEmpty()) return@launch
            val items = tracks.map { it.toMediaItem(container.client()) }
            c.setMediaItems(
                items,
                container.settings.queueIndex.coerceIn(0, items.lastIndex),
                container.settings.queuePositionSeconds * 1000,
            )
            c.prepare()
            pushState(c)
        }
    }

    // ---------- Commands ----------

    fun playAlbum(albumId: String, startIndex: Int = 0) {
        scope.launch {
            val tracks = container.library.tracksForAlbum(albumId)
            playTracks(tracks, startIndex)
        }
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        val client = container.client()
        val total = wholeRecordCount(tracks)
        c.setMediaItems(tracks.map { it.toMediaItem(client, total) }, startIndex, 0L)
        c.prepare()
        c.play()
    }

    /**
     * Puts tracks straight after whatever is playing.
     *
     * With nothing playing there is no "after", so this starts them instead.
     * Doing nothing would be the literal reading and the useless one.
     */
    fun playNext(tracks: List<Track>) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        if (c.mediaItemCount == 0) return playTracks(tracks)
        val client = container.client()
        val total = wholeRecordCount(tracks)
        c.addMediaItems(c.currentMediaItemIndex + 1, tracks.map { it.toMediaItem(client, total) })
    }

    /** Puts tracks at the end of the queue. Same fallback when nothing is playing. */
    fun addToQueue(tracks: List<Track>) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        if (c.mediaItemCount == 0) return playTracks(tracks)
        val client = container.client()
        val total = wholeRecordCount(tracks)
        c.addMediaItems(tracks.map { it.toMediaItem(client, total) })
    }

    /** Jumps to a position in the queue, for the queue sheet. */
    fun playQueueItem(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        c.seekTo(index, 0L)
        c.play()
    }

    /** Drops one item out of the queue. */
    fun removeQueueItem(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        c.removeMediaItem(index)
    }

    /**
     * The queue as it stands, read straight off the player.
     *
     * Read on demand rather than mirrored into state: a second copy of the
     * queue is a second thing that can disagree with the player, and the
     * player is the one making the sound.
     */
    fun queueSnapshot(): List<QueueItem> {
        val c = controller ?: return emptyList()
        return (0 until c.mediaItemCount).map { i ->
            val item = c.getMediaItemAt(i)
            QueueItem(
                index = i,
                trackId = item.mediaId,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString().orEmpty(),
                artworkUri = item.mediaMetadata.artworkUri?.toString(),
                isCurrent = i == c.currentMediaItemIndex,
            )
        }
    }

    fun playPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun setShuffle(on: Boolean) {
        controller?.shuffleModeEnabled = on
    }

    /** Off, then repeat all, then repeat one, which is the order everybody expects. */
    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun next() = controller?.seekToNextMediaItem()

    fun previous() {
        val c = controller ?: return
        // The convention people expect: near the start of a track, previous
        // goes back a track; further in, it restarts the current one.
        if (c.currentPosition > RESTART_THRESHOLD_MS) c.seekTo(0) else c.seekToPreviousMediaItem()
    }

    fun seekTo(fraction: Float) {
        val c = controller ?: return
        val duration = c.duration
        if (duration > 0) c.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
    }

    private inner class StateListener : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = pushState(player)
    }

    private fun pushState(player: Player) {
        _state.value = PlaybackState(
            hasQueue = player.mediaItemCount > 0,
            isPlaying = player.isPlaying,
            title = player.mediaMetadata.title?.toString().orEmpty(),
            artist = player.mediaMetadata.artist?.toString().orEmpty(),
            artworkUri = player.mediaMetadata.artworkUri?.toString(),
            trackId = player.currentMediaItem?.mediaId,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.coerceAtLeast(0),
            isLocalFile = player.currentMediaItem
                ?.localConfiguration?.uri?.scheme
                ?.let { it != "http" && it != "https" } ?: false,
            shuffle = player.shuffleModeEnabled,
            repeat = when (player.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.One
                Player.REPEAT_MODE_ALL -> RepeatMode.All
                else -> RepeatMode.Off
            },
            queueSize = player.mediaItemCount,
            trackNumber = player.currentMediaItem?.mediaMetadata?.trackNumber ?: 0,
            trackCount = player.currentMediaItem?.mediaMetadata?.totalTrackCount ?: 0,
        )
    }

    private companion object {
        const val RESTART_THRESHOLD_MS = 5_000L
    }
}

data class PlaybackState(
    val hasQueue: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val artworkUri: String? = null,
    val trackId: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    /**
     * Whether the thing playing is a file on this phone rather than a stream.
     * Drives the honest line under the title, which never implies more than
     * MP3 for a stream, because MP3 is what Bandcamp's API serves.
     */
    val isLocalFile: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.Off,
    val queueSize: Int = 0,
    /** The track's own number on its record, and how many there are. */
    val trackNumber: Int = 0,
    val trackCount: Int = 0,
) {
    /**
     * "andante · IV of IX", the programme line.
     *
     * Only the parts that are true. See `Programme` for why there is no
     * dynamic marking in it.
     */
    val programmeLine: String
        get() = com.kamsiob.meedwell.core.library.Programme.line(title, trackNumber, trackCount)

    /** The tempo marking alone, for the mini player's one short line. */
    /**
     * The tone voicing in force, named rather than numbered.
     *
     * Placeholder until the tone engine exists; it reads "As Recorded", which
     * is both the default and the honest answer while nothing is being applied.
     */
    val toneName: String get() = "As Recorded"

    val tempoMark: String?
        get() = com.kamsiob.meedwell.core.library.Programme.tempoIn(title)

    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

enum class RepeatMode { Off, All, One }

/** One row of the queue sheet. */
data class QueueItem(
    val index: Int,
    val trackId: String,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val isCurrent: Boolean,
)

/**
 * How many tracks a list represents, when it is one whole record.
 *
 * Zero for a mixed queue, which is the honest answer: a queue somebody
 * assembled out of six records is not a programme and has no movement numbers.
 */
private fun wholeRecordCount(tracks: List<Track>): Int =
    if (tracks.isNotEmpty() && tracks.all { it.albumId == tracks.first().albumId }) tracks.size else 0

/**
 * Turns a track into something the player can read.
 *
 * **The local file wins.** If a file for this track is on the phone it is
 * played from there, and the stream is not touched. That is the merge rule
 * "prefer the local file for playback" made real, and after Tier C it is how
 * anything a user actually owns gets played.
 */
internal fun Track.toMediaItem(client: SubsonicClient?, totalTrackCount: Int = 0): MediaItem {
    val uri: Uri = when {
        localPath != null -> localPath!!.toUri()
        client != null -> client.streamUrl(id).toUri()
        // Neither a file nor a server. The item is still built so the queue
        // keeps its shape and the row can be marked as unplayable rather than
        // silently vanishing.
        else -> Uri.EMPTY
    }

    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(albumName)
                .setTrackNumber(trackNumber.takeIf { it > 0 })
                // How many movements the record has, so the player can say
                // "IV of IX". Only set when the caller handed over a whole
                // record; a mixed queue is not a programme and gets nothing.
                .setTotalTrackCount(totalTrackCount.takeIf { it > 0 })
                .setArtworkUri(client?.coverArtUrl(coverArtId)?.toUri())
                .setIsPlayable(uri != Uri.EMPTY)
                .build()
        )
        .build()
}
