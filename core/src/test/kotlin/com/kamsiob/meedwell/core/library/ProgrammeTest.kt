package com.kamsiob.meedwell.core.library

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Setting a record as a programme.
 *
 * The tests that matter here are the ones about **not** printing something. A
 * tempo marking that is guessed, or a movement number that is filled in from an
 * incomplete record, is worse than a shorter line: it is the app making a
 * confident claim about somebody's music that happens to be false.
 */
class ProgrammeTest {

    // ---------- Tempo ----------

    @Test
    fun `a tempo marking in the title is found`() {
        assertThat(Programme.tempoIn("II. Andante con moto")).isEqualTo("andante")
        assertThat(Programme.tempoIn("Adagio for Strings")).isEqualTo("adagio")
    }

    /** Longest first, or every allegretto becomes an allegro. */
    @Test
    fun `allegretto is not allegro`() {
        assertThat(Programme.tempoIn("III. Allegretto")).isEqualTo("allegretto")
        assertThat(Programme.tempoIn("IV. Prestissimo")).isEqualTo("prestissimo")
    }

    /**
     * Whole words only. A label called Andante Records, or a piece called Largo
     * Winch, must not acquire a tempo it does not have.
     */
    @Test
    fun `a tempo word inside another word is not a tempo`() {
        assertThat(Programme.tempoIn("Andantette")).isNull()
        assertThat(Programme.tempoIn("Prestonville")).isNull()
    }

    @Test
    fun `most titles have no tempo and get none`() {
        assertThat(Programme.tempoIn("The Long Field at Dusk")).isNull()
        assertThat(Programme.tempoIn("")).isNull()
    }

    @Test
    fun `punctuation does not hide a tempo`() {
        assertThat(Programme.tempoIn("I. Largo, then onward")).isEqualTo("largo")
    }

    // ---------- Movement ----------

    @Test
    fun `a movement is numbered in Roman numerals`() {
        assertThat(Programme.movement(4, 9)).isEqualTo("IV of IX")
        assertThat(Programme.movement(1, 3)).isEqualTo("I of III")
    }

    /**
     * The refusals. A record still being read has a track count of zero, and
     * "I of I" would be a confident statement about a record the app has not
     * finished loading.
     */
    @Test
    fun `an unknown position is left out rather than guessed`() {
        assertThat(Programme.movement(0, 9)).isNull()
        assertThat(Programme.movement(4, 0)).isNull()
        assertThat(Programme.movement(-1, 9)).isNull()
        // A number past the end means the two came from different places.
        assertThat(Programme.movement(12, 9)).isNull()
    }

    // ---------- Roman numerals ----------

    @Test
    fun `roman numerals are correct at the awkward numbers`() {
        assertThat(Programme.roman(4)).isEqualTo("IV")
        assertThat(Programme.roman(9)).isEqualTo("IX")
        assertThat(Programme.roman(14)).isEqualTo("XIV")
        assertThat(Programme.roman(40)).isEqualTo("XL")
        assertThat(Programme.roman(90)).isEqualTo("XC")
        assertThat(Programme.roman(99)).isEqualTo("XCIX")
    }

    /** A record with four hundred movements is not a record. */
    @Test
    fun `absurd numbers fall back to digits rather than a wall of letters`() {
        assertThat(Programme.roman(400)).isEqualTo("400")
        assertThat(Programme.roman(0)).isEqualTo("0")
    }

    // ---------- The whole line ----------

    @Test
    fun `the line carries both parts when both are known`() {
        assertThat(Programme.line("II. Andante con moto", 2, 4)).isEqualTo("andante · II of IV")
    }

    /**
     * The common case for popular music, and the reason the line is built from
     * parts rather than formatted from a template: no lone separator.
     */
    @Test
    fun `a title with no tempo gives only the position`() {
        assertThat(Programme.line("The Long Field at Dusk", 4, 9)).isEqualTo("IV of IX")
    }

    @Test
    fun `nothing known gives an empty line rather than punctuation`() {
        assertThat(Programme.line("The Long Field at Dusk", 0, 0)).isEmpty()
    }

    /**
     * There is no dynamic marking anywhere in the output, on purpose. A file
     * carries no such tag and measuring loudness would describe the master
     * rather than the marking the composer wrote.
     */
    @Test
    fun `no dynamic marking is ever invented`() {
        val line = Programme.line("II. Andante con moto", 2, 4)
        listOf("pp", "mp", "mf", "ff", "forte", "piano").forEach {
            assertThat(line.split(" · ")).doesNotContain(it)
        }
    }
}
