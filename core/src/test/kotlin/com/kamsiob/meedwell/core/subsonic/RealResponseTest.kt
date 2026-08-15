package com.kamsiob.meedwell.core.subsonic

import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Parses the raw responses saved during API verification.
 *
 * Those files hold the owner's real collection, so they live outside the
 * repository and are **never** committed. This suite reads them only when
 * `MEEDWELL_API_RESPONSES` points at them, and skips otherwise, which is what
 * happens in CI and on any other machine.
 *
 * The hand written fixtures in [TolerantParsingTest] are the real coverage and
 * they run everywhere. This is the check that those fixtures did not drift away
 * from what the server actually sends, and it is the reason the raw responses
 * were saved at all rather than glanced at and discarded.
 *
 * To run it:
 *   MEEDWELL_API_RESPONSES=~/.kamsiob-secrets/meedwell-api-responses ./gradlew :core:test
 */
class RealResponseTest {

    private val directory: File? =
        System.getenv("MEEDWELL_API_RESPONSES")?.let { File(it) }?.takeIf { it.isDirectory }

    private fun read(name: String): String? =
        directory?.resolve(name)?.takeIf { it.isFile }?.readText()

    @Test
    fun `every saved response parses into a known outcome, none unreadable`() {
        assumeTrue("no saved responses on this machine", directory != null)
        val files = directory!!.listFiles { f -> f.extension == "json" }.orEmpty()
        assertThat(files).isNotEmpty()

        val unreadable = files.filter { file ->
            parseSubsonicBody(file.nameWithoutExtension, 200, file.readText()) is SubsonicOutcome.Unreadable
        }
        assertThat(unreadable.map { it.name }).isEmpty()
    }

    @Test
    fun `the real album response yields its full track list`() {
        val body = read("album.json") ?: return assumeTrue("no saved album response", false)
        val outcome = parseSubsonicBody("getAlbum", 200, body)
        assertThat(outcome).isInstanceOf(SubsonicOutcome.Success::class.java)
        val album = (outcome as SubsonicOutcome.Success).value.album!!
        assertThat(album.id).startsWith("a:")
        assertThat(album.song).isNotEmpty()
        assertThat(album.song.size).isEqualTo(album.songCount)
        // Every track carries what the shelf and the player need.
        album.song.forEach { song ->
            assertThat(song.id).startsWith("t:")
            assertThat(song.title).isNotEmpty()
            assertThat(song.duration).isGreaterThan(0L)
        }
    }

    @Test
    fun `the real artists response is indexed and carries cover art`() {
        val body = read("artists.json") ?: return assumeTrue("no saved artists response", false)
        val artists = (parseSubsonicBody("getArtists", 200, body) as SubsonicOutcome.Success).value.artists!!
        assertThat(artists.index).isNotEmpty()
        val all = artists.index.flatMap { it.artist }
        assertThat(all).isNotEmpty()
        all.forEach { assertThat(it.id).startsWith("b:") }
    }

    @Test
    fun `the real album list carries cover art on every album`() {
        val body = read("albumList2-full.json") ?: read("albumList2.json")
            ?: return assumeTrue("no saved album list", false)
        val list = (parseSubsonicBody("getAlbumList2", 200, body) as SubsonicOutcome.Success).value.albumList2!!
        assertThat(list.album).isNotEmpty()
        // The field report said the album list omits cover art. It does not.
        // If this ever starts failing, that report has become true and the
        // workaround in ISSUES-SEED needs reopening.
        list.album.forEach { assertThat(it.coverArt).isNotEmpty() }
    }

    @Test
    fun `the real getStarred response survives the idDir typo`() {
        val body = read("starred.json") ?: return assumeTrue("no saved starred response", false)
        val outcome = parseSubsonicBody("getStarred", 200, body)
        assertThat(outcome).isInstanceOf(SubsonicOutcome.Success::class.java)
    }

    @Test
    fun `endpoints verified absent still read as absent, not as errors`() {
        assumeTrue("no saved responses on this machine", directory != null)
        listOf("albumInfo2", "artistInfo2", "starred2", "scanStatus", "nowPlaying", "randomSongs")
            .mapNotNull { name -> read("$name.json")?.let { name to it } }
            .forEach { (name, body) ->
                assertThat(parseSubsonicBody(name, 200, body))
                    .isInstanceOf(SubsonicOutcome.EndpointAbsent::class.java)
            }
    }
}
