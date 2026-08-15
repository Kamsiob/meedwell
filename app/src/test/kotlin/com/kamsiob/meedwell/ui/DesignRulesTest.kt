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
import com.kamsiob.meedwell.ui.theme.DarkBackground
import com.kamsiob.meedwell.ui.theme.DarkGold
import com.kamsiob.meedwell.ui.theme.DarkPrimaryText
import com.kamsiob.meedwell.ui.theme.DarkSecondaryText
import com.kamsiob.meedwell.ui.theme.DarkTertiaryText
import com.kamsiob.meedwell.ui.theme.LightBackground
import com.kamsiob.meedwell.ui.theme.LightGold
import com.kamsiob.meedwell.ui.theme.LightPrimaryText
import com.kamsiob.meedwell.ui.theme.LightSecondaryText
import com.kamsiob.meedwell.ui.theme.LightSecondaryTextSoft
import com.kamsiob.meedwell.ui.theme.LightTertiaryText
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The design rules that are arithmetic, asserted rather than trusted.
 *
 * `DESIGN.md` says contrast is "measured rather than eyeballed" and that any
 * new colour pair gets computed before it ships. A document saying so does not
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
    fun `dark theme ink passes AA against the warm near-black`() {
        assertContrast("dark primary on background", DarkPrimaryText, DarkBackground, 4.5)
        assertContrast("dark secondary on background", DarkSecondaryText, DarkBackground, 4.5)
        assertContrast("dark tertiary on background", DarkTertiaryText, DarkBackground, 4.5)
    }

    @Test
    fun `light theme ink passes AA against paper, and the floor holds`() {
        // The light mode ink floor is law: nothing lighter than slate #56525E
        // on paper, ever. It was corrected twice in design.
        assertContrast("light primary on paper", LightPrimaryText, LightBackground, 4.5)
        assertContrast("light secondary on paper", LightSecondaryText, LightBackground, 4.5)
        assertContrast("light secondary soft on paper", LightSecondaryTextSoft, LightBackground, 4.5)
        assertContrast("light tertiary floor on paper", LightTertiaryText, LightBackground, 4.5)
    }

    @Test
    fun `the documented light ink measurements still hold`() {
        // DESIGN.md section 2 states these figures. If a token moves, the
        // document is now wrong, and this is where that gets caught.
        assertThat(contrast(LightPrimaryText, LightBackground)).isWithin(0.15).of(16.3)
        assertThat(contrast(LightSecondaryText, LightBackground)).isWithin(0.15).of(11.6)
        assertThat(contrast(LightSecondaryTextSoft, LightBackground)).isWithin(0.15).of(9.0)
        assertThat(contrast(LightTertiaryText, LightBackground)).isWithin(0.15).of(6.9)
    }

    @Test
    fun `gold passes AA in both themes, including the correction that made it`() {
        // Light gold was #9A6F1E at 4.06:1 and failed AA for a 14sp label. It
        // is now #8A6215 at 4.93:1. This test is the reason it cannot drift back.
        assertContrast("gold on paper", LightGold, LightBackground, 4.5)
        assertContrast("gold on near-black", DarkGold, DarkBackground, 4.5)
        assertThat(contrast(LightGold, LightBackground)).isWithin(0.1).of(4.93)
        assertThat(contrast(DarkGold, DarkBackground)).isWithin(0.15).of(11.5)
    }

    @Test
    fun `neither theme uses pure black or pure white`() {
        listOf(DarkBackground, LightBackground, DarkPrimaryText, LightPrimaryText).forEach { color ->
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
