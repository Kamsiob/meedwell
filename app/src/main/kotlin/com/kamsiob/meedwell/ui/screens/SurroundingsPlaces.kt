package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.ui.components.DetailHeader
import com.kamsiob.meedwell.ui.components.Hairline
import com.kamsiob.meedwell.ui.components.IconButton
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.components.SectionHead
import com.kamsiob.meedwell.ui.components.SunMark
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * One group of recordings, as its own screen.
 *
 * **This replaces the accordion.** Nine groups that expanded in place meant
 * arriving at Surroundings with a bed playing showed nine closed headings and
 * nothing saying which one held it, and it meant the screen's length changed
 * under your thumb every time you opened one. The grid always drew a group as a
 * destination; the accordion was the shortcut.
 *
 * **One meaning per row tap.** Here, so play it, or pause it if it is the bed
 * that is running. Not here, so open its sheet, which states the size before
 * anything is fetched. A tap never silently starts a transfer, which is what the
 * old shared row tap did.
 *
 * Every row carries the recordist and the license under its title, because that
 * is how the attribution conditions are met wherever a recording appears.
 */
@Composable
fun SurroundingsGroupScreen(
    group: SurroundingsGroup?,
    playingId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    onPause: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onDownloadAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 22.dp)) {
            DetailHeader(group?.title ?: "Surroundings", onBack)
            Text(
                group?.subtitle.orEmpty(),
                style = type.voice,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 11.dp),
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(group?.sounds.orEmpty(), key = { it.id }) { sound ->
                Column(Modifier.padding(horizontal = 22.dp)) {
                    sound.categoryHeader?.let {
                        SectionHead(it, Modifier.padding(top = 18.dp, bottom = 2.dp))
                    }
                    RecordingRow(
                        sound = sound,
                        playing = playingId == sound.id && isPlaying,
                        current = playingId == sound.id,
                        onPlay = { onPlay(sound.id) },
                        onPause = onPause,
                        onOpenDetail = { onOpenDetail(sound.id) },
                    )
                }
            }

            if ((group?.missingCount ?: 0) > 0) {
                item(key = "get-all") {
                    Column(Modifier.padding(horizontal = 22.dp)) {
                        Hairline(Modifier.padding(top = 20.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clickable(role = Role.Button, onClick = onDownloadAll)
                                .padding(vertical = 14.dp)
                                .semantics {
                                    contentDescription =
                                        "Get this whole group. ${group?.costLine.orEmpty()}"
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Get this whole group",
                                    style = type.rowTitle,
                                    color = colors.primaryText,
                                )
                                Text(
                                    group?.costLine.orEmpty(),
                                    style = type.numeric,
                                    color = colors.tertiaryText,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            MeedwellIcon(
                                MeedwellIcons.Download,
                                size = 18.dp,
                                tint = colors.secondaryText,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Storage, which is where removing a recording finally lives.
 *
 * **Bundled and downloaded are separated, and only one of them can be removed.**
 * The three that came with the app are why Surroundings works with no
 * connection, so they are shown, explained, and left alone. Everything else can
 * go and can be fetched again, which is the sentence at the foot.
 */
@Composable
fun SurroundingsStorageScreen(
    state: SurroundingsUiState,
    onBack: () -> Unit,
    onRemove: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 22.dp)) {
            DetailHeader("Surroundings storage", onBack)
            Text(
                state.storageLine,
                style = type.voice,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 40.dp),
            modifier = Modifier.weight(1f),
        ) {
            if (state.bundledRows.isNotEmpty()) {
                item(key = "bundled-head") {
                    Column(Modifier.padding(horizontal = 22.dp)) {
                        SectionHead("Came with the app", Modifier.padding(top = 20.dp, bottom = 2.dp))
                    }
                }
                items(state.bundledRows, key = { "b-${it.id}" }) { sound ->
                    Column(Modifier.padding(horizontal = 22.dp)) {
                        StorageRow(sound = sound, removable = false, onRemove = {}, onOpenDetail = { onOpenDetail(sound.id) })
                    }
                }
                item(key = "bundled-note") {
                    Text(
                        "These came with the app and stay. They are why Surroundings works " +
                            "with no connection at all.",
                        style = type.meta,
                        color = colors.tertiaryText,
                        modifier = Modifier.padding(horizontal = 22.dp).padding(top = 10.dp),
                    )
                }
            }

            item(key = "downloaded-head") {
                Column(Modifier.padding(horizontal = 22.dp)) {
                    SectionHead("Downloaded", Modifier.padding(top = 22.dp, bottom = 2.dp))
                }
            }
            if (state.downloadedRows.isEmpty()) {
                item(key = "downloaded-empty") {
                    Text(
                        "Nothing downloaded yet.",
                        style = type.meta,
                        color = colors.tertiaryText,
                        modifier = Modifier.padding(horizontal = 22.dp).padding(top = 10.dp),
                    )
                }
            }
            items(state.downloadedRows, key = { "d-${it.id}" }) { sound ->
                Column(Modifier.padding(horizontal = 22.dp)) {
                    StorageRow(
                        sound = sound,
                        removable = true,
                        onRemove = { onRemove(sound.id) },
                        onOpenDetail = { onOpenDetail(sound.id) },
                    )
                }
            }
        }
    }
}

/**
 * One recording, in the grammar every Surroundings list shares.
 *
 * Sun, then the name over the recordist and license, then a status word. The
 * status column says what the recording is doing or what it would cost, and it
 * is the only place a download starts, so the row tap can mean exactly one
 * thing.
 */
@Composable
private fun RecordingRow(
    sound: SurroundingsRow,
    playing: Boolean,
    current: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    val away = sound.state == RowState.Away

    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(role = Role.Button) {
                when {
                    away -> onOpenDetail()
                    playing -> onPause()
                    else -> onPlay()
                }
            }
            .padding(vertical = 11.dp)
            .semantics { contentDescription = sound.accessibilityLabel(playing) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SunMark(here = !away, playing = playing, current = current)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                sound.title,
                style = type.rowTitle,
                color = if (current) colors.primaryText else colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // The recordist and the license. This line is the attribution and it
            // is never dropped from any list that shows a recording.
            Text(
                sound.subtitle,
                style = type.meta,
                color = colors.tertiaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            when {
                playing -> "playing"
                current -> "paused"
                sound.state == RowState.Downloading -> "getting"
                away -> sound.sizeLabel
                else -> sound.durationLabel
            },
            style = type.plate,
            color = if (playing || current) colors.mossInk else colors.tertiaryText,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 62.dp),
        )
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

/** A stored recording, with its size and, where allowed, a way to remove it. */
@Composable
private fun StorageRow(
    sound: SurroundingsRow,
    removable: Boolean,
    onRemove: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClick = onOpenDetail)
                .semantics {
                    contentDescription = "${sound.title}. Who recorded it, and under what license."
                },
        ) {
            Text(
                sound.title,
                style = type.rowTitle,
                color = colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                sound.subtitle,
                style = type.meta,
                color = colors.tertiaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (removable) {
            Box(
                Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clickable(role = Role.Button, onClick = onRemove)
                    .padding(horizontal = 10.dp)
                    .semantics { contentDescription = "Remove ${sound.title} from this phone" },
                contentAlignment = Alignment.Center,
            ) {
                Text("Remove", style = type.chip, color = colors.alarm)
            }
        } else {
            Text(
                sound.durationLabel,
                style = type.plate,
                color = colors.tertiaryText,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
    Hairline()
}
