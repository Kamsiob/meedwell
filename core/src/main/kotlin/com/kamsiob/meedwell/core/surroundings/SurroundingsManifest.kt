package com.kamsiob.meedwell.core.surroundings

import com.kamsiob.meedwell.core.subsonic.TolerantBoolean
import com.kamsiob.meedwell.core.subsonic.TolerantInt
import com.kamsiob.meedwell.core.subsonic.TolerantLong
import com.kamsiob.meedwell.core.subsonic.TolerantString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The Surroundings library manifest.
 *
 * Fetched from
 * `https://github.com/Kamsiob/meedwell-surroundings/releases/latest/download/manifest.json`,
 * which points at the newest release rather than a fixed tag, so a library
 * refresh needs no app update.
 *
 * **The field names here are British on purpose.** `licence_name`,
 * `licence_version`, `licence_url`, `licence_short` and `licence_tier` are the
 * keys the owner's own audio pipeline emits, and that manifest is already
 * published and hash verified. The project writes American English everywhere
 * else; renaming these would fork a published data contract to win a spelling
 * argument, so they stay as they are. See `CLAUDE.md`.
 */
@Serializable
data class SurroundingsManifest(
    @SerialName("schema_version") @Serializable(with = TolerantInt::class) val schemaVersion: Int = 0,
    @SerialName("library") @Serializable(with = TolerantString::class) val library: String = "",
    @SerialName("bed_target_db") @Serializable(with = TolerantString::class) val bedTargetDb: String = "",
    @SerialName("sound_count") @Serializable(with = TolerantInt::class) val soundCount: Int = 0,
    @SerialName("packs") val packs: List<SurroundingsPack> = emptyList(),
    @SerialName("sounds") val sounds: List<SurroundingsSound> = emptyList(),
    /**
     * The handful of recordings that ship inside the app.
     *
     * Enough to open Surroundings on a phone with no network and hear
     * something: a fire, rain on leaves, and a rainforest at night. They are
     * deliberately absent from the pack archives, so getting a pack never
     * re-fetches what is already installed.
     */
    @SerialName("bundled_with_app") val bundled: List<SurroundingsBundled> = emptyList(),
)

/** One of the recordings shipped inside the app. */
@Serializable
data class SurroundingsBundled(
    @SerialName("id") @Serializable(with = TolerantString::class) val id: String = "",
    @SerialName("filename") @Serializable(with = TolerantString::class) val filename: String = "",
    @SerialName("sha256") @Serializable(with = TolerantString::class) val sha256: String = "",
)

@Serializable
data class SurroundingsPack(
    @SerialName("pack") @Serializable(with = TolerantString::class) val id: String = "",
    @SerialName("archive") @Serializable(with = TolerantString::class) val archive: String = "",
    @SerialName("sound_count") @Serializable(with = TolerantInt::class) val soundCount: Int = 0,
    @SerialName("archive_bytes") @Serializable(with = TolerantLong::class) val archiveBytes: Long = 0,
    @SerialName("duration_seconds") @Serializable(with = TolerantLong::class) val durationSeconds: Long = 0,
    @SerialName("sha256") @Serializable(with = TolerantString::class) val sha256: String = "",
)

@Serializable
data class SurroundingsSound(
    @SerialName("id") @Serializable(with = TolerantString::class) val id: String = "",
    @SerialName("filename") @Serializable(with = TolerantString::class) val filename: String = "",
    @SerialName("path") @Serializable(with = TolerantString::class) val path: String = "",
    @SerialName("display_name") @Serializable(with = TolerantString::class) val displayName: String = "",
    @SerialName("description") @Serializable(with = TolerantString::class) val description: String = "",
    @SerialName("group") @Serializable(with = TolerantString::class) val group: String = "",
    @SerialName("category_name") @Serializable(with = TolerantString::class) val categoryName: String = "",
    @SerialName("pack") @Serializable(with = TolerantString::class) val pack: String = "",
    @SerialName("freesound_sound_id") @Serializable(with = TolerantString::class) val freesoundId: String = "",
    @SerialName("duration_seconds") @Serializable(with = TolerantLong::class) val durationSeconds: Long = 0,
    @SerialName("file_size_bytes") @Serializable(with = TolerantLong::class) val fileSizeBytes: Long = 0,
    @SerialName("sha256") @Serializable(with = TolerantString::class) val sha256: String = "",
    @SerialName("attribution") val attribution: SurroundingsAttribution = SurroundingsAttribution(),
    @SerialName("loop") val loop: SurroundingsLoop = SurroundingsLoop(),
    @SerialName("loudness") val loudness: SurroundingsLoudness = SurroundingsLoudness(),
)

/**
 * Loop data, which the player needs rather than merely likes.
 *
 * A file sits at essentially full level 50 ms from each edge, so a hard cut from
 * end to start joins two unrelated points in a waveform and clicks. The head and
 * tail levels exist so the incoming loop can be ramped to arrive at the level
 * the outgoing one left; 14 files differ by more than 3 dB and would otherwise
 * drop audibly once per loop.
 */
@Serializable
data class SurroundingsLoop(
    @SerialName("head_level_db") @Serializable(with = TolerantString::class) val headLevelDb: String = "",
    @SerialName("tail_level_db") @Serializable(with = TolerantString::class) val tailLevelDb: String = "",
    @SerialName("head_tail_mismatch_db") @Serializable(with = TolerantString::class) val mismatchDb: String = "",
    @SerialName("within_3db") @Serializable(with = TolerantBoolean::class) val within3Db: Boolean = true,
)

