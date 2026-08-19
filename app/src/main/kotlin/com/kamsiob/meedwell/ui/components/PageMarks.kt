package com.kamsiob.meedwell.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import com.kamsiob.meedwell.ui.theme.Motion
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/** Which page of the player spread is showing. */
enum class PlayerPage { Music, Surroundings }

/**
 * The two page marks at the top of the player spread.
 *
 * **This is the affordance for a two-page spread, and it is not a ViewPager
 * with dots.** The two marks are the app's own iconography:
 *
 *  - the **copper circle on a line** for music, which is the app's mark
 *  - the **sun on a horizon** for Surroundings, which is the same sign used on
 *    the day line and the Surroundings tab
 *
 * The page you are on is **inked** and carries a 16dp moss underline. The other
 * is drawn as a **hairline outline at 30% ink**. So the marks say two things at
 * once: which page you are on, and whether anything is playing on the other
 * one, because a Surroundings mark that is inked while you are on the music
 * page means rain is running underneath.
 *
 * From the grid:
 *
 * ```
 * .pmarks{display:flex;justify-content:center;align-items:flex-end;gap:26px;}
 * .pm{position:relative;padding-bottom:7px;}
 * .pm .ul{...;width:16px;height:2px;background:var(--moss);border-radius:2px;}
 * ```
 */
