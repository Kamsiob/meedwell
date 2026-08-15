package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.playback.PlaybackState
import com.kamsiob.meedwell.ui.components.ContourScrubber
import com.kamsiob.meedwell.ui.components.Cover
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.components.PageEdge
import com.kamsiob.meedwell.ui.components.PageMarks
import com.kamsiob.meedwell.ui.components.PlayerPage
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius
import com.kamsiob.meedwell.ui.theme.Spacing
import kotlin.math.abs

/**
 * The player, as a **two-page horizontal spread**.
 *
 * Music on the left, Surroundings on the right, one swipe apart. This is the
 * single biggest structural correction from the grid: the player was a
 * now-playing screen with Surroundings buried three taps away under More, and
 * the design has them as two pages of one open book.
 *
 * What makes it a spread rather than a pager:
 *
 *  - **Page marks in the app's own iconography**, the copper coin for music and
 *    the sun for Surroundings, inked with a moss underline on the page you are
 *    on and outlined at 30% ink on the other. Not dots.
 *  - **A seven pixel sliver of the facing page** at the screen edge with a
 *    hairline. Without it, the swipe is undiscoverable.
 *  - The Surroundings mark **lights up whenever a sound is running**, even from
 *    the music page, so you can tell rain is playing without leaving the page
 *    you are on.
 *
 * Both pages are always composed. Swiping between two pages that each rebuild
 * on arrival would feel like navigation rather than like turning a page.
 */
@Composable
fun PlayerSpread(
    page: PlayerPage,
    onPageChange: (PlayerPage) -> Unit,
    state: PlaybackState,
    surroundings: SurroundingsPlayingState,
    onCollapse: () -> Unit,
    onMenu: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onOpenArtwork: () -> Unit,
    onOpenQueue: () -> Unit,
    onLove: () -> Unit,
    onSleepTimer: () -> Unit,
    onTone: () -> Unit,
    onSurroundingsPlayPause: () -> Unit,
    onSurroundingsVolume: (Float) -> Unit,
    onSurroundingsCredit: () -> Unit,
    onBrowseSurroundings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors

    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(page) {
                var travelled = 0f
                detectHorizontalDragGestures(
                    onDragStart = { travelled = 0f },
                    onDragEnd = {
                        // A deliberate threshold, so a slightly imprecise tap
                        // on the artwork never turns the page.
                        if (abs(travelled) >= SWIPE_THRESHOLD_PX) {
                            onPageChange(
                                if (travelled < 0) PlayerPage.Surroundings else PlayerPage.Music
                            )
                        }
                    },
                ) { _, delta -> travelled += delta }
            }
            .semantics {
                // The swipe has a named equivalent, because a gesture with no
                // alternative is a feature a TalkBack user does not have.
                customActions = listOf(
                    CustomAccessibilityAction("Surroundings page") {
                        onPageChange(PlayerPage.Surroundings); true
                    },
                    CustomAccessibilityAction("Music page") {
                        onPageChange(PlayerPage.Music); true
                    },
                )
            }
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = Spacing.gutter)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlainIcon(MeedwellIcons.ChevronDown, "Close the player", onCollapse)
                Box(Modifier.weight(1f))
                PlainIcon(MeedwellIcons.Dots, "More actions", onMenu)
            }

            PageMarks(
                page = page,
                surroundingsPlaying = surroundings.isPlaying,
                onSelect = onPageChange,
                modifier = Modifier.padding(top = 12.dp),
            )

            when (page) {
                PlayerPage.Music -> MusicPage(
                    state = state,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onSeek = onSeek,
                    onOpenArtwork = onOpenArtwork,
                    onOpenQueue = onOpenQueue,
                    onLove = onLove,
                    onSleepTimer = onSleepTimer,
                    onTone = onTone,
                )
                PlayerPage.Surroundings -> SurroundingsPage(
                    state = surroundings,
                    onPlayPause = onSurroundingsPlayPause,
                    onVolume = onSurroundingsVolume,
                    onCredit = onSurroundingsCredit,
                    onBrowse = onBrowseSurroundings,
                )
            }
        }

        // The facing page's edge, on whichever side it is.
        PageEdge(
            onRight = page == PlayerPage.Music,
            modifier = Modifier.align(
                if (page == PlayerPage.Music) Alignment.CenterEnd else Alignment.CenterStart
            ),
        )
    }
}

