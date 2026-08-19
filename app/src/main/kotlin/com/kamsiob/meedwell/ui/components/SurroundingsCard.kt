package com.kamsiob.meedwell.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import com.kamsiob.meedwell.ui.theme.Motion
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
 * is also why there is no setting to turn it off: stopping the sound is the
 * whole opt out, and it is one named row at the foot of the opened card.
 *
 * **Stopping is not a gesture.** It was, and the owner lost beds to stray
 * thumbs for weeks. A drag now folds the card and nothing more.
 *
 * ## The transparency, precisely
 *
 * This is the detail most likely to go wrong, so it is spelled out:
 *
 *  - background is the **current theme's own ground at 74 percent**, not white
 *    and not a grey. A translucent white over warm paper goes chalky, which is
 *    why it is tinted with the ground rather than lightened. The reference says
 *    88 and it read as a solid bar; the owner asked for more of the page to come
 *    through.
 *  - a 1px border at 14 percent ink
 *  - one wide, soft drop shadow. A tight dark one reads as a slab
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
    onPlayPause: () -> Unit,
    onVolume: (Float) -> Unit,
    onPick: (String) -> Unit,
    onStop: () -> Unit,
    onOpenAll: () -> Unit,
    /**
     * The card's real height, reported as it is measured.
     *
     * The list behind the card leaves room for it, and that room used to come
     * from a pair of hardcoded guesses: 62dp collapsed, 210dp expanded. The
     * collapsed card is nearer 99dp, so the last row on every shelf sat
     * underneath it, and because the card is a flat 88 percent tint rather than
     * an opaque bar the text ghosted through it and looked like a rendering
     * fault rather than a layout one.
     *
     * A guess could not have been made right, either: the title wraps to two
     * lines on a long recording, and any fixed number is wrong for one of the
     * two cases. So the card measures itself and the room follows.
     */
    onHeightChanged: (Dp) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (!state.visible) return
    val density = LocalDensity.current

    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    // The ground itself, tinted rather than lightened.
    //
    // **Ninety three percent.** This has been walked in both directions: 88 read
    // as a solid bar, 74 let so much of the shelf through that album art behind
    // it fought the words on top and the card became genuinely hard to read.
    // Ninety three keeps the faintest sense of a layer over a page while the
    // text sits on something rather than in front of everything.
    //
    // Still tinted with the ground rather than lightened, because a translucent
    // white over warm paper goes chalky and the ground does not. Still no
    // gradient and no gloss: this should read as paper laid over paper, never as
    // glass.
    //
    // There is still **no backdrop blur**, and not for want of trying. Compose
    // has no modifier that blurs what is behind a composable: `Modifier.blur`
    // blurs the thing's own content, which here would smear the title and the
    // volume line and leave the shelf behind it sharp. Real backdrop blur needs
    // either a third party renderer or making the card its own window, and
    // neither is worth taking on quietly. See `DECISIONS.md`.
    // **Ranked behind the music.** The mini player is opaque with a 10dp
    // shadow; this sits at 90 percent with a 6dp one. Same inset, same radius,
    // so the two read as one family, and the depth order says which is the
    // record and which is the room it is playing in.
    // **Opaque, full stop.** At 90 percent the owner saw the word "Privacy"
    // from the More list apparently printed on the card; at 98 a bright album
    // tile still ghosted through on the shelf grid. A layer that lets anything
    // behind read into its own is not atmosphere, it is noise. The card's rank
    // is carried by its softer shadow and its eyebrow, not by transparency.
    val fill = colors.background
    val edge = if (colors.isDark) Color(0x29EFEEE6) else Color(0x291C2420)

    // The same give-under-a-finger the mini player has, because the two are a
    // family and only one of them answering would read as the other being
    // broken.
    val pressSource = remember { MutableInteractionSource() }
    val pressed by pressSource.collectIsPressedAsState()
    val reducedMotion = MeedwellTheme.reducedMotion
    val give by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.985f else 1f,
        animationSpec = Motion.standard,
        label = "card give",
    )

    // Softer and wider than before, and fainter. A tight dark shadow under a
    // translucent panel is what makes it read as a slab; a diffuse one reads as
    // height.
    val shadowInk = if (colors.isDark) Color(0x99000000) else Color(0x4A1C2420)

    Column(
        modifier
            .fillMaxWidth()
            .onSizeChanged { onHeightChanged(with(density) { it.height.toDp() }) }
            // `.sur { left: 14px; right: 14px }`. Inset from both edges so it
            // reads as a separate floating layer rather than a second bar.
            .padding(horizontal = 12.dp)
            .graphicsLayer {
                scaleX = give
                scaleY = give
            }
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(CARD_RADIUS),
                clip = false,
                ambientColor = shadowInk,
                spotColor = shadowInk,
            )
            .clip(RoundedCornerShape(CARD_RADIUS))
            .background(fill)
            .border(1.dp, edge, RoundedCornerShape(CARD_RADIUS))
            // **A drag folds the card. It no longer stops the sound.**
            //
            // It used to, and it was the worst bug in the app. `dragAmount` is
            // the distance since the last pointer event, not the distance
            // travelled, so a 26px test was cleared by the *first* move of any
            // downward flick: about one touch slop. It also fired mid-gesture
            // rather than on release, so the recording was gone while the finger
            // was still down and there was no aborting it. The card did not
            // follow the finger, so until the sound stopped a drag looked
            // exactly like a list that would not scroll. And the card sits in
            // the thumb arc, just above the mini player.
            //
            // The cruelest part: the rows for *changing* the bed are children of
            // this same gesture, so reaching to swap the recording was the
            // motion that killed it.
            //
            // Now travel is accumulated and judged on release, both directions
            // mean something, and neither is destructive. Stopping has a name
            // and a row of its own, further down. The player spread already did
            // it this way; this is the same shape.
            .pointerInput(state.soundId) {
                var travelled = 0f
                detectVerticalDragGestures(
                    onDragStart = { travelled = 0f },
                    onDragEnd = {
                        if (travelled > COLLAPSE_PX && state.expanded) onToggleExpanded()
                        if (travelled < -COLLAPSE_PX && !state.expanded) onToggleExpanded()
                    },
                    onDragCancel = { travelled = 0f },
                ) { change, delta ->
                    change.consume()
                    travelled += delta
                }
            }
            .clickable(
                interactionSource = pressSource,
                indication = null,
                role = Role.Button,
                onClick = onToggleExpanded,
            )
            .semantics {
                contentDescription = "${state.title}, playing underneath"
                stateDescription = if (state.expanded) "Expanded" else "Collapsed"
            }
            .padding(
                start = 14.dp,
                end = 14.dp,
                top = if (state.expanded) 10.dp else 2.dp,
                bottom = if (state.expanded) 12.dp else 0.dp,
            ),
    ) {
        // **One row, and the level rule along the bottom edge.**
        //
        // The card was three storeys tall while collapsed: a grab handle, the
        // eyebrow and title row, then a full 48dp level line, and the owner's
        // verdict was that it took up way too much space. The clever part of
        // making it small is that nothing had to be given up, only re-homed the
        // way the mini player already re-homed its scrub: the volume is now a
        // thin rule along the card's bottom edge with the same dot the contour
        // wears, tiny speakers at its ends, and an invisible 40dp reach
        // overlapping the row's own padding. So the mini player wears the
        // music's rule on its top edge and this card wears the room's rule on
        // its bottom edge: mirrored twins, half the height, no lost control.
        // The handle went with the height; the drag still folds and opens the
        // card, the chevron still says which way it stands, and the tap still
        // toggles it.
        Box(Modifier.fillMaxWidth()) {
            Column {
                // `.sur .top`: the whole row is the target, at a full 48dp.
                Row(
                    Modifier.defaultMinSize(minHeight = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SunMark(
                        here = true,
                        playing = state.isPlaying,
                        width = 16.dp,
                        height = 12.dp,
                    )
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        // **The card says what it is.** Two stacked cards with
                        // no names was "no clear way to understand what's going
                        // on", and this is the same eyebrow the bed card inside
                        // the player wears.
                        Text("SURROUNDINGS", style = type.plate, color = colors.tertiaryText)
                        Text(
                            state.title,
                            style = type.miniTitle,
                            color = colors.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                    // **Pause, on the card itself**, because the card is the
                    // surface that is on screen when "how do I pause it" comes
                    // up.
                    Box(
                        Modifier
                            .size(48.dp)
                            .clickable(role = Role.Button, onClick = onPlayPause)
                            .semantics {
                                contentDescription =
                                    if (state.isPlaying) "Pause the surroundings" else "Play the surroundings"
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        MeedwellIcon(
                            icon = if (state.isPlaying) MeedwellIcons.Pause else MeedwellIcons.Play,
                            size = 18.dp,
                            tint = colors.secondaryText,
                        )
                    }
                    MeedwellIcon(
                        icon = if (state.expanded) MeedwellIcons.ChevronDown else MeedwellIcons.ChevronRight,
                        size = 14.dp,
                        tint = colors.tertiaryText,
                    )
                }
                if (!state.expanded) Box(Modifier.height(14.dp))
            }

            // The level, as the card's bottom edge. Only drags are consumed,
            // so a tap here still toggles the card the way a tap on the mini
            // player's scrub strip still opens the player.
            if (!state.expanded) {
                val plain = plainVolume(state.volume)
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(LEVEL_REACH)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                onVolume((change.position.x / size.width).coerceIn(0f, 1f))
                            }
                        }
                        .semantics {
                            contentDescription = "Surroundings volume, $plain. Drag to change it."
                            progressBarRangeInfo = ProgressBarRangeInfo(state.volume, 0f..1f)
                        },
                ) {
                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MeedwellIcon(MeedwellIcons.VolumeLow, size = 9.dp, tint = colors.tertiaryText)
                        Box(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Box(
                                Modifier
                                    .align(Alignment.CenterStart)
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(colors.hairline)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(state.volume.coerceIn(0f, 1f))
                                        .height(2.dp)
                                        .background(colors.moss)
                                )
                            }
                            Box(
                                Modifier.fillMaxWidth(state.volume.coerceIn(0.02f, 1f)),
                            ) {
                                Box(
                                    Modifier
                                        .align(Alignment.CenterEnd)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(colors.moss)
                                )
                            }
                        }
                        MeedwellIcon(MeedwellIcons.VolumeHigh, size = 11.dp, tint = colors.tertiaryText)
                    }
                }
            }
        }

        if (state.expanded) {
            // The full-height level line, restored while the card is open and
            // there is room to aim at it.
            LevelLine(
                value = state.volume,
                onChange = onVolume,
                playing = state.isPlaying,
            )
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
                        .defaultMinSize(minHeight = 48.dp)
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
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable(role = Role.Button, onClick = onOpenAll)
                    .padding(top = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("All recordings", style = type.chip, color = colors.primaryText)
                MeedwellIcon(MeedwellIcons.ChevronRight, size = 14.dp, tint = colors.tertiaryText)
            }

            // **Stopping, said in words.**
            //
            // It used to be a swipe and nothing else, which is how it kept
            // happening by accident. An action that ends something should be
            // named, deliberate and in one place, rather than hidden in a
            // gesture that overlaps a scroll. Quiet ink, because this is the
            // least likely thing anybody came here to do.
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 9.dp, bottom = 2.dp)
                    .height(1.dp)
                    .background(colors.hairline)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable(role = Role.Button, onClick = onStop)
                    .padding(top = 9.dp)
                    .semantics { contentDescription = "Stop the surroundings" },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Stop", style = type.chip, color = colors.tertiaryText)
                MeedwellIcon(MeedwellIcons.Fermata, size = 13.dp, tint = colors.tertiaryText)
            }
        }
    }
}

/** `.sur { border-radius: 16px }`. */
private val CARD_RADIUS: Dp = 20.dp

/** Roughly how tall the card is, so lists behind it can leave room. */
fun surroundingsCardHeight(state: SurroundingsCardState): Dp = when {
    !state.visible -> 0.dp
    state.expanded -> 272.dp
    else -> 66.dp
}

/**
 * How far a drag has to actually travel before it folds or opens the card.
 *
 * Accumulated across the whole gesture and judged when the finger lifts, which
 * is the difference between this and the 26px per-event test it replaces.
 */
private const val COLLAPSE_PX = 40f

/**
 * The invisible reach around the bottom-edge level rule, same idea as the mini
 * player's scrub strip: the drawn line stays thin, the finger gets this.
 */
private val LEVEL_REACH = 40.dp


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
    /** Whether the bed is actually running, which the pause mark reads. */
    val isPlaying: Boolean = false,
    val volume: Float = 0.6f,
    /** Only what is already on the phone, most recently used first. */
    val others: List<SurroundingsCardItem> = emptyList(),
)
