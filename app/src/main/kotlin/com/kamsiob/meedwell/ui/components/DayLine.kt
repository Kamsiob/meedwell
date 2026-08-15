package com.kamsiob.meedwell.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import java.util.Calendar

/**
 * The day line, under the Shelf title.
 *
 * A hairline horizon with three ticks and a **copper sun at the actual time of
 * day**. It is the one other place copper is allowed, because it is literally a
 * sun.
 *
 * ```
 * .dayline{position:relative;height:26px;}
 * .dayline .ln{...top:19px;height:1.5px;background:var(--hair-2);}
 * .dayline .tk{...top:19px;width:1px;height:5px;}
 * .dayline .sun{...width:12px;height:12px;border-radius:50%;background:var(--copper);}
 * ```
 *
 * The grid puts the sun at 26 percent for 9:41, which is that time measured
 * against a dawn-to-dusk span rather than against midnight. So the span here is
 * 6am to 9pm: at 9:41 the sun sits at 24.6 percent, near enough the grid's
 * figure to be the same rule.
 *
 * **In the evening the sun sets past the right end**, which is the grid's
 * Lamplight screen. That is not a bug to clamp away: it is the whole idea. The
 * line is a day, and after dusk the day is over.
 */
@Composable
fun DayLine(
    modifier: Modifier = Modifier,
    /** Minutes since midnight. Defaults to now. */
    minuteOfDay: Int = currentMinuteOfDay(),
) {
    val colors = MeedwellTheme.colors
    val fraction = dayFraction(minuteOfDay)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(26.dp)
            .semantics { contentDescription = describe(minuteOfDay) }
    ) {
        val lineY = 19.dp.toPx()
        val tick = 5.dp.toPx()

        drawLine(
            color = colors.hairline2,
            start = Offset(0f, lineY),
            end = Offset(size.width, lineY),
            strokeWidth = 1.5.dp.toPx(),
        )
        listOf(0f, 0.5f, 1f).forEach { at ->
            val x = (size.width * at).coerceIn(0.5f, size.width - 0.5f)
            drawLine(
                color = colors.hairline2,
                start = Offset(x, lineY),
                end = Offset(x, lineY + tick),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val radius = 6.dp.toPx()
        drawCircle(
            // The sun is copper by day. On Lamplight the grid draws it in
            // `--lamp-2` rather than copper, because a copper dot on deep pine
            // reads as a warning light rather than as the sun.
            color = if (colors.isDark) colors.secondaryText else colors.copper,
            radius = radius,
            center = Offset(size.width * fraction, lineY - radius / 2f),
        )
    }
}

/**
 * Where the sun sits, as a fraction of the line.
 *
 * Dawn at 6am is 0 and dusk at 9pm is 1. Past dusk the value goes above 1 and
 * the sun is drawn off the right end, which is what the grid shows at night.
 * Before dawn it goes below 0 and sits off the left, for the same reason.
 */
fun dayFraction(minuteOfDay: Int): Float {
    val dawn = 6 * 60f
    val dusk = 21 * 60f
    return (minuteOfDay - dawn) / (dusk - dawn)
}

private fun currentMinuteOfDay(): Int = Calendar.getInstance().let {
    it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
}

/**
 * The line in words, for a screen reader.
 *
 * A horizon with a dot on it is meaningless read out literally, so this says
 * what it means instead.
 */
private fun describe(minuteOfDay: Int): String = when {
    minuteOfDay < 6 * 60 -> "Before dawn"
    minuteOfDay < 12 * 60 -> "Morning"
    minuteOfDay < 17 * 60 -> "Afternoon"
    minuteOfDay < 21 * 60 -> "Evening"
    else -> "After dusk"
}
