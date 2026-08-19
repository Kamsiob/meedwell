package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.components.Hairline
import com.kamsiob.meedwell.ui.components.SunMark
import com.kamsiob.meedwell.ui.components.LevelLine
import com.kamsiob.meedwell.ui.components.HoldLine
import com.kamsiob.meedwell.ui.components.SectionHead
import com.kamsiob.meedwell.ui.components.DetailHeader
import com.kamsiob.meedwell.ui.components.IconButton
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius

/**
 * Surroundings: a field recording playing under whatever else is on.
 *
 * Three things this screen is careful about, and each of them is a promise the
 * rest of the app makes too:
 *
 *  - **Nothing is fetched without saying what it costs.** Every download
 *    control states its size, its count and its running time before it is
 *    tapped. There is no "get all" that silently spends a gigabyte.
 *  - **Nobody is shown a recording that cannot be credited.** The manifest is
 *    filtered upstream, so anything with a gap in its attribution never reaches
 *    this screen at all. A group whose count looks short is short for that
 *    reason.
 *  - **The maker's name travels with the sound.** It is on the row, on the
 *    playing bar, and in full on the credits screen. Quiet, but never absent.
 */
@Composable
fun SurroundingsScreen(
    state: SurroundingsUiState,
    onPlay: (String) -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onVolume: (Float) -> Unit,
    onDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDownloadGroup: (String) -> Unit,
    onDownloadEverything: () -> Unit,
    onCheckForNew: () -> Unit,
    onRemove: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenGroup: (String) -> Unit,
    onOpenStorage: () -> Unit,
    onSearch: (String) -> Unit,
    onOpenCredits: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 22.dp)) {
                DetailHeader("Surroundings", onBack)
                Text(
                    state.voiceLine,
                    style = type.voice,
                    color = colors.secondaryText,
                    modifier = Modifier.padding(top = 8.dp),
                )

                // **Search, because 111 recordings behind nine headings had no
                // way to be found by name at all.** A hairline pill, the same
                // control the shelf uses, so it is recognized rather than
                // learned.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .defaultMinSize(minHeight = 48.dp)
                        .border(1.dp, colors.hairline2, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MeedwellIcon(MeedwellIcons.Search, size = 14.dp, tint = colors.tertiaryText)
                    Box(
                        Modifier.weight(1f).padding(start = 9.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (state.query.isEmpty()) {
                            Text(
                                "Rain, fire, a room",
                                style = type.chip,
                                color = colors.tertiaryText,
                            )
                        }
                        BasicTextField(
                            value = state.query,
                            onValueChange = onSearch,
                            singleLine = true,
                            textStyle = type.chip.copy(color = colors.primaryText),
                            cursorBrush = SolidColor(colors.mossInk),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Search the recordings" },
                        )
                    }
                    if (state.query.isNotEmpty()) {
                        Box(
                            Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clickable(role = Role.Button) { onSearch("") }
                                .semantics { contentDescription = "Clear the search" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Clear", style = type.chip, color = colors.tertiaryText)
                        }
                    }
                }

                // **What is playing sits at the top.**
                //
                // Grid 13's caption is a direct instruction: "What is playing
                // sits at the top so stopping it never requires hunting." It was
                // pinned to the bottom of the screen instead, below every group,
                // which is the one place you do not look when you arrive
                // wondering what that sound is. Groups also open collapsed, so
                // landing here with a bed running showed nine closed headers and
                // nothing saying which one held it.
                if (state.playingId != null) {
                    SectionHead("Playing", Modifier.padding(top = 18.dp, bottom = 2.dp))
                    PlayingBar(
                        state = state,
                        onPause = onPause,
                        onResume = { onPlay(state.playingId) },
                        onStop = onStop,
                        onVolume = onVolume,
                    )
                }
            }

            if (state.loadError != null) {
                ErrorPanel(state.loadError, Modifier.padding(horizontal = 22.dp, vertical = 16.dp))
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (state.query.isNotBlank()) {
                    if (state.results.isEmpty()) {
                        item(key = "no-results") {
                            Text(
                                "Nothing here matches that.",
                                style = type.meta,
                                color = colors.tertiaryText,
                                modifier = Modifier.padding(horizontal = 22.dp).padding(top = 18.dp),
                            )
                        }
                    }
                    items(state.results, key = { "r-" + it.id }) { sound ->
                        SoundRow(
                            sound = sound,
                            playing = state.playingId == sound.id && state.isPlaying,
                            current = state.playingId == sound.id,
                            onPlay = { onPlay(sound.id) },
                            onPause = onPause,
                            onDownload = { onDownload(sound.id) },
                            onCancel = { onCancelDownload(sound.id) },
                            onRemove = { onRemove(sound.id) },
                            onOpenDetail = { onOpenDetail(sound.id) },
                        )
                    }
                } else {
                    // **What can be played right now, before the doors.**
                    //
                    // The tab used to open on nine closed headings, so arriving
                    // with a bed running showed nothing about it and arriving
                    // fresh showed nothing you could start.
                    if (state.onPhone.isNotEmpty()) {
                        item(key = "here-head") {
                            Column(Modifier.padding(horizontal = 22.dp)) {
                                SectionHead("On this phone", Modifier.padding(top = 20.dp, bottom = 2.dp))
                            }
                        }
                        items(state.onPhone.take(6), key = { "h-" + it.id }) { sound ->
                            SoundRow(
                                sound = sound,
                                playing = state.playingId == sound.id && state.isPlaying,
                                current = state.playingId == sound.id,
                                onPlay = { onPlay(sound.id) },
                                onPause = onPause,
                                onDownload = { onDownload(sound.id) },
                                onCancel = { onCancelDownload(sound.id) },
                                onRemove = { onRemove(sound.id) },
                                onOpenDetail = { onOpenDetail(sound.id) },
                            )
                        }
                    }

                    item(key = "library-head") {
                        Column(Modifier.padding(horizontal = 22.dp)) {
                            SectionHead("The library", Modifier.padding(top = 20.dp, bottom = 2.dp))
                        }
                    }
                    // **A group is a destination now, not an accordion.**
                    //
                    // Opening one in place changed the screen's length under a
                    // thumb and still left eight closed doors above and below.
                    items(state.groups, key = { "g-" + it.id }) { group ->
                        GroupHeader(
                            group = group,
                            onToggle = { onOpenGroup(group.id) },
                            onDownloadAll = { onDownloadGroup(group.id) },
                        )
                    }
                }

                item(key = "foot") {
                    Column(Modifier.padding(horizontal = 22.dp).padding(top = 26.dp)) {
                        Hairline()

                        // Storage, which is the only place a recording can be
                        // removed. It used to have nowhere to live at all.
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clickable(role = Role.Button, onClick = onOpenStorage)
                                .padding(vertical = 14.dp)
                                .semantics { contentDescription = "Surroundings storage" },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Storage",
                                style = type.rowTitle,
                                color = colors.primaryText,
                                modifier = Modifier.weight(1f),
                            )
                            Text(state.storageLine, style = type.meta, color = colors.tertiaryText)
                            MeedwellIcon(
                                MeedwellIcons.ChevronRight,
                                size = 14.dp,
                                tint = colors.tertiaryText,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        Hairline()

                        // The third granularity: the whole thing at once.
                        // Deliberately at the bottom rather than the top, and
                        // deliberately stating a number in gigabytes. It is a
                        // real option, not the one being nudged toward.
                        if (state.missingCount > 0) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(Radius.cover))
                                    .clickable(role = Role.Button, onClick = onDownloadEverything)
                                    .background(colors.background)
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .semantics {
                                        contentDescription = "Get the whole library. ${state.everythingCostLine}"
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Get the whole library", style = type.rowTitle, color = colors.primaryText)
                                    Text(state.everythingCostLine, style = type.numeric, color = colors.tertiaryText)
                                }
                                MeedwellIcon(MeedwellIcons.Download, size = 18.dp, tint = colors.secondaryText)
                            }
                        }

                        // Updates are never automatic. Nothing checks on a
                        // timer, on launch, or in the background; a person taps
                        // a control that says what it will do.
                        Box(
                            Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable(enabled = !state.checking, role = Role.Button, onClick = onCheckForNew)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                if (state.checking) "Asking the library…" else "Check for new recordings",
                                style = type.meta,
                                color = colors.secondaryText,
                            )
                        }
                        Text(
                            state.storageLine,
                            style = type.meta,
                            color = colors.tertiaryText,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                        Box(
                            Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable(role = Role.Button, onClick = onOpenCredits)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                "Everyone whose recording is in here ›",
                                style = type.meta,
                                color = colors.secondaryText,
                            )
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun ErrorPanel(message: String, modifier: Modifier = Modifier) {
    val colors = MeedwellTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.cover))
            .background(colors.recess)
            .padding(16.dp),
    ) {
        Text("The library could not be read", style = MeedwellTheme.typography.rowTitle, color = colors.primaryText)
        Text(
            message,
            style = MeedwellTheme.typography.meta,
            color = colors.secondaryText,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun GroupHeader(group: SurroundingsGroup, onToggle: () -> Unit, onDownloadAll: () -> Unit) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    Column(Modifier.padding(horizontal = 22.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clickable(role = Role.Button, onClick = onToggle)
                .padding(vertical = 10.dp)
                .semantics {
                    contentDescription = if (group.expanded) {
                        "${group.title}, showing ${group.sounds.size}. Tap to close."
                    } else {
                        "${group.title}, ${group.sounds.size} recordings. Tap to open."
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(group.title, style = type.gridTitle, color = colors.primaryText)
                Text(
                    group.subtitle,
                    style = type.meta,
                    color = colors.tertiaryText,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            // How much of this group is already here, as a hairline gauge.
            //
            // Nine group rows were nine identical blocks of two lines, with the
            // only difference buried at the end of a sentence. This gives the
            // column a shape to scan, and the subtitle beside it says the same
            // fact in words, so the mark is never carrying the meaning alone.
            HoldLine(
                here = group.sounds.count { it.state != RowState.Away },
                total = group.sounds.size,
                modifier = Modifier.padding(end = 12.dp),
            )
            MeedwellIcon(
                icon = if (group.expanded) MeedwellIcons.ChevronDown else MeedwellIcons.ChevronRight,
                size = 14.dp,
                tint = colors.tertiaryText,
            )
        }

        // The cost is stated on the control, not behind it.
        if (group.expanded && group.missingCount > 0) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(Radius.cover))
                    .clickable(role = Role.Button, onClick = onDownloadAll)
                    .background(colors.background)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .semantics { contentDescription = "Get all of ${group.title}. ${group.costLine}" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Get all of these", style = type.rowTitle, color = colors.primaryText)
                    Text(group.costLine, style = type.numeric, color = colors.tertiaryText)
                }
                MeedwellIcon(MeedwellIcons.Download, size = 18.dp, tint = colors.secondaryText)
            }
        }
        Box(Modifier.height(4.dp))
    }
}

@Composable
private fun SoundRow(
    sound: SurroundingsRow,
    playing: Boolean,
    current: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Column(Modifier.padding(horizontal = 22.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp)
                .clickable(role = Role.Button) {
                    when {
                        sound.state == RowState.Here && playing -> onPause()
                        sound.state == RowState.Here -> onPlay()
                        sound.state == RowState.Downloading -> onCancel()
                        // Not here, and the body does nothing. Getting it is the
                        // status column's job, where the size is printed.
                        else -> Unit
                    }
                }
                .padding(vertical = 10.dp)
                .semantics { contentDescription = sound.accessibilityLabel(playing) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Present or not, said by a filled or hollow sun.
            //
            // This replaces a badge that swapped between a download arrow and a
            // play triangle in the same slot, which meant the only signal for
            // "is this on my phone" was a shape you had to already know the code
            // for, one row at a time. Filled reads as here down the whole column
            // without reading anything.
            SunMark(
                here = sound.state != RowState.Away,
                playing = playing,
                current = current,
            )

            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    sound.title,
                    style = type.rowTitle,
                    color = if (current) colors.primaryText else colors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    // The maker's name rides on the row itself, not only in a
                    // credits screen somebody has to go and find. The running
                    // time has moved to the status column, where it is one of
                    // the four things that column says.
                    sound.subtitle,
                    style = type.meta,
                    color = colors.tertiaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (sound.failure != null) {
                    Text(
                        sound.failure,
                        style = type.meta,
                        color = colors.primaryText.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // **One column, four answers.**
            //
            // Grid 14 words it exactly: installed items show duration, and ones
            // that are not here show their size. Carrying "playing" and "paused"
            // in the same slot costs nothing and finally puts the state of the
            // bed in the list rather than only in a bar at the far end of the
            // screen. The size figure was already being computed and was reaching
            // the screen reader only, so a sighted person was never told what a
            // download would cost before tapping it.
            //
            // When a recording is not here, this column **is** the download
            // button. The row tap used to do four different things depending on
            // invisible state, so a stray tap could start a transfer. Now the
            // row plays what is already here, and getting something is its own
            // named target with the price on it.
            val away = sound.state == RowState.Away
            Box(
                Modifier
                    .widthIn(min = 62.dp)
                    .defaultMinSize(minHeight = 48.dp)
                    .then(
                        if (away) {
                            Modifier
                                .clickable(role = Role.Button, onClick = onDownload)
                                .semantics {
                                    contentDescription = "Get ${sound.title}, ${sound.sizeLabel}"
                                }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.CenterEnd,
            ) {
                val (statusText, statusInk) = when {
                    playing -> "playing" to colors.mossInk
                    current -> "paused" to colors.mossInk
                    sound.state == RowState.Downloading -> "getting" to colors.secondaryText
                    away -> sound.sizeLabel to colors.tertiaryText
                    else -> sound.durationLabel to colors.tertiaryText
                }
                Text(
                    statusText,
                    style = type.plate,
                    color = statusInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(
                icon = MeedwellIcons.Info,
                contentDescription = "Who recorded ${sound.title}, and under what license",
                onClick = onOpenDetail,
                size = 17.dp,
                tint = colors.tertiaryText,
            )
        }
        Hairline()
    }
}



/**
 * The bed that is playing, pinned to the bottom.
 *
 * Carries the recordist's name, because a credit that only exists two screens
 * away is a credit most people never see.
 */
@Composable
private fun PlayingBar(
    state: SurroundingsUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onVolume: (Float) -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    // **No pseudo container.** This used to be a rounded box filled with the
    // same color as the page behind it, which is an invisible card whose only
    // effect was to indent its own text. What sets this block apart is that it
    // is at the top under its own section head.
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            SunMark(
                here = true,
                playing = state.isPlaying,
                width = 20.dp,
                height = 16.dp,
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    state.playingTitle,
                    style = type.h2,
                    color = colors.primaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.playingDescription.isNotBlank()) {
                    Text(
                        state.playingDescription,
                        style = type.tempo,
                        color = colors.tertiaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Text(
                    state.playingCredit,
                    style = type.meta,
                    color = colors.tertiaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }

        LevelLine(
            value = state.volume,
            onChange = onVolume,
            playing = state.isPlaying,
            modifier = Modifier.padding(top = 4.dp),
        )

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(colors.moss)
                    .clickable(role = Role.Button) { if (state.isPlaying) onPause() else onResume() }
                    .semantics {
                        contentDescription =
                            if (state.isPlaying) "Pause the surroundings" else "Play the surroundings"
                    },
                contentAlignment = Alignment.Center,
            ) {
                MeedwellIcon(
                    icon = if (state.isPlaying) MeedwellIcons.Pause else MeedwellIcons.Play,
                    size = 16.dp,
                    tint = colors.background,
                )
            }
            // **Stopping is a word, not an ✕.**
            //
            // The cross said "put this panel away" everywhere else in the app
            // and on the platform, and here it ended the sound. The glyph and
            // the effect disagreed.
            Box(
                Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clickable(role = Role.Button, onClick = onStop)
                    .padding(start = 14.dp, end = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Stop", style = type.chip, color = colors.tertiaryText)
            }
        }
    }
}


// ---------- State ----------

/** Where one recording stands on this phone. */
enum class RowState { Here, Downloading, Away }

data class SurroundingsRow(
    val id: String,
    /**
     * What the recording is, in the words of the person who catalogd it.
     *
     * Deliberately **not** the uploader's own title. Those are filenames as
     * often as names: "campfire deer camp 21 minutes - wind - snow - sleet -
     * part 5" tells you almost nothing at a glance and is unreadable truncated
     * to a row. The catalog description says what you would actually hear. The
     * original title is not lost; it is on the credit sheet, where somebody
     * looking for the source needs it.
     */
    val title: String,
    /** The recordist and the license, generated from the manifest, never typed. */
    val subtitle: String,
    val state: RowState,
    /**
     * The category this recording belongs to, shown only on the first row of
     * each one. Thirty recordings of water in a flat list is a wall; the same
     * thirty under "Rain on leaves", "Rain in a city street" and "Distant
     * thunder" is a thing you can choose from.
     */
    val categoryHeader: String? = null,
    val durationLabel: String = "",
    val progress: Float = 0f,
    val sizeLabel: String = "",
    val failure: String? = null,
) {
    fun accessibilityLabel(playing: Boolean): String = when {
        state == RowState.Here && playing -> "$title, playing. Tap to pause."
        state == RowState.Here -> "$title, on this phone. Tap to play."
        state == RowState.Downloading -> "$title, downloading. Tap to stop."
        else -> "$title, not here yet. Tap to get it, $sizeLabel."
    }
}

data class SurroundingsGroup(
    val id: String,
    val title: String,
    val subtitle: String,
    val costLine: String,
    val missingCount: Int,
    val expanded: Boolean,
    val sounds: List<SurroundingsRow>,
)

data class SurroundingsUiState(
    val groups: List<SurroundingsGroup> = emptyList(),
    val playingId: String? = null,
    val playingTitle: String = "",
    /**
     * The catalog's one line about what this recording is, which the player
     * spread sets under the title. Carried in the manifest and, until now, never
     * shown anywhere.
     */
    val playingDescription: String = "",
    /** Its group, which the player's plate draws its field from. */
    val playingGroup: String = "",
    val playingCredit: String = "",
    val isPlaying: Boolean = false,
    val volume: Float = 0.6f,
    /** What is typed into the library's search field. */
    val query: String = "",
    /** Matches for that query, flat, across every group. */
    val results: List<SurroundingsRow> = emptyList(),
    /** Everything on this phone, most recently used first. */
    val onPhone: List<SurroundingsRow> = emptyList(),
    /** Came with the app, and cannot be removed. */
    val bundledRows: List<SurroundingsRow> = emptyList(),
    /** Fetched later, and can be removed again. */
    val downloadedRows: List<SurroundingsRow> = emptyList(),
    val hereCount: Int = 0,
    val totalCount: Int = 0,
    val storageLine: String = "",
    val everythingCostLine: String = "",
    val missingCount: Int = 0,
    val checking: Boolean = false,
    val loadError: String? = null,
) {
    /**
     * The screen's one serif line, and it is a description rather than a count.
     *
     * It used to read "55 of 111 on this phone", which answers a question nobody
     * arriving here is asking. Somebody opening this tab for the first time
     * needs to know what Surroundings is, and the honest answer states both uses
     * in nine words. The counts moved to the group rows and the storage line,
     * where they are attached to something you can act on.
     */
    val voiceLine: String
        get() = when {
            loadError != null -> "Something is wrong with the library file."
            totalCount == 0 -> "Nothing here yet."
            else -> "Recordings of real places. Put one under your music, or enjoy one on its own."
        }
}
