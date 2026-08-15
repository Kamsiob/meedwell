package com.kamsiob.meedwell.core.surroundings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The gate on what may be offered, and the sentence shown before anything is
 * fetched. Both are promises rather than conveniences, so both are tested.
 */
class DownloadsTest {

    private fun complete() = SurroundingsAttribution(
        recordistName = "someone",
        soundPageUrl = "https://freesound.org/s/1/",
        licenseName = "Creative Commons Attribution",
        licenseShort = "CC BY",
        licenseVersion = "4.0",
        licenseUrl = "https://creativecommons.org/licenses/by/4.0/",
        modified = true,
        modificationNote = "Trimmed, level matched and re-encoded.",
    )

    private fun sound(
        id: String,
        pack: String = "p1",
        bytes: Long = 1_048_576,
        seconds: Long = 600,
        sha: String = "abc",
        filename: String = "$id.opus",
        attribution: SurroundingsAttribution = complete(),
    ) = SurroundingsSound(
        id = id,
        filename = filename,
        pack = pack,
        fileSizeBytes = bytes,
        durationSeconds = seconds,
        sha256 = sha,
        attribution = attribution,
    )

    // ---------- The hard rule ----------

    @Test
    fun `a complete entry is offerable`() {
        assertThat(sound("1").isOfferable).isTrue()
    }

    /**
     * Every field, one at a time. Credit is the condition of shipping the file,
     * so any gap in it removes the file, not the credit.
     */
    @Test
    fun `any missing attribution field removes the recording`() {
        val gaps = listOf(
            complete().copy(recordistName = ""),
            complete().copy(soundPageUrl = ""),
            complete().copy(licenseName = ""),
            complete().copy(licenseVersion = ""),
            complete().copy(licenseUrl = ""),
            complete().copy(modified = false),
            complete().copy(modificationNote = ""),
        )
        gaps.forEach { assertThat(sound("1", attribution = it).isOfferable).isFalse() }
    }

    /** A file that cannot be verified cannot be installed. */
    @Test
    fun `no checksum means no download`() {
        assertThat(sound("1", sha = "").isOfferable).isFalse()
    }

    /** A download whose cost cannot be stated cannot be consented to. */
    @Test
    fun `no size means no download`() {
        assertThat(sound("1", bytes = 0).isOfferable).isFalse()
    }

    @Test
    fun `no filename means no download`() {
        assertThat(sound("1", filename = "").isOfferable).isFalse()
    }

    @Test
    fun `the offerable list drops exactly the invalid entries`() {
        val manifest = SurroundingsManifest(
            sounds = listOf(
                sound("1"),
                sound("2", attribution = complete().copy(recordistName = "")),
                sound("3"),
            )
        )
        assertThat(Downloads.offerable(manifest).map { it.id }).containsExactly("1", "3").inOrder()
    }

    // ---------- Packs ----------

    /**
     * A pack is one archive, so one invalid recording poisons the whole thing:
     * taking it would install a file the app is not allowed to ship.
     */
    @Test
    fun `a pack containing an invalid recording is not whole`() {
        val manifest = SurroundingsManifest(
            sounds = listOf(
                sound("1", pack = "p1"),
                sound("2", pack = "p1", sha = ""),
            )
        )
        assertThat(Downloads.packIsWhole(manifest, "p1")).isFalse()
    }

    @Test
    fun `a pack of valid recordings is whole`() {
        val manifest = SurroundingsManifest(
            sounds = listOf(sound("1", pack = "p1"), sound("2", pack = "p1"))
        )
        assertThat(Downloads.packIsWhole(manifest, "p1")).isTrue()
    }

    @Test
    fun `an empty pack is not whole`() {
        assertThat(Downloads.packIsWhole(SurroundingsManifest(), "p1")).isFalse()
    }

    // ---------- What it costs ----------

    /** The cost is what is missing, never the catalog total. */
    @Test
    fun `recordings already here are not charged for again`() {
        val sounds = listOf(sound("1"), sound("2"), sound("3"))
        assertThat(Downloads.bytesToFetch(sounds, alreadyHave = setOf("1", "2")))
            .isEqualTo(1_048_576)
    }

