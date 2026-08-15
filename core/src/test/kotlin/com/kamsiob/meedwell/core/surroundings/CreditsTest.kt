package com.kamsiob.meedwell.core.surroundings

import com.google.common.truth.Truth.assertThat
import com.kamsiob.meedwell.core.subsonic.SubsonicJson
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Attribution is a license condition on 21 of the 111 recordings, so these are
 * compliance tests rather than formatting tests. A failure here is not an ugly
 * screen, it is shipping somebody's work without the credit they asked for.
 */
class CreditsTest {

    private fun sound(
        name: String = "rain_city_street_01.opus",
        recordist: String = "Garuda1982",
        licenseName: String = "Creative Commons Attribution 4.0 International",
        licenseShort: String = "CC BY",
        version: String = "4.0",
        licenseUrl: String = "https://creativecommons.org/licenses/by/4.0/",
        sourcePage: String = "https://freesound.org/people/Garuda1982/sounds/852917/",
        modified: Boolean = true,
        note: String = "Trimmed, level matched and re-encoded to Opus.",
        extra: String = "",
    ) = SurroundingsSound(
        id = name,
        filename = name,
        displayName = "Rain on a city street",
        attribution = SurroundingsAttribution(
            recordistName = recordist,
            soundPageUrl = sourcePage,
            licenseName = licenseName,
            licenseShort = licenseShort,
            licenseVersion = version,
            licenseUrl = licenseUrl,
            modified = modified,
            modificationNote = note,
            extraConditions = extra,
        ),
    )

    // ---------- The hard rule ----------

    @Test
    fun `a recording missing any attribution field is never offerable`() {
        // Credit is the condition of shipping the file, not a display detail.
        val cases = listOf(
            "recordist" to sound(recordist = ""),
            "source page" to sound(sourcePage = ""),
            "license name" to sound(licenseName = ""),
            "license version" to sound(version = ""),
            "license url" to sound(licenseUrl = ""),
            "modification note" to sound(note = ""),
            "modified flag" to sound(modified = false),
        )
        cases.forEach { (what, s) ->
            assertThat(s.attribution.isComplete).isFalse()
            assertThat(Credits.offerable(listOf(s))).isEmpty()
            assertThat(Credits.full(s)).isNull()
            assertThat(Credits.oneLine(s)).isEmpty()
        }
        assertThat(cases).hasSize(7)
    }

    @Test
    fun `a rejected recording says which fields were missing`() {
        // Silently dropping an entry is how a licensing bug hides.
        val rejections = Credits.rejected(listOf(sound(recordist = "", licenseUrl = "")))
        assertThat(rejections).hasSize(1)
        assertThat(rejections.single().missingFields).containsExactly("recordist name", "license link")
    }

    @Test
    fun `a complete recording is offerable and produces both credit forms`() {
        val s = sound()
        assertThat(s.attribution.isComplete).isTrue()
        assertThat(Credits.oneLine(s)).isEqualTo("Garuda1982 · CC BY 4.0")
        val block = Credits.full(s)!!
        assertThat(block.recordist).isEqualTo("Garuda1982")
        // Once, not twice. The name already carries its version, and appending
        // licence_version to it produced "... International 4.0" on the credit
        // sheet, the same doubling that once gave "CC BY 4.0 4.0" on the
        // credits screen. See LicenseNameTest.
        assertThat(block.licenseFullName).isEqualTo("Creative Commons Attribution 4.0 International")
        assertThat(block.modificationNote).isNotEmpty()
        assertThat(block.creditIsRequired).isTrue()
    }

    // ---------- Which licenses actually compel a credit ----------

    @Test
    fun `CC0 does not require a credit, and is credited anyway`() {
        val cc0 = sound(
            licenseName = "CC0 1.0 Universal",
            licenseShort = "CC0",
            version = "1.0",
            licenseUrl = "https://creativecommons.org/publicdomain/zero/1.0/",
        )
        assertThat(cc0.attribution.creditIsRequired).isFalse()
        // Still shown. A library built from other people's careful work should
        // say whose work it is.
        assertThat(Credits.oneLine(cc0)).isEqualTo("Garuda1982 · CC0 1.0")
    }

    @Test
    fun `the license version is never dropped, because the versions differ`() {
        // CC BY 3.0 and CC BY 4.0 are different licenses with different terms.
        // A credit reading only "CC BY" would be wrong for one of them.
        val three = sound(licenseShort = "CC BY", version = "3.0")
        val four = sound(licenseShort = "CC BY", version = "4.0")
        assertThat(Credits.oneLine(three)).contains("3.0")
        assertThat(Credits.oneLine(four)).contains("4.0")
        assertThat(Credits.oneLine(three)).isNotEqualTo(Credits.oneLine(four))
    }

    // ---------- Extra conditions ----------

    @Test
    fun `an extra condition is carried word for word, never summarized`() {
        val exact = "Credit \"kevp888 or Kevin Luce, and www.freesound.org\""
        val block = Credits.full(sound(recordist = "kevp888", extra = exact))!!
        assertThat(block.extraConditions).isEqualTo(exact)
    }