@Composable
private fun MusicPage(
    state: PlaybackState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onOpenArtwork: () -> Unit,
    onOpenQueue: () -> Unit,
    onLove: () -> Unit,
    onSleepTimer: () -> Unit,
    onTone: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    // Artwork complete, never scrimmed and never written on.
    Cover(
        url = state.artworkUri,
        title = state.title,
        cornerRadius = Radius.cover,
        contentDescription = "Cover of ${state.title}. Open the artwork viewer.",
        modifier = Modifier
            .padding(top = 15.dp)
            .fillMaxWidth()
            // Square, because Bandcamp art is square and the law is that
            // artwork is shown complete and never cropped. The grid's 286x210
            // band suits its landscape mock gradients; letterboxing real square
            // art into it would put empty ground either side of the one thing
            // on the screen that is the record.
            .aspectRatio(1f)
            .clickable(role = Role.Button, onClick = onOpenArtwork),
    )

    Column(
        Modifier.fillMaxWidth().padding(top = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            state.title,
            style = type.playerTitle,
            color = colors.primaryText,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            state.artist,
            style = type.rowTitle,
            color = colors.secondaryText,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
        // "andante · IV of IX", in the serif. Dynamics replace numbers.
        // Absent entirely when neither part is known, rather than an empty row.
        val programme = state.programmeLine
        if (programme.isNotBlank()) {
            Text(
                programme,
                style = type.dynamics,
                color = colors.tertiaryText,
                modifier = Modifier.padding(top = 7.dp),
            )
        }
    }

    ContourScrubber(
        progress = state.progress,
        seed = state.trackId.orEmpty().ifBlank { state.title },
        onSeek = onSeek,
        positionLabel = formatClock(state.positionMs),
        durationLabel = formatClock(state.durationMs),
        modifier = Modifier.padding(top = 14.dp),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatClock(state.positionMs), style = type.meta, color = colors.tertiaryText)
        Text(formatClock(state.durationMs), style = type.meta, color = colors.tertiaryText)
    }

    // `.tr { gap: 34px }`, with a 56px filled circle for play.
    Row(
        Modifier.fillMaxWidth().padding(top = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlainIcon(MeedwellIcons.Previous, "Previous", onPrevious, size = 18.dp)
        Box(Modifier.width(34.dp))
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.primaryText)
                .clickable(role = Role.Button, onClick = onPlayPause)
                .semantics { contentDescription = if (state.isPlaying) "Pause" else "Play" },
            contentAlignment = Alignment.Center,
        ) {
            MeedwellIcon(
                icon = if (state.isPlaying) MeedwellIcons.Pause else MeedwellIcons.Play,
                size = 17.dp,
                tint = colors.background,
            )
        }
        Box(Modifier.width(34.dp))
        PlainIcon(MeedwellIcons.Next, "Next", onNext, size = 18.dp)
    }

    // `.subrow`: heart, fermata, tone, queue, share. All tertiary, all quiet.
    Row(
        Modifier.fillMaxWidth().padding(top = 19.dp, start = 3.dp, end = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlainIcon(MeedwellIcons.Heart, "Love this piece", onLove, size = 16.dp)
        // The fermata, which is the sleep timer's mark everywhere. There is no
        // moon in this app.
        PlainIcon(MeedwellIcons.Fermata, "Sleep timer", onSleepTimer, size = 16.dp)
        Box(
            Modifier
                .defaultMinSize(minHeight = 48.dp)
                .clickable(role = Role.Button, onClick = onTone)
                .semantics { contentDescription = "Tone, currently ${state.toneName}" },
            contentAlignment = Alignment.Center,
        ) {
            Text("Tone: ${state.toneName}", style = type.dynamics, color = colors.tertiaryText)
        }
        PlainIcon(MeedwellIcons.QueueOpen, "The queue", onOpenQueue, size = 16.dp)
    }
    Box(Modifier.height(22.dp))
}

