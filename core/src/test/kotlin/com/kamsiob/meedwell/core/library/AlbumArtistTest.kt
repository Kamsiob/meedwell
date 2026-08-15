package com.kamsiob.meedwell.core.library

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The album artist is recovered from the tracks, so the cases that matter are
 * the messy ones: compilations, splits, and albums whose tracks arrived only
 * partly filled in.
 */
class AlbumArtistTest {

    @Test
    fun `one artist throughout is that artist`() {
        assertThat(resolveAlbumArtistId(listOf("b:1", "b:1", "b:1"))).isEqualTo("b:1")
    }

    @Test
    fun `a single track is enough`() {
        assertThat(resolveAlbumArtistId(listOf("b:9"))).isEqualTo("b:9")
    }

    @Test
    fun `no tracks gives no answer`() {
        assertThat(resolveAlbumArtistId(emptyList())).isNull()
    }

    @Test
    fun `tracks with no artist id do not vote`() {
        assertThat(resolveAlbumArtistId(listOf("", "", "b:2"))).isEqualTo("b:2")
        assertThat(resolveAlbumArtistId(listOf("", ""))).isNull()
    }

    /**
     * The case that made this a vote rather than a first-track read. A guest
     * opening the record must not become the album artist.
     */
    @Test
    fun `a guest on track one does not take the record`() {
        val ids = listOf("b:guest") + List(11) { "b:owner" }
        assertThat(resolveAlbumArtistId(ids)).isEqualTo("b:owner")
    }

    /**
     * An even split has no album artist. Answering here would put a genuine
     * two-artist record on one of their pages and not the other.
     */
    @Test
    fun `an even split gives no answer`() {
        assertThat(resolveAlbumArtistId(listOf("b:1", "b:1", "b:2", "b:2"))).isNull()
    }

    /** One more than the runner up is a decision, even by a single track. */
    @Test
    fun `a plurality of one still decides`() {
        assertThat(resolveAlbumArtistId(listOf("b:1", "b:1", "b:2"))).isEqualTo("b:1")
    }

    /** A three way tie is still a tie. */
    @Test
    fun `a three way tie gives no answer`() {
        assertThat(resolveAlbumArtistId(listOf("b:1", "b:2", "b:3"))).isNull()
    }

    /** A three way race with a leader is decided. */
    @Test
    fun `a three way race with a leader is decided`() {
        assertThat(resolveAlbumArtistId(listOf("b:1", "b:1", "b:2", "b:3"))).isEqualTo("b:1")
    }
}
