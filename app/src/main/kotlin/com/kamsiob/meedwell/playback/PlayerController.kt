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
                if (c.isPlaying) {
                    tickSleep(c)
                    pushState(c)
                }
            }
        }
    }

    private var ticker: kotlinx.coroutines.Job? = null

    /**
     * The tone control, bound to the player's own audio session.
     *
     * Held here because it has to follow the session, which changes when the
     * player is rebuilt, and nothing else knows when that happens.
     */
    val tone = ToneController()

    /**
     * The remembered voicing, restored before anything connects.
     *
     * `apply` records the choice and then returns, because there is no audio
     * session yet; the real work happens again on connect. Without this the Tone
     * screen would show "As Recorded" on a cold start until the first track
     * played, which reads as the setting having been forgotten.
     *
     * Declared after `tone` on purpose. Kotlin runs initializers in declaration
     * order, so an init block above it would be applying to null.
     */
    init {
        tone.apply(com.kamsiob.meedwell.core.library.Voicing.byName(container.settings.voicing))
        _state.value = _state.value.copy(
            voicing = tone.voicing,
            voicingName = tone.voicing.label,
        )
    }

    /**
     * Applies a voicing and remembers it.
     *
     * The state is updated directly rather than through `pushState`, because
     * that needs a live controller and somebody can perfectly well sit on the
     * Tone screen with nothing playing. Going through the player would leave the
     * checkmark on the old row until the next track started.
     */
    fun setVoicing(voicing: com.kamsiob.meedwell.core.library.Voicing) {
        container.settings.voicing = voicing.name
        tone.apply(voicing)
        _state.value = _state.value.copy(
            voicing = tone.voicing,
            voicingName = tone.voicing.label,
            toneAvailable = tone.available,
        )
    }

    // ---------- The sleep timer ----------

    /**
     * When the music stops, in epoch millis, or null for no timer.
     *
     * Held as an **absolute moment** rather than as a remaining duration, so it
     * survives the process being paused, the ticker being late, and the phone
     * sleeping. A countdown decremented on a timer drifts; a wall-clock target
     * cannot.
     */
    private var sleepAt: Long? = null

    /** Stop when the current piece ends, rather than at a clock time. */
    private var sleepAtEndOfPiece: Boolean = false

    /**
     * Sets a timer, or clears it.
     *
     * Stops the music **and** the surroundings together, which is why the
     * caller passes a hook rather than this reaching for the other player: one
     * of them stopping and leaving rain running is the failure that wakes
     * somebody up.
     */
    fun setSleepTimer(minutes: Int?) {
        sleepAtEndOfPiece = false
        sleepAt = minutes?.let { System.currentTimeMillis() + it * 60_000L }
        if (sleepAt == null) controller?.volume = 1f
        pushState(controller ?: return)
    }

    /** Stop when this piece finishes. */
    fun setSleepAtEndOfPiece(on: Boolean) {
        sleepAt = null
        sleepAtEndOfPiece = on
        controller?.volume = 1f
        pushState(controller ?: return)
    }

    /** Called when the timer runs out, so the ambience stops with the music. */
    var onSleep: (() -> Unit)? = null

    /**
     * Applies the fade and stops when the moment arrives.
     *
     * Runs on the same one second ticker as the clock, so there is no second
     * timer to fall out of step with the first.
     */
    private fun tickSleep(c: MediaController) {
        val target = sleepAt
        if (target != null) {
            val remaining = (target - System.currentTimeMillis()) / 1000
            if (remaining <= 0) {
                c.volume = 1f
                c.pause()
                sleepAt = null
                onSleep?.invoke()
                return
            }
            c.volume = com.kamsiob.meedwell.core.library.SleepPlan.gainAt(remaining)
            return
        }
        if (sleepAtEndOfPiece && c.mediaItemCount > 0) {
            val duration = c.duration
            if (duration > 0 && c.currentPosition >= duration - 400) {
                c.pause()
                sleepAtEndOfPiece = false
                onSleep?.invoke()
            }
        }
    }

    fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val c = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = c
                c.addListener(StateListener())
                tone.apply(com.kamsiob.meedwell.core.library.Voicing.byName(container.settings.voicing))
                restoreQueue(c)
                pushState(c)
                startTicker()
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun release() {
        tone.release()
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
        if (!container.settings.resumeQueueOnOpening) return
        scope.launch {
            val saved = container.database.queue().all().map { it.trackId }
            if (saved.isEmpty()) return@launch
            val tracks = container.library.tracks(saved)
            if (tracks.isEmpty()) return@launch
            // The count has to travel with the restored queue too.
            //
            // Every other path passes `wholeRecordCount(tracks)`; this one did
            // not, so it defaulted to zero and the player's programme line, the
            // "IV of IX" between the title and the scrubber, was permanently
            // blank after any restart. It came back only if you started a fresh
            // queue, which is why it looked intermittent rather than broken.
            val total = wholeRecordCount(tracks)
            val items = tracks.map { it.toMediaItem(container.client(), total) }
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
    /**
     * Reorders the queue, one commit per completed drag.
     *
     * `MASTER_SPEC` has said "drag reorder, swipe to remove" since version one;
     * the remove existed and the reorder never did.
     */
    fun moveQueueItem(from: Int, to: Int) {
        val c = controller ?: return
        if (from == to || from !in 0 until c.mediaItemCount || to !in 0 until c.mediaItemCount) return
        c.moveMediaItem(from, to)
        pushState(c)
    }

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
                durationSeconds = (item.mediaMetadata.durationMs ?: 0L) / 1000,
                trackNumber = item.mediaMetadata.trackNumber ?: 0,
                wholeRecordCount = item.mediaMetadata.totalTrackCount ?: 0,
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
            album = player.mediaMetadata.albumTitle?.toString().orEmpty(),
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
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem(),
            sleepSecondsRemaining = sleepAt?.let {
                ((it - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
            },
            sleepAtEndOfPiece = sleepAtEndOfPiece,
            voicing = tone.voicing,
            voicingName = tone.voicing.label,
            toneAvailable = tone.available,
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
    /**
     * The record this track belongs to.
     *
     * The grid sets "Bride Callanan - Harp Music for Early Hours" under the
     * title: artist *and* album. Only the artist was carried, so the player's
     * title block said half of what it was drawn to say, on the screen with the
     * least to read.
     */
    val album: String = "",
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
    /**
     * Whether skipping can actually do anything.
     *
     * The transport used to be drawn identically whether or not it could move,
     * so at the end of a record "next" was a button that swallowed taps in
     * silence. A control that cannot act should look like it cannot act, which
     * is also how somebody discovers that repeat is what they wanted.
     */
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    /** The track's own number on its record, and how many there are. */
    val trackNumber: Int = 0,
    val trackCount: Int = 0,
    /** Seconds left on the sleep timer, or null when there is none. */
    val sleepSecondsRemaining: Long? = null,
    val sleepAtEndOfPiece: Boolean = false,
    val voicing: com.kamsiob.meedwell.core.library.Voicing =
        com.kamsiob.meedwell.core.library.Voicing.AsRecorded,
    val voicingName: String = "As Recorded",
    /** False on a phone whose platform did not offer an equalizer at all. */
    val toneAvailable: Boolean = true,
) {
    /** The timer in a word or two, for the More screen's right-hand column. */
    val sleepLabel: String
        get() = when {
            sleepAtEndOfPiece -> "end of this piece"
            sleepSecondsRemaining != null ->
                com.kamsiob.meedwell.core.library.SleepPlan.countdown(sleepSecondsRemaining)
            else -> "Off"
        }

    /**
     * "andante · IV of IX", the programme line.
     *
     * Only the parts that are true. See `Programme` for why there is no
     * dynamic marking in it.
     */
    val programmeLine: String
        get() = com.kamsiob.meedwell.core.library.Programme.line(title, trackNumber, trackCount)

    /** The tempo marking alone, for the mini player's one short line. */
    /** The tone voicing in force, named rather than numbered. */
    val toneName: String get() = voicingName

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
    val durationSeconds: Long = 0,
    /** Its number on its record, for the bill's Roman numeral. */
    val trackNumber: Int = 0,
    /** Nonzero only when the whole queue is one record: then it is a programme. */
    val wholeRecordCount: Int = 0,
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
                // The running time rides with the item so the queue can total
                // what is left, which is the number somebody planning an
                // evening actually wants.
                .setDurationMs((durationSeconds * 1000).takeIf { it > 0 })
                // How many movements the record has, so the player can say
                // "IV of IX". Only set when the caller handed over a whole
                // record; a mixed queue is not a programme and gets nothing.
                .setTotalTrackCount(totalTrackCount.takeIf { it > 0 })
                // The same stable URL the shelf uses. Minting a fresh salt here
                // gave every media item a different artwork URI, so the
                // notification refetched art it already had on every track.
                .setArtworkUri(
                    com.kamsiob.meedwell.ui.screens.CoverUrls.of(coverArtId)?.toUri()
                )
                .setIsPlayable(uri != Uri.EMPTY)
                .build()
        )
        .build()
}
