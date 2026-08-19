package com.kamsiob.meedwell.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import com.kamsiob.meedwell.ui.theme.Motion
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import kotlin.math.abs
import kotlin.math.sin

/**
 * The parts every Surroundings surface is built from.
 *
 * **One vocabulary, four surfaces.** The library screen, the player's facing
 * page, the bed card on the music page and the floating card used to share
 * almost nothing: three different sliders with three different track heights, a
 * badge here and a sun there, two different row shapes. That is the mechanical
 * reason somebody who designed the app could not use it, because there was
 * nothing to learn once. Everything here is used by all four.
 */

/**
 * Present or absent, playing or stopped, as a sun on a horizon.
 *
 * **Risen means playing. Set means stopped.** The state is carried by the sun's
 * position as well as by its ink, so it survives being small, being glanced at,
 * and being looked at by somebody who does not see color well. Filled means the
 * recording is on this phone; hollow means it is not here yet.
 *
 * Never copper. Copper belongs to the app's mark and to the day line's sun, and
 * borrowing it here would make two different things look like one.
 */
@Composable
fun SunMark(
    here: Boolean,
    playing: Boolean = false,
    current: Boolean = false,
    width: Dp = 18.dp,
    height: Dp = 14.dp,
) {
    val colors = MeedwellTheme.colors
    // **The sign performs its own meaning.** Risen means playing, and the sun
    // now actually rises when play starts and settles back when it stops,
    // slower on the way down because things leave attention more slowly than
    // they arrive in it. Fires only on a state change; otherwise it is still.
    val reduced = MeedwellTheme.reducedMotion
    val lift by animateFloatAsState(
        targetValue = if (playing) 1f else 0f,
        animationSpec = if (reduced) snap() else tween(if (playing) 340 else Motion.enter, easing = Motion.Settle),
        label = "sun lift",
    )
    val ink by animateColorAsState(
        targetValue = when {
            playing || current -> colors.mossInk
            here -> colors.secondaryText
            else -> colors.tertiaryText
        },
        animationSpec = if (reduced) snap() else tween(340, easing = Motion.Settle),
        label = "sun ink",
    )
    Canvas(
        Modifier
            .size(width = width, height = height)
            .clearAndSetSemantics {}
    ) {
        val stroke = if (here) 1.7.dp.toPx() else 1.3.dp.toPx()
        val r = 3.4.dp.toPx()
        val horizonY = size.height - 2.5.dp.toPx()
        val cx = size.width / 2f
        // Risen while it plays, resting on the line when it does not.
        val cy = horizonY - r - lift * 1.5.dp.toPx()

        if (here) {
            drawCircle(color = ink, radius = r, center = Offset(cx, cy))
        } else {
            drawCircle(
                color = ink,
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 1.3.dp.toPx()),
            )
        }
        drawLine(
            color = ink,
            start = Offset(0f, horizonY),
            end = Offset(size.width, horizonY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * The one level control, on every surface that has one.
 *
 * **A sun at one end and a word at the other.** There is no volume control for
 * the music anywhere in this app, so a bare slider on the player was read as one
 * every time. The sun says what this belongs to and the word says where it is
 * set, and the word is the same vocabulary the screen reader has always used, so
 * the two finally agree.
 *
 * Not `pp` to `ff`. The owner did not recognize those marks on his own app, and
 * a notation nobody can read is decoration.
 */
@Composable
fun LevelLine(
    value: Float,
    onChange: (Float) -> Unit,
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors

    // **A small speaker at each end, and that is all.** The line wore a sun at
    // one end and the level spelled out in a word at the other, and the owner's
    // verdict was that a volume control is not that complicated. He is right:
    // two speakers say it to anybody, in any language. The word survives for
    // the screen reader, where it is the only honest way to say a level aloud.
    Row(
        modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MeedwellIcon(MeedwellIcons.VolumeLow, size = 13.dp, tint = colors.tertiaryText)
        Box(
            Modifier
                .weight(1f)
                .padding(start = 10.dp, end = 10.dp)
                .height(48.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onChange((offset.x / size.width).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        onChange((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                }
                .semantics {
                    contentDescription = "Surroundings level, ${plainVolume(value)}"
                    progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.hairline)
            )
            Box(
                Modifier
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.moss)
            )
            // The knob, kept whole at either end by measuring the track.
            val density = LocalDensity.current
            BoxKnob(value = value, color = colors.moss, density = density)
        }
        MeedwellIcon(MeedwellIcons.VolumeHigh, size = 15.dp, tint = colors.tertiaryText)
    }
}

/** The moss dot at the fill's end, inset so it never half-leaves the track. */
@Composable
private fun BoxKnob(value: Float, color: Color, density: androidx.compose.ui.unit.Density) {
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth(value.coerceIn(0f, 1f))
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 6.dp)
                    .size(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

/**
 * How much of a group is already on this phone, as a hairline gauge.
 *
 * A rule with a moss overstroke, not a filled bar and not a ring. It gives nine
 * otherwise identical group rows a shape to scan, and the words beside it say
 * the same fact, so color is never carrying meaning alone.
 */
@Composable
fun HoldLine(here: Int, total: Int, modifier: Modifier = Modifier) {
    val colors = MeedwellTheme.colors
    val fraction = if (total <= 0) 0f else (here.toFloat() / total).coerceIn(0f, 1f)
    Canvas(
        modifier
            .size(width = 44.dp, height = 8.dp)
            .clearAndSetSemantics {}
    ) {
        val y = size.height / 2f
        drawLine(
            color = colors.hairline,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx(),
        )
        if (fraction > 0f) {
            drawLine(
                color = colors.moss,
                start = Offset(0f, y),
                end = Offset(size.width * fraction, y),
                strokeWidth = 1.7.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * An engraved diagram of the place a recording came from.
 *
 * **These recordings have no artwork, and inventing some would be a lie.** The
 * player's facing page carried a filled gray rectangle with nothing in it, which
 * is a card containing an absence. A generic player would put a stock photograph
 * of rain here. This draws what you are about to hear as a printed figure: a
 * horizon, a sun, and a field of strokes chosen by what kind of place it is.
 *
 * Deterministic, seeded from the recording's own id, so one recording always
 * draws the same plate and becomes recognizable. Same argument as the contour
 * scrubber: a shape that changed on every glance would be decoration.
 */
@Composable
fun FieldPlate(
    seed: String,
    group: String,
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val hash = abs(seed.hashCode())

    Canvas(
        modifier
            .fillMaxWidth()
            .height(132.dp)
            .clearAndSetSemantics {}
    ) {
        val w = size.width
        val h = size.height
        val horizonY = h * 0.62f
        val hair = colors.hairline
        val hair2 = colors.hairline2

        drawLine(
            color = hair2,
            start = Offset(0f, horizonY),
            end = Offset(w, horizonY),
            strokeWidth = 1.5.dp.toPx(),
        )

        // The sun, risen while it plays.
        val sunX = w * (0.34f + 0.17f * (hash % 3))
        val sunR = 9.dp.toPx()
        drawCircle(
            color = if (playing) colors.moss else hair2,
            radius = sunR,
            center = Offset(sunX, horizonY - sunR - if (playing) 10.dp.toPx() else 2.dp.toPx()),
            style = Stroke(width = 1.4.dp.toPx()),
        )

        val key = group.lowercase()
        val thin = 1.dp.toPx()

        fun contours(count: Int, spacing: Float, amp: Float) {
            for (i in 0 until count) {
                val y = horizonY + spacing * (i + 1)
                if (y > h) break
                val span = w * (0.6f + 0.4f * ((hash + i) % 5) / 5f)
                val left = (w - span) / 2f
                var x = left
                var prev = Offset(x, y)
                while (x < left + span) {
                    x += 6f
                    val next = Offset(x, y + amp * sin((x + hash % 40) / 26f))
                    drawLine(hair, prev, next, strokeWidth = thin)
                    prev = next
                }
            }
        }

        fun uprights(count: Int, minH: Float, maxH: Float, tick: Boolean, ink: Color = hair) {
            for (i in 0 until count) {
                val x = w * (i + 0.5f) / count
                val tall = minH + ((hash + i * 37) % 100) / 100f * (maxH - minH)
                drawLine(ink, Offset(x, h), Offset(x, h - tall), strokeWidth = thin)
                if (tick) {
                    drawLine(
                        ink,
                        Offset(x - 3.dp.toPx(), h - tall + 5.dp.toPx()),
                        Offset(x + 3.dp.toPx(), h - tall + 5.dp.toPx()),
                        strokeWidth = thin,
                    )
                }
            }
        }

        when {
            key.contains("rain") || key.contains("water") || key.contains("weather") ->
                contours(7, 9.dp.toPx(), 3.dp.toPx())

            key.contains("rainforest") || key.contains("jungle") ->
                uprights(11, 20.dp.toPx(), 58.dp.toPx(), tick = true)

            key.contains("river") || key.contains("lake") || key.contains("sea") ->
                contours(6, 11.dp.toPx(), 4.dp.toPx())

            key.contains("wind") || key.contains("air") -> {
                for (i in 0 until 5) {
                    val y = horizonY - 12.dp.toPx() * (i + 1)
                    var x = 0f
                    var prev = Offset(x, y)
                    while (x < w) {
                        x += 7f
                        val next = Offset(x, y + 8.dp.toPx() * sin((x + i * 30f) / 34f))
                        drawLine(hair, prev, next, strokeWidth = thin)
                        prev = next
                    }
                }
            }

            key.contains("fire") -> uprights(9, 10.dp.toPx(), 26.dp.toPx(), tick = false, ink = hair2)

            key.contains("forest") || key.contains("countryside") ->
                uprights(13, 16.dp.toPx(), 44.dp.toPx(), tick = false)

            key.contains("room") || key.contains("people") || key.contains("human") -> {
                for (i in 0 until 24) {
                    val x = w * ((hash + i * 61) % 100) / 100f
                    val y = horizonY + (h - horizonY) * ((hash + i * 29) % 100) / 100f
                    drawCircle(hair, radius = 1.2.dp.toPx(), center = Offset(x, y))
                }
            }

            key.contains("train") || key.contains("boat") || key.contains("plane") ||
                key.contains("transit") -> {
                val y = h * 0.82f
                drawLine(hair, Offset(0f, y), Offset(w, y), strokeWidth = 1.5.dp.toPx())
                for (i in 0 until 16) {
                    val x = w * (i + 0.5f) / 16
                    drawLine(
                        hair,
                        Offset(x, y - 4.dp.toPx()),
                        Offset(x, y + 4.dp.toPx()),
                        strokeWidth = thin,
                    )
                }
            }

            key.contains("machine") || key.contains("hum") || key.contains("mechanical") -> {
                for (i in 0 until 18) {
                    val x = w * (i + 0.5f) / 18
                    drawLine(
                        hair,
                        Offset(x, horizonY),
                        Offset(x, horizonY - 18.dp.toPx()),
                        strokeWidth = thin,
                    )
                }
            }

            else -> contours(7, 9.dp.toPx(), 3.dp.toPx())
        }
    }
}

/**
 * The level in words, which is what the screen shows and what TalkBack reads.
 *
 * One vocabulary, so the two can never drift.
 */
fun plainVolume(value: Float): String = when {
    value <= 0.01f -> "silent"
    value < 0.2f -> "very quiet"
    value < 0.4f -> "quiet"
    value < 0.6f -> "middling"
    value < 0.8f -> "loud"
    else -> "very loud"
}