    @Test
    fun `invalid recordings are never counted into a cost`() {
        val sounds = listOf(sound("1"), sound("2", sha = ""))
        assertThat(Downloads.bytesToFetch(sounds, emptySet())).isEqualTo(1_048_576)
    }

    @Test
    fun `sizes read the way a download manager writes them`() {
        assertThat(Downloads.humanSize(0)).isEqualTo("nothing to fetch")
        assertThat(Downloads.humanSize(1_048_576)).isEqualTo("1.0 MB")
        assertThat(Downloads.humanSize(3_901_300)).isEqualTo("3.7 MB")
        assertThat(Downloads.humanSize(144_045_092)).isEqualTo("137.4 MB")
    }

    /** Never "1024.0 MB". */
    @Test
    fun `a gigabyte is a gigabyte`() {
        assertThat(Downloads.humanSize(1_131_413_504L)).isEqualTo("1.05 GB")
    }

    @Test
    fun `durations are hours and minutes`() {
        assertThat(Downloads.humanDuration(0)).isEqualTo("no time at all")
        assertThat(Downloads.humanDuration(30)).isEqualTo("under a minute")
        assertThat(Downloads.humanDuration(600)).isEqualTo("10 min")
        assertThat(Downloads.humanDuration(3600)).isEqualTo("1 hour")
        assertThat(Downloads.humanDuration(3660)).isEqualTo("1 hour 1 min")
        assertThat(Downloads.humanDuration(18_390)).isEqualTo("5 hours 6 min")
    }

    /** Size first, because that is the thing being agreed to. */
    @Test
    fun `the cost line leads with the size`() {
        val sounds = listOf(sound("1", bytes = 3_901_300, seconds = 489))
        assertThat(Downloads.costLine(sounds, emptySet()))
            .isEqualTo("3.7 MB · 1 recording · 8 min of sound")
    }

    @Test
    fun `the cost line says so when there is nothing to do`() {
        val sounds = listOf(sound("1"))
        assertThat(Downloads.costLine(sounds, alreadyHave = setOf("1")))
            .isEqualTo("You already have all of these.")
    }
}

/**
 * License names, which are the one place a small formatting slip is a legal
 * surface rather than a cosmetic one.
 */
class LicenseNameTest {

    private fun attribution(name: String, short: String, version: String) =
        SurroundingsAttribution(licenseName = name, licenseShort = short, licenseVersion = version)

    /**
     * The three shapes the published library actually contains. Every one of
     * them already carries its version inside its name, which is exactly why
     * appending it produced "CC0 1.0 Universal 1.0" on the credit sheet.
     */
    @Test
    fun `the version is stated exactly once`() {
        assertThat(attribution("CC0 1.0 Universal", "CC0", "1.0").licenseFullName)
            .isEqualTo("CC0 1.0 Universal")
        assertThat(attribution("Creative Commons Attribution 4.0 International", "CC BY 4.0", "4.0").licenseFullName)
            .isEqualTo("Creative Commons Attribution 4.0 International")
        assertThat(attribution("Creative Commons Attribution 3.0 Unported", "CC BY 3.0", "3.0").licenseFullName)
            .isEqualTo("Creative Commons Attribution 3.0 Unported")
    }

    /** A name that genuinely lacks its version still gets one. */
    @Test
    fun `a version missing from the name is added`() {
        assertThat(attribution("Creative Commons Attribution", "CC BY", "4.0").licenseFullName)
            .isEqualTo("Creative Commons Attribution 4.0")
    }

    /** Three and four are different licenses, so the version is never dropped. */
    @Test
    fun `the version is never dropped`() {
        val three = attribution("Creative Commons Attribution", "CC BY", "3.0").licenseFullName
        val four = attribution("Creative Commons Attribution", "CC BY", "4.0").licenseFullName
        assertThat(three).isNotEqualTo(four)
        assertThat(three).contains("3.0")
        assertThat(four).contains("4.0")
    }

    @Test
    fun `the short label does not double either`() {
        assertThat(attribution("CC0 1.0 Universal", "CC0", "1.0").licenseLabel).isEqualTo("CC0 1.0")
        assertThat(attribution("Creative Commons Attribution 4.0 International", "CC BY 4.0", "4.0").licenseLabel)
            .isEqualTo("CC BY 4.0")
    }
}
