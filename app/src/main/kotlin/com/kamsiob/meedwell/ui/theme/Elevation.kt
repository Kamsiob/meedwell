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
 * Spacing and radii, read out of `reference/meedwell-screen-grid-CURRENT.html`.
 *
 * ## There is no elevation scale any more
 *
 * This file used to hold six elevation tokens and a `meedwellShadow` helper
 * applied to covers, thumbnails, buttons, the mini player and every sheet. The
 * grid has **no shadows at all** except one, on a bottom sheet, and no filled
 * or raised containers anywhere. Structure is carried by hairlines and
 * whitespace.
 *
 * That single habit, reaching for a card to group things, is most of what made
 * the first build look like every other music app. So the helper is gone rather
 * than merely unused: there is nothing left to reach for.
 *
 * The one survivor is `sheetShadow`, which is in the grid as
 * `box-shadow:0 -18px 40px -24px rgba(28,36,32,.4)` on `.sheet`, and it is a
 * sheet lifting off the page rather than a card sitting on it.
 */
object Spacing {
    /**
     * The screen gutter. **22dp on every screen without exception.**
     *
     * `.sc { padding: 0 22px }` in the grid, with no per-screen override
     * anywhere in the file. The old value was 26dp and drifted to 20 and 24 in
     * places, which is what "generous spacing" produces when it is written down
     * as prose instead of a number.
     */
    val gutter: Dp = 22.dp

    /** Between a heading and the line under it. */
    val tight: Dp = 8.dp
    val small: Dp = 12.dp
    val medium: Dp = 18.dp
}

/**
 * Corner radii, all of them from the grid.
 *
 * Note how few there are, and how small. A design carried by hairlines has
 * little use for rounding, because there are almost no filled shapes to round.
 */
object Radius {
    /** `.cov { border-radius: 5px }`. Album artwork, at any size. */
    val cover: Dp = 5.dp

    /** `.mini .ar { border-radius: 4px }`. The mini player's small artwork. */
    val miniArtwork: Dp = 4.dp

    /** `.sheet { border-radius: 20px 20px 0 0 }`. */
    val sheet: Dp = 20.dp

    /** `.btn`, `.chip`, `.tog`, `.sfield`, `.spill`: all `999px`. */
    val pill: Dp = 999.dp
}

/**
 * The one shadow in the design, on a bottom sheet.
 *
 * Applied **before** the background and clip in a modifier chain, so it is cast
 * by the shape rather than clipped away by it.
 */
@Composable
fun Modifier.sheetShadow(shape: Shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet)): Modifier {
    val dark = MeedwellTheme.colors.isDark
    return this.shadow(
        elevation = 18.dp,
        shape = shape,
        clip = false,
        // rgba(28,36,32,.4) on paper. On Lamplight the same shadow against a
        // near-black ground would be invisible, so it goes to true black and
        // the sheet is separated by its hairline instead.
        ambientColor = if (dark) Color(0x66000000) else Color(0x661C2420),
        spotColor = if (dark) Color(0x66000000) else Color(0x661C2420),
    )
}
