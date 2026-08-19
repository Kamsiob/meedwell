package com.kamsiob.meedwell.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import com.kamsiob.meedwell.MainActivity
import com.kamsiob.meedwell.R
import com.kamsiob.meedwell.meedwell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Keeps a Surroundings bed alive, and gives it something in the shade.
 *
 * **Ambient sound was the only audio in this app with no controls outside it.**
 * The bed ran on a player built inside a view model, with no foreground service
 * and no notification, which meant three things at once: it could not be paused
 * from the shade or the lock screen, it could not be stopped without reopening
 * the app and finding the card, and Android could end it at any moment because
 * nothing was holding the process up. The one sound most likely to be playing
 * with the phone face down was the one you could not reach.
 *
 * That is what this fixes. While a bed is loaded there is a notification naming
 * the recording, with pause and stop on it, and the service holds the process
 * for as long as the sound is meant to be there.
 *
 * **Not a media session.** The music already owns one, and a second session
 * would fight it for the headset button and the lock screen transport: pressing
 * play on a headset should start the music, never the rain. So this is a plain
 * foreground service with two explicit actions, and the bed stays a bed.
 *
 * The audio focus rule is untouched and lives in `SurroundingsPlayer`: the bed
 * never requests focus, which is the whole reason starting or changing one
 * cannot interrupt whatever is playing over it.
 */
@UnstableApi
class SurroundingsService : Service() {

    private val scope = CoroutineScope(SupervisorJob())
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()

        // The service follows the bed rather than being told twice. Whoever
        // changes the sound updates `SurroundingsBed`, and the notification
        // catches up on its own.
        SurroundingsBed.state
            .onEach { bed ->
                if (!bed.present) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@onEach
                }
                val notification = build(bed)
                if (!started) {
                    startForegroundCompat(notification)
                    started = true
                } else {
                    getSystemService(NotificationManager::class.java)
                        ?.notify(NOTIFICATION_ID, notification)
                }
            }
            .launchIn(scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val player = applicationContext.meedwell.surroundingsPlayer
        when (intent?.action) {
            ACTION_PAUSE -> {
                player.pause()
                SurroundingsBed.setPlaying(false)
            }

            ACTION_RESUME -> {
                player.resume()
                SurroundingsBed.setPlaying(true)
            }

            ACTION_STOP -> {
                player.stop()
                SurroundingsBed.clear()
            }
        }

        // A bed that is already gone should not leave a service behind, which is
        // what happens if the state was cleared before this arrived.
        if (!SurroundingsBed.state.value.present) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else if (!started) {
            startForegroundCompat(build(SurroundingsBed.state.value))
            started = true
        }

        // Not sticky. A restarted service with no sound would be a notification
        // about silence, and nothing here should start playing on its own.
        return START_NOT_STICKY
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun build(bed: SurroundingsBed.State): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        fun action(name: String, label: String, icon: Int) = NotificationCompat.Action(
            icon,
            label,
            PendingIntent.getService(
                this,
                name.hashCode(),
                Intent(this, SurroundingsService::class.java).setAction(name),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(bed.title.ifBlank { "Surroundings" })
            // Says what it is rather than what it is called. Somebody glancing
            // at the shade wants to know why there is rain coming out of their
            // phone, not the name of a feature.
            .setContentText(if (bed.playing) "Playing underneath" else "Held")
            .setOngoing(bed.playing)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .apply {
                if (bed.playing) {
                    addAction(action(ACTION_PAUSE, "Pause", R.drawable.ic_launcher_foreground))
                } else {
                    addAction(action(ACTION_RESUME, "Play", R.drawable.ic_launcher_foreground))
                }
                addAction(action(ACTION_STOP, "Stop", R.drawable.ic_launcher_foreground))
            }
            .build()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Surroundings",
                // Low: it is a thing you already asked for, and it should never
                // make a sound of its own on top of the one it is playing.
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows the field recording playing underneath your music."
                setShowBadge(false)
            }
        )
    }

    override fun onDestroy() {
        scope.cancel()
        started = false
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "surroundings_playing"

        /** Distinct from playback (1001) and downloads (2101). */
        private const val NOTIFICATION_ID = 2102

        private const val ACTION_PAUSE = "com.kamsiob.meedwell.BED_PAUSE"
        private const val ACTION_RESUME = "com.kamsiob.meedwell.BED_RESUME"
        private const val ACTION_STOP = "com.kamsiob.meedwell.BED_STOP"

        /** Called when a bed starts, so the process and the shade both know. */
        fun start(context: Context) {
            context.startForegroundService(
                Intent(context, SurroundingsService::class.java)
            )
        }
    }
}
