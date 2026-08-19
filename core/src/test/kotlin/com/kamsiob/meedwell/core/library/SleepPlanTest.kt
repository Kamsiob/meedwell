package com.kamsiob.meedwell.core.library

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The sleep timer, which is the one feature whose failure happens while nobody
 * is awake to see it. Tested here rather than by setting a timer and waiting.
 */
class SleepPlanTest {

    @Test
    fun `full gain until the fade begins`() {
        assertThat(SleepPlan.gainAt(600)).isEqualTo(1f)
        assertThat(SleepPlan.gainAt(SleepPlan.FADE_SECONDS)).isEqualTo(1f)
    }

    @Test
    fun `the fade falls smoothly to silence`() {
        assertThat(SleepPlan.gainAt(45)).isWithin(0.001f).of(0.5f)
        assertThat(SleepPlan.gainAt(9)).isWithin(0.001f).of(0.1f)
        assertThat(SleepPlan.gainAt(0)).isEqualTo(0f)
    }

    /** Time does not go backwards, and neither does the gain. */
    @Test
    fun `a timer past its end is silent rather than negative`() {
        assertThat(SleepPlan.gainAt(-30)).isEqualTo(0f)
    }

    @Test
    fun `the fade is only the last stretch`() {
        assertThat(SleepPlan.isFading(600)).isFalse()
        assertThat(SleepPlan.isFading(60)).isTrue()
        assertThat(SleepPlan.isFading(0)).isFalse()
    }

    @Test
    fun `the countdown reads as a clock`() {
        assertThat(SleepPlan.countdown(2_700)).isEqualTo("45:00")
        assertThat(SleepPlan.countdown(65)).isEqualTo("1:05")
        assertThat(SleepPlan.countdown(7_265)).isEqualTo("2:01:05")
    }

    /** Never "0:-3" on a timer somebody is watching drift to zero. */
    @Test
    fun `the countdown never goes negative`() {
        assertThat(SleepPlan.countdown(-5)).isEqualTo("0:00")
    }

    /**
     * The disclosure. A timer that says forty five minutes and starts fading at
     * forty four has lied to somebody trying to fall asleep.
     */
    @Test
    fun `the fade start is stated and is correct`() {
        assertThat(SleepPlan.fadeBeginsIn(2_700)).isEqualTo(2_700 - SleepPlan.FADE_SECONDS)
    }

    /** A timer shorter than the fade is all fade, and says nothing rather than "in 0:00". */
    @Test
    fun `a very short timer discloses no separate fade start`() {
        assertThat(SleepPlan.fadeBeginsIn(60)).isNull()
        assertThat(SleepPlan.fadeBeginsIn(SleepPlan.FADE_SECONDS)).isNull()
    }

    @Test
    fun `the presets and the range are the ones the screen offers`() {
        // Extended to three hours on 16 August 2026: sixty minutes as a
        // ceiling was shorter than the long-form work this player is for.
        assertThat(SleepPlan.PRESETS).containsExactly(15, 30, 45, 60, 90, 120, 180).inOrder()
        assertThat(SleepPlan.MIN_MINUTES).isEqualTo(5)
        assertThat(SleepPlan.MAX_MINUTES).isEqualTo(180)
    }
}
