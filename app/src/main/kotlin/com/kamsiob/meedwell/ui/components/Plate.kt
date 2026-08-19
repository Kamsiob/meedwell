package com.kamsiob.meedwell.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * The engraved plate of seed heads.
 *
 * Three stems on a ground line, drawn the way a botanical plate in an old field
 * guide is drawn: one stroke weight, no shading, no fill except the seed at the
 * tip of each stem.
 *
 * **It carries no information and that is the job.** The grid puts it at rest
 * points, where a screen has finished saying its piece and the alternative is
 * blank paper. It sets the register before a single album loads: this is an app
 * about patience, and the drawing says so faster than another sentence would.
 *
 * Paths, viewBox and stroke weight are lifted from screen 01 of the grid
 * unchanged. Drawn rather than shipped as an asset so it takes its color from
 * the theme and stays a hairline in both.
 */
@Composable
fun SeedHeadPlate(modifier: Modifier = Modifier) {
    val stroked = remember { PLATE_PATHS.map { PathParser().parsePathString(it).toPath() } }
    val seeds = remember { PathParser().parsePathString(PLATE_SEEDS).toPath() }
    val ink = MeedwellTheme.colors.tertiaryText

    Canvas(
        modifier
            .width(PLATE_WIDTH)
            .height(PLATE_HEIGHT)
            // A drawing with nothing to read. Announcing it would be reading
            // out the paper rather than the words on it.
            .clearAndSetSemantics {}
    ) {
        val factor = size.width / VIEW_WIDTH
        scale(scale = factor, pivot = Offset.Zero) {
            stroked.forEach { path ->
                drawPath(
                    path = path,
                    color = ink,
                    alpha = 0.85f,
                    style = Stroke(width = 1.05f, cap = StrokeCap.Round),
                )
            }
            drawPath(path = seeds, color = ink, alpha = 0.85f)
        }
    }
}

private const val VIEW_WIDTH = 178f
private val PLATE_WIDTH = 178.dp
private val PLATE_HEIGHT = 84.dp

/** The three stems and the ground line they stand on. */
private val PLATE_PATHS = listOf(
    "M89,84 C89,55 87,34 83,12 M83,12 C78,22 73,27 66,30 M83,12 C87,24 93,30 100,33 " +
        "M85,38 C79,43 72,45 65,45 M86,52 C93,54 100,54 106,51",
    "M45,84 C45,64 42,49 37,36 M37,36 C33,42 28,46 22,47 M38,51 C43,55 49,56 54,55",
    "M131,84 C131,67 134,52 139,41 M139,41 C143,46 149,49 155,49 M136,55 C131,59 125,60 120,59",
    "M8,84 L170,84",
)

/**
 * The seed at the tip of each stem, as filled circles.
 *
 * Written as path data rather than three `drawCircle` calls so the whole plate
 * scales through one transform and the seeds cannot drift off their stems.
 */
private const val PLATE_SEEDS =
    "M83,8 m-2.3,0 a2.3,2.3 0 1,0 4.6,0 a2.3,2.3 0 1,0 -4.6,0 " +
        "M37,32 m-1.9,0 a1.9,1.9 0 1,0 3.8,0 a1.9,1.9 0 1,0 -3.8,0 " +
        "M139,37 m-1.9,0 a1.9,1.9 0 1,0 3.8,0 a1.9,1.9 0 1,0 -3.8,0"


/**
 * The engraver's closing mark: an arc over a single stem.
 *
 * Grid screen 07 sets it after an album's last movement and captions it "The
 * engraved sprig closes the sheet the way a printed score closes a page." It is
 * how a printed page says finished rather than cut off, and it goes only at
 * real rest points: the album's end and the queue's foot. Never on the player,
 * per the recorded decision that its page closes with nothing.
 */
@Composable
fun ClosingSprig(modifier: Modifier = Modifier) {
    val ink = MeedwellTheme.colors.tertiaryText
    androidx.compose.foundation.Canvas(
        modifier
            .size(width = 108.dp, height = 26.dp)
            .clearAndSetSemantics {}
    ) {
        val sx = size.width / 108f
        val sy = size.height / 26f
        val stroke = Stroke(width = 1.05.dp.toPx(), cap = StrokeCap.Round)
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
        val path = Path().apply {
            moveTo(p(6f, 21f).x, p(6f, 21f).y)
            cubicTo(p(29f, 8f).x, p(29f, 8f).y, p(79f, 8f).x, p(79f, 8f).y, p(102f, 21f).x, p(102f, 21f).y)
            moveTo(p(54f, 13f).x, p(54f, 13f).y)
            cubicTo(p(54f, 9f).x, p(54f, 9f).y, p(54f, 6f).x, p(54f, 6f).y, p(54f, 3f).x, p(54f, 3f).y)
            moveTo(p(54f, 3f).x, p(54f, 3f).y)
            cubicTo(p(50f, 6f).x, p(50f, 6f).y, p(46f, 7f).x, p(46f, 7f).y, p(42f, 7f).x, p(42f, 7f).y)
            moveTo(p(54f, 3f).x, p(54f, 3f).y)
            cubicTo(p(58f, 6f).x, p(58f, 6f).y, p(62f, 7f).x, p(62f, 7f).y, p(66f, 7f).x, p(66f, 7f).y)
        }
        drawPath(path, color = ink.copy(alpha = 0.8f), style = stroke)
    }
}
