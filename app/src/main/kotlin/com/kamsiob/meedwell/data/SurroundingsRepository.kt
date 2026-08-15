package com.kamsiob.meedwell.data

import android.content.Context
import com.kamsiob.meedwell.core.subsonic.SubsonicJson
import com.kamsiob.meedwell.core.surroundings.Credits
import com.kamsiob.meedwell.core.surroundings.LicenseGroup
import com.kamsiob.meedwell.core.surroundings.SurroundingsManifest
import com.kamsiob.meedwell.core.surroundings.SurroundingsSound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The Surroundings library, as the app sees it.
 *
 * The manifest ships **inside the app** as an asset, so the credits screen and
 * the three bundled recordings work from the moment it installs, with no network
 * call and no empty state. A newer manifest fetched from the release replaces it
 * in place later; the bundled copy is the floor, not the ceiling.
 *
 * That matters legally as well as practically. Twenty-one recordings are CC BY,
 * where the credit is a condition of use, and a credits screen that needs the
 * network to render is a credits screen that is sometimes blank.
 */
class SurroundingsRepository(private val context: Context) {

    private var cached: SurroundingsManifest? = null

    /**
     * The manifest, from the downloaded copy if there is one, otherwise the
     * bundled asset.
     */
    suspend fun manifest(): SurroundingsManifest = withContext(Dispatchers.IO) {
        cached ?: load().also { cached = it }
    }

    /**
     * Why the manifest could not be read, when it could not be.
     *
     * Kept rather than swallowed. An earlier version returned an empty manifest
     * on any failure, and the credits screen rendered with the software notices
     * and no recordings at all: a licensing surface silently showing nothing,
     * which is the one failure mode this screen must not have. A blank credits
     * screen has to be loud.
     */
    @Volatile
    var loadError: String? = null
        private set

    private fun load(): SurroundingsManifest {
        val downloaded = context.filesDir.resolve(DOWNLOADED_MANIFEST)
        val source: String
        val text: String? = when {
            downloaded.isFile -> {
                source = "the downloaded manifest"
                runCatching { downloaded.readText() }
                    .onFailure { loadError = "Could not read $source: ${it.message}" }
                    .getOrNull()
            }
            else -> {
                source = "the bundled manifest"
                runCatching {
                    context.assets.open(BUNDLED_MANIFEST).bufferedReader().use { it.readText() }
                }
                    .onFailure { loadError = "Could not open $source: ${it.message}" }
                    .getOrNull()
            }
        }

        if (text.isNullOrBlank()) {
            if (loadError == null) loadError = "The manifest was empty."
            return SurroundingsManifest()
        }

        return runCatching { SubsonicJson.decodeFromString<SurroundingsManifest>(text) }
            .onFailure { loadError = "Could not read the credits: ${it::class.simpleName}: ${it.message}" }
            .onSuccess { loadError = null }
            .getOrDefault(SurroundingsManifest())
    }

    /**
     * Only recordings whose attribution is complete.
     *
     * The hard rule, enforced here rather than at the download call site so
     * there is one place it can be got wrong. An entry missing any attribution
     * field is never offered, however good the audio is.
     */
    suspend fun offerable(): List<SurroundingsSound> = Credits.offerable(manifest().sounds)

    /** Anything the library holds that cannot be offered, and what it is missing. */
    suspend fun rejected() = Credits.rejected(manifest().sounds)

    suspend fun creditsByLicense(): List<LicenseGroup> = Credits.byLicense(manifest().sounds)

    suspend fun creditsSummary(): String = Credits.summary(manifest().sounds)

    /**
     * Fetches the newest published manifest, if the user asks for it.
     *
     * **Never automatic.** A library refresh can add recordings, and adding
     * things to somebody's app without being asked is how an app starts making
     * decisions on their behalf. Nothing here runs on a timer, on launch, or in
     * the background: it happens when a person taps a control that says what it
     * will do.
     *
     * The new manifest is only kept if it parses **and** describes at least as
     * much as the one already in use. A truncated or half-written answer that
     * happened to be valid JSON would otherwise silently shrink the library and
     * orphan files already on the phone.
     *
     * Returns a plain sentence describing what happened, for the interface.
     */
    suspend fun refreshManifest(client: okhttp3.OkHttpClient): String = withContext(Dispatchers.IO) {
        val before = manifest().sounds.size
        val request = okhttp3.Request.Builder().url(LATEST_MANIFEST_URL).build()

        val fetched: String? = try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (io: java.io.IOException) {
            return@withContext "Could not reach the library. That is usually the connection on this phone."
        }

        if (fetched.isNullOrBlank()) {
            return@withContext "The library did not answer properly. It may be busy; try again shortly."
        }

        val parsed = runCatching { SubsonicJson.decodeFromString<SurroundingsManifest>(fetched) }.getOrNull()
            ?: return@withContext "The library sent something Meedwell could not read."

        if (parsed.sounds.size < before) {
            return@withContext "The library answered with fewer recordings than you already have, " +
                "so nothing was changed."
        }

        val saved = runCatching {
            context.filesDir.resolve(DOWNLOADED_MANIFEST).writeText(fetched)
        }.isSuccess
        if (!saved) return@withContext "The new library list could not be saved."

        cached = parsed
        loadError = null

        when (val added = parsed.sounds.size - before) {
            0 -> "Nothing new. The library is the same as the one you have."
            1 -> "One new recording is available."
            else -> "$added new recordings are available."
        }
    }

    private companion object {
        const val BUNDLED_MANIFEST = "surroundings/manifest.json"
        const val DOWNLOADED_MANIFEST = "surroundings-manifest.json"
        const val LATEST_MANIFEST_URL =
            "https://github.com/Kamsiob/meedwell-surroundings/releases/latest/download/manifest.json"
    }
}
