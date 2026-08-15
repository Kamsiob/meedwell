package com.kamsiob.meedwell.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shadows, which the app had none of.
 *
 * The reference gets its entire sense of physical objects from them: a cover
 * casts `0 16px 34px -14px rgba(0,0,0,.7)`, a thumbnail `0 8px 18px -8px`, the
 * white pill `0 14px 34px -12px` **plus** a faint white bloom, the mini player
 * `0 18px 40px -16px`. Without any of it every cover looks pasted onto the
 * background rather than sitting on a shelf, which is a strange thing for an app
 * whose whole argument is that records are objects you own.
 *
 * Compose's `shadow` takes an elevation rather than the offset, blur and spread
 * a CSS shadow gives, so these are the elevations that produce a comparable
 * result on a near-black ground, tuned by eye on the device rather than
 * converted arithmetically.
 *
 * **Ambient and spot colors are set explicitly.** The platform default is a
 * neutral black that reads gray over `#0B0B0E`; a slightly warmer, fully opaque
 * black keeps the shadow reading as depth rather than as a smudge.
 */
object Elevation {

    /** A list-row thumbnail. Present but nearly subliminal. */
    val thumb: Dp = 4.dp

    /** A grid cover. The record sitting on the shelf. */
    val cover: Dp = 10.dp

    /** The full-bleed album header and the now-playing cover. */
    val hero: Dp = 16.dp

    /** The primary pill button. */
    val button: Dp = 8.dp

    /** The mini player, which floats clear of the tab bar. */
    val floating: Dp = 12.dp

    /** A bottom sheet. */
    val sheet: Dp = 20.dp
}

/**
 * Shadow colors, which are not the same in both themes.
 *
 * On near-black, a shadow has to be darker than a background that is already
 * almost black, so it is very nearly pure and cast strongly enough to be seen
 * at all.
 *
 * On the light theme those same values are a disaster: a near-black shadow at
 * full elevation under an off-white card renders as a grey smear with a visible
 * edge, which reads as a rendering fault rather than as depth. The light
 * shadow is therefore a low-alpha warm grey, cast shorter, so a card looks
 * lifted rather than stained.
 */
private val DarkShadowAmbient = Color(0xFF05050A)
private val DarkShadowSpot = Color(0xFF000006)
private val LightShadowAmbient = Color(0x1C3A342B)
private val LightShadowSpot = Color(0x2B2A251E)

/**
 * The light theme's shadows are shorter as well as fainter.
 *
 * Elevation in Material's model is a physical height, and a tall card on a
 * white ground throws a long shadow that swamps a quiet layout. The proportion
 * here was chosen by looking at the mini player, which is the most elevated
 * thing that sits over content.
 */
private const val LIGHT_ELEVATION_SCALE = 0.55f

/**
 * A shadow that reads as depth in either theme.
 *
 * Always applied **before** the background and clip in a modifier chain, so the
 * shadow is cast by the shape rather than clipped away by it. Getting that order
 * wrong silently produces no shadow at all, which is how the app ended up with
 * none.
 */
@Composable
fun Modifier.meedwellShadow(
    elevation: Dp,
    shape: Shape = RoundedCornerShape(0.dp),
): Modifier {
    val dark = MeedwellTheme.colors.isDark
    return this.shadow(
        elevation = if (dark) elevation else elevation * LIGHT_ELEVATION_SCALE,
        shape = shape,
        clip = false,
        ambientColor = if (dark) DarkShadowAmbient else LightShadowAmbient,
        spotColor = if (dark) DarkShadowSpot else LightShadowSpot,
    )
}

/**
 * The spacing scale.
 *
 * Every screen was typing its own gutter: 16, 22, 26 and 30 all appeared, none
 * of them the reference's. One token now, so a number in a layout is a decision
 * rather than a guess.
 */
object Spacing {
    /** The screen gutter. `.pad` in the reference is 24px, so 30dp. */
    val gutter: Dp = 26.dp

    /** Between a heading and the line under it. */
    val tight: Dp = 8.dp
    val small: Dp = 12.dp
    val medium: Dp = 18.dp
    val large: Dp = 26.dp

    /** Grid gaps. The reference is 20px column, 25px row. */
    val gridColumn: Dp = 16.dp
    val gridRow: Dp = 22.dp
}

/**
 * Corner radii, taken from the reference rather than halved.
 *
 * `.sq` is 9px, so 11dp. `.art` is 12px, so 15dp. The now-playing cover is 16px,
 * so 20dp. The build had 5, 7 and 14, which is why the grid read as sharp tiles.
 */
object Radius {
    val thumb: Dp = 11.dp
    val cover: Dp = 15.dp
    val hero: Dp = 20.dp
    val panel: Dp = 16.dp
    val sheet: Dp = 26.dp
    /** The mini player. `.mini` is 15px, so 18.75dp. */
    val floating: Dp = 19.dp
}
