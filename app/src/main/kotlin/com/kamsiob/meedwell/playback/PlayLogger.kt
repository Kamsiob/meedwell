package com.kamsiob.meedwell.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.kamsiob.meedwell.AppContainer
import com.kamsiob.meedwell.data.db.PlayEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Writes the play log.
 *
 * Everything on-device that looks like intelligence reads this one table:
 * History, the Forgotten Shelf, and the most-played sort. Without it, all three
 * are permanently empty, which is exactly what they were until this existed.
 *
 * **A play is not a start.** Skipping past a track after four seconds is not
 * listening to it, and counting it would poison the Forgotten Shelf with
 * records the listener never actually heard. So a row is written only when
 * enough of the track went by, and it records how much.
 *
 * Append only. Rows are never updated, so a play is a recorded fact rather than
 * a counter somebody can argue with. The only thing that ever removes one is
 * Erase listening history, which genuinely empties the table.
 */
class PlayLogger(private val container: AppContainer) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentId: String? = null
    private var currentAlbumId: String? = null
    private var listenedMs: Long = 0L
    private var lastTickAt: Long = 0L
    private var durationMs: Long = 0L

    fun attach(player: Player) {
        player.addListener(object : Player.Listener {

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // The outgoing track is settled before the new one starts, so a
                // track that ran to its end is credited even though the
                // transition is what tells us about it.
                flush()
                currentId = mediaItem?.mediaId
                currentAlbumId = null
                listenedMs = 0L
                lastTickAt = 0L
                durationMs = 0L
                currentId?.let { id ->
                    scope.launch {
                        currentAlbumId = container.database.tracks().byId(id)?.albumId
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val now = System.currentTimeMillis()
                if (isPlaying) {
                    lastTickAt = now
                } else {
                    accumulate(now)
                    // Pausing is a good moment to settle, because the process
                    // may not get another one.
                    durationMs = player.duration.coerceAtLeast(0)
                    flush()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) durationMs = player.duration.coerceAtLeast(0)
                if (state == Player.STATE_ENDED) {
                    accumulate(System.currentTimeMillis())
                    flush()
                }
            }
        })
    }

    private fun accumulate(now: Long) {
        if (lastTickAt > 0L) {
            listenedMs += (now - lastTickAt).coerceAtLeast(0)
            lastTickAt = 0L
        }
    }

    /**
     * Writes a row if enough of the track actually went by.
     *
     * "Enough" is whichever comes first: half the track, or a flat minute. Half
     * suits a three minute song; the flat minute stops a forty minute piece
     * from needing twenty minutes before it counts as heard at all.
     */
    private fun flush() {
        val id = currentId ?: return
        val listened = listenedMs / 1000
        currentId = null
        listenedMs = 0L
        if (listened < MINIMUM_SECONDS) return

        val duration = durationMs / 1000
        val halfway = if (duration > 0) duration / 2 else Long.MAX_VALUE
        val enough = listened >= minOf(halfway, SUBSTANTIAL_SECONDS)
        if (!enough) return

        val albumId = currentAlbumId
        scope.launch {
            val resolvedAlbum = albumId ?: container.database.tracks().byId(id)?.albumId ?: return@launch
            container.database.playEvents().record(
                PlayEventEntity(
                    trackId = id,
                    albumId = resolvedAlbum,
                    playedAt = System.currentTimeMillis() / 1000,
                    playedSeconds = listened,
                    completed = duration > 0 && listened >= duration - COMPLETION_SLACK_SECONDS,
                )
            )
        }
    }

    private companion object {
        /** Below this, nothing is recorded at all. A four second skip is not a play. */
        const val MINIMUM_SECONDS = 20L

        /** A flat minute counts as heard, however long the piece is. */
        const val SUBSTANTIAL_SECONDS = 60L

        /** Close enough to the end to call it finished. */
        const val COMPLETION_SLACK_SECONDS = 10L
    }
}
