package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.components.AmbientGlow
import com.kamsiob.meedwell.ui.components.GlowTone
import com.kamsiob.meedwell.ui.components.IconButton
import com.kamsiob.meedwell.ui.components.IconEdge
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.theme.Elevation
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Radius
import com.kamsiob.meedwell.ui.theme.meedwellShadow

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
    onToggleGroup: (String) -> Unit,
    onOpenCredits: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Box(modifier.fillMaxSize()) {
        AmbientGlow(tone = GlowTone.Teal)
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 22.dp)) {
                IconButton(
                    icon = MeedwellIcons.Back,
                    contentDescription = "Back",
                    onClick = onBack,
                    size = 25.dp,
                    tint = colors.primaryText,
                    edge = IconEdge.Start,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text("Surroundings", style = type.largeHeading, color = colors.primaryText)
                Text(
                    state.voiceLine,
                    style = type.voiceSmall,
                    color = colors.secondaryText,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (state.loadError != null) {
                ErrorPanel(state.loadError, Modifier.padding(horizontal = 22.dp, vertical = 16.dp))
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                modifier = Modifier.weight(1f),
            ) {
                state.groups.forEach { group ->
                    item(key = "g-" + group.id) {
                        GroupHeader(
                            group = group,
                            onToggle = { onToggleGroup(group.id) },
                            onDownloadAll = { onDownloadGroup(group.id) },
                        )
                    }
                    if (group.expanded) {
                        group.sounds.forEach { sound ->
                            sound.categoryHeader?.let { header ->
                                item(key = "c-" + sound.id) {
                                    Text(
                                        header.uppercase(),
                                        style = MeedwellTheme.typography.capsEyebrow,
                                        color = MeedwellTheme.colors.tertiaryText,
                                        modifier = Modifier
                                            .padding(horizontal = 22.dp)
                                            .padding(top = 18.dp, bottom = 4.dp),
                                    )
                                }
                            }
                            item(key = sound.id) {
                                SoundRow(
                                    sound = sound,
                                    playing = state.playingId == sound.id && state.isPlaying,
                                    onPlay = { onPlay(sound.id) },
                                    onPause = onPause,
                                    onDownload = { onDownload(sound.id) },
                                    onCancel = { onCancelDownload(sound.id) },
                                    onRemove = { onRemove(sound.id) },
                                    onOpenDetail = { onOpenDetail(sound.id) },
                                )
                            }
                        }
                    }
                }

                item(key = "foot") {
                    Column(Modifier.padding(horizontal = 22.dp).padding(top = 26.dp)) {
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))

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
                                    .clip(RoundedCornerShape(Radius.panel))
                                    .clickable(role = Role.Button, onClick = onDownloadEverything)
                                    .background(colors.surfacePanel)
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
                                style = type.metadata,
                                color = colors.secondaryText,
                            )
                        }
                        Text(
                            state.storageLine,
                            style = type.metadata,
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
                                style = type.metadata,
                                color = colors.secondaryText,
                            )
                        }
                    }
                }
            }

            if (state.playingId != null) {
                PlayingBar(
                    state = state,
                    onPause = onPause,
                    onResume = { onPlay(state.playingId) },
                    onStop = onStop,
                    onVolume = onVolume,
                )
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
            .clip(RoundedCornerShape(Radius.panel))
            .background(colors.surfacePanel)
            .padding(16.dp),
    ) {
        Text("The library could not be read", style = MeedwellTheme.typography.rowTitle, color = colors.primaryText)
        Text(
            message,
            style = MeedwellTheme.typography.metadata,
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
                Text(group.title, style = type.cardTitle, color = colors.primaryText)
                Text(
                    group.subtitle,
                    style = type.metadata,
                    color = colors.tertiaryText,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            MeedwellIcon(
                icon = if (group.expanded) MeedwellIcons.ChevronDown else MeedwellIcons.ChevronRight,
                size = 16.dp,
                tint = colors.tertiaryText,
            )
        }

        // The cost is stated on the control, not behind it.
        if (group.expanded && group.missingCount > 0) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(Radius.panel))
                    .clickable(role = Role.Button, onClick = onDownloadAll)
                    .background(colors.surfacePanel)
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
                        else -> onDownload()
                    }
                }
                .padding(vertical = 10.dp)
                .semantics { contentDescription = sound.accessibilityLabel(playing) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StateBadge(state = sound.state, playing = playing, progress = sound.progress)

            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    sound.title,
                    style = type.rowTitle,
                    color = if (playing) colors.primaryText else colors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    // The maker's name rides on the row itself, not only in a
                    // credits screen somebody has to go and find. The running
                    // time sits with it because for a bed it is the second
                    // thing anybody wants to know.
                    listOf(sound.durationLabel, sound.subtitle)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = type.metadata,
                    color = colors.tertiaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (sound.failure != null) {
                    Text(
                        sound.failure,
                        style = type.metadata,
                        color = colors.primaryText.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            IconButton(
                icon = MeedwellIcons.Info,
                contentDescription = "Who recorded ${sound.title}, and under what license",
                onClick = onOpenDetail,
                size = 17.dp,
                tint = colors.tertiaryText,
            )
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))
    }
}

