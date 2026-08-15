package com.kamsiob.meedwell.core.library

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SortKeysTest {

    @Test
    fun `a leading article is dropped so the shelf reads the way people expect`() {
        assertThat(SortKeys.sortKey("The Long Static")).isEqualTo("long static")
        assertThat(SortKeys.sortKey("A Winged Victory")).isEqualTo("winged victory")
        assertThat(SortKeys.sortKey("An Ocean")).isEqualTo("ocean")
    }

    @Test
    fun `an article that is part of the name is not stripped`() {
        // "Theatre" begins with "the" and is not an article.
        assertThat(SortKeys.sortKey("Theatre of Voices")).isEqualTo("theatre of voices")
        assertThat(SortKeys.sortKey("Anderson")).isEqualTo("anderson")
    }

    @Test
    fun `leading punctuation is dropped so titles file under their first letter`() {
        assertThat(SortKeys.sortKey("...And Star Power")).isEqualTo("and star power")
        assertThat(SortKeys.sortKey("'Round Midnight")).isEqualTo("round midnight")
        assertThat(SortKeys.sortKey("(Sandy) Alex G")).isEqualTo("sandy alex g")
    }

    @Test
    fun `a name that is only punctuation does not become empty`() {
        // An empty sort key would put the row in an unfindable place.
        assertThat(SortKeys.sortKey("!!!")).isEqualTo("!!!")
        assertThat(SortKeys.sortKey("...")).isEqualTo("...")
    }

    @Test
    fun `only the first article is stripped`() {
        assertThat(SortKeys.sortKey("The The")).isEqualTo("the")
    }

    @Test
    fun `anything not a latin letter files under hash rather than an invented letter`() {
        assertThat(SortKeys.indexLetter(SortKeys.sortKey("日本語"))).isEqualTo("#")
        assertThat(SortKeys.indexLetter(SortKeys.sortKey("4AD"))).isEqualTo("#")
        assertThat(SortKeys.indexLetter(SortKeys.sortKey("Ada Vex"))).isEqualTo("A")
        assertThat(SortKeys.indexLetter(SortKeys.sortKey("The Long Static"))).isEqualTo("L")
    }

    @Test
    fun `the index is built once and gives each letter its first position`() {
        val keys = listOf("ada vex", "ada vex", "brine", "long static", "long static", "woven hills")
        val index = SortKeys.buildIndex(keys)
        assertThat(index).containsExactly(
            SortKeys.IndexEntry("A", 0),
            SortKeys.IndexEntry("B", 2),
            SortKeys.IndexEntry("L", 3),
            SortKeys.IndexEntry("W", 5),
        ).inOrder()
    }

    @Test
    fun `an empty shelf produces an empty index rather than throwing`() {
        assertThat(SortKeys.buildIndex(emptyList())).isEmpty()
    }
}
