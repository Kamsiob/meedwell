package com.kamsiob.meedwell.core.subsonic

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The tolerant parsing suite.
 *
 * Fixtures are hand written to the shapes verification actually found on 15
 * August 2026, using the demo album `DESIGN.md` reserves for exactly this
 * ("Copper Lines" by The Long Static) rather than the owner's real collection.
 * A public repository has no business carrying somebody's music library, and
 * the shapes are what these tests are about in any case.
 *
 * Every case here is either something the server really did, or something the
 * tolerant contract promises to survive whether or not it has happened yet.
 */
class TolerantParsingTest {

    // ---------- The five failure shapes ----------

    @Test
    fun `empty body with a 5xx is a rejected login, not an unreadable response`() {
        val outcome = parseSubsonicBody("getArtists", 500, "")
        assertThat(outcome).isEqualTo(SubsonicOutcome.AuthRejected)
    }

    @Test
    fun `the bad version body means the route is absent, not a protocol mismatch`() {
        val outcome = parseSubsonicBody("download", 200, """{"error":true,"error_message":"bad version"}""")
        assertThat(outcome).isEqualTo(SubsonicOutcome.EndpointAbsent("download"))
    }

    @Test
    fun `an XML failure is read rather than crashing the JSON parser`() {
        // unstar returns exactly this, ignoring f=json, on every call.
        val xml = """
            <subsonic-response status="failed" version="1.16.1" type="BandcampServer" serverVersion="1.0" openSubsonic="true">
              <error code="0" message="unknown error" />
            </subsonic-response>
        """.trimIndent()
        val outcome = parseSubsonicBody("unstar", 200, xml)
        assertThat(outcome).isInstanceOf(SubsonicOutcome.XmlFailure::class.java)
        val failure = outcome as SubsonicOutcome.XmlFailure
        assertThat(failure.code).isEqualTo(0)
        assertThat(failure.message).isEqualTo("unknown error")
    }

    @Test
    fun `a real subsonic error keeps its code and message`() {
        val body = """{"subsonic-response":{"status":"failed","version":"1.16.1","error":{"code":70,"message":"not found"}}}"""
        val outcome = parseSubsonicBody("getAlbum", 200, body)
        assertThat(outcome).isEqualTo(SubsonicOutcome.ServerError(70, "not found"))
    }

    @Test
    fun `a healthy response comes back as success`() {
        val body = """{"subsonic-response":{"status":"ok","version":"1.16.1","type":"BandcampServer","openSubsonic":true}}"""
        val outcome = parseSubsonicBody("ping", 200, body)
        assertThat(outcome).isInstanceOf(SubsonicOutcome.Success::class.java)
        assertThat((outcome as SubsonicOutcome.Success).value.type).isEqualTo("BandcampServer")
    }

    // ---------- Numbers arriving in the wrong notation ----------

    @Test
    fun `durations parse whether they arrive as integer, float, or string`() {
        fun durationOf(raw: String): Long {
            val body = """{"subsonic-response":{"status":"ok","album":{"id":"a:1","name":"Copper Lines","song":[{"id":"t:1","duration":$raw}]}}}"""
            val outcome = parseSubsonicBody("getAlbum", 200, body) as SubsonicOutcome.Success
            return outcome.value.album!!.song.first().duration
        }
        // The reported beta quirk, which did not reproduce and is handled anyway.
        assertThat(durationOf("280")).isEqualTo(280L)
        assertThat(durationOf("280.0")).isEqualTo(280L)
        assertThat(durationOf("280.7")).isEqualTo(280L)
        assertThat(durationOf("\"280\"")).isEqualTo(280L)
        assertThat(durationOf("null")).isEqualTo(0L)
        assertThat(durationOf("\"not a number at all\"")).isEqualTo(0L)
    }

    @Test
    fun `every numeric field on a song is tolerant, not just duration`() {
        val body = """
            {"subsonic-response":{"status":"ok","album":{"id":"a:1","song":[{
              "id":"t:1","title":"Copper Lines",
              "track":"2","year":2023.0,"bitRate":"256.0","size":6518179.9,"duration":"280"
            }]}}}
        """.trimIndent()
        val song = (parseSubsonicBody("getAlbum", 200, body) as SubsonicOutcome.Success).value.album!!.song.first()
        assertThat(song.track).isEqualTo(2)
        assertThat(song.year).isEqualTo(2023)
        assertThat(song.bitRate).isEqualTo(256)
        assertThat(song.size).isEqualTo(6518179L)
        assertThat(song.duration).isEqualTo(280L)
    }

    // ---------- Absence, null, and unknown fields ----------

    @Test
    fun `a song missing almost every field still parses`() {
        val body = """{"subsonic-response":{"status":"ok","album":{"song":[{"id":"t:1"}]}}}"""
        val song = (parseSubsonicBody("getAlbum", 200, body) as SubsonicOutcome.Success).value.album!!.song.first()
        assertThat(song.id).isEqualTo("t:1")
        assertThat(song.title).isEmpty()
        assertThat(song.duration).isEqualTo(0L)
        assertThat(song.isDir).isFalse()
    }