/**
 * The one glyph that says where a recording stands.
 *
 * Four states in one slot rather than four different controls, so the eye
 * learns one place to look: here and quiet, here and playing, arriving, or not
 * here yet.
 */
@Composable
private fun StateBadge(state: RowState, playing: Boolean, progress: Float) {
    val colors = MeedwellTheme.colors
    Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
        when {
            state == RowState.Downloading -> {
                // A ring that fills, drawn rather than borrowed, so it matches
                // the rest of the app's line weight.
                Box(
                    Modifier
                        .size(30.dp)
                        .semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                        }
                        .clip(CircleShape)
                        .background(colors.surfacePanel),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size((30 * progress.coerceIn(0.08f, 1f)).dp)
                            .clip(CircleShape)
                            .background(colors.secondaryText.copy(alpha = 0.55f))
                    )
                }
            }
            state == RowState.Here && playing -> MeedwellIcon(
                MeedwellIcons.Pause,
                size = 19.dp,
                tint = colors.primaryText,
            )
            state == RowState.Here -> MeedwellIcon(
                MeedwellIcons.Play,
                size = 19.dp,
                tint = colors.secondaryText,
            )
            else -> MeedwellIcon(
                MeedwellIcons.Download,
                size = 18.dp,
                tint = colors.tertiaryText,
            )
        }
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

    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .meedwellShadow(Elevation.floating, RoundedCornerShape(Radius.floating))
            .clip(RoundedCornerShape(Radius.floating))
            .background(colors.surfacePanel)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                icon = if (state.isPlaying) MeedwellIcons.Pause else MeedwellIcons.Play,
                contentDescription = if (state.isPlaying) "Pause the surroundings" else "Play the surroundings",
                onClick = { if (state.isPlaying) onPause() else onResume() },
                size = 20.dp,
                tint = colors.primaryText,
            )
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    state.playingTitle,
                    style = type.rowTitle,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.playingCredit,
                    style = type.metadata,
                    color = colors.tertiaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                icon = MeedwellIcons.Close,
                contentDescription = "Stop the surroundings",
                onClick = onStop,
                size = 17.dp,
                tint = colors.tertiaryText,
            )
        }

        VolumeSlider(
            value = state.volume,
            onChange = onVolume,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/**
 * The volume of the bed.
 *
 * Its own control rather than a Material slider, for the same reason every
 * other surface here is: a borrowed one arrives with its own track height,
 * thumb and ripple, and would be the only thing on screen not drawn by this
 * app. It is also a plain drag rather than a thumb to grab, which is far easier
 * to hit than a small circle.
 */
@Composable
private fun VolumeSlider(value: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val colors = MeedwellTheme.colors
    var trackWidth by remember { mutableFloatStateOf(1f) }

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MeedwellIcon(MeedwellIcons.VolumeLow, size = 15.dp, tint = colors.tertiaryText)
        Box(
            Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
                // A tall target around a thin track: the line is 4dp and the
                // reach is 44, which is what makes it usable one handed.
                .height(44.dp)
                .onSizeChanged { if (it.width > 0) trackWidth = it.width.toFloat() }
                .semantics {
                    contentDescription = "Surroundings volume"
                    progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onChange((offset.x / trackWidth).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        onChange((change.position.x / trackWidth).coerceIn(0f, 1f))
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.primaryText.copy(alpha = 0.16f))
            )
            Box(
                Modifier
                    .fillMaxWidth(value.coerceIn(0.001f, 1f))
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.primaryText.copy(alpha = 0.75f))
            )
        }
        MeedwellIcon(MeedwellIcons.VolumeHigh, size = 17.dp, tint = colors.tertiaryText)
    }
}

// ---------- State ----------

/** Where one recording stands on this phone. */
enum class RowState { Here, Downloading, Away }

data class SurroundingsRow(
    val id: String,
    /**
     * What the recording is, in the words of the person who catalogued it.
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
    val playingCredit: String = "",
    val isPlaying: Boolean = false,
    val volume: Float = 0.6f,
    val hereCount: Int = 0,
    val totalCount: Int = 0,
    val storageLine: String = "",
    val everythingCostLine: String = "",
    val missingCount: Int = 0,
    val checking: Boolean = false,
    val loadError: String? = null,
) {
    val voiceLine: String
        get() = when {
            loadError != null -> "Something is wrong with the library file."
            totalCount == 0 -> "Nothing here yet."
            hereCount == totalCount -> "All $totalCount recordings are on this phone."
            hereCount == 0 -> "$totalCount recordings, waiting to be fetched."
            else -> "$hereCount of $totalCount on this phone"
        }
}
