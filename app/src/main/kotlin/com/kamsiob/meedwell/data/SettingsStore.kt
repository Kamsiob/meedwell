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
        const val KEY_WIFI_ONLY = "wifi_only_downloads"
        const val KEY_LAST_BACKUP = "last_backup_at"
        const val KEY_SURROUNDINGS_SOUND = "surroundings_sound"
        const val KEY_SURROUNDINGS_VOLUME = "surroundings_volume"
    }
}
