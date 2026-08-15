package com.kamsiob.meedwell.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.kamsiob.meedwell.core.subsonic.SubsonicCredentials

/**
 * The only place Bandcamp credentials ever live.
 *
 * The rule, from `MASTER_SPEC.md` section 4 and `DECISIONS.md`, is absolute:
 * credentials never enter the database, never enter an export, never enter a
 * log, and never enter a crash report. The database is deliberately plain
 * portable SQLite precisely so it can be handed around; that only stays safe
 * because nothing sensitive is in it.
 *
 * `SubsonicCredentials` overrides `toString` to redact, so even an accidental
 * log of the whole object cannot leak the password.
 */
class CredentialStore(context: Context) {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        FILE_NAME,
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    val isConnected: Boolean
        get() = !prefs.getString(KEY_USERNAME, null).isNullOrBlank()

    fun read(): SubsonicCredentials? {
        val server = prefs.getString(KEY_SERVER, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val password = prefs.getString(KEY_PASSWORD, null) ?: return null
        if (username.isBlank() || password.isBlank()) return null
        return SubsonicCredentials(server, username, password)
    }

    fun save(credentials: SubsonicCredentials) {
        prefs.edit()
            .putString(KEY_SERVER, credentials.serverUrl)
            .putString(KEY_USERNAME, credentials.username)
            .putString(KEY_PASSWORD, credentials.password)
            .apply()
    }

    /**
     * Disconnecting removes the credentials and nothing else.
     *
     * The shelf, the listening history and the lists stay, because they are the
     * user's and losing them is not what "disconnect" means to anyone. Deleting
     * one's own data is a separate, explicit act.
     */
    fun clear() {
        prefs.edit()
            .remove(KEY_SERVER)
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .apply()
    }

    companion object {
        private const val FILE_NAME = "meedwell_credentials"
        private const val KEY_SERVER = "server"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"

        /** Prefilled on the Connect screen, exactly as Bandcamp presents it. */
        const val DEFAULT_SERVER = "https://bandcamp.com/api/subsonic"
    }
}
