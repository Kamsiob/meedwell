package com.kamsiob.meedwell.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import kotlin.math.abs
import kotlin.math.sin

/**
 * The waveform, which is the scrubber.
 *
 * Played bars burn bright, the remainder sits dim, and dragging anywhere seeks.
 * The mini player's waveform **stills when paused**. Reduced motion gets the
 * static envelope.
 *
 * **Accessibility is the point of most of this file.** `DESIGN.md` section 8
 * says it plainly: this is a custom drag control, it is invisible to TalkBack
 * without explicit seek semantics, and it is the app's signature interaction
 * and therefore its most likely accessibility failure. So it carries a slider
 * role, a spoken position and duration, and increment and decrement actions,
 * rather than being a pretty shape that a screen reader cannot see.
 *
 * The amplitude data itself comes from Meedwell's own decoder through a custom
 * `AudioProcessor` tap, which needs no permissions. **Never the `Visualizer`
 * API**, which would require the microphone. Until that tap is built, this
 * draws a deterministic placeholder envelope derived from the track, which is
 * honest about being a shape rather than pretending to be the music.
 */
@Composable
fun Waveform(
    progress: Float,
    animate: Boolean,
    modifier: Modifier = Modifier,
    /** Real amplitudes when available. Empty means the placeholder envelope. */
    amplitudes: List<Float> = emptyList(),
    barCount: Int = 28,
    onSeek: ((Float) -> Unit)? = null,
    positionLabel: String? = null,
    durationLabel: String? = null,
) {
    val colors = MeedwellTheme.colors
    val reducedMotion = MeedwellTheme.reducedMotion

    val envelope = remember(amplitudes, barCount) {
        if (amplitudes.isNotEmpty()) amplitudes else placeholderEnvelope(barCount)
    }

    // The animation only ever breathes the bars slightly; it never invents
    // amplitude. Stilled when paused and under reduced motion.
    val breathe = if (animate && !reducedMotion) {
        val transition = rememberInfiniteTransition(label = "waveform")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1_100), RepeatMode.Reverse),
            label = "waveform breathe",
        ).value
    } else {
        0.5f
    }

    var widthPx by remember { mutableFloatStateOf(1f) }

    val seekModifier = if (onSeek != null) {
        Modifier
            .pointerInput(Unit) {
                detectTapGestures { offset -> onSeek((offset.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .then(seekModifier)
            .semantics {
                if (onSeek != null) {
                    // A slider role with a spoken position, so TalkBack can both
                    // read and change it. Without this the signature interaction
                    // simply does not exist for a screen reader user.
                    contentDescription = buildString {
                        append("Playback position")
                        if (positionLabel != null && durationLabel != null) {
                            append(", $positionLabel of $durationLabel")
                        }
                    }
                    progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                    setProgress { target ->
                        onSeek(target.coerceIn(0f, 1f))
                        true
                    }
                }
            }
    ) {
        widthPx = size.width
        val gap = 2.5.dp.toPx()
        val barWidth = ((size.width - gap * (envelope.size - 1)) / envelope.size).coerceAtLeast(1f)
        val playedUpTo = size.width * progress.coerceIn(0f, 1f)

        envelope.forEachIndexed { index, amplitude ->
            val x = index * (barWidth + gap)
            // The breathe is a few percent, never enough to misrepresent the
            // shape of the music.
            val breathed = amplitude * (0.94f + 0.12f * breatheFor(index, breathe))
            val height = (size.height * breathed.coerceIn(0.08f, 1f))
            val top = (size.height - height) / 2f

            val played = x + barWidth / 2f <= playedUpTo
            drawRoundRectCompat(
                color = if (played) colors.primaryText else colors.primaryText.copy(alpha = 0.26f),
                topLeft = Offset(x, top),
                size = Size(barWidth, height),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundRectCompat(
    color: Color,
    topLeft: Offset,
    size: Size,
) {
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2f),
    )
}

/** Offsets the breathe per bar so the row moves like a wave rather than as a block. */
private fun breatheFor(index: Int, phase: Float): Float =
    abs(sin((index * 0.6f) + phase * 3.1f))

/**
 * A deterministic placeholder envelope.
 *
 * Deliberately **not** random: a shape that changes every recomposition would
 * be a lie about the music twice over. This is stable for a given bar count, so
 * it reads as an unknown shape rather than as noise. It is replaced by real
 * amplitudes as soon as the decoder tap exists.
 */
private fun placeholderEnvelope(barCount: Int): List<Float> =
    (0 until barCount).map { i ->
        val t = i.toFloat() / barCount
        val body = 0.45f + 0.35f * abs(sin(t * 6.2f))
        val fadeIn = (t * 6f).coerceAtMost(1f)
        val fadeOut = ((1f - t) * 6f).coerceAtMost(1f)
        (body * fadeIn * fadeOut).coerceIn(0.12f, 1f)
    }
