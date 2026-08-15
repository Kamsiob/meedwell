package com.kamsiob.meedwell

import android.content.Context
import com.kamsiob.meedwell.core.subsonic.SubsonicClient
import com.kamsiob.meedwell.data.CredentialStore
import com.kamsiob.meedwell.data.LibraryRepository
import com.kamsiob.meedwell.data.OkHttpSubsonicEngine
import com.kamsiob.meedwell.data.SurroundingsDownloader
import com.kamsiob.meedwell.data.SurroundingsRepository
import com.kamsiob.meedwell.data.SurroundingsStore
import com.kamsiob.meedwell.data.SettingsStore
import com.kamsiob.meedwell.data.db.MeedwellDatabase

/**
 * Where the app's few long-lived objects are made.
 *
 * Deliberately a plain container rather than a dependency injection framework.
 * This app has one database, one HTTP engine, one repository and one settings
 * store, and a framework would add a compile step, a set of annotations and a
 * failure mode to a problem that fits on one screen. The person maintaining
 * this does not write code, so the code that exists should be the kind another
 * agent can read end to end without learning a framework first.
 */
class AppContainer(context: Context) {

    /** Exposed so the player controller can bind to the service. */
    val appContext: Context = context.applicationContext

    val database: MeedwellDatabase by lazy { MeedwellDatabase.get(appContext) }

    val credentials: CredentialStore by lazy { CredentialStore(appContext) }

    val settings: SettingsStore by lazy { SettingsStore(appContext) }

    val library: LibraryRepository by lazy { LibraryRepository(database) }

    val surroundings: SurroundingsRepository by lazy { SurroundingsRepository(appContext) }

    /**
     * One HTTP client for the whole app.
     *
     * Shared so connections and thread pools are pooled rather than duplicated
     * per feature, and so there is exactly one place to look when asking what
     * this app can talk to.
     */
    val httpClient: okhttp3.OkHttpClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    /** Where Surroundings recordings live, and the only thing that puts one there. */
    val surroundingsStore: SurroundingsStore by lazy { SurroundingsStore(appContext) }

    val surroundingsDownloader: SurroundingsDownloader by lazy {
        SurroundingsDownloader(appContext, surroundingsStore, settings, httpClient)
    }

    private val engine by lazy { OkHttpSubsonicEngine() }

    /**
     * A client for the saved credentials, or null when no account is connected.
     *
     * Null is the normal, supported state rather than an error: local files only
     * is a second product, not a fallback, and every caller has to handle a
     * missing client without showing sync language.
     */
    fun client(): SubsonicClient? {
        val creds = credentials.read() ?: return null
        return SubsonicClient(engine = engine, credentials = creds)
    }

    /** A client for credentials being tested, before anything is saved. */
    fun clientFor(credentials: com.kamsiob.meedwell.core.subsonic.SubsonicCredentials): SubsonicClient =
        SubsonicClient(engine = engine, credentials = credentials)
}
