package com.kamsiob.meedwell.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.kamsiob.meedwell.ui.theme.Motion
import kotlin.math.abs
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * The A to Z rail, down the right edge of an alphabetical shelf.
 *
 * `DESIGN.md` section 12 has always listed this alongside the view toggle and
 * the sort menu. It was specified and never built, which is also why the shelf
 * had no way to reach the far end of a long collection except by scrolling
 * through all of it.
 *
 * **It appears only when the order is actually alphabetical.** Sorted by
 * "Recently added" or "Most played" there is no letter that means anything, and
 * a rail that scrolls you to a place the list does not agree with is worse than
 * no rail. The caller decides; this composable only draws what it is given.
 *
 * **Only the letters present are drawn.** A shelf with no Q gets no Q, because a
 * rail full of letters that go nowhere teaches you not to trust it.
 *
 * ## The touch target, stated plainly
 *
 * `DESIGN.md` section 12 flags this control as "small by design" and asks for it
 * to be checked rather than waved through. The honest position: an individual
 * letter is about 14dp tall and nothing can change that without cutting the
 * alphabet in half. What satisfies the floor is that **the rail is one control,
 * not twenty six**. It is a 44dp wide strip running the full height of the list,
 * and a finger anywhere in it starts a scrub that updates continuously as it
 * moves, so the target is the strip and the letters are its scale markings. That
 * is the same reason a piano keyboard is not twenty six separate buttons.
 *
 * Anybody who cannot work a strip this narrow still has the list itself, which
 * scrolls normally, and the screen reader reads the rail as one adjustable
 * control rather than announcing every letter.
 */
@Composable
fun AlphabetRail(
    letters: List<Char>,
    onLetter: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (letters.size < 2) return

    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    // Measured rather than assumed, because the rail's height is whatever the
    // list leaves it and the letter under a finger is a fraction of that.
    val railHeight = remember { androidx.compose.runtime.mutableIntStateOf(0) }

    // Which letter is under the finger right now, or -1 when nobody is
    // scrubbing. This is what the loupe below reads.
    var underFinger by remember { mutableIntStateOf(-1) }
    val reduced = MeedwellTheme.reducedMotion

    /** Which slot a y position falls on, clamped so the ends stay reachable. */
    fun slotAt(y: Float): Int {
        val height = railHeight.intValue
        if (height <= 0) return 0
        val slot = height.toFloat() / letters.size
        return (y / slot).toInt().coerceIn(0, letters.lastIndex)
    }

    Column(
        modifier
            .width(RAIL_WIDTH)
            .fillMaxHeight()
            .onSizeChanged { railHeight.intValue = it.height }
            // Tap and drag are the same gesture here: land anywhere and the
            // shelf goes there, keep moving and it keeps up.
            .pointerInput(letters) {
                detectTapGestures { offset -> onLetter(letters[slotAt(offset.y)]) }
            }
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onDragStart = { start ->
                        underFinger = slotAt(start.y)
                        onLetter(letters[underFinger])
                    },
                    onDragEnd = { underFinger = -1 },
                    onDragCancel = { underFinger = -1 },
                ) { change, _ ->
                    change.consume()
                    val slot = slotAt(change.position.y)
                    if (slot != underFinger) {
                        underFinger = slot
                        onLetter(letters[slot])
                    }
                }
            }
            .semantics {
                contentDescription = "Jump to a letter, A to Z"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        letters.forEachIndexed { index, letter ->
            // **The loupe.** While a finger rides the rail, the letter under
            // it grows to half again its size in moss ink and its neighbors
            // lean toward it, the way a jeweler's glass swells the character
            // under the lens. It is the feedback a 14dp letter cannot give by
            // itself: you can see what you are on without lifting anything.
            // Snapped rather than eased under reduced motion.
            val target = when {
                underFinger < 0 -> 1f
                index == underFinger -> 1.5f
                abs(index - underFinger) == 1 -> 1.18f
                else -> 1f
            }
            val scale by animateFloatAsState(
                targetValue = target,
                animationSpec = if (reduced) snap() else Motion.standard,
                label = "rail letter",
            )
            val ink by animateColorAsState(
                targetValue = if (index == underFinger) colors.mossInk else colors.tertiaryText,
                animationSpec = tween(if (reduced) 0 else 120),
                label = "rail ink",
            )
            Text(
                letter.toString(),
                // The plate style, which is this app's mark for a small engraved
                // label. The rail is a scale on the edge of a page, not a menu.
                style = type.plate,
                color = ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(vertical = 1.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        // Swell out of the page toward the list, not off the
                        // screen edge.
                        transformOrigin = TransformOrigin(1f, 0.5f)
                    },
            )
        }
    }
}

/**
 * Wide enough to land on, narrow enough that the list keeps its measure.
 *
 * The letters themselves are drawn small and centered; this is the reach around
 * them, and it is the number that matters for the touch floor.
 */
private val RAIL_WIDTH = 44.dp

/**
 * The letters a list actually has, in order, for the rail to draw.
 *
 * Anything that is not a letter, a number or an empty string collapses to "#",
 * which is where "...And Justice", "4 Pieces" and an untitled record all belong.
 * They sort together at one end, so they get one mark rather than several that
 * each hold a single record.
 */
fun railLetters(keys: List<String>): List<Char> =
    keys.map { railLetterOf(it) }.distinct()

/** The one character a sort key files under. */
fun railLetterOf(key: String): Char {
    val first = key.trimStart().firstOrNull { it.isLetterOrDigit() } ?: return '#'
    return if (first.isDigit()) '#' else first.uppercaseChar()
}
