package com.kamsiob.meedwell.core.surroundings

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs
import kotlin.math.log10

/**
 * The loop arithmetic, checked against the numbers in `REQUIREMENTS.md` rather
 * than against itself. These are the decisions that are either right or
 * audible, and an ear is a bad regression test.
 */
class LoopMixTest {

    private fun sound(
        makeup: String = "0.0",
        head: String = "-50.0",
        tail: String = "-50.0",
        truePeak: String = "-13.6",
        duration: Long = 600,
    ) = SurroundingsSound(
        id = "1",
        filename = "x.opus",
        durationSeconds = duration,
        loop = SurroundingsLoop(headLevelDb = head, tailLevelDb = tail),
        loudness = SurroundingsLoudness(makeupGainDb = makeup, truePeakDbtp = truePeak),
    )

    // ---------- R1: the crossfade must preserve power ----------

    /**
     * The whole reason for cos and sin rather than a linear ramp. A linear
     * crossfade of two uncorrelated signals dips about 3 dB in the middle,
     * which is heard once per loop.
     */
    @Test
    fun `summed power is constant across the whole fade`() {
        for (step in 0..100) {
            val t = step / 100.0
            val (out, incoming) = LoopMix.crossfadeWeights(t)
            val power = out * out + incoming * incoming
            assertThat(abs(power - 1.0)).isLessThan(1e-9)
        }
    }

    @Test
    fun `the fade starts on the outgoing loop and ends on the incoming one`() {
        val (outStart, inStart) = LoopMix.crossfadeWeights(0.0)
        assertThat(outStart).isWithin(1e-9).of(1.0)
        assertThat(inStart).isWithin(1e-9).of(0.0)

        val (outEnd, inEnd) = LoopMix.crossfadeWeights(1.0)
        assertThat(outEnd).isWithin(1e-9).of(0.0)
        assertThat(inEnd).isWithin(1e-9).of(1.0)
    }

    @Test
    fun `the halfway point is equal and three decibels down on each side`() {
        val (out, incoming) = LoopMix.crossfadeWeights(0.5)
        assertThat(out).isWithin(1e-9).of(incoming)
        // Each side at -3.01 dB is exactly what makes the sum come out at unity.
        assertThat(20 * log10(out)).isWithin(0.01).of(-3.01)
    }

    /** A position outside the fade is not a crash, it is the nearest end. */
    @Test
    fun `positions outside the fade clamp`() {
        assertThat(LoopMix.crossfadeWeights(-1.0)).isEqualTo(LoopMix.crossfadeWeights(0.0))
        assertThat(LoopMix.crossfadeWeights(9.0)).isEqualTo(LoopMix.crossfadeWeights(1.0))
    }

    @Test
    fun `the fade sits inside the two to three second window`() {
        assertThat(LoopMix.CROSSFADE_SECONDS).isAtLeast(2.0)
        assertThat(LoopMix.CROSSFADE_SECONDS).isAtMost(3.0)
    }

    // ---------- R2: makeup gain ----------

    @Test
    fun `makeup gain comes from the manifest`() {
        assertThat(LoopMix.makeupGainDb(sound(makeup = "10.79"))).isWithin(1e-9).of(10.79)
    }

    /** Nothing shipped needs more than 16.89 dB, and R2 allows 18. */
    @Test
    fun `makeup gain is capped at eighteen decibels`() {
        assertThat(LoopMix.makeupGainDb(sound(makeup = "25.0"))).isWithin(1e-9).of(18.0)
    }

    /** Negative makeup is not a thing. A file already at target needs none. */
    @Test
    fun `negative makeup gain is refused`() {
        assertThat(LoopMix.makeupGainDb(sound(makeup = "-4.0"))).isWithin(1e-9).of(0.0)
    }

    @Test
    fun `a missing or unreadable makeup figure means no makeup`() {
        assertThat(LoopMix.makeupGainDb(sound(makeup = ""))).isEqualTo(0.0)
        assertThat(LoopMix.makeupGainDb(sound(makeup = "loud"))).isEqualTo(0.0)
    }

    // ---------- R3: the loop point ----------

    /** The worked example from REQUIREMENTS.md. */
    @Test
    fun `mismatch is head minus tail`() {
        val s = sound(head = "-48.86", tail = "-61.77")
        assertThat(LoopMix.loopMismatchDb(s)).isWithin(1e-9).of(12.91)
    }

    /**
     * The incoming loop is held down by the full mismatch as it arrives and let
     * back up across the fade, which is what turns a step into a swell.
     */
    @Test
    fun `compensation holds the incoming loop down and releases it`() {
        val mismatch = 6.85
        assertThat(LoopMix.loopCompensationDb(mismatch, 0.0)).isWithin(1e-9).of(-6.85)
        assertThat(LoopMix.loopCompensationDb(mismatch, 0.5)).isWithin(1e-9).of(-3.425)
        assertThat(LoopMix.loopCompensationDb(mismatch, 1.0)).isWithin(1e-9).of(0.0)
    }

    /** A file whose ends match gets the plain crossfade and nothing else. */
    @Test
    fun `a matched file is never compensated`() {
        val s = sound(head = "-50.0", tail = "-50.0")
        val mismatch = LoopMix.loopMismatchDb(s)
        assertThat(mismatch).isEqualTo(0.0)
        for (step in 0..10) {
            assertThat(LoopMix.loopCompensationDb(mismatch, step / 10.0)).isEqualTo(0.0)
        }
    }