@Serializable
data class SurroundingsLoudness(
    @SerialName("makeup_gain_needed_db") @Serializable(with = TolerantString::class) val makeupGainDb: String = "",
    @SerialName("needs_high_makeup") @Serializable(with = TolerantBoolean::class) val needsHighMakeup: Boolean = false,
    /**
     * The file's measured true peak, in dBTP, on the encoded audio.
     *
     * This is what lets the limiter stay out of the signal path: whether it has
     * anything to do at a given gain is arithmetic against this figure rather
     * than a guess, and on most files at their intended level the answer is no.
     */
    @SerialName("true_peak_dbtp") @Serializable(with = TolerantString::class) val truePeakDbtp: String = "",
    /** The recording's own average level, for reference. */
    @SerialName("bed_level_db") @Serializable(with = TolerantString::class) val bedLevelDb: String = "",
)

/**
 * Who made a recording, under what license, and the fact that it was changed.
 *
 * For 21 of the 111 recordings this is a **license condition** rather than a
 * courtesy: CC BY requires the credit, the license, and a statement that the
 * work was modified. Every file here was trimmed, level matched and re-encoded,
 * so the modification statement applies to all of them.
 *
 * The remaining 90 are CC0 and need no credit at all. They are credited anyway,
 * because a library built out of other people's careful work should say whose
 * work it is.
 */
@Serializable
data class SurroundingsAttribution(
    @SerialName("recordist_name") @Serializable(with = TolerantString::class) val recordistName: String = "",
    @SerialName("recordist_profile_url") @Serializable(with = TolerantString::class) val recordistProfileUrl: String = "",
    @SerialName("sound_page_url") @Serializable(with = TolerantString::class) val soundPageUrl: String = "",
    // British keys, deliberately. See the class doc on SurroundingsManifest.
    @SerialName("licence_name") @Serializable(with = TolerantString::class) val licenseName: String = "",
    @SerialName("licence_short") @Serializable(with = TolerantString::class) val licenseShort: String = "",
    @SerialName("licence_version") @Serializable(with = TolerantString::class) val licenseVersion: String = "",
    @SerialName("licence_url") @Serializable(with = TolerantString::class) val licenseUrl: String = "",
    @SerialName("licence_tier") @Serializable(with = TolerantInt::class) val licenseTier: Int = 0,
    /**
     * Conditions the uploader asked for beyond the license itself. Nine
     * recordings carry one. Reproduced word for word wherever it is shown,
     * because paraphrasing somebody's credit request is not crediting them.
     */
    @SerialName("extra_conditions") @Serializable(with = TolerantString::class) val extraConditions: String = "",
    @SerialName("modified") @Serializable(with = TolerantBoolean::class) val modified: Boolean = false,
    @SerialName("modification_note") @Serializable(with = TolerantString::class) val modificationNote: String = "",
) {
    /**
     * Whether this recording may be offered at all.
     *
     * **The hard rule.** A manifest entry missing any attribution field is
     * treated as invalid and its recording is never offered for download,
     * however good the audio is. Credit is the condition of shipping the file,
     * not a display detail that can degrade gracefully.
     */
    val isComplete: Boolean
        get() = recordistName.isNotBlank() &&
            soundPageUrl.isNotBlank() &&
            licenseName.isNotBlank() &&
            licenseVersion.isNotBlank() &&
            licenseUrl.isNotBlank() &&
            modified &&
            modificationNote.isNotBlank()

    /** True where the license itself compels the credit, rather than good manners. */
    val creditIsRequired: Boolean
        get() = !licenseShort.equals("CC0", ignoreCase = true) &&
            !licenseName.contains("CC0", ignoreCase = true) &&
            !licenseName.contains("Public Domain", ignoreCase = true)

    /**
     * "CC BY 4.0", "CC0 1.0", or the full name when there is no short form.
     *
     * The short form in the manifest is inconsistent, and it has to be handled
     * rather than trusted: `CC BY 4.0` already carries its version while `CC0`
     * does not. Appending the version blindly produced "CC BY 4.0 4.0" on the
     * credits screen, which is the kind of thing that reads as carelessness on
     * exactly the surface that must not look careless.
     *
     * The version is never dropped, only never doubled: CC BY 3.0 and CC BY 4.0
     * are different licenses with different terms.
     */
    /**
     * The license written out, with its version stated exactly once.
     *
     * Every license name in the published library already carries its own
     * version: "CC0 1.0 Universal", "Creative Commons Attribution 4.0
     * International". Appending `licence_version` to those gave "CC0 1.0
     * Universal 1.0" on the credit sheet, which is the same doubling that once
     * produced "CC BY 4.0 4.0" on the credits screen. The version is never
     * dropped, because CC BY 3.0 and CC BY 4.0 are different licenses with
     * different terms; it is only never said twice.
     */
    val licenseFullName: String
        get() = when {
            licenseName.isBlank() -> licenseLabel
            licenseVersion.isBlank() -> licenseName
            licenseName.contains(licenseVersion) -> licenseName
            else -> "$licenseName $licenseVersion"
        }

    val licenseLabel: String
        get() = when {
            licenseShort.isBlank() ->
                listOf(licenseName, licenseVersion).filter { it.isNotBlank() }.joinToString(" ")
            licenseVersion.isBlank() || licenseShort.endsWith(licenseVersion) -> licenseShort
            else -> "$licenseShort $licenseVersion"
        }
}