    @Test
    fun `an extra condition survives into the grouped credits screen`() {
        val exact = "Recorded by Martin Scaiff, credit him by name in addition to Yarmonics"
        val groups = Credits.byLicense(listOf(sound(recordist = "Yarmonics", extra = exact)))
        assertThat(groups.single().entries.single().extraConditions).isEqualTo(exact)
    }

    @Test
    fun `the version is never doubled when the short form already carries it`() {
        // The manifest is inconsistent here and it has to be handled rather
        // than trusted: "CC BY 4.0" already carries its version, "CC0" does
        // not. Appending blindly printed "CC BY 4.0 4.0" on the credits screen.
        val ccby = sound(licenseShort = "CC BY 4.0", version = "4.0")
        assertThat(ccby.attribution.licenseLabel).isEqualTo("CC BY 4.0")

        val cc0 = sound(
            licenseName = "CC0 1.0 Universal", licenseShort = "CC0", version = "1.0",
            licenseUrl = "https://creativecommons.org/publicdomain/zero/1.0/",
        )
        assertThat(cc0.attribution.licenseLabel).isEqualTo("CC0 1.0")

        // And with no short form at all, the full name plus version.
        val bare = sound(licenseShort = "", licenseName = "Some License", version = "2.0")
        assertThat(bare.attribution.licenseLabel).isEqualTo("Some License 2.0")
    }

    @Test
    fun `every label in the published library reads cleanly`() {
        assumeTrue("no published manifest on this machine", realManifest != null)
        val manifest = SubsonicJson.decodeFromString<SurroundingsManifest>(realManifest!!.readText())
        val labels = manifest.sounds.map { it.attribution.licenseLabel }.distinct()
        // No label may repeat its own version, and each must name one.
        labels.forEach { label ->
            val version = label.substringAfterLast(' ')
            assertThat(label.split(' ').count { it == version }).isEqualTo(1)
        }
        assertThat(labels).containsExactly("CC0 1.0", "CC BY 4.0", "CC BY 3.0")
    }

    // ---------- Grouping ----------

    @Test
    fun `licenses that impose conditions are listed before the ones that do not`() {
        // A reader checking whether the app honors its obligations should not
        // have to scroll past ninety public domain entries to find them.
        val sounds = listOf(
            sound(name = "a", licenseName = "CC0 1.0 Universal", licenseShort = "CC0", version = "1.0"),
            sound(name = "b", licenseShort = "CC BY", version = "4.0"),
            sound(name = "c", licenseShort = "CC BY", version = "3.0"),
        )
        val groups = Credits.byLicense(sounds)
        assertThat(groups.first().creditIsRequired).isTrue()
        assertThat(groups.last().licenseLabel).isEqualTo("CC0 1.0")
    }

    @Test
    fun `incomplete entries never reach the credits screen`() {
        val groups = Credits.byLicense(listOf(sound(name = "ok"), sound(name = "bad", recordist = "")))
        assertThat(groups.sumOf { it.entries.size }).isEqualTo(1)
    }

    // ---------- Against the real published manifest ----------

    /**
     * Runs against the manifest actually published to the Surroundings release,
     * when it is present on the machine. Skipped elsewhere, including CI, since
     * the hand written cases above are the real coverage.
     *
     * To run it:
     *   MEEDWELL_SURROUNDINGS_MANIFEST=~/Kamiob\ Apps/meedwell-surroundings/manifest.json ./gradlew :core:test
     */
    private val realManifest: File? =
        System.getenv("MEEDWELL_SURROUNDINGS_MANIFEST")?.let { File(it) }?.takeIf { it.isFile }

    @Test
    fun `every recording in the published library has complete attribution`() {
        assumeTrue("no published manifest on this machine", realManifest != null)
        val manifest = SubsonicJson.decodeFromString<SurroundingsManifest>(realManifest!!.readText())

        assertThat(manifest.sounds).hasSize(manifest.soundCount)
        assertThat(manifest.sounds).isNotEmpty()

        val rejected = Credits.rejected(manifest.sounds)
        assertThat(rejected.map { "${it.filename}: ${it.missingFields}" }).isEmpty()

        // Every recording carries a license with a version, because CC BY 3.0
        // and CC BY 4.0 are not the same license.
        manifest.sounds.forEach { s ->
            assertThat(s.attribution.licenseVersion).isNotEmpty()
            assertThat(s.attribution.modified).isTrue()
        }
    }

    @Test
    fun `the published library carries no em dash in any credit text`() {
        assumeTrue("no published manifest on this machine", realManifest != null)
        val manifest = SubsonicJson.decodeFromString<SurroundingsManifest>(realManifest!!.readText())
        val offenders = manifest.sounds.filter { s ->
            listOf(
                s.attribution.extraConditions,
                s.attribution.modificationNote,
                s.attribution.recordistName,
                s.attribution.licenseName,
            ).any { it.contains('—') }
        }
        assertThat(offenders.map { it.filename }).isEmpty()
    }
}
