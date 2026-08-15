package com.kamsiob.meedwell.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.theme.Copper
import com.kamsiob.meedwell.ui.theme.MarkField

/**
 * The Meedwell mark: a copper coin at rest in a shallow open cradle.
 *
 * `DESIGN.md` section 7 is binding, and its construction rules are what this
 * function implements rather than approximates:
 *
 *  - The coin rests at the **lowest point of the cradle, touching it**. Never
 *    sunk into it, never floating above it. That contact is the whole idea:
 *    the meed, held well, come to rest.
 *  - The cradle is a **stroke**, the coin is a **fill**. The pairing is what
 *    keeps the mark two shapes an eight year old could redraw.
 *  - The cradle's arc is shallow, its ends level with each other, stopping
 *    short of the frame so the arc breathes.
 *  - Nothing else is in frame.
 *  - Flat. No gradients, no glow, no dimension. A three dimensional treatment
 *    and the Siob era circle-on-flat-line construction were both retired.
 *
 * The cradle stroke scales with the mark: hairline at favicon sizes, deliberate
 * at icon sizes, never heavier than the coin's radius reads.
 *
 * **This is never used as a placeholder for missing album art.** The mark's one
 * job is to never be confused with artwork, and borrowing it for a cover would
 * undo that. See `MissingCover` for what is drawn instead.
 */
@Composable
fun MeedwellMark(
    size: Dp,
    modifier: Modifier = Modifier,
    markColor: Color = Copper,
    fieldColor: Color = MarkField,
    /** The bare form drops the rounded square frame, for inline lockups. */
    bare: Boolean = false,
    contentDescription: String? = null,
) {
    val shaped = if (bare) modifier else modifier.clip(RoundedCornerShape(percent = 22))
    Canvas(
        modifier = shaped
            .size(size)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else Modifier
            )
    ) {
        val w = this.size.width
        val h = this.size.height

        if (!bare) drawRect(color = fieldColor, size = this.size)

        // Proportions read straight off the design reference, as fractions of
        // the frame. Keeping them named makes the construction rules checkable
        // rather than buried in arithmetic.
        val cradleLeft = w * CRADLE_LEFT
        val cradleRight = w * CRADLE_RIGHT
        val cradleEndsY = h * CRADLE_ENDS_Y      // where the arc's ends finish, level
        val cradleLowestY = h * CRADLE_LOWEST_Y  // the arc's lowest point
        val strokeWidth = w * CRADLE_STROKE
        val coinRadius = w * COIN_RADIUS

        // Two quadratics, meeting at the bottom centre. Written this way rather
        // than as an ellipse arc on purpose: the join is exactly the arc's
        // lowest point, so "the coin rests at the cradle's lowest point" is
        // arithmetic rather than a hope about where an arc bottoms out. An
        // earlier version used arcTo with a rect whose lowest point fell below
        // the canvas, and the coin floated. It is easy to get subtly wrong.
        val cradle = Path().apply {
            moveTo(cradleLeft, cradleEndsY)
            quadraticTo(cradleLeft, cradleLowestY, w / 2f, cradleLowestY)
            quadraticTo(cradleRight, cradleLowestY, cradleRight, cradleEndsY)
        }
        drawPath(
            path = cradle,
            color = markColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )

        // The coin. Its lower edge touches the inside of the cradle stroke at
        // the cradle's lowest point. Touching, never sunk, never floating: this
        // one line is the mark's whole argument.
        val coinCenterY = cradleLowestY - (strokeWidth / 2f) - coinRadius
        drawCircle(
            color = markColor,
            radius = coinRadius,
            center = Offset(w / 2f, coinCenterY),
        )
    }
}

/** The wordmark lockup: mark plus "Meedwell" in Instrument Sans 700, tight tracking. */
val MarkInlineSize: Dp = 26.dp

/**
 * The mark's geometry, as fractions of the frame, taken from the design
 * reference. Shared with `res/drawable/ic_launcher_foreground.xml`, which draws
 * the same two shapes for the launcher and must be changed with these.
 *
 * The relationship that has to hold: the coin's lower edge and the cradle
 * stroke's inner edge meet exactly at the arc's lowest point, which is
 *
 *   COIN_RADIUS * 2 + CRADLE_STROKE / 2 == CRADLE_LOWEST_Y - coinTop
 *
 * There is a unit test that asserts the touch point rather than trusting these
 * numbers to stay consistent through a later tweak.
 */
internal const val CRADLE_LEFT = 0.17f
internal const val CRADLE_RIGHT = 0.83f
internal const val CRADLE_ENDS_Y = 0.52f
internal const val CRADLE_LOWEST_Y = 0.78f
internal const val CRADLE_STROKE = 0.027f
internal const val COIN_RADIUS = 0.13f
