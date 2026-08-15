package com.kamsiob.meedwell.data

import android.content.Context
import com.kamsiob.meedwell.core.surroundings.SurroundingsSound
import com.kamsiob.meedwell.core.surroundings.isOfferable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Where Surroundings recordings live on the phone, and the only thing that puts
 * one there.
 *
 * **App-private storage, deliberately.** These files are not the user's music
 * and do not belong in their music library: putting them under
 * `filesDir` keeps them out of every other app's media scanner, out of
 * gallery-style listings, and means uninstalling Meedwell takes a gigabyte of
 * ambience with it rather than leaving it behind.
 *
 * **Nothing is installed unverified.** A partial file is written to a temporary
 * name, its SHA-256 is checked against the manifest, and only then is it moved
 * into place. The move is a rename inside one directory, which is atomic on
 * every filesystem Android uses, so a recording is either fully present and
 * correct or absent. There is no state in between for playback to trip over.
 *
 * The three bundled recordings live in assets rather than here, and are copied
 * across on first use. They are checked on the way too: a corrupted asset is
 * nearly impossible and would be silent, which is the worst combination.
 */
class SurroundingsStore(private val context: Context) {

    private val audioDir: File
        get() = File(context.filesDir, "surroundings").apply { mkdirs() }

    private val partialDir: File
        get() = File(context.filesDir, "surroundings/.partial").apply { mkdirs() }

    /** Where a recording lives once it is installed. */
    fun fileFor(sound: SurroundingsSound): File = File(audioDir, sound.filename)

    /** Where a recording is written while it is still arriving. */
    fun partialFor(sound: SurroundingsSound): File = File(partialDir, sound.filename + ".part")

    /**
     * Whether a recording is here and playable.
     *
     * Presence is a file of the right size, not merely a file. A truncated
     * download that somehow survived the atomic install would otherwise look
     * present and play as a fraction of itself.
     */
    fun isPresent(sound: SurroundingsSound): Boolean {
        val file = fileFor(sound)
        return file.isFile && file.length() == sound.fileSizeBytes
    }

    /** The ids of everything currently here, for costing a download. */
    fun presentIds(sounds: List<SurroundingsSound>): Set<String> =
        sounds.filter { isPresent(it) }.map { it.id }.toSet()

    /** How many bytes Surroundings is using on this phone. */
    fun bytesUsed(): Long =
        (audioDir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L) +
            (partialDir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L)

    /**
     * How much of a recording has already arrived, for resuming.
     *
     * Zero when there is nothing, and zero when what is there is longer than
     * the file should be, which can only mean the partial belongs to a
     * different version of the recording and must not be resumed onto.
     */
    fun partialBytes(sound: SurroundingsSound): Long {
        val partial = partialFor(sound)
        if (!partial.isFile) return 0
        val length = partial.length()
        return if (length in 1 until sound.fileSizeBytes) length else 0
    }

    /**
     * Verifies a finished partial and moves it into place.
     *
     * Returns null on success, or a plain sentence saying what was wrong. The
     * partial is deleted on a checksum failure: keeping it would mean resuming
     * onto known-bad bytes forever, which is the one way a download can fail
     * permanently and silently.
     */
    suspend fun install(sound: SurroundingsSound): String? = withContext(Dispatchers.IO) {
        val partial = partialFor(sound)
        if (!partial.isFile) return@withContext "The download did not leave a file behind."

        if (partial.length() != sound.fileSizeBytes) {
            partial.delete()
            return@withContext "The recording arrived the wrong size, so it was thrown away."
        }

        val actual = runCatching { sha256(partial.inputStream()) }
            .getOrElse { return@withContext "The recording could not be read after downloading." }

        if (!actual.equals(sound.sha256, ignoreCase = true)) {
            partial.delete()
            return@withContext "The recording did not match its checksum, so it was thrown away. " +
                "Nothing was installed."
        }

        val target = fileFor(sound)
        target.delete()
        // A rename inside one directory. Either it is there whole or not there.
        if (!partial.renameTo(target)) {
            return@withContext "The recording downloaded correctly but could not be put in place."
        }
        null
    }

    /**
     * Copies a bundled recording out of assets, checking it on the way.
     *
     * Returns null on success or a sentence on failure, the same as `install`,
     * so the three that ship with the app follow exactly the same path into
     * storage as the hundred and eight that do not.
     */
    suspend fun installBundled(sound: SurroundingsSound): String? = withContext(Dispatchers.IO) {
        if (!sound.isOfferable) return@withContext "That recording is missing its credit and was not installed."
        val partial = partialFor(sound)
        runCatching {
            context.assets.open("surroundings/audio/${sound.filename}").use { input ->
                partial.outputStream().use { output -> input.copyTo(output) }
            }
        }.getOrElse {
            partial.delete()
            return@withContext "The recording that ships with the app could not be unpacked."
        }
        install(sound)
    }

    /** Removes one recording. */
    fun remove(sound: SurroundingsSound) {
        fileFor(sound).delete()
        partialFor(sound).delete()
    }

    /** Removes everything, including anything half-arrived. */
    fun removeAll() {
        audioDir.listFiles()?.forEach { if (it.isFile) it.delete() }
        partialDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Deletes partials that no longer belong to anything.
     *
     * Run at startup. A recording removed from the library between releases
     * would otherwise leave bytes on the phone that nothing can ever finish or
     * name.
     */
    fun sweepOrphans(sounds: List<SurroundingsSound>) {
        val known = sounds.map { it.filename }.toSet()
        partialDir.listFiles()?.forEach { file ->
            if (file.name.removeSuffix(".part") !in known) file.delete()
        }
        audioDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name !in known) file.delete()
        }
    }

    private fun sha256(stream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        stream.use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
