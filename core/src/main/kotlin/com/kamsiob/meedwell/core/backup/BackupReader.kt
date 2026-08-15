package com.kamsiob.meedwell.core.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reading an export file, carefully.
 *
 * Restore is the half of data portability that gets shipped untested, and a
 * restore that half-works is worse than one that refuses: the user believes
 * their history is back and only notices the gaps months later. So this refuses
 * clearly rather than doing its best.
 *
 * **It never silently drops what it does not understand.** A file from a later
 * version of the app may hold sections this one has never heard of. Those
 * sections are named in the result, so the app can say "your history and hearts
 * came back; there were two things in this file this version does not know
 * about" rather than pretending a clean import.
 */
object BackupReader {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
    }

    /** What reading a file produced. */
    sealed interface Result {
        data class Ok(
            val file: BackupFile,
            /**
             * Top-level sections this version does not recognize, in the order
             * they appeared. Reported rather than dropped.
             */
            val unknownSections: List<String>,
        ) : Result

        /** A plain sentence for the interface. Never a parser message. */
        data class Unreadable(val message: String) : Result
    }

    fun write(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    /**
     * Reads an export file.
     *
     * The version is checked before the contents, because a file from the
     * future must be refused rather than half read: fields this version happens
     * to recognize would come back and everything else would vanish, which is
     * the exact silent partial restore this whole design exists to prevent.
     */
    fun read(text: String): Result {
        if (text.isBlank()) return Result.Unreadable("That file is empty.")

        val tree = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
            ?: return Result.Unreadable(
                "That does not look like a Meedwell export. It may be a different kind of file."
            )

        val version = runCatching {
            tree["format_version"]?.jsonPrimitive?.content?.toIntOrNull()
        }.getOrNull()

        if (version == null) {
            return Result.Unreadable(
                "That does not look like a Meedwell export. It has no format version in it."
            )
        }
        if (version > BackupFile.FORMAT_VERSION) {
            return Result.Unreadable(
                "That export was written by a newer version of Meedwell. Update the app and try " +
                    "again. Nothing has been changed."
            )
        }

        val parsed = runCatching { json.decodeFromString(BackupFile.serializer(), text) }.getOrNull()
            ?: return Result.Unreadable(
                "That export could not be read all the way through, so nothing was restored."
            )

        return Result.Ok(parsed, unknownSections = tree.keys.filter { it !in KNOWN_SECTIONS })
    }

    /**
     * The top-level keys this version writes and understands.
     *
     * Anything else in a file is named back to the user rather than ignored.
     */
    private val KNOWN_SECTIONS = setOf(
        "format_version", "written_by", "written_at", "note",
        "plays", "loved_tracks", "loved_albums", "resume_points",
        "lists", "local_files", "watched_folders", "settings",
    )
}
