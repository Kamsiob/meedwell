package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.playback.PlaybackState
import com.kamsiob.meedwell.playback.RepeatMode
import com.kamsiob.meedwell.ui.components.Cover
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.components.Waveform
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * Screen 16 in the visual reference: now playing.
 *
 * **The single sanctioned text-over-color moment in the entire app.** The wash
 * behind this screen is a palette-derived color field, which is *not* the
 * artwork, clamped below a brightness ceiling so white always passes on any
 * album in either theme. The complete cover sits above it, untouched.
 *
 * That distinction is the whole reason the adaptive-scrim law could be retired:
 * a color field can be clamped with no worst case, and artwork cannot.
 *
 * The live waveform is the scrubber. Swiping the cover skips; tapping it opens
 * the artwork viewer; the two gestures never conflict because the swipe needs
 * horizontal travel and the tap does not.
 */
@Composable
fun NowPlayingScreen(
    state: PlaybackState,
    washColor: Color,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onOpenArtwork: () -> Unit,
    onOpenQueue: () -> Unit,
    onMenu: () -> Unit,
    onShuffle: (Boolean) -> Unit,
    onCycleRepeat: () -> Unit,
    onLove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {

        // The wash. A color field, clamped, never the artwork itself.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to washColor,
                        0.55f to washColor.copy(alpha = 0.45f),
                        1f to colors.background,
                    )
                )
        )
        // A floor under the text half, so white passes regardless of what the
        // palette produced. This is the clamp, and it is why there is no worst
        // case here.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.72f),
                    )
                )
        )

        Column(Modifier.fillMaxSize().padding(horizontal = 26.dp)) {

            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable(role = Role.Button, onClick = onCollapse)
                        .semantics { contentDescription = "Close the player" },
                    contentAlignment = Alignment.Center,
                ) { MeedwellIcon(MeedwellIcons.ChevronDown, size = 20.dp, tint = Color.White) }
                Box(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable(role = Role.Button, onClick = onMenu)
                        .semantics { contentDescription = "More actions for this track" },
                    contentAlignment = Alignment.Center,
                ) { MeedwellIcon(MeedwellIcons.Dots, size = 20.dp, tint = Color.White) }
            }

            // The cover, complete, above the wash. Swipe to skip, tap to open.
            Cover(
                url = state.artworkUri,
                title = state.title,
                cornerRadius = 14.dp,
                modifier = Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth(0.72f)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally)
                    .clickable(role = Role.Button, onClick = onOpenArtwork)
                    .pointerInput(state.trackId) {
                        var traveled = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { traveled = 0f },
                            onDragEnd = {
                                // A deliberate threshold, so a slightly
                                // imprecise tap never becomes a skip.
                                if (traveled <= -SWIPE_THRESHOLD_PX) onNext()
                                else if (traveled >= SWIPE_THRESHOLD_PX) onPrevious()
                            },
                        ) { _, delta -> traveled += delta }
                    }
                    .semantics {
                        contentDescription = "Cover of ${state.title}. Open the artwork viewer."
                        // TalkBack users get the skip gestures as named
                        // actions, because a gesture with no equivalent is a
                        // feature that user does not have.
                        customActions = listOf(
                            CustomAccessibilityAction("Next track") { onNext(); true },
                            CustomAccessibilityAction("Previous track") { onPrevious(); true },
                        )
                    },
            )

            Box(Modifier.weight(1f))

            Column(Modifier.padding(bottom = 26.dp)) {
                Text("From the shelf", style = type.voiceSmall, color = Color.White.copy(alpha = 0.74f))
                Text(
                    text = state.title,
                    style = type.sectionHeading,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = state.artist,
                    style = type.body,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Honest about what is being played. Verified: Bandcamp serves
                // MP3 V0, so nothing here may imply more.
                Text(
                    text = state.qualityLine.uppercase(),
                    style = type.capsEyebrow,
                    color = Color.White.copy(alpha = 0.58f),
                    modifier = Modifier.padding(top = 12.dp),
                )

                Waveform(
                    progress = state.progress,
                    animate = state.isPlaying,
                    onSeek = onSeek,
                    positionLabel = formatDuration(state.positionMs / 1000),
                    durationLabel = formatDuration(state.durationMs / 1000),
                    // Padding before the size, not after. Applying padding to a
                    // sized modifier shrinks the canvas inside it: the first
                    // version left 20dp of drawing space inside a 38dp box and
                    // the waveform rendered as a row of dots.
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .fillMaxWidth()
                        .height(46.dp),
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        formatDuration(state.positionMs / 1000),
                        style = type.metadata,
                        color = Color.White.copy(alpha = 0.58f),
                    )
                    Text(
                        "-" + formatDuration(((state.durationMs - state.positionMs) / 1000).coerceAtLeast(0)),
                        style = type.metadata,
                        color = Color.White.copy(alpha = 0.58f),
                    )
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TransportButton("Previous track", MeedwellIcons.Previous, onPrevious)
                    Box(Modifier.width(40.dp))
                    TransportButton(
                        description = if (state.isPlaying) "Pause" else "Play",
                        icon = if (state.isPlaying) MeedwellIcons.Pause else MeedwellIcons.Play,
                        onClick = onPlayPause,
                        large = true,
                    )
                    Box(Modifier.width(40.dp))
                    TransportButton("Next track", MeedwellIcons.Next, onNext)
                }

                // The second row: the things you set once and forget, kept
                // deliberately quieter than the transport above so the thumb
                // never lands on repeat while reaching for play.
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SecondaryButton(
                        description = if (state.shuffle) "Shuffle is on. Turn it off." else "Shuffle",
                        icon = MeedwellIcons.Shuffle,
                        active = state.shuffle,
                        onClick = { onShuffle(!state.shuffle) },
                    )
                    SecondaryButton(
                        description = when (state.repeat) {
                            RepeatMode.Off -> "Repeat is off. Repeat everything."
                            RepeatMode.All -> "Repeating everything. Repeat this one."
                            RepeatMode.One -> "Repeating this one. Turn repeat off."
                        },
                        icon = if (state.repeat == RepeatMode.One) MeedwellIcons.RepeatOne else MeedwellIcons.Repeat,
                        active = state.repeat != RepeatMode.Off,
                        onClick = onCycleRepeat,
                    )
                    SecondaryButton(
                        description = "Love this track",
                        icon = MeedwellIcons.Heart,
                        active = false,
                        onClick = onLove,
                    )
                    // An icon rather than the word it used to be. Three glyphs
                    // and one word in a row of four reads as an afterthought
                    // stuck on the end, and the row is a set.
                    SecondaryButton(
                        description = if (state.queueSize > 1) {
                            "Open the queue. ${state.queueSize} tracks."
                        } else {
                            "Open the queue"
                        },
                        icon = MeedwellIcons.QueueOpen,
                        active = false,
                        onClick = onOpenQueue,
                    )
                }
            }
        }
    }
}

