package com.kamsiob.meedwell.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * The floating Surroundings card.
 *
 * Measured from `reference/meedwell-surroundings-card.html`, which governs this
 * component the way the 25 screen grid governs everything else.
 *
 * **It exists only while a sound is playing.** Nothing floats by default. It
 * arrives when a bed starts and leaves when it stops, so it is always a
 * consequence of something the listener did rather than an interruption. That
 * is also why there is no setting to turn it off: swiping it away stops the
 * sound, which is the whole opt out.
 *
 * ## The transparency, precisely
 *
 * This is the detail most likely to go wrong, so it is spelled out:
 *
 *  - background is the **current theme's own ground at 88 percent**, not white
 *    and not a grey. A translucent white over warm paper goes chalky, which is
 *    why it is tinted with the ground rather than lightened.
 *  - a 1px border at 16 percent ink
 *  - one soft drop shadow, `0 10px 26px -14px` at 42 percent ink
 *  - **no backdrop blur, no frosted glass, no gradient, no Material dialog
 *    surface.** Content behind must ghost through faintly. A blur here would
 *    read as glassmorphism, which is the generic look being removed from this
 *    build.
 *
 * ## It expands downward from a fixed anchor
 *
 * The top edge and the volume line do not move when it opens, so a finger
 * already resting on the volume is not displaced. Only the height animates.
 *
 * ## The list is not a browser
 *
 * Four recordings at most, only ones already on the phone, most recently used
 * first with the playing one marked. Not scrollable. If it listed everything it
 * would be a second Surroundings tab living in a corner, and "All recordings"
 * exists precisely so it does not have to be.
 */
@Composable
fun SurroundingsCard(
    state: SurroundingsCardState,
    onToggleExpanded: () -> Unit,
    onVolume: (Float) -> Unit,
    onPick: (String) -> Unit,
    onStop: () -> Unit,
    onOpenAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.visible) return

    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    // The ground itself at 88 percent. Tinted rather than lightened.
    val fill = colors.background.copy(alpha = 0.88f)
    val edge = if (colors.isDark) Color(0x29EFEEE6) else Color(0x291C2420)
    val shadowInk = if (colors.isDark) Color(0xCC000000) else Color(0x6B1C2420)

    Column(
        modifier
            .fillMaxWidth()
            // `.sur { left: 14px; right: 14px }`. Inset from both edges so it
            // reads as a separate floating layer rather than a second bar.
            .padding(horizontal = 14.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(CARD_RADIUS),
                clip = false,
                ambientColor = shadowInk,
                spotColor = shadowInk,
            )
            .clip(RoundedCornerShape(CARD_RADIUS))
            .background(fill)
            .border(1.dp, edge, RoundedCornerShape(CARD_RADIUS))
            .pointerInput(state.soundId) {
                detectVerticalDragGestures { change, dragAmount ->
                    // Swipe down stops the sound and takes the card with it.
                    if (dragAmount > SWIPE_AWAY_PX) {
                        change.consume()
                        onStop()
                    }
                }
            }
            .clickable(role = Role.Button, onClick = onToggleExpanded)
            .semantics {
                contentDescription = "${state.title}, playing underneath"
                stateDescription = if (state.expanded) "Expanded" else "Collapsed"
            }
            .padding(
                start = 14.dp,
                end = 14.dp,
                top = if (state.expanded) 12.dp else 10.dp,
                bottom = if (state.expanded) 13.dp else 11.dp,
            ),
    ) {
        // `.sur .top`
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.title,
                style = type.miniTitle,
                color = colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            MeedwellIcon(
                icon = if (state.expanded) MeedwellIcons.ChevronDown else MeedwellIcons.ChevronRight,
                size = 11.dp,
                tint = colors.tertiaryText,
            )
        }

        // `.sur .vol`, 3px, and it sits at the same offset in both states so a
        // finger already on it is not displaced when the card opens.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 9.dp)
                // A tall reach around a 3px line.
                .height(20.dp)
                .pointerInput(state.soundId) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        onVolume((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(state.soundId) {
                    detectTapGestures { offset -> onVolume((offset.x / size.width).coerceIn(0f, 1f)) }
                }
                .semantics {
                    // pp and ff mean nothing read aloud, so the value is in
                    // plain words here even though the screen shows dynamics.
                    contentDescription = "Surroundings volume, ${plainVolume(state.volume)}"
                    progressBarRangeInfo = ProgressBarRangeInfo(state.volume, 0f..1f)
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (colors.isDark) Color(0x29EFEEE6) else Color(0x241C2420))
            )
            Box(
                Modifier
                    .fillMaxWidth(state.volume.coerceIn(0f, 1f))
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.moss)
            )
            Box(
                Modifier
                    .padding(start = 0.dp)
                    .fillMaxWidth(state.volume.coerceIn(0f, 1f)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(colors.moss))
            }
        }

        // `.sur .ends`
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("pp", style = type.plate, color = colors.tertiaryText)
            Text("ff", style = type.plate, color = colors.tertiaryText)
        }

        if (state.expanded) {
            // `.sur .rule { margin: 11px 0 2px }`
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 11.dp, bottom = 2.dp)
                    .height(1.dp)
                    .background(colors.hairline)
            )

            state.others.take(4).forEach { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        // 44dp minimum, per the build notes.
                        .defaultMinSize(minHeight = 44.dp)
                        .clickable(role = Role.Button) { onPick(item.id) }
                        .padding(vertical = 8.dp)
                        .semantics {
                            contentDescription =
                                if (item.playing) "${item.title}, playing" else "Play ${item.title}"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Text(
                        item.title,
                        style = type.miniTitle.copy(
                            fontWeight = if (item.playing) {
                                androidx.compose.ui.text.font.FontWeight.SemiBold
                            } else {
                                androidx.compose.ui.text.font.FontWeight.Normal
                            }
                        ),
                        color = if (item.playing) colors.mossInk else colors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(item.duration, style = type.plate, color = colors.tertiaryText)
                }
            }

            // `.sur .all`
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .height(1.dp)
                    .background(colors.hairline)
            ) {}
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .clickable(role = Role.Button, onClick = onOpenAll)
                    .padding(top = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("All recordings", style = type.chip, color = colors.primaryText)
                MeedwellIcon(MeedwellIcons.ChevronRight, size = 11.dp, tint = colors.tertiaryText)
            }
        }
    }
}

/** `.sur { border-radius: 16px }`. */
private val CARD_RADIUS: Dp = 16.dp

/** Roughly how tall the card is, so lists behind it can leave room. */
fun surroundingsCardHeight(state: SurroundingsCardState): Dp = when {
    !state.visible -> 0.dp
    state.expanded -> 210.dp
    else -> 62.dp
}

/** How far a downward drag has to travel before it means "stop". */
private const val SWIPE_AWAY_PX = 26f

/** The volume in words, because pp and ff mean nothing read aloud. */
private fun plainVolume(value: Float): String = when {
    value <= 0.01f -> "silent"
    value < 0.2f -> "very quiet"
    value < 0.4f -> "quiet"
    value < 0.6f -> "middling"
    value < 0.8f -> "loud"
    else -> "very loud"
}

data class SurroundingsCardItem(
    val id: String,
    val title: String,
    val duration: String,
    val playing: Boolean,
)

data class SurroundingsCardState(
    val visible: Boolean = false,
    val expanded: Boolean = false,
    val soundId: String? = null,
    val title: String = "",
    val volume: Float = 0.6f,
    /** Only what is already on the phone, most recently used first. */
    val others: List<SurroundingsCardItem> = emptyList(),
)
