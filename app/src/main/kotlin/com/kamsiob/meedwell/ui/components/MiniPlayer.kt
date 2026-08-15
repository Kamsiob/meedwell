package com.kamsiob.meedwell.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.playback.PlaybackState
import com.kamsiob.meedwell.ui.theme.Elevation
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius
import com.kamsiob.meedwell.ui.theme.meedwellShadow

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
    modifier: Modifier = Modifier,
) {
    if (!state.hasQueue) return

    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    val shape = RoundedCornerShape(Radius.floating)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .meedwellShadow(Elevation.floating, shape)
            .clip(shape)
            .background(colors.surfacePanel)
            .border(0.5.dp, colors.hairline, shape)
            .clickable(role = Role.Button, onClick = onOpen)
            .semantics {
                contentDescription = "Now playing: ${state.title} by ${state.artist}. Open the player."
            }
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(
            url = state.artworkUri,
            title = state.title,
            cornerRadius = 5.dp,
            modifier = Modifier.size(40.dp),
        )
        Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
            Text(
                text = state.title,
                // Title and artist were the same size and weight, separated
                // only by colour. The reference gives the title its own weight.
                style = type.cardTitle,
                color = colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.artist,
                style = type.metadata,
                color = colors.tertiaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Waveform(
            progress = state.progress,
            animate = state.isPlaying,
            // Eight bars, as the reference draws it. The default of 28 needed
            // 67dp of gaps inside a 64dp box, so every bar clamped to one
            // physical pixel and the row rendered as a grey smear that read
            // like a loading skeleton.
            barCount = 8,
            modifier = Modifier
                .padding(end = 10.dp)
                .size(width = 80.dp, height = 22.dp),
        )
        IconButton(
            icon = if (state.isPlaying) MeedwellIcons.Pause else MeedwellIcons.Play,
            contentDescription = if (state.isPlaying) "Pause" else "Play",
            onClick = onPlayPause,
            size = 20.dp,
            tint = colors.primaryText,
        )
    }
}
