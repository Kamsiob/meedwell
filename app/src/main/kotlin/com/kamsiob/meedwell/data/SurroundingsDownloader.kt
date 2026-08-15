package com.kamsiob.meedwell.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.kamsiob.meedwell.core.surroundings.SurroundingsSound
import com.kamsiob.meedwell.core.surroundings.isOfferable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Fetches Surroundings recordings from the library's GitHub release.
 *
 * Four things this does that a naive download does not, each because of a
 * failure somebody would otherwise hit on a phone:
 *
 *  - **It resumes.** These are multi-megabyte files and some are twenty-five.
 *    A download that starts over every time the train goes into a tunnel never
 *    finishes on a bad connection. A `Range` request continues from whatever
 *    arrived, and a server that ignores the header is detected rather than
 *    silently appended to.
 *  - **It refuses to start when it should not.** Wi-Fi only means Wi-Fi only,
 *    checked at the moment of asking rather than remembered from earlier.
 *  - **It never installs what it fetched.** Verification and atomic placement
 *    belong to `SurroundingsStore`, which the transfer hands off to. Nothing
 *    here can put a file where playback will find it.
 *  - **It says what went wrong in words.** No status codes, no exception class
 *    names. Somebody whose download failed wants to know whether to try again.
 */
class SurroundingsDownloader(
    private val context: Context,
    private val store: SurroundingsStore,
    private val settings: SettingsStore,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        // Generous, because these are big files on a phone. The connect timeout
        // is what catches a dead network; a slow one is still working.
        .readTimeout(90, TimeUnit.SECONDS)
        .build(),
) {

    /** How a single download ended. */
    sealed interface Outcome {
        data object Done : Outcome
        /** Stopped on purpose. Not a failure, and never shown as one. */
        data object Cancelled : Outcome
        data class Failed(val message: String, val canRetry: Boolean = true) : Outcome
    }

    /**
     * Fetches one recording and installs it.
     *
     * @param onProgress bytes so far and bytes in total, called often enough
     *   for a progress bar and not so often that it floods the main thread.
     */
    suspend fun fetch(
        sound: SurroundingsSound,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ): Outcome = withContext(Dispatchers.IO) {

        // The hard rule, again, at the last possible moment. Even if a screen
        // somehow offered this, it does not get fetched.
        if (!sound.isOfferable) {
            return@withContext Outcome.Failed(
                "That recording is missing the credit it has to carry, so Meedwell will not fetch it.",
                canRetry = false,
            )
        }

        if (store.isPresent(sound)) return@withContext Outcome.Done

        when (val blocked = networkObjection()) {
            null -> Unit
            else -> return@withContext blocked
        }

        val partial = store.partialFor(sound)
        partial.parentFile?.mkdirs()
        var have = store.partialBytes(sound)

        val request = Request.Builder()
            .url(urlFor(sound))
            .apply { if (have > 0) header("Range", "bytes=$have-") }
            .build()

        try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 416 -> {
                        // The server says there is nothing past where we are,
                        // which means the partial is stale rather than short.
                        partial.delete()
                        return@withContext Outcome.Failed(
                            "The download was out of step with the library. Try again and it will start fresh."
                        )
                    }
                    response.code == 404 -> return@withContext Outcome.Failed(
                        "That recording is not in the library any more.",
                        canRetry = false,
                    )
                    !response.isSuccessful -> return@withContext Outcome.Failed(
                        "The library did not answer properly. It may be busy; try again shortly."
                    )
                }

                // A server that ignores Range answers 200 with the whole file.
                // Appending that to what we have would produce a file of the
                // right length made of the wrong bytes, which the checksum
                // would catch, but only after downloading it twice.
                if (have > 0 && response.code != 206) have = 0

                val body = response.body ?: return@withContext Outcome.Failed(
                    "The library answered with nothing in it. Try again shortly."
                )

                RandomAccessFile(partial, "rw").use { out ->
                    out.setLength(have)
                    out.seek(have)
                    val buffer = ByteArray(BUFFER_BYTES)
                    var written = have
                    var sinceReport = 0L
                    body.byteStream().use { input ->
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read <= 0) break
                            out.write(buffer, 0, read)
                            written += read
                            sinceReport += read
                            if (sinceReport >= REPORT_EVERY_BYTES) {
                                onProgress(written, sound.fileSizeBytes)
                                sinceReport = 0
                            }
                        }
                    }
                    onProgress(written, sound.fileSizeBytes)
                }
            }
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            // The partial survives on purpose, so stopping and starting again
            // continues rather than restarts.
            throw cancellation
        } catch (io: IOException) {
            return@withContext Outcome.Failed(
                "The connection dropped partway. What arrived is kept, so trying again picks up where it stopped."
            )
        }

        // Verification and placement, which live where they cannot be skipped.
        val problem = store.install(sound)
        return@withContext if (problem == null) Outcome.Done else Outcome.Failed(problem)
    }

    /**
     * Why a download must not start right now, or null if it may.
     *
     * Checked immediately before each transfer rather than once at the top of a
     * batch: a phone leaving Wi-Fi partway through a nine-pack must not carry
     * on spending mobile data because it was on Wi-Fi when the user tapped.
     */
    fun networkObjection(): Outcome.Failed? {
        val manager = context.getSystemService(ConnectivityManager::class.java)
            ?: return null
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
            ?: return Outcome.Failed("This phone is not on a network at the moment.")

        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return Outcome.Failed("This phone is not on a network at the moment.")
        }
        val unmetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        if (settings.wifiOnlyDownloads && !unmetered) {
            return Outcome.Failed(
                "Downloads are set to Wi-Fi only, and this is mobile data. " +
                    "You can change that in Settings, or wait for Wi-Fi.",
                canRetry = false,
            )
        }
        return null
    }

    /**
     * Where a recording is fetched from.
     *
     * Individual files are release assets, one per recording, named by their
     * filename. Release assets have no bandwidth limit, which is the whole
     * reason the audio is not in the repository.
     */
    private fun urlFor(sound: SurroundingsSound): String =
        "$RELEASE_BASE/${sound.filename}"

    companion object {
        const val RELEASE_BASE =
            "https://github.com/Kamsiob/meedwell-surroundings/releases/download/library-v1.0.0"

        /** The stable address of the newest manifest, whatever release it is in. */
        const val LATEST_MANIFEST =
            "https://github.com/Kamsiob/meedwell-surroundings/releases/latest/download/manifest.json"

        private const val BUFFER_BYTES = 64 * 1024
        private const val REPORT_EVERY_BYTES = 256 * 1024L
    }
}