    /** A file that ends louder than it starts is lifted rather than held down. */
    @Test
    fun `compensation works in both directions`() {
        assertThat(LoopMix.loopCompensationDb(-5.0, 0.0)).isWithin(1e-9).of(5.0)
    }

    // ---------- Where the fade goes ----------

    @Test
    fun `the fade begins two and a half seconds before the end`() {
        assertThat(LoopMix.crossfadeStartSeconds(600.0)).isWithin(1e-9).of(597.5)
    }

    /** A file too short for a full fade gets a shorter one, not a truncated one. */
    @Test
    fun `a short file gets a proportional fade`() {
        assertThat(LoopMix.effectiveFadeSeconds(3.0)).isWithin(1e-9).of(1.0)
        assertThat(LoopMix.crossfadeStartSeconds(3.0)).isWithin(1e-9).of(2.0)
    }

    @Test
    fun `the fade start is never negative`() {
        assertThat(LoopMix.crossfadeStartSeconds(0.0)).isEqualTo(0.0)
    }

    // ---------- Decibels ----------

    /**
     * Unity has to be exactly one. The usual power round trip returns
     * 0.9999999999999999, and a path that never quite reaches unity is a path
     * that is always slightly wrong.
     */
    @Test
    fun `zero decibels is exactly unity`() {
        assertThat(LoopMix.dbToLinear(0.0)).isEqualTo(1.0)
    }

    @Test
    fun `six decibels is about double`() {
        assertThat(LoopMix.dbToLinear(6.02)).isWithin(0.001).of(2.0)
    }

    @Test
    fun `decibels round trip`() {
        for (db in listOf(-40.0, -12.0, -3.0, 0.0, 6.0, 16.89)) {
            assertThat(LoopMix.linearToDb(LoopMix.dbToLinear(db))).isWithin(1e-9).of(db)
        }
    }

    @Test
    fun `silence has a floor rather than an infinity`() {
        assertThat(LoopMix.linearToDb(0.0)).isEqualTo(-160.0)
    }

    // ---------- The limiter's own arithmetic ----------

    /**
     * The point of stating the true peak in the manifest: on most files at
     * their intended level the limiter has nothing to do and stays out of the
     * signal path.
     */
    @Test
    fun `the limiter stays out when the peak clears the ceiling`() {
        val s = sound(truePeak = "-13.6")
        assertThat(LoopMix.limiterEngages(s, 10.79)).isFalse()
    }

    @Test
    fun `the limiter engages when gain would push a peak over`() {
        val s = sound(truePeak = "-13.6")
        assertThat(LoopMix.limiterEngages(s, 13.0)).isTrue()
    }

    @Test
    fun `headroom is the distance from the stated peak to the ceiling`() {
        assertThat(LoopMix.headroomDb(sound(truePeak = "-13.6"))).isWithin(1e-9).of(12.6)
    }

    // ---------- Volume ----------

    @Test
    fun `full volume is the recording's own level plus the headroom`() {
        val s = sound(makeup = "10.79")
        assertThat(LoopMix.playbackGainDb(s, 1.0))
            .isWithin(1e-9).of(10.79 + LoopMix.BED_HEADROOM_DB)
    }

    /**
     * The bug this headroom exists for.
     *
     * The library is normalised so almost every file needs no makeup at all, so
     * full volume used to apply a gain of exactly zero: the bed played at the
     * level it was mastered to sit under music at, and on its own it was barely
     * audible. Full volume has to be meaningfully louder than the matched bed
     * level or the top of the control means nothing.
     */
    @Test
    fun `full volume is well above the matched bed level`() {
        val typical = sound(makeup = "0.0")
        assertThat(LoopMix.playbackGainDb(typical, 1.0)).isGreaterThan(6.0)
    }

    @Test
    fun `zero volume is silence`() {
        assertThat(LoopMix.playbackGainDb(sound(), 0.0)).isEqualTo(-160.0)
    }

    /**
     * Halfway down the slider is a clear drop rather than the barely audible
     * 6 dB a linear control would give, which is what makes the bottom half of
     * the control usable for a bed sitting under music.
     */
    @Test
    fun `halfway down the slider is a real drop`() {
        // Stated as the drop from full rather than as an absolute gain, so the
        // claim is about the taper itself and stays true whatever headroom the
        // top of the control is given.
        val s = sound(makeup = "0.0")
        val drop = LoopMix.playbackGainDb(s, 1.0) - LoopMix.playbackGainDb(s, 0.5)
        assertThat(drop).isWithin(0.01).of(12.04)
    }

    /**
     * Two recordings needing very different makeup sound equally loud at the
     * same volume. The makeup is the file's business, not the listener's.
     */
    @Test
    fun `volume means the same thing on every recording`() {
        val quiet = sound(makeup = "16.89")
        val loud = sound(makeup = "0.0")
        val difference = LoopMix.playbackGainDb(quiet, 0.6) - LoopMix.playbackGainDb(loud, 0.6)
        assertThat(difference).isWithin(1e-9).of(16.89)
    }
}
