package com.kamsiob.meedwell.data

import android.content.Context
import androidx.core.content.edit
import com.kamsiob.meedwell.ui.theme.ThemeChoice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Settings, in plain SharedPreferences.
 *
 * Not encrypted, because nothing here is sensitive: a theme choice and a few
 * toggles. The one genuinely sensitive thing the app holds lives in
 * `CredentialStore` and nowhere else.
 *
 * Every setting is exported and restored, so adding one here means adding it to
 * the export format too.
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /**
     * **Daylight is the default.** The grid opens on warm paper.
     *
     * The fallback also catches the old stored values, `Dark` and `Light`, which
     * no longer parse. Anyone upgrading lands on Daylight rather than crashing
     * or silently getting the alternate.
     */
    var theme: ThemeChoice
        get() = runCatching { ThemeChoice.valueOf(prefs.getString(KEY_THEME, null) ?: "") }
            .getOrDefault(ThemeChoice.Daylight)
        set(value) = prefs.edit { putString(KEY_THEME, value.name) }

    var shelfGrid: Boolean
        get() = prefs.getBoolean(KEY_SHELF_GRID, true)
        set(value) = prefs.edit { putBoolean(KEY_SHELF_GRID, value) }

    var gapless: Boolean
        get() = prefs.getBoolean(KEY_GAPLESS, true)
        set(value) = prefs.edit { putBoolean(KEY_GAPLESS, value) }

    /** "Remember position on long tracks", meaning pieces over twenty minutes. */
    var rememberLongTrackPosition: Boolean
        get() = prefs.getBoolean(KEY_LONG_RESUME, true)
        set(value) = prefs.edit { putBoolean(KEY_LONG_RESUME, value) }

    /**
     * Where the queue was when the app last knew. Written with the queue itself
     * so that reopening lands on the same track at the same position, paused.
     */
    var queueIndex: Int
        get() = prefs.getInt(KEY_QUEUE_INDEX, 0)
        set(value) = prefs.edit { putInt(KEY_QUEUE_INDEX, value) }

    var queuePositionSeconds: Long
        get() = prefs.getLong(KEY_QUEUE_POSITION, 0L)
        set(value) = prefs.edit { putLong(KEY_QUEUE_POSITION, value) }

    /**
     * Whether opening the app puts the last queue back, paused.
     *
     * On by default. Off means Meedwell opens with nothing loaded, which is what
     * somebody who uses this alongside another player will want: the queue
     * reappearing is helpful right up until it is the thing you had forgotten
     * about.
     *
     * Either way nothing plays on its own. This chooses whether the queue is
     * **there**, never whether it starts.
     */
    var resumeQueueOnOpening: Boolean
        get() = prefs.getBoolean(KEY_RESUME_QUEUE, true)
        set(value) = prefs.edit { putBoolean(KEY_RESUME_QUEUE, value) }

    /**
     * Whether the choice about mobile data has been put to somebody once.
     *
     * The default is Wi-Fi only and stays that way, but a default nobody was
     * told about is a setting they have to go and discover after a download
     * refuses. Asked at the first download, answered once, and changeable in
     * Settings forever after.
     *
     * Not exported, for the same reason as the one below: it is a fact about
     * this install rather than about the person.
     */
    var hasAskedCellular: Boolean
        get() = prefs.getBoolean(KEY_ASKED_CELLULAR, false)
        set(value) = prefs.edit { putBoolean(KEY_ASKED_CELLULAR, value) }

    /**
     * Whether the notification permission has been put to somebody once.
     *
     * Deliberately **not** exported. It is a fact about this install rather than
     * about the user, and restoring it onto a fresh phone would suppress the one
     * ask on the device that has not had it.
     */
    var hasAskedNotifications: Boolean
        get() = prefs.getBoolean(KEY_ASKED_NOTIFICATIONS, false)
        set(value) = prefs.edit { putBoolean(KEY_ASKED_NOTIFICATIONS, value) }

    /**
     * The same question, asked again at the first download, and tracked apart on
     * purpose.
     *
     * **The two asks buy different things.** Refusing the playback ask costs the
     * shade controls, which is a fair thing to shrug at. Refusing it should not
     * also decide, months later and silently, that a download somebody started
     * runs with nothing to show for it: a foreground service whose notification
     * the system will not draw is invisible work, which is the one thing a
     * background download must never be, and is what Play requires a foreground
     * service to show.
     *
     * This was not theoretical. A phone here had notifications ungranted, and a
     * whole group downloaded start to finish with no sign of it anywhere.
     *
     * Not exported, for the same reason as the one above: a fact about this
     * install rather than about the person.
     */
    var hasAskedDownloadNotifications: Boolean
        get() = prefs.getBoolean(KEY_ASKED_DOWNLOAD_NOTIFICATIONS, false)
        set(value) = prefs.edit { putBoolean(KEY_ASKED_DOWNLOAD_NOTIFICATIONS, value) }

    /**
     * Where the day line starts and ends, in minutes since midnight.
     *
     * **Not sunrise and sunset, and never derived from where you are.** Real
     * solar times need a latitude, and this app asks for no location. The clock
     * itself comes from the phone, so a change of timezone moves the sun without
     * anybody telling Meedwell anything; these two are the hours the line is
     * drawn between, and they are the listener's to set.
     *
     * Six to nine is the grid's own span and a fair default for most of the
     * world most of the year. Somebody in Tromsø in January can say otherwise.
     */
    var dawnMinute: Int
        get() = prefs.getInt(KEY_DAWN, 6 * 60)
        set(value) = prefs.edit { putInt(KEY_DAWN, value.coerceIn(0, 23 * 60 + 59)) }

    var duskMinute: Int
        get() = prefs.getInt(KEY_DUSK, 21 * 60)
        set(value) = prefs.edit { putInt(KEY_DUSK, value.coerceIn(0, 23 * 60 + 59)) }

    var lastSyncAt: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_SYNC, value) }

    /**
     * Set once the user has chosen a path on the Welcome screen, so that
     * choosing "just play my local files" is remembered and they are not asked
     * again every launch.
     */
    var hasChosenPath: Boolean
        get() = prefs.getBoolean(KEY_CHOSEN_PATH, false)
        set(value) = prefs.edit { putBoolean(KEY_CHOSEN_PATH, value) }

    /**
     * The tone voicing, by name.
     *
     * Stored as a name rather than a set of numbers so that a future curve
     * revision reaches everybody who chose that voicing, instead of freezing
     * whatever the gains happened to be the day they picked it.
     */
    var voicing: String
        get() = prefs.getString(KEY_VOICING, null) ?: DEFAULT_VOICING
        set(value) = prefs.edit { putString(KEY_VOICING, value) }

    /** When the last export was written, or zero for never. */
    var lastBackupAt: Long
        get() = prefs.getLong(KEY_LAST_BACKUP, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_BACKUP, value) }

    /**
     * Whether Surroundings recordings may be fetched over mobile data.
     *
     * **On by default, and the default is the careful one.** The largest single
     * recording is twenty-five megabytes and the whole library is over five
     * hundred. Somebody who taps a pack without noticing they are off Wi-Fi
     * should not discover the difference on their bill.
     */
    var wifiOnlyDownloads: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY, true)
        set(value) = prefs.edit { putBoolean(KEY_WIFI_ONLY, value) }

    /** The Surroundings recording playing under the music, if any. */
    var surroundingsSoundId: String?
        get() = prefs.getString(KEY_SURROUNDINGS_SOUND, null)
        set(value) = prefs.edit { putString(KEY_SURROUNDINGS_SOUND, value) }

    /**
     * How often each Surroundings recording has been started.
     *
     * **A short list has to be the right short list.** The player and the tab
     * both show a handful of what is on the phone, and picking that handful by
     * whatever order the manifest happened to be in is the same as picking it at
     * random. Somebody who reaches for rain every night should find rain first.
     *
     * A tally rather than a history: no timestamps, nothing that could
     * reconstruct when somebody was awake. It never leaves the phone, and it is
     * a few dozen bytes.
     */
    var surroundingsPlays: Map<String, Int>
        get() = prefs.getString(KEY_SURROUNDINGS_PLAYS, null).orEmpty()
            .split(';')
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split('=')
                if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
            }
            .toMap()
        set(value) = prefs.edit {
            putString(
                KEY_SURROUNDINGS_PLAYS,
                value.entries.joinToString(";") { "${it.key}=${it.value}" },
            )
        }

    /** One more play for this recording. */
    fun noteSurroundingsPlay(id: String) {
        val now = surroundingsPlays.toMutableMap()
        now[id] = (now[id] ?: 0) + 1
        surroundingsPlays = now
    }

    /** Its volume, 0 through 1. */
    var surroundingsVolume: Float
        get() = prefs.getFloat(KEY_SURROUNDINGS_VOLUME, 0.6f)
        set(value) = prefs.edit { putFloat(KEY_SURROUNDINGS_VOLUME, value.coerceIn(0f, 1f)) }

    fun observeChanges(): Flow<Unit> = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(Unit)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(Unit)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private companion object {
        const val FILE = "meedwell_settings"
        const val KEY_THEME = "theme"
        const val KEY_SHELF_GRID = "shelf_grid"
        const val KEY_GAPLESS = "gapless"
        const val KEY_LONG_RESUME = "long_resume"
        const val KEY_LAST_SYNC = "last_sync"
        const val KEY_QUEUE_INDEX = "queue_index"
        const val KEY_QUEUE_POSITION = "queue_position"
        const val KEY_CHOSEN_PATH = "chosen_path"
        const val KEY_RESUME_QUEUE = "resume_queue_on_opening"
        const val KEY_ASKED_NOTIFICATIONS = "asked_notifications"
        const val KEY_ASKED_DOWNLOAD_NOTIFICATIONS = "asked_download_notifications"
        const val KEY_ASKED_CELLULAR = "asked_cellular"
        const val KEY_DAWN = "dawn_minute"
        const val KEY_DUSK = "dusk_minute"
        const val KEY_WIFI_ONLY = "wifi_only_downloads"
        const val KEY_LAST_BACKUP = "last_backup_at"
        const val KEY_VOICING = "voicing"

        /**
         * The voicing a fresh install starts on, per grid screen 03.
         *
         * **Not `AsRecorded`.** The design ships with a voicing already applied
         * and declares it during onboarding, which is the whole reason screen 03
         * exists: a default that alters playback has to be stated, not
         * discovered. The opt-out sits on that same screen, so nobody meets this
         * without being told.
         */
        const val DEFAULT_VOICING = "Orchestral"
        const val KEY_SURROUNDINGS_SOUND = "surroundings_sound"
        const val KEY_SURROUNDINGS_VOLUME = "surroundings_volume"
        const val KEY_SURROUNDINGS_PLAYS = "surroundings_plays"
    }
}
