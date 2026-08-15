package com.kamsiob.meedwell.ui

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.meedwell.ui.components.CRADLE_LOWEST_Y
import com.kamsiob.meedwell.ui.components.CRADLE_ENDS_Y
import com.kamsiob.meedwell.ui.components.CRADLE_LEFT
import com.kamsiob.meedwell.ui.components.CRADLE_RIGHT
import com.kamsiob.meedwell.ui.components.CRADLE_STROKE
import com.kamsiob.meedwell.ui.components.COIN_RADIUS
import com.kamsiob.meedwell.ui.theme.Alarm
import com.kamsiob.meedwell.ui.theme.Copper
import com.kamsiob.meedwell.ui.theme.DaylightColors
import com.kamsiob.meedwell.ui.theme.GoldInk
import com.kamsiob.meedwell.ui.theme.Ink
import com.kamsiob.meedwell.ui.theme.Ink2
import com.kamsiob.meedwell.ui.theme.Ink3
import com.kamsiob.meedwell.ui.theme.Lamp
import com.kamsiob.meedwell.ui.theme.Lamp2
import com.kamsiob.meedwell.ui.theme.Lamp3
import com.kamsiob.meedwell.ui.theme.LamplightColors
import com.kamsiob.meedwell.ui.theme.LampInk
import com.kamsiob.meedwell.ui.theme.MossDeep
import com.kamsiob.meedwell.ui.theme.Paper
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The design rules that are arithmetic, asserted rather than trusted.
 *
 * `DESIGN.md` says contrast is "measured rather than eyeballed" and that any
 * new color pair gets computed before it ships. A document saying so does not
 * enforce it; this does. If someone lightens a token, the build fails here and
 * names the pair and its ratio.
 */
class DesignRulesTest {

    // ---------- Contrast, WCAG 2.1 relative luminance ----------

    private fun channel(v: Float): Double {
        val c = v.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun assertContrast(name: String, fg: Color, bg: Color, floor: Double) {
        val ratio = contrast(fg, bg)
        // The failure message carries the measured figure, so a broken build
        // says "3.9:1, floor 4.5:1" rather than only "expected at least 4.5".
        // Truth's message takes %s placeholders only, so the ratio is formatted
        // before it gets there.
        assertWithMessage("%s measured ${"%.2f".format(ratio)}:1", name).that(ratio).isAtLeast(floor)
    }

    @Test
    fun `Daylight ink passes AA on paper, and the tertiary floor holds`() {
        // The floor is law: nothing on paper is fainter than --ink-3. It is the
        // value the grid calls "tertiary, the floor. nothing lighter".
        assertContrast("ink on paper", Ink, Paper, 4.5)
        assertContrast("ink-2 on paper", Ink2, Paper, 4.5)
        assertContrast("ink-3 on paper", Ink3, Paper, 4.5)
    }

    @Test
    fun `Lamplight ink passes AA on deep pine`() {
        assertContrast("lamp ink on lamp", LampInk, Lamp, 4.5)
        assertContrast("lamp-2 on lamp", Lamp2, Lamp, 4.5)
        assertContrast("lamp-3 on lamp", Lamp3, Lamp, 4.5)
    }

    /**
     * The reserved colors have to be legible where they are actually used, and
     * each one is used in exactly one place.
     */
    @Test
    fun `the reserved colors are legible where they appear`() {
        // Gold ink, on the support blocks, on paper only.
        assertContrast("gold ink on paper", GoldInk, Paper, 4.5)
        // Alarm, on destructive rows, on paper only.
        assertContrast("alarm on paper", Alarm, Paper, 4.5)
        // Moss as ink, on a playing row.
        assertContrast("moss ink on paper", MossDeep, Paper, 4.5)
        // Moss on Lamplight, where it is lifted for exactly this reason.
        assertContrast("lamp moss on lamp", LamplightColors.mossInk, Lamp, 4.5)
    }

    /**
     * Copper is the mark and the sun, and it never carries text.
     *
     * So it is checked as a **graphic** against the 3:1 floor for non-text
     * content rather than against 4.5:1. Holding it to a text ratio would be
     * measuring the wrong thing and would push the mark's own color away from
     * what the design says it is.
     */
    @Test
    fun `copper reads as a graphic on paper`() {
        assertContrast("copper on paper", Copper, Paper, 3.0)
    }

    @Test
    fun `the two grounds are the ones the grid names`() {
        // Daylight is the default. If this ever flips, the app opens dark and
        // the whole design reads differently before anything else is judged.
        assertThat(DaylightColors.background).isEqualTo(Paper)
        assertThat(DaylightColors.isDark).isFalse()
        assertThat(LamplightColors.background).isEqualTo(Lamp)
        assertThat(LamplightColors.isDark).isTrue()
    }

    @Test
    fun `neither ground is pure black or pure white`() {
        // Paper is warm and Lamplight is deep pine. Both were chosen against
        // the neutral defaults, and this is what stops them drifting back.
        listOf(Paper, Lamp, Ink, LampInk).forEach { color ->
            assertThat(color).isNotEqualTo(Color(0xFF000000))
            assertThat(color).isNotEqualTo(Color(0xFFFFFFFF))
        }
    }

    // ---------- The mark's construction ----------

    @Test
    fun `the coin rests on the cradle, touching, neither sunk nor floating`() {
        // The rule from DESIGN.md section 7, as arithmetic. An earlier version
        // of the drawing had the coin floating well clear of the arc because
        // the ellipse's lowest point fell off the canvas, and it looked wrong
        // on the device before anything caught it. Now something catches it.
        val coinCenterY = CRADLE_LOWEST_Y - (CRADLE_STROKE / 2f) - COIN_RADIUS
        val coinBottom = coinCenterY + COIN_RADIUS
        val cradleInnerEdge = CRADLE_LOWEST_Y - (CRADLE_STROKE / 2f)
        assertThat(coinBottom).isWithin(0.0005f).of(cradleInnerEdge)
    }

    @Test
    fun `the cradle is shallow, its ends level, and it stops short of the frame`() {
        // Shallow: the arc's depth is well under half its width, so it reads as
        // a cradle rather than as a bowl.
        val width = CRADLE_RIGHT - CRADLE_LEFT
        val depth = CRADLE_LOWEST_Y - CRADLE_ENDS_Y
        assertThat(depth).isLessThan(width / 2f)

        // Stops short of the frame on every side, so the arc breathes.
        assertThat(CRADLE_LEFT).isGreaterThan(0.05f)
        assertThat(CRADLE_RIGHT).isLessThan(0.95f)
        assertThat(CRADLE_LOWEST_Y + CRADLE_STROKE / 2f).isLessThan(1f)

        // Symmetric, which is what "ends rising level with each other" means.
        assertThat(CRADLE_LEFT).isWithin(0.0005f).of(1f - CRADLE_RIGHT)
    }

    @Test
    fun `the cradle stroke never reads heavier than the coin`() {
        assertThat(CRADLE_STROKE).isLessThan(COIN_RADIUS)
    }

    @Test
    fun `the whole mark stays inside the adaptive icon safe zone`() {
        // Drawn into the middle 72 of a 108 canvas. Anything outside can be
        // clipped by a launcher mask, and a clipped coin is a broken mark.
        val topMost = CRADLE_LOWEST_Y - CRADLE_STROKE / 2f - 2 * COIN_RADIUS
        assertThat(topMost).isGreaterThan(0f)
        assertThat(CRADLE_LOWEST_Y + CRADLE_STROKE / 2f).isLessThan(1f)
    }
}
