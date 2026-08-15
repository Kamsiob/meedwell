package com.kamsiob.meedwell.core.surroundings

/**
 * Turns the manifest into the credit text the app shows.
 *
 * Everything the interface displays about a recording's provenance is generated
 * here, from `manifest.json`, and never hand maintained. That is the point: a
 * credit typed into a string resource drifts the moment the library changes, and
 * a drifted credit on a CC BY file is a license breach rather than a stale
 * label.
 *
 * Pure Kotlin, so the rules are testable without a device.
 */
object Credits {

    /**
     * The one line under a playing recording.
     *
     * Deliberately quiet: recordist, license, and nothing else. It is a line of
     * tertiary ink under an ambience player, not a banner. Everything the
     * license actually compels lives one tap behind it in [full], which is what
     * makes the subtle version legitimate rather than a shortcut.
     */
    fun oneLine(sound: SurroundingsSound): String {
        val a = sound.attribution
        if (!a.isComplete) return ""
        return "${a.recordistName} · ${a.licenseLabel}"
    }

    /**
     * The complete block, shown on the recording's detail sheet.
     *
     * Carries every element CC BY requires: who made it, where it came from,
     * which license and **which version** of that license, and an explicit
     * statement that the recording was changed. Any extra condition the
     * uploader asked for is reproduced word for word rather than summarized.
     */
    fun full(sound: SurroundingsSound): CreditBlock? {
        val a = sound.attribution
        if (!a.isComplete) return null
        return CreditBlock(
            title = sound.displayName.ifBlank { sound.filename },
            recordist = a.recordistName,
            recordistUrl = a.recordistProfileUrl.takeIf { it.isNotBlank() },
            sourceUrl = a.soundPageUrl,
            licenseLabel = a.licenseLabel,
            licenseFullName = listOf(a.licenseName, a.licenseVersion)
                .filter { it.isNotBlank() }
                .joinToString(" "),
            licenseUrl = a.licenseUrl,
            modificationNote = a.modificationNote,
            extraConditions = a.extraConditions.takeIf { it.isNotBlank() },
            creditIsRequired = a.creditIsRequired,
        )
    }

    /**
     * The whole library, grouped by license, for the credits screen.
     *
     * Grouped rather than listed flat because 111 entries in one column is a
     * wall, and because the grouping itself tells the reader something true:
     * most of this library is public domain, and the part that is not is small
     * enough to read.
     *
     * Ordered so the licenses that impose conditions come first. A reader
     * checking whether the app honors its obligations should not have to scroll
     * past ninety CC0 entries to find the twenty-one that matter.
     */
    fun byLicense(sounds: List<SurroundingsSound>): List<LicenseGroup> =
        sounds
            .filter { it.attribution.isComplete }
            .groupBy { it.attribution.licenseLabel }
            .map { (label, group) ->
                LicenseGroup(
                    licenseLabel = label,
                    licenseUrl = group.first().attribution.licenseUrl,
                    creditIsRequired = group.first().attribution.creditIsRequired,
                    entries = group
                        .map { sound ->
                            LicenseEntry(
                                id = sound.id.ifBlank { sound.filename },
                                title = sound.displayName.ifBlank { sound.filename },
                                recordist = sound.attribution.recordistName,
                                sourceUrl = sound.attribution.soundPageUrl,
                                extraConditions = sound.attribution.extraConditions
                                    .takeIf { it.isNotBlank() },
                            )
                        }
                        .sortedBy { it.recordist.lowercase() },
                )
            }
            .sortedWith(
                compareByDescending<LicenseGroup> { it.creditIsRequired }
                    .thenBy { it.licenseLabel }
            )

    /**
     * Recordings that cannot be offered, and why.
     *
     * Returned rather than silently dropped so the reason is visible in a log
     * and in a test. An entry disappearing without explanation is how a
     * licensing bug hides.
     */
    fun rejected(sounds: List<SurroundingsSound>): List<Rejection> =
        sounds.filterNot { it.attribution.isComplete }.map { sound ->
            val a = sound.attribution
            val missing = buildList {
                if (a.recordistName.isBlank()) add("recordist name")
                if (a.soundPageUrl.isBlank()) add("source link")
                if (a.licenseName.isBlank()) add("license name")
                if (a.licenseVersion.isBlank()) add("license version")
                if (a.licenseUrl.isBlank()) add("license link")
                if (!a.modified || a.modificationNote.isBlank()) add("modification statement")
            }
            Rejection(sound.filename.ifBlank { sound.id }, missing)
        }

    /** Only recordings whose attribution is complete may be offered at all. */
    fun offerable(sounds: List<SurroundingsSound>): List<SurroundingsSound> =
        sounds.filter { it.attribution.isComplete }

    /**
     * The line at the top of the credits screen.
     *
     * Says what the page is and nothing else. An earlier version counted how
     * many recordings were licensed on condition of credit, which read like the
     * app explaining licensing law to somebody who came to find out who made
     * the rain. The obligations are met by the page itself, not by narrating
     * them.
     */
    fun summary(sounds: List<SurroundingsSound>): String {
        val offerable = offerable(sounds)
        val people = offerable.map { it.attribution.recordistName }.distinct().size
        return when {
            offerable.isEmpty() -> "No recordings yet."
            people <= 1 -> "${offerable.size} recordings, and the person who made them."
            else -> "${offerable.size} recordings, from $people people who put them out for anyone to use."
        }
    }
}

data class CreditBlock(
    val title: String,
    val recordist: String,
    val recordistUrl: String?,
    val sourceUrl: String,
    val licenseLabel: String,
    val licenseFullName: String,
    val licenseUrl: String,
    val modificationNote: String,
    val extraConditions: String?,
    val creditIsRequired: Boolean,
)

data class LicenseEntry(
    val id: String,
    val title: String,
    val recordist: String,
    val sourceUrl: String,
    val extraConditions: String?,
)

data class LicenseGroup(
    val licenseLabel: String,
    val licenseUrl: String,
    val creditIsRequired: Boolean,
    val entries: List<LicenseEntry>,
)

data class Rejection(val filename: String, val missingFields: List<String>)
