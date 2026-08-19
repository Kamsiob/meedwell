package com.kamsiob.meedwell.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import kotlin.math.sin

/**
 * Pull to refresh, played on the mark itself.
 *
 * The mark is a copper coin at rest in an open cradle, and the whole idea of it
 * is that contact: the meed, held well, come to rest. So the gesture borrows the
 * metaphor rather than bolting a spinner onto it.
 *
 *  - **Pulling lifts the coin out of the cradle.** The further you pull, the
 *    higher it rises, and it keeps rising a little past the trigger point so the
 *    gesture never feels like it has hit a wall.
 *  - **Letting go drops it back in, and it rocks until it settles.** While the
 *    sync is running the coin rolls from side to side along the inside of the
 *    cradle, the way a coin does in a bowl before it lies flat.
 *
 * The rocking traces the **real cradle curve** rather than a decorative wobble:
 * the coin's height at any point is computed from the same parabola the mark is
 * drawn with, so it rides the bowl instead of floating over it. Sliding it along
 * a straight line would read as a coin skating on ice.
 *
 * The cradle never moves. Only the coin does, which keeps the mark recognisable
 * at every frame of the animation.
 */
@Composable
fun MarkRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val copper = MeedwellTheme.colors.copper

    // One full rock, left to right and back. Slow enough to read as settling
    // rather than as a busy-spinner in disguise.
    val rock = rememberInfiniteTransition(label = "coin-rock")
    val phase by rock.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "coin-phase",
    )

    val pull = state.distanceFraction.coerceIn(0f, 1.4f)

    Canvas(
        modifier
            // Travels down with the finger, and parks just below the status bar
            // while the sync runs. Done here rather than with the Material
            // indicator modifier, which this version does not expose.
            .offset {
                val travel = if (isRefreshing) 1f else pull.coerceAtMost(1f)
                IntOffset(0, (travel * TRAVEL.toPx()).toInt())
            }
            .size(INDICATOR_SIZE)
            // The gesture and its result are both already announced by the
            // shelf. A drawing of a coin has nothing to add to that.
            .clearAndSetSemantics {}
    ) {
        val w = size.width
        val h = size.height

        val cradleLeft = w * CRADLE_LEFT
        val cradleRight = w * CRADLE_RIGHT
        val cradleEndsY = h * CRADLE_ENDS_Y
        val cradleLowestY = h * CRADLE_LOWEST_Y
        val strokeWidth = w * CRADLE_STROKE
        val coinRadius = w * COIN_RADIUS

        // One alpha for the whole mark, cradle included.
        //
        // The cradle used to be drawn unconditionally, so once a sync finished
        // it stayed on screen as a bare copper arc under the title with no coin
        // in it: a piece of the logo, orphaned, that looked like a rendering
        // fault. Nothing is drawn at rest now.
        val alpha = if (isRefreshing) 1f else (pull * 1.6f).coerceIn(0f, 1f)
        if (alpha <= 0f) return@Canvas

        val cradle = Path().apply {
            moveTo(cradleLeft, cradleEndsY)
            quadraticTo(cradleLeft, cradleLowestY, w / 2f, cradleLowestY)
            quadraticTo(cradleRight, cradleLowestY, cradleRight, cradleEndsY)
        }
        drawPath(
            path = cradle,
            color = copper,
            alpha = alpha,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )

        // Where the coin sits when it is home, touching the cradle's inside.
        val restY = cradleLowestY - (strokeWidth / 2f) - coinRadius
        val halfSpan = cradleRight - w / 2f
        val rise = cradleLowestY - cradleEndsY

        val centre: Offset = if (isRefreshing) {
            // Rolling. Kept well inside the rim so the coin never looks like
            // it is about to escape the cradle.
            val dx = halfSpan * 0.46f * sin(phase * 2f * Math.PI.toFloat())
            val bowlY = cradleLowestY - rise * (dx / halfSpan) * (dx / halfSpan)
            Offset(w / 2f + dx, bowlY - (strokeWidth / 2f) - coinRadius)
        } else {
            // Lifting. Straight up out of the cradle, by up to a little more
            // than its own diameter at full pull.
            Offset(w / 2f, restY - pull * coinRadius * 2.4f)
        }

        drawCircle(color = copper, radius = coinRadius, center = centre, alpha = alpha)
    }
}

/**
 * Deliberately larger than the mark reads elsewhere.
 *
 * The coin has to travel to be legible as motion, and the cradle has to stay
 * recognisable while it does. Below about this size the roll reads as a jitter.
 */
private val INDICATOR_SIZE = 46.dp

/** How far the mark rides down from the top edge at full pull. */
private val TRAVEL = 72.dp