@Composable
private fun SurroundingsPage(
    state: SurroundingsPlayingState,
    onPlayPause: () -> Unit,
    onVolume: (Float) -> Unit,
    onCredit: () -> Unit,
    onBrowse: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    if (!state.hasSound) {
        Column(
            Modifier.fillMaxWidth().padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Nothing underneath", style = type.playerTitle, color = colors.primaryText)
            Text(
                "A field recording can sit under the music. Rain, a fire, a room with people in it.",
                style = type.body,
                color = colors.secondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
            Box(
                Modifier
                    .padding(top = 20.dp)
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable(role = Role.Button, onClick = onBrowse)
                    .semantics { contentDescription = "Choose a sound" },
                contentAlignment = Alignment.Center,
            ) {
                Text("Choose a sound", style = type.button, color = colors.primaryText)
            }
        }
        return
    }

    // A plate rather than artwork: these recordings have no cover, and inventing
    // one would be decorating somebody's field recording.
    Box(
        Modifier
            .padding(top = 15.dp)
            .fillMaxWidth()
            .height(142.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(Radius.cover))
            .background(colors.recess)
    )

    Column(
        Modifier.fillMaxWidth().padding(top = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            state.title,
            style = type.playerTitle,
            color = colors.primaryText,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (state.description.isNotBlank()) {
            Text(
                state.description,
                style = type.dynamics,
                color = colors.secondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        // One tertiary line, tappable, exactly as the grid has it.
        Box(
            Modifier
                .padding(top = 6.dp)
                .defaultMinSize(minHeight = 48.dp)
                .clickable(role = Role.Button, onClick = onCredit)
                .semantics { contentDescription = "Who recorded this, and under what license" },
            contentAlignment = Alignment.Center,
        ) {
            Text(state.credit, style = type.meta, color = colors.tertiaryText, textAlign = TextAlign.Center)
        }
    }

    // Volume in dynamics, pp to ff, never 0 to 100.
    DynamicsSlider(
        value = state.volume,
        onChange = onVolume,
        modifier = Modifier.padding(top = 15.dp),
    )

    Row(
        Modifier.fillMaxWidth().padding(top = 22.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.moss)
                .clickable(role = Role.Button, onClick = onPlayPause)
                .semantics {
                    contentDescription = if (state.isPlaying) "Pause the surroundings" else "Play the surroundings"
                },
            contentAlignment = Alignment.Center,
        ) {
            MeedwellIcon(
                icon = if (state.isPlaying) MeedwellIcons.Pause else MeedwellIcons.Play,
                size = 17.dp,
                tint = colors.background,
            )
        }
    }

    Box(Modifier.height(18.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(role = Role.Button, onClick = onBrowse),
        contentAlignment = Alignment.Center,
    ) {
        Text("All recordings", style = type.button, color = colors.secondaryText)
    }
}

/**
 * A volume control labelled in dynamics.
 *
 * `pp` to `ff`, because this app numbers nothing it can name. From the grid:
 * a 2.5px moss track with a 12px moss dot, and the two ends set in the serif.
 */
@Composable
fun DynamicsSlider(
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                // A tall reach around a thin line, which is what makes a 2.5px
                // track usable with a thumb.
                .height(44.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        onChange((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onChange((offset.x / size.width).coerceIn(0f, 1f))
                    }
                }
                .semantics {
                    contentDescription = "Volume, ${dynamicName(value)}"
                    progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.fillMaxWidth().height(2.5.dp).background(colors.hairline))
            Box(
                Modifier
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .height(2.5.dp)
                    .background(colors.moss)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("pp", style = type.dynamics, color = colors.tertiaryText)
            Text("ff", style = type.dynamics, color = colors.tertiaryText)
        }
    }
}

/** The dynamic a volume sits at, for a screen reader that cannot see the line. */
fun dynamicName(value: Float): String = when {
    value <= 0.01f -> "silent"
    value < 0.2f -> "pianissimo"
    value < 0.4f -> "piano"
    value < 0.6f -> "mezzo piano"
    value < 0.8f -> "mezzo forte"
    value < 0.95f -> "forte"
    else -> "fortissimo"
}

@Composable
private fun PlainIcon(
    icon: MeedwellIcons,
    description: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 15.dp,
) {
    Box(
        Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        MeedwellIcon(icon = icon, size = size, tint = MeedwellTheme.colors.tertiaryText)
    }
}

private fun formatClock(ms: Long): String {
    if (ms < 0) return "--:--"
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

private const val SWIPE_THRESHOLD_PX = 90f

/** What the Surroundings page of the spread needs to draw itself. */
data class SurroundingsPlayingState(
    val title: String = "",
    val description: String = "",
    val credit: String = "",
    val isPlaying: Boolean = false,
    val volume: Float = 0.6f,
    val hasSound: Boolean = false,
)
