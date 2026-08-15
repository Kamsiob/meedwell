package com.kamsiob.meedwell.core.subsonic

import com.google.common.truth.Truth.assertThat
import com.kamsiob.meedwell.core.model.Origin
import com.kamsiob.meedwell.core.model.Provenance
import com.kamsiob.meedwell.core.model.Track
import org.junit.Test

class SubsonicMapperTest {

    // ---------- Dates ----------

    @Test
    fun `bandcamp's date format parses to the right instant`() {
        // The format the API really uses, verified 15 August 2026.
        val parsed = parseSubsonicDate("07 Aug 2026 16:24:01 GMT")
        assertThat(parsed).isNotNull()
        // 2026-08-07T16:24:01Z
        assertThat(parsed).isEqualTo(1786119841L)
    }

    @Test
    fun `the epoch and a leap day both land correctly`() {
        assertThat(parseSubsonicDate("01 Jan 1970 00:00:00 GMT")).isEqualTo(0L)
        assertThat(parseSubsonicDate("29 Feb 2024 12:00:00 GMT")).isEqualTo(1709208000L)
        assertThat(parseSubsonicDate("12 Dec 2012 00:00:00 GMT")).isEqualTo(1355270400L)
    }

    @Test
    fun `an unreadable date is null rather than a confidently wrong one`() {
        // "On your shelf since" simply does not appear, which beats showing the
        // wrong month with total confidence.
        listOf("", "   ", "not a date", "32 Xyz 2026 00:00:00 GMT", "07 Aug", "07 Zzz 2026 00:00:00 GMT")
            .forEach { assertThat(parseSubsonicDate(it)).isNull() }
    }

    // ---------- Genres, including Bandcamp's duplicates ----------

    @Test
    fun `duplicate genres collapse, keeping the first spelling seen`() {
        val merged = mergeGenres(
            "soundtrack",
            listOf("soundtrack", "Celtic", "no ai", "celtic", "soundtrack").map { SubsonicGenreName(it) },
        )
        assertThat(merged).containsExactly("soundtrack", "Celtic", "no ai").inOrder()
    }

    @Test
    fun `blank genres are dropped rather than becoming an empty tag`() {
        assertThat(mergeGenres("", listOf(SubsonicGenreName(""), SubsonicGenreName("  ")))).isEmpty()
    }

    // ---------- Albums ----------

    @Test
    fun `an album maps with the fields the shelf needs`() {
        val album = SubsonicAlbum(
            id = "a:3375168501",
            name = "Copper Lines",
            artist = "The Long Static",
            artistId = "b:1582411",
            coverArt = "ca:116605828",
            songCount = 10,
            duration = 2820,
            created = "07 Aug 2026 16:24:01 GMT",
            genre = "drone",
            genres = listOf(SubsonicGenreName("drone"), SubsonicGenreName("ambient")),
        ).toDomain()

        assertThat(album.name).isEqualTo("Copper Lines")
        assertThat(album.trackCount).isEqualTo(10)
        assertThat(album.addedAt).isEqualTo(1786119841L)
        assertThat(album.genres).containsExactly("drone", "ambient").inOrder()
        assertThat(album.origin).isEqualTo(Origin.Bandcamp)
    }

    @Test
    fun `an album falls back to title when name is absent`() {
        // search3 sends both; other Subsonic servers send only one.
        val album = SubsonicAlbum(id = "a:1", name = "", title = "Ground Hum").toDomain()
        assertThat(album.name).isEqualTo("Ground Hum")
    }

    // ---------- Provenance, which is the Tier C mechanic ----------

    @Test
    fun `a fully present album reads as yours`() {
        val album = SubsonicAlbum(id = "a:1", songCount = 10).toDomain().copy(localTrackCount = 10)
        assertThat(album.isFullyPresent).isTrue()
        assertThat(album.provenance).isEqualTo(Provenance.Yours)
    }

    @Test
    fun `a partly present album is honest about being partial, never rounded up`() {
        // This is the difference between the app and a marketing claim. Eight
        // of ten tracks is eight of ten, not "yours".
        val album = SubsonicAlbum(id = "a:1", songCount = 10).toDomain().copy(localTrackCount = 8)
        assertThat(album.isFullyPresent).isFalse()
        assertThat(album.provenance).isEqualTo(Provenance.PartlyHere(8, 10))
    }

    @Test
    fun `an album with no local files reads as streaming`() {
        val album = SubsonicAlbum(id = "a:1", songCount = 10).toDomain()
        assertThat(album.provenance).isEqualTo(Provenance.Streaming)
    }

    @Test
    fun `a local-only album says it is on this phone, not that it is streaming`() {
        val album = SubsonicAlbum(id = "local:1", songCount = 0).toDomain(Origin.Local)
        assertThat(album.provenance).isEqualTo(Provenance.OnThisPhone)
    }

    @Test
    fun `an album with no track count cannot claim to be fully present`() {
        // Zero of zero is not "yours". Without this, an album whose track list
        // failed to load would display the ownership marker on no evidence.
        val album = SubsonicAlbum(id = "a:1", songCount = 0).toDomain().copy(localTrackCount = 0)
        assertThat(album.isFullyPresent).isFalse()
    }

    // ---------- Tracks ----------

    @Test
    fun `a track maps, and a missing disc number becomes disc one`() {
        val track = SubsonicSong(
            id = "t:1", albumId = "a:1", title = "Copper Lines", artist = "The Long Static",
            track = 2, duration = 280, suffix = "mp3", bitRate = 256, size = 6518179,
        ).toDomain()

        assertThat(track.trackNumber).isEqualTo(2)
        // Bandcamp sends no discNumber at all, so everything streamed is disc 1.
        assertThat(track.discNumber).isEqualTo(1)
        assertThat(track.isPresentLocally).isFalse()
    }

    @Test
    fun `albumId falls back to parent when only the older field is sent`() {
        val track = SubsonicSong(id = "t:1", albumId = "", parent = "a:9").toDomain()
        assertThat(track.albumId).isEqualTo("a:9")
    }

    @Test
    fun `long-form detection is on the twenty minute boundary`() {
        // Drives "Resume from 22:40" and the Settings toggle.
        fun of(seconds: Long) = SubsonicSong(id = "t:1", duration = seconds).toDomain()
        assertThat(of(Track.LONG_FORM_SECONDS - 1).isLongForm).isFalse()
        assertThat(of(Track.LONG_FORM_SECONDS).isLongForm).isTrue()
        assertThat(of(24 * 60 + 12).isLongForm).isTrue()
    }

    // ---------- Playlists ----------

    @Test
    fun `a playlist from the account is shown and not editable`() {
        // The API implements no way to create, edit or delete one, so anything
        // arriving from it must not offer an edit control.
        val playlist = SubsonicPlaylist(id = "p:1", name = "Late shift").toDomain()
        assertThat(playlist.fromBandcamp).isTrue()
        assertThat(playlist.isEditable).isFalse()
    }
}
