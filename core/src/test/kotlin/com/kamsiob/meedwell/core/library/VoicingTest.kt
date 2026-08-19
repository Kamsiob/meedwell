package com.kamsiob.meedwell.core.library

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The tone curves.
 *
 * The test that matters most is the one asserting nothing is ever boosted. A
 * boost adds gain that was not in the master, and on a loud passage it runs out
 * of headroom and clips. It is also the easy mistake: a future curve written to
 * make something "warmer" would reach for +2 at 160 without thinking.
 */
class VoicingTest {

    @Test
    fun `no voicing ever boosts any band`() {
        Voicing.entries.forEach { voicing ->
            voicing.curve.forEach { gain ->
                assertThat(gain).isAtMost(0.0)
            }
        }
    }

    /** Shallow on purpose: a hand on the mix rather than a hand over it. */
    @Test
    fun `no cut is deeper than three decibels`() {
        Voicing.entries.forEach { voicing ->
            voicing.curve.forEach { gain ->
                assertThat(gain).isAtLeast(-3.0)
            }
        }
    }

    @Test
    fun `as recorded is genuinely flat`() {
        assertThat(Voicing.AsRecorded.isFlat).isTrue()
        assertThat(Voicing.AsRecorded.curve).containsExactly(0.0, 0.0, 0.0, 0.0, 0.0)
    }

    @Test
    fun `every other voicing does something`() {
        Voicing.entries.filter { it != Voicing.AsRecorded }.forEach {
            assertThat(it.isFlat).isFalse()
        }
    }

    @Test
    fun `every voicing has a gain for every frequency`() {
        Voicing.entries.forEach {
            assertThat(it.curve).hasSize(Voicing.FREQUENCIES.size)
        }
    }

    // ---------- Interpolation onto a device's own bands ----------

    @Test
    fun `the curve reads back exactly at its own frequencies`() {
        val curve = Voicing.Ambient.curve
        Voicing.FREQUENCIES.forEachIndexed { index, hz ->
            assertThat(Voicing.gainAt(curve, hz)).isWithin(1e-9).of(curve[index])
        }
    }

    /**
     * A phone's equalizer has whatever bands its chip offers, so the curve has
     * to answer for frequencies it was never drawn at.
     */
    @Test
    fun `a frequency between two points lands between their gains`() {
        val curve = Voicing.Piano.curve
        // Between 160 (-2.5) and 800 (-1.0).
        val at400 = Voicing.gainAt(curve, 400)
        assertThat(at400).isGreaterThan(-2.5)
        assertThat(at400).isLessThan(-1.0)
    }

    /** Log spacing, because that is how hearing spaces frequencies. */
    @Test
    fun `interpolation is logarithmic rather than linear in hertz`() {
        val curve = listOf(0.0, -2.0, 0.0, 0.0, 0.0)
        // The midpoint between 40 and 160 in log space is 80, not 100.
        val at80 = Voicing.gainAt(curve, 80)
        assertThat(at80).isWithin(0.05).of(-1.0)
    }

    @Test
    fun `frequencies outside the curve clamp to its ends`() {
        val curve = Voicing.Ambient.curve
        assertThat(Voicing.gainAt(curve, 20)).isEqualTo(curve.first())
        assertThat(Voicing.gainAt(curve, 20_000)).isEqualTo(curve.last())
    }

    /** Interpolating a boost-free curve can never produce a boost. */
    @Test
    fun `interpolation never invents a boost`() {
        Voicing.entries.forEach { voicing ->
            (20..20_000 step 37).forEach { hz ->
                assertThat(Voicing.gainAt(voicing.curve, hz)).isAtMost(0.0)
            }
        }
    }

    @Test
    fun `an unknown stored name falls back to as recorded`() {
        assertThat(Voicing.byName(null)).isEqualTo(Voicing.AsRecorded)
        assertThat(Voicing.byName("Dubstep")).isEqualTo(Voicing.AsRecorded)
        assertThat(Voicing.byName("Piano")).isEqualTo(Voicing.Piano)
    }
}
