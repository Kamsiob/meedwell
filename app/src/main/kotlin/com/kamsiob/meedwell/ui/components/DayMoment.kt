package com.kamsiob.meedwell.ui.components

import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import java.util.Calendar
import java.util.Date
import kotlin.math.cos
import kotlin.math.sin

/**
 * What the day line says when you touch its sun.
 *
 * A small panel: the mark for the hour, the time, a word for where in the day
 * you are, and the span the line is drawn across. Nothing else. It is a caption
 * for a drawing rather than a weather widget, and the moment it starts listing
 * things it stops being elegant.
 *
 * ## The moon
 *
 * `DESIGN.md` says there is no moon in this app, and that rule was written for
 * the sleep timer, where a moon would have meant "night" for something that is
 * about rest at any hour. **Here the rule is deliberately reversed**, at the
 * owner's instruction, because this drawing is about the time of day and nothing
 * else: after dusk a sun would simply be wrong. The sleep timer keeps its
 * fermata and gains no moon.
 *
 * ## What it does not claim
 *
 * It says **dawn** and **dusk**, not sunrise and sunset, and the difference is
 * not pedantry. Real solar times need a latitude, and this app declares that it
 * asks for no location, ever. So the panel reports the only thing it honestly
 * knows: the span the line itself is drawn across, six in the morning to nine at
 * night. Saying "sunrise" over a number that is the same in Reykjavik and
 * Nairobi would be the one dishonest sentence in the app.
 */
