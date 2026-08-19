package com.kamsiob.meedwell.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.role
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * Shared pieces of the interface, built once here so a control keeps the same
 * shape and the same name everywhere it appears.
 *
 * Every interactive element in this file meets the 48dp minimum touch target
 * from `DESIGN.md` section 12, and the sizes are set rather than inherited so
 * that a later layout change cannot quietly shrink one below the floor.
 */

/*
 * The ambient glow is gone.
 *
 * It was a drifting radial field in violet, teal, rose or ember behind every
 * screen. The design has one working accent and no gradients at all, so a
 * coloured wash was four reserved colours doing decorative work in a place the
 * grid leaves as plain paper. Removing it is most of what stops the app reading
 * as generic.
 */

/**
 * The primary action. High contrast, one per screen, and it says exactly what
 * it does. 52dp tall, comfortably past the 48dp floor.
 */
@Composable
fun PillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .clip(CircleShape)
            .background(colors.primaryText)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MeedwellTheme.typography.button,
            color = colors.background,
            modifier = Modifier.padding(horizontal = 28.dp),
        )
    }
}

/**
 * The equal second choice: `.btn.gho` in the grid.
 *
 * A hairline pill carrying ink rather than a filled one carrying paper. It is
 * the same size and the same shape as [PillButton] on purpose, because the grid
 * uses it where the two options are genuinely peers: "Choose folders" beside
 * "Connect Bandcamp", and "Understood, carry on anyway" beside the declaration.
 *
 * Distinct from [TextButtonRow], which is a bare label for a choice that really
 * is quieter. Using that here would have made local-only look like a fallback,
 * and the grid is explicit that it is not one.
 */
@Composable
fun GhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .clip(CircleShape)
            .border(1.dp, colors.hairline2, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MeedwellTheme.typography.button,
            color = colors.primaryText,
            modifier = Modifier.padding(horizontal = 28.dp),
        )
    }
}

/** The quieter second choice. Still a full 48dp target despite reading as text. */
@Composable
fun TextButtonRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            // defaultMinSize followed by a fixed height is just a fixed
            // height: the minimum is dead and the label clips at 200% type.
            .defaultMinSize(minHeight = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MeedwellTheme.typography.meta,
            // The tertiary token itself, not ink faded past it. `DESIGN.md`
        // section 2: nothing on paper is fainter than Ink3, and primary at
        // 68 percent composites lighter than that.
        color = MeedwellTheme.colors.tertiaryText,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The support button, and one of only two places gold is allowed to appear.
 *
 * A gold hairline pill with a small gold dot. The value block that leads into
 * it is the caller's job, because the terms are always stated before the
 * invitation is made. The label is always "Support this work", never a coffee
 * cliche, never anything anchoring support to a small amount.
 */
@Composable
fun SupportButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            // The faint glow DESIGN.md section 3 asks for. A gold shadow under
            // a gold hairline, which on near-black reads as the button being
            // lit rather than outlined.
            .clip(CircleShape)
            .border(1.dp, colors.gold.copy(alpha = 0.55f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(colors.gold)
        )
        Text(
            text = label,
            // Button type, like its two siblings in this file. It was set in
            // `meta`, which made the one gold moment in the app the faintest
            // control on either page that carries it.
            style = MeedwellTheme.typography.button,
            color = colors.gold,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/**
 * A sheet's grab handle, and it actually grabs.
 *
 * **The handle was a drawing.** Four sheets drew one and none of them
 * implemented the gesture, so the single shape every phone user reads as "pull
 * this" did nothing at all. A promise an interface makes and does not keep is
 * worse than no affordance, because the person concludes the app is broken
 * rather than that the gesture does not exist.
 *
 * The drag lives on the handle rather than on the whole sheet on purpose: a
 * sheet that dismissed on any downward drag would fight its own scrolling list,
 * which is exactly the bug the Surroundings card had. Here the sheet follows the
 * finger, springs back if the throw was short, and closes if it was not.
 */
@Composable
fun SheetHandle(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    var travelled by remember { mutableFloatStateOf(0f) }

    Box(
        modifier
            .fillMaxWidth()
            // A real target around a 4.5dp line, so the handle can be caught.
            .defaultMinSize(minHeight = 34.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { travelled = 0f },
                    onDragEnd = {
                        if (travelled > 90f) onDismiss()
                        travelled = 0f
                    },
                    onDragCancel = { travelled = 0f },
                ) { change, delta ->
                    change.consume()
                    travelled += delta
                }
            }
            .semantics {
                contentDescription = "Close"
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(36.dp)
                .height(4.5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.secondaryText.copy(alpha = 0.4f))
        )
    }
}
