package com.kamsiob.meedwell.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.kamsiob.meedwell.ui.theme.Motion
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * The five-line staff.
 *
 * **It appears in exactly three places and nowhere else:**
 *
 *  1. At a section header, running from the label to the right margin.
 *  2. Behind the contour scrubber on the player.
 *  3. Behind the tone curve.
 *
 * There is **no ruled background behind screens**. The first version of this
 * design had a full-bleed manuscript ruling on every screen; it is gone, and a
 * screen with no section header has no staff on it at all.
 *
 * From the grid:
 *
 * ```
 * .staff{display:flex;flex-direction:column;gap:3px;flex:1;}
 * .staff i{display:block;height:1px;background:var(--hair);}
 * .staff i:nth-child(3){background:var(--hair-2);}
 * ```
 *
 * Five lines, 1px each, 3px apart, and the **middle line is heavier**. That
 * middle line is what makes it read as a staff rather than as a stack of rules.
 */
@Composable
fun Staff(modifier: Modifier = Modifier) {
    val colors = MeedwellTheme.colors

    // **The staff is ruled on, not stamped on.** When a section head enters
    // the page its five lines draw left to right on the Rule curve, each a
    // breath behind the one above, the same gesture the contour uses to write
    // itself onto the player. It is the smallest motion in the app and the
    // most repeated, which is exactly why it carries: every screen with a
    // section quietly behaves like a page being set. Reduced motion rules the
    // lines instantly.
    val reduced = MeedwellTheme.reducedMotion
    val ruled = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) ruled.animateTo(1f, tween(360, easing = Motion.Rule))
    }

    Canvas(
        modifier
            .height(STAFF_HEIGHT)
            // Decoration. A screen reader announcing five horizontal lines
            // would be reading out the paper rather than the words on it.
            .clearAndSetSemantics {}
    ) {
        val line = 1.dp.toPx()
        val gap = 3.dp.toPx()
        repeat(5) { index ->
            val y = index * (line + gap) + line / 2
            // The cascade: line n starts a beat after line n-1 and all five
            // finish inside the same 360ms.
            val reach = ((ruled.value * 1.24f) - index * 0.06f).coerceIn(0f, 1f)
            if (reach <= 0f) return@repeat
            drawLine(
                color = if (index == 2) colors.hairline2 else colors.hairline,
                start = Offset(0f, y),
                end = Offset(size.width * reach, y),
                strokeWidth = line,
            )
        }
    }
}

/** Five 1px lines with 3px between them, plus the last line's own height. */
val STAFF_HEIGHT: Dp = 5.dp + 3.dp * 4

/**
 * A section header: a small caps label, then the staff running to the margin.
 *
 * ```
 * .shead{display:flex;align-items:center;gap:11px;}
 * ```
 *
 * The label and the staff share a baseline, and the staff takes whatever width
 * is left. This is the only structural divider in the app: there are no cards
 * to group things into, so a section is a label with a stave after it.
 */
@Composable
fun SectionHead(
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Text(
            label.uppercase(),
            style = MeedwellTheme.typography.section,
            color = MeedwellTheme.colors.tertiaryText,
        )
        Staff(Modifier.weight(1f))
    }
}

/**
 * A section head with no label, which is the staff alone.
 *
 * Used under the wordmark on the declaration screen, where the staff marks the
 * start of a section that does not need naming.
 */
@Composable
fun StaffRule(modifier: Modifier = Modifier) {
    Staff(modifier.fillMaxWidth())
}

/**
 * The plain hairline, `.hr { height:1px; background:var(--hair) }`.
 *
 * Used between rows and to divide groups that are not sections. Everything in
 * this design is either separated by one of these or by whitespace; nothing is
 * separated by a box.
 */
@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color? = null) {
    val stroke = color ?: MeedwellTheme.colors.hairline
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(stroke)
            .clearAndSetSemantics {}
    )
}

/**
 * Room for the widest numeral a programme will print.
 *
 * Roman numerals grow with the count: VIII is four glyphs, XVIII five,
 * XXVIII six. A fixed 30dp column held I through VII and wrapped everything
 * past it, which on a long record broke numerals across two lines. The column
 * is sized once from the highest number in the list, so every row in one
 * programme shares one margin and the titles all start at the same place.
 */
fun numeralColumnWidth(highest: Int): androidx.compose.ui.unit.Dp = when {
    highest >= 28 -> 58.dp
    highest >= 18 -> 50.dp
    highest >= 8 -> 42.dp
    else -> 30.dp
}
