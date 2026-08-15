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
import com.kamsiob.meedwell.ui.theme.GlowEmber
import com.kamsiob.meedwell.ui.theme.GlowRose
import com.kamsiob.meedwell.ui.theme.GlowTeal
import com.kamsiob.meedwell.ui.theme.GlowViolet
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * Shared pieces of the interface, built once here so a control keeps the same
 * shape and the same name everywhere it appears.
 *
 * Every interactive element in this file meets the 48dp minimum touch target
 * from `DESIGN.md` section 12, and the sizes are set rather than inherited so
 * that a later layout change cannot quietly shrink one below the floor.
 */

enum class GlowTone { Violet, Teal, Rose, Ember }

/**
 * The ambient wash: a soft radial field that drifts over roughly 16 seconds.
 *
 * Gated behind reduced motion, which is not optional. When the system asks for
 * reduced motion the field is still drawn, because it carries the warmth of the
 * screen, but it does not move at all.
 */
@Composable
fun AmbientGlow(
    tone: GlowTone,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val reducedMotion = MeedwellTheme.reducedMotion

    val color = when (tone) {
        GlowTone.Violet -> GlowViolet
        GlowTone.Teal -> GlowTeal
        GlowTone.Rose -> GlowRose
        GlowTone.Ember -> GlowEmber
    }
    // Half opacity in light theme, per DESIGN.md section 2.
    val tuned = if (colors.isDark) color else color.copy(alpha = color.alpha * 0.5f)

    val drift = if (reducedMotion) {
        0.5f
    } else {
        val transition = rememberInfiniteTransition(label = "ambient drift")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 16_000),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "ambient drift position",
        ).value
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp)
            .drawBehind {
                val centerX = size.width * (0.44f + 0.12f * drift)
                val centerY = size.height * (0.30f + 0.10f * drift)
                val radius = size.maxDimension * (0.62f + 0.06f * drift)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(tuned, Color.Transparent),
                        center = Offset(centerX, centerY),
                        radius = radius,
                    ),
                    size = Size(size.width, size.height),
                )
            }
    )
}

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
            .height(52.dp)
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

/** The quieter second choice. Still a full 48dp target despite reading as text. */
@Composable
fun TextButtonRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .height(48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MeedwellTheme.typography.metadata,
            color = MeedwellTheme.colors.primaryText.copy(alpha = 0.68f),
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
            .defaultMinSize(minHeight = 50.dp)
            .height(50.dp)
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
            style = MeedwellTheme.typography.metadata,
            color = colors.gold,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