@Composable
private fun TransportButton(
    description: String,
    icon: MeedwellIcons,
    onClick: () -> Unit,
    large: Boolean = false,
) {
    Box(
        Modifier
            .size(if (large) 64.dp else 52.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        // The play control is deliberately larger than the skips. It is the one
        // a thumb reaches for without looking.
        MeedwellIcon(icon = icon, size = if (large) 40.dp else 24.dp, tint = Color.White)
    }
}

/**
 * A control in the second row.
 *
 * Smaller and dimmer than the transport, and **on is carried by opacity rather
 * than by a colored fill.** A tinted pill for "shuffle is on" would be the only
 * accent color on a screen whose entire premise is one clamped wash, and it
 * would put a second bright thing next to the play button.
 *
 * On also gets a dot beneath it, so the state does not rest on brightness alone
 * for anybody who cannot see the difference.
 */
@Composable
private fun SecondaryButton(
    description: String,
    icon: MeedwellIcons,
    active: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        MeedwellIcon(
            icon = icon,
            size = 21.dp,
            tint = Color.White.copy(alpha = if (active) 1f else 0.55f),
        )
        if (active) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 7.dp)
                    .size(3.5.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

private const val SWIPE_THRESHOLD_PX = 120f

/**
 * What is actually being played, said plainly.
 *
 * A local file names its format. A stream says MP3, because that is what
 * Bandcamp's API serves and no copy anywhere in this app may imply otherwise.
 */
private val PlaybackState.qualityLine: String
    get() = if (isLocalFile) "Playing the file on this phone" else "Streaming, MP3 from Bandcamp"