@Composable
fun DayMomentPanel(
    minuteOfDay: Int,
    span: DaySpan = DaySpan.Default,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    val context = LocalContext.current
    val night = isNight(minuteOfDay, span)

    // Rises and fades, once, and not at all under reduce-motion.
    val entered by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(if (MeedwellTheme.reducedMotion) 0 else 260),
        label = "day-moment",
    )

    Column(
        modifier
            .width(232.dp)
            .graphicsLayer {
                alpha = entered
                translationY = (1f - entered) * 10.dp.toPx()
            }
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp),
                clip = false,
                ambientColor = if (colors.isDark) Color(0x99000000) else Color(0x451C2420),
                spotColor = if (colors.isDark) Color(0x99000000) else Color(0x451C2420),
            )
            .clip(RoundedCornerShape(20.dp))
            .background(colors.background)
            .border(
                1.dp,
                if (colors.isDark) Color(0x29EFEEE6) else Color(0x291C2420),
                RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The hour's own mark, drawn rather than set in a typeface, so it
        // carries the same hand as the staff and the cradle.
        if (night) {
            MoonGlyph(size = 34.dp, ink = colors.secondaryText)
        } else {
            SunGlyph(size = 34.dp, ink = colors.copper)
        }

        Text(
            text = clockLabel(context, minuteOfDay),
            style = type.numeric.copy(fontSize = 27.sp),
            color = colors.primaryText,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = partOfDay(minuteOfDay, span),
            style = type.voice,
            color = colors.secondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )

        // The line in miniature, with the sun where it actually is. Repeating
        // the drawing rather than describing it is what ties the panel to the
        // thing that was touched.
        MiniDayLine(
            fraction = dayFraction(minuteOfDay),
            night = night,
            modifier = Modifier.padding(top = 16.dp),
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // The same clock the time above is on. These read 6:00 and 21:00
            // while the hour above them read 8:03 PM, which is two clocks in
            // one panel and the sort of thing that looks like a bug even when
            // nobody can say why.
            Text(
                "DAWN ${clockLabel(context, span.usable.dawnMinute)}",
                style = type.plate,
                color = colors.tertiaryText,
            )
            Text(
                "DUSK ${clockLabel(context, span.usable.duskMinute)}",
                style = type.plate,
                color = colors.tertiaryText,
            )
        }

        Text(
            // Three short sentences, not a lecture. The first says where the
            // time came from, the second says what was not taken to get it, and
            // the third says it is yours to change. Anything longer turns a
            // caption into a policy page.
            "Based on your phone's settings. No location is used. " +
                "Change dawn and dusk any time in Settings.",
            style = type.meta,
            color = colors.tertiaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

/** The sun: a disc with short rays, in the manner of an engraved plate. */
@Composable
private fun SunGlyph(size: androidx.compose.ui.unit.Dp, ink: Color) {
    Canvas(Modifier.size(size).clearAndSetSemantics {}) {
        val centre = Offset(this.size.width / 2f, this.size.height / 2f)
        val disc = this.size.minDimension * 0.26f
        drawCircle(color = ink, radius = disc, center = centre)

        val inner = disc * 1.5f
        val outer = disc * 2.0f
        val stroke = this.size.minDimension * 0.035f
        repeat(8) { i ->
            val angle = (Math.PI * 2 * i / 8).toFloat()
            val dx = cos(angle)
            val dy = sin(angle)
            drawLine(
                color = ink,
                start = Offset(centre.x + dx * inner, centre.y + dy * inner),
                end = Offset(centre.x + dx * outer, centre.y + dy * outer),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * The moon: one disc with another taken out of it.
 *
 * Carved with a clearing blend inside an offscreen layer rather than by painting
 * a second circle in the background colour. Painting the bite would only look
 * right on one background, and this panel sits on paper in one theme and on
 * near-black in the other.
 */
@Composable
private fun MoonGlyph(size: androidx.compose.ui.unit.Dp, ink: Color) {
    Canvas(
        Modifier
            .size(size)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .clearAndSetSemantics {}
    ) {
        val r = this.size.minDimension * 0.36f
        val centre = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(color = ink, radius = r, center = centre)
        drawCircle(
            color = Color.Black,
            radius = r * 0.86f,
            center = Offset(centre.x + r * 0.52f, centre.y - r * 0.20f),
            blendMode = BlendMode.Clear,
        )
    }
}

/** The day line again, small, inside the panel. */
@Composable
private fun MiniDayLine(
    fraction: Float,
    night: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    Canvas(
        modifier
            .fillMaxWidth()
            .height(14.dp)
            .clearAndSetSemantics {}
    ) {
        val y = size.height * 0.5f
        drawLine(
            color = colors.hairline2,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.5.dp.toPx(),
        )
        listOf(0f, 0.5f, 1f).forEach { at ->
            val x = (size.width * at).coerceIn(0.5f, size.width - 0.5f)
            drawLine(
                color = colors.hairline2,
                start = Offset(x, y),
                end = Offset(x, y + 4.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
        }
        // Inset by its own radius, so midnight at either end of the day still
        // draws a whole dot inside a 232dp panel rather than half of one against
        // the edge.
        val dot = 4.dp.toPx()
        val travel = (size.width - dot * 2f).coerceAtLeast(0f)
        drawCircle(
            color = if (night) colors.secondaryText else colors.copper,
            radius = dot,
            center = Offset(dot + travel * fraction.coerceIn(0f, 1f), y - 2.dp.toPx()),
        )
    }
}

/** Before dawn or after dusk, which is when the mark becomes a moon. */
internal fun isNight(minuteOfDay: Int, span: DaySpan = DaySpan.Default): Boolean {
    val safe = span.usable
    return minuteOfDay < safe.dawnMinute || minuteOfDay >= safe.duskMinute
}

/** The time, in whichever of the two clocks the phone is set to. */
private fun clockLabel(context: android.content.Context, minuteOfDay: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        set(Calendar.MINUTE, minuteOfDay % 60)
    }
    return DateFormat.getTimeFormat(context).format(Date(calendar.timeInMillis))
}

/** Where in the day this is, in a word. */
internal fun partOfDay(minuteOfDay: Int, span: DaySpan = DaySpan.Default): String {
    val safe = span.usable
    // Morning, afternoon and evening are split across the listener's own day
    // rather than at fixed clock hours, so a span of 5am to 11pm still reads
    // sensibly instead of calling half of it "evening".
    val length = (safe.duskMinute - safe.dawnMinute).toFloat()
    val through = (minuteOfDay - safe.dawnMinute) / length
    return when {
        minuteOfDay < safe.dawnMinute -> "Before dawn"
        minuteOfDay >= safe.duskMinute -> "After dusk"
        through < 0.38f -> "Morning"
        through < 0.72f -> "Afternoon"
        else -> "Evening"
    }
}
