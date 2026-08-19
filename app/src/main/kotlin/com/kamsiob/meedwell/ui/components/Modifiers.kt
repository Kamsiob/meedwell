package com.kamsiob.meedwell.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Motion

/**
 * Tap to open, long press to open the action sheet.
 *
 * Wrapped in one place because the action sheet appears on **every** surface a
 * track or album lives on, with the same eight verbs in the same order. Having
 * one helper is what makes "one shared component rather than per-screen
 * variants" true in the code rather than only in the specification.
 *
 * **The row answers the finger with its own rule.** There is no ripple in this
 * design and nothing to fill, so a press used to be invisible until the sheet
 * arrived, which on a long press is a long time to wonder whether anything
 * heard you. While a finger is down, the row's bottom edge takes a darker
 * hairline, the way the page's existing rule takes more ink under a nib. It is
 * feedback with no new shape, no fill and no color the design does not already
 * own, and because every row goes through this one helper, every row answers
 * the same way.
 *
 * The long press also gets a named accessibility action, because a gesture that
 * has no equivalent for a TalkBack user is a feature that user does not have.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.combinedClickableCompat(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLongClickLabel: String = "More actions",
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val ink by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(if (pressed) 60 else 140, easing = Motion.Settle),
        label = "row press",
    )
    val rule = MeedwellTheme.colors.hairline2

    this
        .drawBehind {
            if (ink > 0.01f) {
                drawLine(
                    color = rule.copy(alpha = rule.alpha * ink),
                    start = Offset(0f, size.height - 0.5.dp.toPx()),
                    end = Offset(size.width, size.height - 0.5.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
        .combinedClickable(
            interactionSource = interaction,
            indication = null,
            role = Role.Button,
            onClick = onClick,
            onLongClick = onLongClick,
            onLongClickLabel = onLongClickLabel,
        )
}
