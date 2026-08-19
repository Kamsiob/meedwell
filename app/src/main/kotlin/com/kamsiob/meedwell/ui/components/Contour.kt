package com.kamsiob.meedwell.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.kamsiob.meedwell.ui.theme.Motion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import kotlin.math.abs
import kotlin.math.sin

/**
 * The scrubber: a contour line drawn on a five-line staff.
 *
 * **This is not a progress bar.** It reads as a phrase moving through a score,
 * which is the single most identifying thing on the player and the reason the
 * screen does not look like every other music app. From the grid:
 *
 *  - five staff lines behind it, the same staff used at section heads
 *  - the played portion in **moss**
 *  - the remainder at **24% ink**
 *  - a **4.6px dot** at the current position
 *
 * The line itself is deterministic rather than random: the same track draws the
 * same contour every time it is opened. A shape that changed on every glance
 * would be decoration, and this one is a position you can point at.
 *
 * It is deliberately **not** a waveform. A waveform is a claim about the audio,
 * and drawing a made-up one over somebody's record would be a small lie told
 * very confidently. A contour claims nothing except where you are.
 */
@Composable
fun ContourScrubber(
    progress: Float,
    /** Seeds the shape, so one piece always draws the same phrase. */
    seed: String,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    positionLabel: String? = null,
    durationLabel: String? = null,
) {
    val colors = MeedwellTheme.colors
    val clamped = progress.coerceIn(0f, 1f)
    var width by remember { mutableFloatStateOf(1f) }

    val points = remember(seed) { contourPoints(seed) }
    val haptics = LocalHapticFeedback.current
    val reduced = MeedwellTheme.reducedMotion

    // **The phrase writes itself onto the staff.** When a new piece arrives the
    // staff rules out left to right and the contour is drawn along it, whole,
    // in remainder ink, before the played and unplayed halves take over. Keyed
    // on the seed and never on progress, so it happens once per piece and the
    // scrubber never replays it mid listen. The same honesty argument as the
    // contour itself: this draws only what is true, in the order a hand would.
    val written = remember(seed) { androidx.compose.animation.core.Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(seed) {
        if (!reduced && written.value < 1f) {
            written.animateTo(1f, tween(360, easing = Motion.Rule))
        }
    }

    // **The phrase answers the finger.** While a finger is down the dot swells
    // and the whole remainder lifts a step, so the phrase brightens under the
    // hand; on release the mark settles back and a single tick confirms the
    // seek landed. This is the app's signature control acknowledging the
    // pointing, not a tooltip.
    var touching by remember { mutableStateOf(false) }
    val dotRadius by animateFloatAsState(
        targetValue = if (touching && !reduced) 3.1f else 2.3f,
        animationSpec = tween(if (touching) 80 else 140, easing = Motion.Settle),
        label = "contour dot",
    )
    val remainderAlpha by animateFloatAsState(
        targetValue = if (touching) 0.34f else 0.24f,
        animationSpec = tween(100, easing = Motion.Settle),
        label = "contour remainder",
    )

    Canvas(
        modifier
            .fillMaxWidth()
            .height(CONTOUR_HEIGHT)
            .pointerInput(seed) {
                detectTapGestures(
                    onPress = {
                        touching = true
                        tryAwaitRelease()
                        touching = false
                    },
                ) { offset -> onSeek((offset.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(seed) {
                detectHorizontalDragGestures(
                    onDragStart = { touching = true },
                    onDragEnd = {
                        touching = false
                        // The mark landing, felt once.
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDragCancel = { touching = false },
                ) { change, _ ->
                    onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
            .semantics {
                contentDescription = when {
                    positionLabel != null && durationLabel != null ->
                        "Position, $positionLabel of $durationLabel. Drag to move through the piece."
                    else -> "Position in the piece. Drag to move."
                }
                progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f)
            }
    ) {
        width = size.width
        val line = 1.dp.toPx()
        val reveal = written.value

        // The staff, five lines evenly through the height, middle one heavier,
        // ruled left to right slightly ahead of the phrase being written on it.
        val staffTop = size.height * 0.12f
        val staffGap = (size.height * 0.76f) / 4f
        val staffReach = size.width * (reveal * 1.35f).coerceAtMost(1f)
        repeat(5) { index ->
            val y = staffTop + index * staffGap
            drawLine(
                color = if (index == 2) colors.hairline2 else colors.hairline,
                start = Offset(0f, y),
                end = Offset(staffReach, y),
                strokeWidth = line,
            )
        }

        // The contour, split at the position so the two halves take their own
        // colors. Split by rebuilding rather than by clipping, so the join is a
        // real point on the curve and the dot sits exactly on it.
        val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)

        if (reveal < 1f) {
            // Still being written: the whole phrase in remainder ink, as far as
            // the pen has got. No split and no dot until it is on the page.
            drawPath(
                path = contourPath(points, from = 0f, to = reveal, width = size.width, height = size.height),
                color = colors.primaryText.copy(alpha = 0.24f),
                style = stroke,
            )
            return@Canvas
        }

        val cut = clamped

        drawPath(
            path = contourPath(points, from = 0f, to = cut, width = size.width, height = size.height),
            color = colors.moss,
            style = stroke,
        )
        drawPath(
            path = contourPath(points, from = cut, to = 1f, width = size.width, height = size.height),
            // `rgba(28,36,32,.24)` at rest, lifted a step under a finger.
            color = colors.primaryText.copy(alpha = remainderAlpha),
            style = stroke,
        )

        // **The ink is still wet at the nib.** The last stretch before the dot
        // is drawn a little heavier, tapering back, so the phrase reads as
        // being written at the pace of the music. Not an animation: it moves
        // only because the position moves, and is perfectly still when paused,
        // so reduced motion has nothing here to remove.
        if (cut > 0.001f) {
            drawPath(
                path = contourPath(
                    points,
                    from = (cut - 0.03f).coerceAtLeast(0f),
                    to = cut,
                    width = size.width,
                    height = size.height,
                ),
                color = colors.moss,
                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        val dotY = contourYAt(points, cut) * size.height
        drawCircle(
            color = colors.moss,
            // 4.6dp is the dot's diameter per the grid. The old arithmetic
            // cancelled itself and shipped the diameter as the radius, so the
            // player's most prominent mark drew at twice its spec.
            radius = dotRadius.dp.toPx(),
            center = Offset(size.width * cut, dotY),
        )
    }
}

/** The grid draws the contour in a 64px band. */
val CONTOUR_HEIGHT: Dp = 64.dp

/**
 * The control points of one piece's contour.
 *
 * Deterministic from the seed, so the same track always draws the same phrase.
 * The values are kept well inside the staff, between 0.18 and 0.82 of the
 * height, so the line never touches the outer staff lines and never reads as
 * clipped.
 */
private fun contourPoints(seed: String, count: Int = 9): FloatArray {
    var hash = seed.fold(0L) { acc, c -> acc * 31 + c.code }
    if (hash == 0L) hash = 1
    return FloatArray(count) { index ->
        hash = hash * 6364136223846793005L + 1442695040888963407L
        val unit = abs(sin(hash.toDouble() * 0.000000001 + index)).toFloat()
        0.22f + unit * 0.56f
    }
}

/** The contour's height at a fraction along, in 0 to 1 of the band. */
private fun contourYAt(points: FloatArray, t: Float): Float {
    if (points.isEmpty()) return 0.5f
    val span = (points.size - 1).toFloat()
    val at = (t.coerceIn(0f, 1f) * span)
    val i = at.toInt().coerceIn(0, points.size - 1)
    val j = (i + 1).coerceAtMost(points.size - 1)
    val local = at - i
    // Smoothstep between control points, which is what makes it a curve rather
    // than a saw.
    val eased = local * local * (3f - 2f * local)
    return points[i] + (points[j] - points[i]) * eased
}

/** A slice of the contour as a path, in pixels. */
private fun contourPath(points: FloatArray, from: Float, to: Float, width: Float, height: Float): Path {
    val path = Path()
    if (to <= from) return path
    val steps = 64
    for (step in 0..steps) {
        val t = from + (to - from) * (step / steps.toFloat())
        val x = width * t
        val y = contourYAt(points, t) * height
        if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    return path
}
