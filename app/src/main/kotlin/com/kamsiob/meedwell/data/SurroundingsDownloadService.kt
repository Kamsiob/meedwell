package com.kamsiob.meedwell.data

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
import com.kamsiob.meedwell.AppContainer
import com.kamsiob.meedwell.meedwell
import com.kamsiob.meedwell.MainActivity
import com.kamsiob.meedwell.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Downloads Surroundings recordings, and keeps doing it when you leave.
 *
 * **A foreground service rather than a coroutine on a screen.** The queue used
 * to run in the view model's scope, so leaving the app put the whole thing at
 * the mercy of whether Android felt like keeping the process: a pack set going
 * and then backgrounded would quietly stop, and the only sign was that the
 * recordings were not there later.
 *
 * The notification is the price of that and is not a nuisance to be minimised:
 * a long download that cannot be seen or stopped is worse than one that shows
 * itself. It carries what is being fetched, how far along it is, and how many
 * are behind it.
 *
 * **One at a time, in order.** The same rule the coordinator had, kept for the
 * same reason: the network could not manage more, but a hundred half-finished
 * files could not be resumed sensibly and the progress of any one of them would
 * be meaningless.
 *
 * Everything about resuming, verifying and atomically installing a file is still
 * `SurroundingsDownloader`'s job. This only decides what order things happen in
 * and keeps the process alive while they do.
 */
class SurroundingsDownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob())
    private var worker: Job? = null

    /**
     * When the notification was last updated, and what it said.
     *
     * **Android sheds notification updates above five a second**, and a download
     * callback fires far more often than that. The result was not a slow
     * notification, it was no notification: the system logged
     * "Shedding notify (update) ... rate limit (5.0) exceeded" over and over and
     * the shade stayed empty while the download ran invisibly. Throttling here
     * is what makes it appear at all, which Play requires of a foreground
     * service and which anybody watching a 100 MB download deserves.
     */
    private var lastNotifyAt = 0L
    private var lastFraction = -1f
    private lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        // The application's own container, not a second copy of it.
        container = applicationContext.meedwell
        createChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ENQUEUE -> {
                val ids = intent.getStringArrayListExtra(EXTRA_IDS).orEmpty()
                SurroundingsDownloads.set { state ->
                    val already = state.queued.toSet() + setOfNotNull(state.workingOn)
                    state.copy(
                        queued = state.queued + ids.filterNot { it in already },
                        // A fresh attempt clears whatever went wrong last time,
                        // or a row would keep showing an error about a download
                        // that is currently running.
                        failures = state.failures - ids.toSet(),
                    )
                }
                goForeground()
                pump()
            }

            ACTION_CANCEL -> {
                val id = intent.getStringExtra(EXTRA_ID)
                SurroundingsDownloads.set { it.copy(queued = it.queued.filterNot { q -> q == id }) }
                if (SurroundingsDownloads.state.value.workingOn == id) {
                    worker?.cancel()
                    worker = null
                    SurroundingsDownloads.set { it.copy(workingOn = null, progress = 0f) }
                    pump()
                } else {
                    stopIfIdle()
                }
            }

            ACTION_CANCEL_ALL -> {
                SurroundingsDownloads.set { it.copy(queued = emptyList()) }
                worker?.cancel()
                worker = null
                SurroundingsDownloads.set { it.copy(workingOn = null, progress = 0f) }
                stopIfIdle()
            }
        }
        // Not sticky: a restarted service with no queue would be a notification
        // about nothing. The queue is deliberately not persisted, because a
        // download somebody asked for last week is not one they want resumed
        // silently on a Monday.
        return START_NOT_STICKY
    }

    /** Takes the next recording, or stops the service if there is none. */
    private fun pump() {
        if (worker?.isActive == true) return
        val next = SurroundingsDownloads.state.value.queued.firstOrNull()
        if (next == null) {
            stopIfIdle()
            return
        }

        SurroundingsDownloads.set {
            it.copy(queued = it.queued.drop(1), workingOn = next, progress = 0f)
        }

        worker = scope.launch {
            val sound = container.surroundings.offerable().firstOrNull { it.id == next }
            if (sound == null) {
                SurroundingsDownloads.set { it.copy(workingOn = null) }
                worker = null
                pump()
                return@launch
            }

            notify(sound.displayName.ifBlank { "a recording" }, 0f, force = true)
            val outcome = container.surroundingsDownloader.fetch(sound) { got, total ->
                val fraction = if (total > 0) got.toFloat() / total else 0f
                SurroundingsDownloads.set { it.copy(progress = fraction) }
                notify(sound.displayName.ifBlank { "a recording" }, fraction)
            }

            when (outcome) {
                is SurroundingsDownloader.Outcome.Done,
                is SurroundingsDownloader.Outcome.Cancelled -> Unit

                is SurroundingsDownloader.Outcome.Failed -> {
                    SurroundingsDownloads.set {
                        it.copy(failures = it.failures + (next to outcome.message))
                    }
                    // A permanent failure would take the rest of a pack down
                    // with it, one identical error at a time. Say it once and
                    // stop rather than failing ninety more times.
                    if (!outcome.canRetry) {
                        SurroundingsDownloads.set { it.copy(queued = emptyList()) }
                    }
                }
            }

            SurroundingsDownloads.set { it.copy(workingOn = null, progress = 0f) }
            worker = null
            pump()
        }
    }

    private fun stopIfIdle() {
        if (!SurroundingsDownloads.state.value.busy) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun goForeground() {
        val notification = build("Getting recordings", 0f, indeterminate = true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun notify(title: String, fraction: Float, force: Boolean = false) {
        val now = System.currentTimeMillis()
        val movedEnough = kotlin.math.abs(fraction - lastFraction) >= 0.01f
        // Twice a second at most, and only when the bar would visibly move.
        // Well inside the system's limit of five, with room for the odd extra
        // update from a track change arriving at the same moment.
        if (!force && (now - lastNotifyAt < MIN_NOTIFY_GAP_MS || !movedEnough)) return
        lastNotifyAt = now
        lastFraction = fraction
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, build(title, fraction, indeterminate = false))
    }

    private fun build(title: String, fraction: Float, indeterminate: Boolean): Notification {
        val remaining = SurroundingsDownloads.state.value.remaining
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(
                when {
                    remaining > 1 -> "$remaining recordings to go"
                    else -> "Surroundings"
                }
            )
            .setProgress(100, (fraction * 100).toInt(), indeterminate)
            .setOngoing(true)
            .setSilent(true)
            // **Shown at once, not ten seconds from now.**
            //
            // Android defers a foreground service notification on a low
            // importance channel by up to ten seconds, on the theory that a
            // short job should not flash a notification at somebody. Most of
            // these finish inside that window on Wi-Fi, so the notification
            // never appeared at all: the download ran invisibly, which is the
            // one thing a background download must not do, and is what Play
            // requires a foreground service to show.
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                // Low: it is a progress bar, not news. It should be findable in
                // the shade and never make a sound or push anything aside.
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows Surroundings recordings while they are being fetched."
                setShowBadge(false)
            }
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "surroundings_downloads"
        private const val NOTIFICATION_ID = 2101

        /** Two a second, against a system limit of five. */
        private const val MIN_NOTIFY_GAP_MS = 500L

        private const val ACTION_ENQUEUE = "com.kamsiob.meedwell.DOWNLOAD_ENQUEUE"
        private const val ACTION_CANCEL = "com.kamsiob.meedwell.DOWNLOAD_CANCEL"
        private const val ACTION_CANCEL_ALL = "com.kamsiob.meedwell.DOWNLOAD_CANCEL_ALL"
        private const val EXTRA_IDS = "ids"
        private const val EXTRA_ID = "id"

        fun enqueue(context: Context, ids: List<String>) {
            if (ids.isEmpty()) return
            val intent = Intent(context, SurroundingsDownloadService::class.java).apply {
                action = ACTION_ENQUEUE
                putStringArrayListExtra(EXTRA_IDS, ArrayList(ids))
            }
            context.startForegroundService(intent)
        }

        fun cancel(context: Context, id: String) {
            context.startService(
                Intent(context, SurroundingsDownloadService::class.java).apply {
                    action = ACTION_CANCEL
                    putExtra(EXTRA_ID, id)
                }
            )
        }

        fun cancelAll(context: Context) {
            context.startService(
                Intent(context, SurroundingsDownloadService::class.java).apply {
                    action = ACTION_CANCEL_ALL
                }
            )
        }
    }
}
