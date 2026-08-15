package com.kamsiob.meedwell.ui.components

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
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        PageMark(
            inked = page == PlayerPage.Music,
            underlined = page == PlayerPage.Music,
            label = "Music page",
            selected = page == PlayerPage.Music,
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

        Box(Modifier.width(26.dp))

        PageMark(
            // Lit whenever a sound is running, even from the music page. That
            // is how you know rain is playing without leaving the page you are
            // on, and it is the whole reason these are marks rather than dots.
            inked = page == PlayerPage.Surroundings || surroundingsPlaying,
            underlined = page == PlayerPage.Surroundings,
            label = if (surroundingsPlaying) "Surroundings page, a sound is playing" else "Surroundings page",
            selected = page == PlayerPage.Surroundings,
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
}

@Composable
private fun PageMark(
    inked: Boolean,
    underlined: Boolean,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    draw: androidx.compose.ui.graphics.drawscope.DrawScope.(
        color: androidx.compose.ui.graphics.Color,
        stroke: Float,
    ) -> Unit,
) {
    val colors = MeedwellTheme.colors
    // Copper when inked, because these marks are the app's own sign and copper
    // is the mark's color. At 30% ink when not, which is the grid's outline.
    val color = if (inked) colors.copper else colors.primaryText.copy(alpha = 0.3f)

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
        Box(
            Modifier
                .width(16.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (underlined) colors.moss else androidx.compose.ui.graphics.Color.Transparent)
        )
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
    Row(modifier.fillMaxHeight().width(8.dp)) {
        if (onRight) {
            Box(Modifier.width(1.dp).fillMaxHeight().background(colors.hairline))
            Box(Modifier.width(7.dp).fillMaxHeight().background(tint))
        } else {
            Box(Modifier.width(7.dp).fillMaxHeight().background(tint))
            Box(Modifier.width(1.dp).fillMaxHeight().background(colors.hairline))
        }
    }
}
