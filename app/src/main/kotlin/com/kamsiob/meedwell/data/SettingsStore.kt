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

    /** Dark is the default, per `DESIGN.md` section 2. */
    var theme: ThemeChoice
        get() = runCatching { ThemeChoice.valueOf(prefs.getString(KEY_THEME, null) ?: "") }
            .getOrDefault(ThemeChoice.Dark)
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
    }
}
