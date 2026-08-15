package com.kamsiob.meedwell.ui

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.meedwell.ui.components.MAX_WASH_LUMINANCE
import com.kamsiob.meedwell.ui.components.clampForWhiteText
import com.kamsiob.meedwell.ui.components.contrastWithWhite
import com.kamsiob.meedwell.ui.components.relativeLuminance
import org.junit.Test

/**
 * The now-playing wash is the single place in the app where words sit on
 * colour, and `DESIGN.md` permits it on one condition: that the clamp has **no
 * worst case**. That is a testable claim, so it is tested rather than asserted.
 *
 * The old adaptive-scrim law was retired precisely because it could not make
 * this promise. If these tests ever fail, the replacement has the same flaw the
 * original was retired for.
 */
class WashClampTest {

    /** WCAG AA for large text. The wash carries a title at 32sp. */
    private val floor = 4.5

    @Test
    fun `white passes on the clamped wash for every fully saturated colour`() {
        // Sweep the corners and edges of the colour cube, which is where a
        // naive fixed darkening factor fails: bright yellow and cyan stay far
        // too light while dark blue goes needlessly muddy.
        val corners = listOf(
            Color.White, Color.Black, Color.Red, Color.Green, Color.Blue,
            Color.Yellow, Color.Cyan, Color.Magenta,
        )
        corners.forEach { source ->
            val wash = clampForWhiteText(source)
            val ratio = contrastWithWhite(wash)
            assertWithMessage("white on wash from $source measured ${"%.2f".format(ratio)}:1")
                .that(ratio).isAtLeast(floor)
        }
    }

    @Test
    fun `white passes on the clamped wash across the whole colour cube`() {
        // A coarse but genuine sweep. 6^3 = 216 colours, every one of which has
        // to pass, because "any album, either theme" is the actual claim.
        var worst = Double.MAX_VALUE
        var worstColor = Color.Black
        for (r in 0..5) for (g in 0..5) for (b in 0..5) {
            val source = Color(r / 5f, g / 5f, b / 5f)
            val wash = clampForWhiteText(source)
            val ratio = contrastWithWhite(wash)
            if (ratio < worst) {
                worst = ratio
                worstColor = source
            }
        }
        assertWithMessage("worst case was $worstColor at %.2f:1".format(worst))
            .that(worst).isAtLeast(floor)
    }

    @Test
    fun `the clamp never returns anything above the luminance ceiling`() {
        for (r in 0..4) for (g in 0..4) for (b in 0..4) {
            val wash = clampForWhiteText(Color(r / 4f, g / 4f, b / 4f))
            assertThat(relativeLuminance(wash)).isAtMost(MAX_WASH_LUMINANCE + 1e-9)
        }
    }

    @Test
    fun `a very dark cover still produces a colour rather than a black rectangle`() {
        // The wash exists to carry the record's own colour. Clamping something
        // nearly black to exactly black would lose the point of having one.
        val wash = clampForWhiteText(Color(0.01f, 0.01f, 0.02f))
        assertThat(relativeLuminance(wash)).isGreaterThan(0.0)
    }

    @Test
    fun `the clamp is stable, so a second pass changes nothing`() {
        // Recomposition must not walk the colour steadily darker.
        listOf(Color.Yellow, Color.Cyan, Color(0.4f, 0.2f, 0.5f)).forEach { source ->
            val once = clampForWhiteText(source)
            val twice = clampForWhiteText(once)
            assertThat(relativeLuminance(twice)).isWithin(1e-9).of(relativeLuminance(once))
        }
    }
}
