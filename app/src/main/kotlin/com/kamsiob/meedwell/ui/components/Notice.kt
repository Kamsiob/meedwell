package com.kamsiob.meedwell.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius
import kotlinx.coroutines.delay

/**
 * A short line confirming something happened, then gone.
 *
 * Deliberately not a Material snackbar. A snackbar brings its own type scale,
 * its own corner radius and its own timing, and this app's argument is that
 * every surface is made rather than inherited.
 *
 * Three rules it follows:
 *
 *  - **It never carries an action.** Anything worth doing about the message is
 *    worth a sheet. A vanishing button is a button that punishes reading speed.
 *  - **It is announced.** A live region, so a screen reader hears the
 *    confirmation that a sighted user sees.
 *  - **It dismisses on tap.** Waiting out a message you have already read is a
 *    small, avoidable indignity.
 */
@Composable
fun Notice(
    text: String?,
    /**
     * How much furniture sits at the bottom of this screen already.
     *
     * A notice pinned to the bottom of the window landed on top of the mini
     * player and the surroundings bar, with two lines of text crossing a track
     * title. It has to be told what is below it, because it is drawn over the
     * whole app and cannot see the screen it is floating above.
     */
    liftedBy: Dp = 0.dp,
    onDismiss: () -> Unit,
) {
    // Keyed on the text, so a second notice arriving restarts the clock rather
    // than inheriting the tail of the first one's.
    LaunchedEffect(text) {
        if (text != null) {
            delay(NOTICE_MS)
            onDismiss()
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = text != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            val colors = MeedwellTheme.colors
            Box(
                Modifier
                    .navigationBarsPadding()
                    .padding(bottom = liftedBy)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.cover))
                    .background(colors.background)
                    .clickable(role = Role.Button, onClick = onDismiss)
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = text.orEmpty()
                    },
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text.orEmpty(),
                    style = MeedwellTheme.typography.meta,
                    color = colors.primaryText,
                )
            }
        }
    }
}

/**
 * Long enough to read a full sentence without hurrying, short enough not to sit
 * over the interface. Four seconds, which is the accessibility guidance floor
 * for a message of this length rather than a number picked by eye.
 */
private const val NOTICE_MS = 4_000L
