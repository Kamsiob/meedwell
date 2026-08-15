package com.kamsiob.meedwell.playback

import androidx.media3.common.Player
import com.kamsiob.meedwell.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Keeps the queue and the position on disk.
 *
 * A release blocker, not polish. The requirement is exact: kill the process
 * mid-track, reboot the phone, reopen the app, and land on the same queue with
 * the same track current, the position matching within a second, playback
 * paused, and nothing re-syncing before the queue is usable.
 *
 * Written on every meaningful change rather than on a timer, because a timer
 * loses whatever happened in the last interval, and process death does not
 * announce itself first.
 */
object QueuePersistence {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun attach(player: Player, container: AppContainer) {
        player.addListener(object : Player.Listener {

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                save(player, container)
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                save(player, container)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Position is worth saving whenever playback starts or stops,
                // since those are the moments a user is most likely to be
                // interrupted by whatever kills the process next.
                save(player, container)
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                save(player, container)
            }
        })
    }

    private fun save(player: Player, container: AppContainer) {
        val ids = (0 until player.mediaItemCount).mapNotNull { index ->
            player.getMediaItemAt(index).mediaId.takeIf { it.isNotBlank() }
        }
        val currentIndex = player.currentMediaItemIndex
        val positionSeconds = (player.currentPosition / 1000).coerceAtLeast(0)
        val currentId = ids.getOrNull(currentIndex)

        scope.launch {
            container.database.queue().replace(ids)
            container.settings.queueIndex = currentIndex
            container.settings.queuePositionSeconds = positionSeconds

            // Resume points on long pieces are a separate, deliberate feature:
            // losing your place in a twenty minute piece is a real loss, and
            // losing it in a three minute song is not. Only long pieces are
            // remembered, and only when the setting is on.
            if (container.settings.rememberLongTrackPosition && currentId != null) {
                val track = container.database.tracks().byId(currentId)
                if (track != null && track.durationSeconds >= LONG_FORM_SECONDS) {
                    container.database.tracks().setResumePosition(currentId, positionSeconds)
                }
            }
        }
    }

    private const val LONG_FORM_SECONDS = 20 * 60L
}
