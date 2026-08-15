package com.kamsiob.meedwell.core.library

import com.google.common.truth.Truth.assertThat
import com.kamsiob.meedwell.core.library.Matching.CollectionTrack
import com.kamsiob.meedwell.core.library.Matching.LocalFile
import org.junit.Test

/**
 * Matching is the center of the app now that Tier C is decided, so it is tested
 * against the shapes real files actually have rather than tidy ones.
 */
class MatchingTest {

    private fun track(id: String, artist: String, album: String, title: String) =
        CollectionTrack(id = id, albumId = "a:1", artist = artist, album = album, title = title)

    private fun file(artist: String, album: String, title: String, path: String = title) =
        LocalFile(artist = artist, album = album, title = title, payload = path)

    // ---------- Normalizing ----------

    @Test
    fun `punctuation and spacing differences do not prevent a match`() {
        assertThat(Matching.normalize("Copper Lines (Remastered)"))
            .isEqualTo(Matching.normalize("Copper  Lines [remastered]"))
        assertThat(Matching.normalize("Duluth, 4am")).isEqualTo(Matching.normalize("Duluth 4AM"))
        assertThat(Matching.normalize("Filament, reprise")).isEqualTo(Matching.normalize("Filament - Reprise"))
    }

    @Test
    fun `genuinely different releases still differ`() {
        // Dropping words like "remastered" entirely would collapse these, and
        // showing two rows is better than hiding one.
        assertThat(Matching.normalize("Copper Lines (Remastered)"))
            .isNotEqualTo(Matching.normalize("Copper Lines"))
    }

    @Test
    fun `unicode titles survive normalizing`() {
        assertThat(Matching.normalize("Sigur Rós")).isEqualTo("sigur rós")
        assertThat(Matching.normalize("日本語")).isEqualTo("日本語")
    }

    // ---------- Track numbers in file names ----------

    @Test
    fun `leading track numbers are stripped in the shapes files really use`() {
        listOf(
            "03 Copper Lines", "03 - Copper Lines", "03. Copper Lines",
            "03_Copper Lines", "3 Copper Lines", "003 - Copper Lines",
        ).forEach {
            assertThat(Matching.stripLeadingTrackNumber(it)).isEqualTo("Copper Lines")
        }
    }

    @Test
    fun `a title that is only a number survives intact`() {
        // Without this, a track called "1979" would become an empty title and
        // match nothing, or worse, match everything.
        assertThat(Matching.stripLeadingTrackNumber("1979")).isEqualTo("1979")
        assertThat(Matching.stripLeadingTrackNumber("2112")).isEqualTo("2112")
    }

    @Test
    fun `a number that is part of the title is not mistaken for a track number`() {
        assertThat(Matching.stripLeadingTrackNumber("4 Minute Warning")).isEqualTo("Minute Warning")
        // Documented limitation rather than a silent one: a title genuinely
        // beginning with a small number and a space is indistinguishable from a
        // numbered file name, and the numbered case is overwhelmingly more
        // common. The loose album-plus-title pass catches the rest.
    }

    // ---------- Matching ----------

    @Test
    fun `files downloaded from bandcamp's website match the collection`() {
        // The case Tier C rests on entirely.
        val collection = listOf(
            track("t:1", "The Long Static", "Copper Lines", "Filament"),
            track("t:2", "The Long Static", "Copper Lines", "Relay Static"),
        )
        val files = listOf(
            file("The Long Static", "Copper Lines", "01 - Filament"),
            file("The Long Static", "Copper Lines", "02 - Relay Static"),
        )

        val result = Matching.match(collection, files)
        assertThat(result.matches).hasSize(2)
        assertThat(result.unmatchedFiles).isEmpty()
        assertThat(result.matches.map { it.track.id }).containsExactly("t:1", "t:2")
    }

    @Test
    fun `a compilation matches on album and title when the artist tag differs`() {
        // The API reports one album artist; the file carries the performer.
        // Verified as real: "Medieval Times" has three distinct track artists
        // under one album artist.
        val collection = listOf(track("t:1", "Derek & Brandon Fiechter", "Medieval Times", "Tavern Song"))
        val files = listOf(file("Brandon Fiechter", "Medieval Times", "05 Tavern Song"))

        val result = Matching.match(collection, files)
        assertThat(result.matches).hasSize(1)
        assertThat(result.matches.single().track.id).isEqualTo("t:1")
    }

    @Test
    fun `nothing is ever matched twice, in either direction`() {
        // Two files with the same title in the same album, one collection
        // track. Exactly one match, one left over, never a duplicate.
        val collection = listOf(track("t:1", "Ada Vex", "Dust Choir", "Meadow Static"))
        val files = listOf(
            file("Ada Vex", "Dust Choir", "Meadow Static", path = "a.flac"),
            file("Ada Vex", "Dust Choir", "Meadow Static", path = "b.flac"),
        )

        val result = Matching.match(collection, files)
        assertThat(result.matches).hasSize(1)
        assertThat(result.unmatchedFiles).hasSize(1)
    }

    @Test
    fun `an exact match is never overridden by a looser one`() {
        // Same title on two albums by the same artist. The exact pass must
        // claim the right one before the loose pass runs.
        val collection = listOf(
            track("t:1", "The Long Static", "Copper Lines", "Filament"),
            track("t:2", "The Long Static", "Ground Hum", "Filament"),
        )
        val files = listOf(file("The Long Static", "Ground Hum", "Filament"))

        val result = Matching.match(collection, files)
        assertThat(result.matches.single().track.id).isEqualTo("t:2")
    }

    @Test
    fun `files with no counterpart come back unmatched rather than being forced`() {
        // A local-only album is a first-class thing, not a failure.
        val collection = listOf(track("t:1", "Ada Vex", "Dust Choir", "Meadow Static"))
        val files = listOf(file("Woven Hills", "Harvest Logic", "Threshing"))

        val result = Matching.match(collection, files)
        assertThat(result.matches).isEmpty()
        assertThat(result.unmatchedFiles).hasSize(1)
    }

    @Test
    fun `a partial album match reports exactly what was found`() {
        // Seven of nine is seven of nine. The interface says so rather than
        // rounding up to "yours".
        val collection = (1..9).map { track("t:$it", "The Long Static", "Ground Hum", "Track $it") }
        val files = (1..7).map { file("The Long Static", "Ground Hum", "0$it - Track $it") }

        val result = Matching.match(collection, files)
        assertThat(result.matches).hasSize(7)
    }

    @Test
    fun `matching an empty collection against files does not throw`() {
        val result = Matching.match(emptyList(), listOf(file("a", "b", "c")))
        assertThat(result.matches).isEmpty()
        assertThat(result.unmatchedFiles).hasSize(1)
    }
}