@Composable
fun PageMarks(
    page: PlayerPage,
    /** Whether a Surroundings bed is running, which lights its mark either way. */
    surroundingsPlaying: Boolean,
    onSelect: (PlayerPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    // **The underline travels.**
    //
    // It used to be drawn per mark and flipped between 16dp and 0dp instantly,
    // so the single highest-information pixel on the screen, the one that says
    // which page you are on, teleported. It is one mark now, sliding the 48dp
    // between the two centers on the same settle the shelf switcher uses. This
    // is the app's own "this one" idiom, animated the way it already animates
    // elsewhere, not a new idea.
    val underlineAt by animateDpAsState(
        targetValue = if (page == PlayerPage.Music) (-24).dp else 24.dp,
        animationSpec = if (MeedwellTheme.reducedMotion) snap() else tween(Motion.turn, easing = Motion.Settle),
        label = "page underline",
    )

    Box(modifier.fillMaxWidth()) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        PageMark(
            inked = page == PlayerPage.Music,
            label = "Music page",
            selected = page == PlayerPage.Music,
            // Copper, because this mark *is* the app's mark.
            inkedColor = MeedwellTheme.colors.copper,
            onClick = { onSelect(PlayerPage.Music) },
        ) { color, stroke ->
            // The app's mark: a coin resting on a line.
            drawCircle(
                color = color,
                radius = size.minDimension * 0.2f,
                center = Offset(size.width / 2f, size.height * 0.34f),
            )
            drawLine(
                color = color,
                start = Offset(size.width * 0.11f, size.height * 0.68f),
                end = Offset(size.width * 0.89f, size.height * 0.68f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }

        // No spacer. Each mark already carries a 48dp target, so a 26dp gap
        // between them put their centers 74dp apart where the grid sets 46, and
        // they read as two stray icons rather than one paired signature.
        Box(Modifier.width(0.dp))

        PageMark(
            // Lit whenever a sound is running, even from the music page. That
            // is how you know rain is playing without leaving the page you are
            // on, and it is the whole reason these are marks rather than dots.
            inked = page == PlayerPage.Surroundings || surroundingsPlaying,
            label = if (surroundingsPlaying) "Surroundings page, a sound is playing" else "Surroundings page",
            selected = page == PlayerPage.Surroundings,
            // **Moss, not copper.** The grid inks this one in the accent, and
            // copper is reserved for the app's own mark and the day-line sun.
            // Two copper suns a thumb apart would make both mean nothing.
            inkedColor = MeedwellTheme.colors.moss,
            onClick = { onSelect(PlayerPage.Surroundings) },
        ) { color, stroke ->
            // The sun on a horizon, outlined rather than filled so it reads as
            // a different sign from the coin.
            drawCircle(
                color = color,
                radius = size.minDimension * 0.19f,
                center = Offset(size.width / 2f, size.height * 0.32f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
            )
            drawLine(
                color = color,
                start = Offset(size.width * 0.09f, size.height * 0.7f),
                end = Offset(size.width * 0.91f, size.height * 0.7f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }

    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .offset(x = underlineAt)
            .width(16.dp)
            .height(2.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MeedwellTheme.colors.moss)
    )
    }
}

@Composable
private fun PageMark(
    inked: Boolean,
    label: String,
    selected: Boolean,
    inkedColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    draw: androidx.compose.ui.graphics.drawscope.DrawScope.(
        color: androidx.compose.ui.graphics.Color,
        stroke: Float,
    ) -> Unit,
) {
    val colors = MeedwellTheme.colors
    // Each mark has its own inked colour, and both go to a 30% ink outline
    // when they are not the page you are on.
    val color by animateColorAsState(
        targetValue = if (inked) inkedColor else colors.primaryText.copy(alpha = 0.3f),
        animationSpec = tween(200, easing = Motion.Settle),
        label = "mark ink",
    )

    Column(
        Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics {
                contentDescription = label
                this.selected = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.height(14.dp))
        Canvas(Modifier.size(20.dp)) {
            draw(color, if (inked) 2.dp.toPx() else 1.4.dp.toPx())
        }
        // `.pm { padding-bottom: 7px }` then the underline.
        Box(Modifier.height(7.dp))
        // Space reserved for the traveling underline, which is drawn once at
        // the row level rather than per mark.
        Box(Modifier.width(16.dp).height(2.dp))
    }
}

/**
 * The seven pixel sliver of the facing page, at the screen edge.
 *
 * ```
 * .edge{position:absolute;top:0;bottom:0;width:7px;background:rgba(28,36,32,.045);}
 * .edge.r{right:0;border-left:1px solid var(--hair);}
 * ```
 *
 * It is what makes the player read as a spread rather than as one screen: a
 * page has another page beside it, and you can see its edge. Without this the
 * horizontal swipe is undiscoverable.
 */
@Composable
fun PageEdge(onRight: Boolean, modifier: Modifier = Modifier) {
    val colors = MeedwellTheme.colors
    val tint = colors.primaryText.copy(alpha = 0.045f)
    val reduced = MeedwellTheme.reducedMotion

    // **The edge participates in the turn.** It used to teleport from one side
    // to the other. Now, when the leaf turns, the sliver dips out, crosses, and
    // widens for a beat as it lands, which is the physical fact of a page being
    // turned: the sheet's edge lifts before it lies flat. This is the spread's
    // own metaphor answering, not an effect laid on top of it.
    val fade = remember { androidx.compose.animation.core.Animatable(1f) }
    val widen = remember { androidx.compose.animation.core.Animatable(7f) }
    var shownSide by remember { androidx.compose.runtime.mutableStateOf(onRight) }
    LaunchedEffect(onRight) {
        if (shownSide == onRight) return@LaunchedEffect
        if (reduced) {
            shownSide = onRight
            return@LaunchedEffect
        }
        fade.animateTo(0f, tween(90, easing = Motion.Leave))
        shownSide = onRight
        widen.snapTo(13f)
        fade.animateTo(1f, tween(160, easing = Motion.Settle))
        widen.animateTo(7f, tween(190, easing = Motion.Rule))
    }

    Row(
        modifier
            .fillMaxHeight()
            .width((widen.value + 1f).dp)
            .graphicsLayer { alpha = fade.value }
    ) {
        if (shownSide) {
            Box(Modifier.width(1.dp).fillMaxHeight().background(colors.hairline))
            Box(Modifier.width(widen.value.dp).fillMaxHeight().background(tint))
        } else {
            Box(Modifier.width(widen.value.dp).fillMaxHeight().background(tint))
            Box(Modifier.width(1.dp).fillMaxHeight().background(colors.hairline))
        }
    }
}
