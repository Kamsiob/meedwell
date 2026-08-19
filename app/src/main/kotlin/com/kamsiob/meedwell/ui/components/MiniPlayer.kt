package com.kamsiob.meedwell.ui.components

import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.playback.PlaybackState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.kamsiob.meedwell.ui.theme.Motion
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius

/**
 * The mini player, above the tab bar.
 *
 * Carries a waveform rather than a progress bar, because the waveform is the
 * music's own portrait and the app's signature interaction. It **stills when
 * paused**, which is how the reference specifies it, and it is static under
 * reduced motion.
 */
@Composable
fun MiniPlayer(
    state: PlaybackState,
    onPlayPause: () -> Unit,
    onOpen: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.hasQueue) return

    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    // Where a finger has dragged the rule to, or null when nobody is scrubbing.
    // Held locally so the line follows the finger immediately instead of waiting
    // for the player to report back, and so the seek is issued once on release
    // rather than on every pixel of the drag.
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    val shown = scrubbing ?: state.progress.coerceIn(0f, 1f)

    // **The card gives slightly under a finger**, the way a key gives before
    // it sounds: 0.985 on the standard spring, nothing under reduced motion.
    // Same response as the Surroundings card, because the two are a family.
    val pressSource = remember { MutableInteractionSource() }
    val pressed by pressSource.collectIsPressedAsState()
    val reduced = MeedwellTheme.reducedMotion
    val give by animateFloatAsState(
        targetValue = if (pressed && !reduced) 0.985f else 1f,
        animationSpec = Motion.standard,
        label = "card give",
    )

    // **Its own card, and deliberately not the same card as the bed's.**
    //
    // It used to be a full width slab of the page's own ground, so on a screen
    // where a Surroundings card was already floating above it the two ran
    // together into one grey mass and neither read as a thing. It is a card now,
    // inset from both edges with a hairline and a soft shadow.
    //
    // The two are told apart on purpose, and by more than position: this one is
    // fully opaque and carries the record's own artwork, while the bed's card is
    // translucent, has no artwork it could honestly show, and carries the sun.
    // Solid and pictorial against translucent and drawn.
    val edge = if (colors.isDark) Color(0x29EFEEE6) else Color(0x291C2420)
    val shadowInk = if (colors.isDark) Color(0x99000000) else Color(0x3D1C2420)

    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .graphicsLayer {
                scaleX = give
                scaleY = give
            }
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp),
                clip = false,
                ambientColor = shadowInk,
                spotColor = shadowInk,
            )
            .clip(RoundedCornerShape(20.dp))
            .background(colors.background)
            .border(1.dp, edge, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = pressSource,
                indication = null,
                role = Role.Button,
                onClick = onOpen,
            )
            .semantics {
                contentDescription = "Now playing: ${state.title} by ${state.artist}. Open the player."
            },
    ) {
        // `.mini .pg`: a 2px rule across the very top, filled in moss as far as
        // the piece has got. This is the only progress indicator outside the
        // player itself, and it is a rule rather than a bar because there are
        // no filled containers in this design to put a bar inside.
        //
        // **It scrubs.** The rule is 2px, which no finger can hit, so the touch
        // area is a 22dp strip with the rule drawn at the top of it. Only drags
        // are handled here: a horizontal drag has to pass the touch slop before
        // it is consumed, so a plain tap is never swallowed and still opens the
        // player the way a tap anywhere else on the mini player does. That split
        // is what lets one strip carry two gestures without either fighting.
        // **The reach overlaps the row instead of stacking above it.**
        //
        // The 48dp strip used to sit in the layout flow, which put 48dp of
        // empty card above the artwork: the touch target was right and the
        // layout was paying for it in blank ground. The strip is an overlay
        // now, its top half over the card's head and its bottom half over the
        // row's own padding, so the drag keeps its full reach and the card
        // keeps only 18dp of it as visible height. Taps in the strip still
        // open the player, because the card's clickable is an ancestor and the
        // strip only ever consumes drags.
        //
        // **The rule carries a dot at the play head, permanently.** A line
        // with a mark on it reads as draggable; a bare line reads as a
        // progress bar. It is the same dot the contour and the level line
        // already wear, so it costs nothing to learn.
        Box(Modifier.fillMaxWidth()) {
            Column {
                Box(Modifier.height(18.dp))
                MiniRow(state, shown, onPlayPause)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(SCRUB_REACH)
                    .pointerInput(state.durationMs) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                scrubbing = (offset.x / size.width).coerceIn(0f, 1f)
                            },
                            onDragEnd = {
                                scrubbing?.let(onSeek)
                                scrubbing = null
                            },
                            onDragCancel = { scrubbing = null },
                        ) { change, _ ->
                            change.consume()
                            scrubbing = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                    }
                    .semantics {
                        contentDescription = "Position in this piece. Drag to move through it."
                        progressBarRangeInfo = ProgressBarRangeInfo(shown, 0f..1f)
                    },
            ) {
                Box(
                    Modifier
                        .padding(top = 7.dp)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(colors.hairline)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(shown)
                            .height(2.dp)
                            .background(colors.moss)
                    )
                }
                Box(Modifier.padding(top = 3.dp).fillMaxWidth(shown.coerceAtLeast(0.02f))) {
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(colors.moss)
                    )
                }
            }
        }
    }
}

/** The artwork, titles and the play mark: the card's visible body. */
@Composable
private fun MiniRow(
    state: PlaybackState,
    shown: Float,
    onPlayPause: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
        Row(
            Modifier.padding(start = 14.dp, end = 12.dp, top = 4.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cover(
                url = state.artworkUri,
                title = state.title,
                cornerRadius = Radius.cover,
                modifier = Modifier.size(52.dp),
            )
            Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
                Text(
                    text = state.title,
                    style = type.rowTitle,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    // The grid shows "Bride Callanan · andante": the performer
                    // and the tempo, not a running time. Dynamics replace
                    // numbers wherever a number would otherwise do.
                    text = listOfNotNull(state.artist.takeIf { it.isNotBlank() }, state.tempoMark)
                        .joinToString(" · "),
                    style = type.meta,
                    color = colors.tertiaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            IconButton(
                icon = if (state.isPlaying) MeedwellIcons.Pause else MeedwellIcons.Play,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                onClick = onPlayPause,
                size = 24.dp,
                tint = colors.primaryText,
            )
        }
}

/**
 * The reach around the scrub rule.
 *
 * The drawn line stays as thin as the grid sets it; this is the invisible box
 * around it, which was 22dp and under half the 48dp floor.
 */
private val SCRUB_REACH = 48.dp
