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
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun release() {
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
        c.setMediaItems(tracks.map { it.toMediaItem(client) }, startIndex, 0L)
        c.prepare()
        c.play()
    }

    fun playPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
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
) {
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

/**
 * Turns a track into something the player can read.
 *
 * **The local file wins.** If a file for this track is on the phone it is
 * played from there, and the stream is not touched. That is the merge rule
 * "prefer the local file for playback" made real, and after Tier C it is how
 * anything a user actually owns gets played.
 */
internal fun Track.toMediaItem(client: SubsonicClient?): MediaItem {
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
                .setArtworkUri(client?.coverArtUrl(coverArtId)?.toUri())
                .setIsPlayable(uri != Uri.EMPTY)
                .build()
        )
        .build()
}
