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
            drawLine(
                color = if (index == 2) colors.hairline2 else colors.hairline,
                start = Offset(0f, y),
                end = Offset(size.width, y),
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
