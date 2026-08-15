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

    private companion object {
        const val BUNDLED_MANIFEST = "surroundings/manifest.json"
        const val DOWNLOADED_MANIFEST = "surroundings-manifest.json"
    }
}
