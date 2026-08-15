package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.core.model.Album
import com.kamsiob.meedwell.core.model.Provenance
import com.kamsiob.meedwell.data.db.WatchedFolderEntity
import com.kamsiob.meedwell.ui.components.AmbientGlow
import com.kamsiob.meedwell.ui.components.CoverThumb
import com.kamsiob.meedwell.ui.components.GlowTone
import com.kamsiob.meedwell.ui.components.PillButton
import com.kamsiob.meedwell.ui.components.TextButtonRow
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * Screen 26 in the visual reference: "Your files".
 *
 * This screen exists because verification found that Bandcamp's API streams a
 * collection and will not release the files. It **replaces** the marquee
 * Downloads screen rather than sitting beside it, because offering a download
 * button that cannot work would be exactly the dishonesty this app is built
 * against.
 *
 * The ownership claim is untouched. It was always about portable files rather
 * than about who fetches them, and only who fetches them changed.
 */
@Composable
fun YourFilesScreen(
    state: YourFilesState,
    onAddFolder: () -> Unit,
    onRemoveFolder: (WatchedFolderEntity) -> Unit,
    onRescan: () -> Unit,
    onGetFromBandcamp: () -> Unit,
    onAlbumClick: (Album) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Box(modifier.fillMaxSize()) {
        AmbientGlow(tone = GlowTone.Ember)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .height(48.dp)
                    .clickable(role = Role.Button, onClick = onBack)
                    .semantics { contentDescription = "Back" },
                contentAlignment = Alignment.CenterStart,
            ) { Text("‹", style = type.sectionHeading, color = colors.primaryText) }

            Text("Your files", style = type.largeHeading, color = colors.primaryText)
            Text(
                text = state.voiceLine,
                style = type.voiceSmall,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 8.dp),
            )

            // The honest limit, stated at the moment it matters rather than
            // buried in a help screen.
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(colors.surfacePanel)
                    .padding(14.dp),
            ) {
                Text(
                    "Bandcamp's API streams your collection but does not hand over the files. So Meedwell " +
                        "doesn't pretend to: download them from Bandcamp the way you always have, point " +
                        "Meedwell at the folder, and they join the shelf as what they are, files you own.",
                    style = type.metadata,
                    color = colors.secondaryText,
                )
            }

            if (state.connected) {
                PillButton(
                    label = "Get your files from Bandcamp ↗",
                    onClick = onGetFromBandcamp,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
            }

            Text(
                "WATCHED FOLDERS",
                style = type.capsEyebrow,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 24.dp),
            )

            if (state.folders.isEmpty()) {
                Text(
                    "None yet. Point Meedwell at wherever your music lives and everything there joins the " +
                        "shelf. Nothing leaves this phone.",
                    style = type.metadata,
                    color = colors.tertiaryText,
                    modifier = Modifier.padding(top = 10.dp),
                )
            } else {
                state.folders.forEach { folder ->
                    Column {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(folder.displayName, style = type.rowTitle, color = colors.primaryText)
                                Text(
                                    text = folder.subtitle(),
                                    style = type.metadata,
                                    color = colors.tertiaryText,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                            Box(
                                Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .height(48.dp)
                                    .clickable(role = Role.Button) { onRemoveFolder(folder) }
                                    .semantics { contentDescription = "Stop watching ${folder.displayName}" }
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Remove", style = type.metadata, color = colors.secondaryText)
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                TextButtonRow(
                    label = "Add a folder",
                    onClick = onAddFolder,
                    modifier = Modifier.weight(1f),
                )
                if (state.folders.isNotEmpty()) {
                    TextButtonRow(
                        label = if (state.scanning) "Looking" else "Look again",
                        onClick = { if (!state.scanning) onRescan() },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (state.matched.isNotEmpty()) {
                Text(
                    "MATCHED TO YOUR SHELF",
                    style = type.capsEyebrow,
                    color = colors.secondaryText,
                    modifier = Modifier.padding(top = 22.dp),
                )
                state.matched.forEach { album ->
                    Column {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .clickable(role = Role.Button) { onAlbumClick(album) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CoverThumb(url = album.coverUrl, title = album.name, size = 44.dp)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(album.name, style = type.rowTitle, color = colors.primaryText, maxLines = 1)
                                Text(
                                    // Honest per-album counts. Eight of ten is
                                    // eight of ten, never rounded up to whole.
                                    text = when (val p = album.provenance) {
                                        Provenance.Yours -> "All ${album.trackCount} tracks found"
                                        is Provenance.PartlyHere -> "${p.found} of ${p.total} tracks found"
                                        Provenance.OnThisPhone -> "On this phone"
                                        Provenance.Streaming -> "Streaming"
                                    },
                                    style = type.metadata,
                                    color = colors.tertiaryText,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))
                    }
                }
            }

            state.lastResult?.let { result ->
                Text(
                    text = result,
                    style = type.metadata,
                    color = colors.tertiaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                )
            }

            Text(
                "Same promise either way: plain files, any player can read them, they outlive this app. " +
                    "Only the way they arrive changes.",
                style = type.metadata,
                color = colors.tertiaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 34.dp),
            )
        }
    }
}

data class YourFilesState(
    val folders: List<WatchedFolderEntity> = emptyList(),
    val matched: List<Album> = emptyList(),
    val connected: Boolean = false,
    val scanning: Boolean = false,
    val lastResult: String? = null,
) {
    val voiceLine: String
        get() = when {
            folders.isEmpty() -> "Where owned music lives"
            matched.isEmpty() -> "Watching, and nothing matched yet"
            else -> "${matched.size} ${if (matched.size == 1) "record" else "records"} here as files"
        }
}

private fun WatchedFolderEntity.subtitle(): String =
    if (lastScannedAt == null) "Not looked at yet" else "$trackCount ${if (trackCount == 1) "file" else "files"} seen"