    @Test
    fun `nulls where the schema promises values are absorbed`() {
        val body = """
            {"subsonic-response":{"status":"ok","album":{"id":"a:1","name":null,"artist":null,
             "coverArt":null,"songCount":null,"duration":null,"song":null}}}
        """.trimIndent()
        val album = (parseSubsonicBody("getAlbum", 200, body) as SubsonicOutcome.Success).value.album!!
        assertThat(album.name).isEmpty()
        assertThat(album.songCount).isEqualTo(0)
        assertThat(album.song).isEmpty()
    }

    @Test
    fun `unknown fields are ignored rather than fatal`() {
        // Bandcamp already sends artistImageUrl. It will send more.
        val body = """
            {"subsonic-response":{"status":"ok","album":{"id":"a:1","name":"Copper Lines",
             "somethingBandcampAddedLater":{"nested":[1,2,3]},"anotherNewField":"whatever"}}}
        """.trimIndent()
        val outcome = parseSubsonicBody("getAlbum", 200, body)
        assertThat(outcome).isInstanceOf(SubsonicOutcome.Success::class.java)
        assertThat((outcome as SubsonicOutcome.Success).value.album!!.name).isEqualTo("Copper Lines")
    }

    // ---------- The XML heritage leaking into JSON ----------

    @Test
    fun `a single object where an array was promised reads as a one item list`() {
        // An album with one track is where this bites, and it bites on real
        // collections rather than on contrived ones.
        val body = """{"subsonic-response":{"status":"ok","album":{"id":"a:1","song":{"id":"t:1","title":"Filament"}}}}"""
        val album = (parseSubsonicBody("getAlbum", 200, body) as SubsonicOutcome.Success).value.album!!
        assertThat(album.song).hasSize(1)
        assertThat(album.song.first().title).isEqualTo("Filament")
    }

    @Test
    fun `one unreadable item in a list does not lose the rest of the list`() {
        val body = """
            {"subsonic-response":{"status":"ok","album":{"id":"a:1","song":[
              {"id":"t:1","title":"Filament"},
              "this is a bare string where an object belongs",
              {"id":"t:3","title":"Relay Static"}
            ]}}}
        """.trimIndent()
        val album = (parseSubsonicBody("getAlbum", 200, body) as SubsonicOutcome.Success).value.album!!
        assertThat(album.song.map { it.title }).containsExactly("Filament", "Relay Static")
    }

    // ---------- Bugs found in Bandcamp's own data ----------

    @Test
    fun `the idDir typo in getStarred is read rather than lost`() {
        // getStarred really returns idDir where every other endpoint says isDir.
        val body = """
            {"subsonic-response":{"status":"ok","starred":{"artist":[],"album":[],
             "song":[{"id":"t:1","title":"Copper Lines","idDir":false,"duration":280}]}}}
        """.trimIndent()
        val starred = (parseSubsonicBody("getStarred", 200, body) as SubsonicOutcome.Success).value.starred!!
        assertThat(starred.song).hasSize(1)
        assertThat(starred.song.first().title).isEqualTo("Copper Lines")
    }

    @Test
    fun `duplicate genres survive parsing and are the mapper's problem, not the parser's`() {
        val body = """
            {"subsonic-response":{"status":"ok","album":{"id":"a:1","genre":"drone",
             "genres":[{"name":"drone"},{"name":"ambient"},{"name":"drone"}]}}}
        """.trimIndent()
        val album = (parseSubsonicBody("getAlbum", 200, body) as SubsonicOutcome.Success).value.album!!
        assertThat(album.genres.map { it.name }).containsExactly("drone", "ambient", "drone")
        assertThat(album.genres.map { it.name }.distinct()).containsExactly("drone", "ambient")
    }

    // ---------- Hostile input ----------

    @Test
    fun `truncated json is unreadable rather than a crash`() {
        val outcome = parseSubsonicBody("getAlbum", 200, """{"subsonic-response":{"status":"ok","album":{"id":"a:1",""")
        assertThat(outcome).isInstanceOf(SubsonicOutcome.Unreadable::class.java)
    }

    @Test
    fun `an HTML error page is unreadable rather than mistaken for XML`() {
        // A proxy or captive portal answering instead of Bandcamp. It starts
        // with "<", so it must not be mistaken for a subsonic XML failure with
        // a meaningful code.
        val outcome = parseSubsonicBody("getArtists", 200, "<html><body>Gateway Timeout</body></html>")
        assertThat(outcome).isInstanceOf(SubsonicOutcome.XmlFailure::class.java)
        assertThat((outcome as SubsonicOutcome.XmlFailure).message).isEqualTo("unknown error")
    }

    @Test
    fun `very long and unicode text passes through intact`() {
        val long = "y".repeat(4000)
        val body = """{"subsonic-response":{"status":"ok","album":{"id":"a:1","name":"$long","artist":"Sigur Rós 日本語 🎧"}}}"""
        val album = (parseSubsonicBody("getAlbum", 200, body) as SubsonicOutcome.Success).value.album!!
        assertThat(album.name).hasLength(4000)
        assertThat(album.artist).isEqualTo("Sigur Rós 日本語 🎧")
    }
}
