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
    modifier: Modifier = Modifier,
) {
    if (!state.hasQueue) return

    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Column(
        modifier
            .fillMaxWidth()
            .background(colors.background)
            .clickable(role = Role.Button, onClick = onOpen)
            .semantics {
                contentDescription = "Now playing: ${state.title} by ${state.artist}. Open the player."
            },
    ) {
        // `.mini .pg`: a 2px rule across the very top, filled in moss as far as
        // the piece has got. This is the only progress indicator outside the
        // player itself, and it is a rule rather than a bar because there are
        // no filled containers in this design to put a bar inside.
        Box(Modifier.fillMaxWidth().height(2.dp).background(colors.hairline)) {
            Box(
                Modifier
                    .fillMaxWidth(state.progress.coerceIn(0f, 1f))
                    .height(2.dp)
                    .background(colors.moss)
            )
        }

        Row(
            // `.mini { padding: 9px 22px 10px }`.
            Modifier.padding(start = 22.dp, end = 22.dp, top = 9.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cover(
                url = state.artworkUri,
                title = state.title,
                // `.mini .ar { width:34px; height:34px; border-radius:4px }`.
                cornerRadius = Radius.miniArtwork,
                modifier = Modifier.size(34.dp),
            )
            Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                Text(
                    text = state.title,
                    style = type.miniTitle,
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
                    style = type.miniSub,
                    color = colors.tertiaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            IconButton(
                icon = if (state.isPlaying) MeedwellIcons.Pause else MeedwellIcons.Play,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                onClick = onPlayPause,
                size = 15.dp,
                tint = colors.primaryText,
            )
        }
    }
}
