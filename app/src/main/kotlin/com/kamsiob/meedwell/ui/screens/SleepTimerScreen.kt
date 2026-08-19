package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.core.library.SleepPlan
import com.kamsiob.meedwell.ui.components.DetailHeader
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.components.SectionHead
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import java.util.Calendar

/**
 * Grid screen 23: the sleep timer.
 *
 * **The fermata is the mark**, and it lends the copy its meaning: a fermata is
 * hold, then rest, which is exactly what this does. There is no moon anywhere
 * in this app; a moon means night, and this is not about night.
 *
 * Two things the grid insists on and most sleep timers get wrong:
 *
 *  - **Stop at the end of the piece is a peer**, not an afterthought tucked
 *    under the presets. For the music this app is for, finishing the movement
 *    is often the thing somebody actually wants.
 *  - **The fade is disclosed with its real clock time.** A timer that says
 *    forty five minutes and begins fading at forty four has lied by a minute to
 *    somebody trying to fall asleep. The screen says when it will begin.
 *
 * It stops the music and the surroundings together. One of them stopping and
 * leaving rain running is the failure that wakes somebody up.
 */
@Composable
fun SleepTimerScreen(
    secondsRemaining: Long?,
    atEndOfPiece: Boolean,
    currentPieceTitle: String,
    secondsLeftInPiece: Long,
    onSetMinutes: (Int?) -> Unit,
    onSetEndOfPiece: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    val running = secondsRemaining != null || atEndOfPiece

    Column(
        modifier
            .fillMaxSize()
            // Scrolls, because it does not always fit. With the timer running
            // this screen grows a countdown block, and at a large system font
            // scale on a short phone the "Turn it off" button fell off the
            // bottom with no way to reach it. A screen whose only exit is
            // below the fold is a trap.
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        DetailHeader("Sleep timer", onBack)
        Text(
            "Stops the music and the surroundings together.",
            style = type.voice,
            color = colors.secondaryText,
            modifier = Modifier.padding(top = 8.dp),
        )

        // The presets, as outline chips. Selected is a filled chip, which is
        // the one place the grid does fill a shape: `.chip.on`.
        Row(
            Modifier.fillMaxWidth().padding(top = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            SleepPlan.PRESETS.forEach { minutes ->
                val selected = !atEndOfPiece && secondsRemaining != null &&
                    kotlin.math.abs(secondsRemaining - minutes * 60L) < 60
                Chip(
                    label = "$minutes",
                    selected = selected,
                    onClick = { onSetMinutes(if (selected) null else minutes) },
                    description = "$minutes minutes",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            "min",
            style = type.meta,
            color = colors.tertiaryText,
            modifier = Modifier.padding(top = 6.dp),
        )

        SectionHead("Or", Modifier.padding(top = 24.dp))

        // A peer, not an afterthought.
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clickable(role = Role.Button) { onSetEndOfPiece(!atEndOfPiece) }
                .padding(vertical = 13.dp)
                .semantics {
                    contentDescription = "Stop at the end of this piece"
                    selected = atEndOfPiece
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Stop at the end of this piece",
                    style = type.rowTitle,
                    color = if (atEndOfPiece) colors.mossInk else colors.primaryText,
                )
                if (currentPieceTitle.isNotBlank()) {
                    Text(
                        // The real clock time it will end, and the real title.
                        // "Ends soon" would be the version that says nothing.
                        "Ends at ${clockIn(secondsLeftInPiece)}, after $currentPieceTitle",
                        style = type.rowSub,
                        color = colors.tertiaryText,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            MeedwellIcon(
                icon = MeedwellIcons.ChevronRight,
                size = 13.dp,
                tint = colors.tertiaryText,
            )
        }

        if (running) {
            Column(
                Modifier.fillMaxWidth().padding(top = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MeedwellIcon(MeedwellIcons.Fermata, size = 30.dp, tint = colors.secondaryText)
                Text(
                    if (atEndOfPiece) {
                        SleepPlan.countdown(secondsLeftInPiece)
                    } else {
                        SleepPlan.countdown(secondsRemaining ?: 0)
                    },
                    style = type.h1,
                    color = colors.primaryText,
                    modifier = Modifier.padding(top = 10.dp),
                )
                val fadeIn = if (atEndOfPiece) null else secondsRemaining?.let { SleepPlan.fadeBeginsIn(it) }
                Text(
                    when {
                        atEndOfPiece -> "It will stop when this piece finishes, with no fade."
                        // Not "ritardando": that is a slowing of tempo, and what happens
                        // is a fade. The one audience guaranteed to catch the wrong
                        // Italian is this one.
                        fadeIn != null -> "Easing down to silence at ${clockIn(fadeIn)}."
                        else -> "Fading to silence now."
                    },
                    style = type.meta,
                    color = colors.tertiaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Box(
                    Modifier
                        .padding(top = 18.dp)
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable(role = Role.Button) {
                            onSetMinutes(null)
                            onSetEndOfPiece(false)
                        }
                        .semantics { contentDescription = "Turn the sleep timer off" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Turn it off", style = type.button, color = colors.secondaryText)
                }
            }
        }
    }
}

@Composable
private fun Chip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    Box(
        modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(colors.primaryText)
                else Modifier.border(1.dp, colors.hairline2, CircleShape)
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
            }
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MeedwellTheme.typography.chip,
            color = if (selected) colors.background else colors.primaryText,
        )
        Box(Modifier.height(34.dp))
    }
}

/**
 * A wall-clock time this many seconds from now, in the phone's own clock format.
 *
 * The grid states an actual time rather than a duration, which is the whole
 * point: somebody setting a timer at night wants to know when, not how long.
 */
@Composable
private fun clockIn(seconds: Long): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    val c = Calendar.getInstance()
    c.add(Calendar.SECOND, seconds.toInt())
    // The phone's own clock format. A 12-hour listener was reading "22:32" on
    // the one screen they only ever visit at night.
    return android.text.format.DateFormat.getTimeFormat(context).format(c.time)
}
