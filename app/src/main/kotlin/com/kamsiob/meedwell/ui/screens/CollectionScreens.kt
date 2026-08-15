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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.core.model.Album
import com.kamsiob.meedwell.core.model.Track
import com.kamsiob.meedwell.ui.components.AmbientGlow
import com.kamsiob.meedwell.ui.components.CoverSquare
import com.kamsiob.meedwell.ui.components.CoverThumb
import com.kamsiob.meedwell.ui.components.GlowTone
import com.kamsiob.meedwell.ui.components.IconButton
import com.kamsiob.meedwell.ui.components.IconEdge
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.components.PillButton
import com.kamsiob.meedwell.ui.components.combinedClickableCompat
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * Screen 24: History.
 *
 * Nearly free, as the reference notes: the on-device play log already had to
 * exist for the Forgotten Shelf, so this is a reading of it grouped by day.
 * Nothing about it leaves the phone, and Erase listening history genuinely
 * empties the table rather than hiding it.
 */
@Composable
fun HistoryScreen(
    days: List<HistoryDay>,
    onTrackClick: (HistoryEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Box(modifier.fillMaxSize()) {
        AmbientGlow(tone = GlowTone.Teal)
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
            IconButton(
                icon = MeedwellIcons.Back,
                contentDescription = "Back",
                onClick = onBack,
                size = 25.dp,
                tint = colors.primaryText,
                edge = IconEdge.Start,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text("History", style = type.largeHeading, color = colors.primaryText)
            Text(
                "The same log the forgotten shelf reads from",
                style = type.voiceSmall,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (days.isEmpty()) {
                EmptyNote(
                    "Nothing here yet. Play something and it turns up, on this phone and nowhere else."
                )
                return@Column
            }

            LazyColumn(contentPadding = PaddingValues(top = 14.dp, bottom = 120.dp)) {
                days.forEach { day ->
                    item(key = "d-" + day.label) {
                        Text(
                            day.label.uppercase(),
                            style = type.capsEyebrow,
                            color = colors.secondaryText,
                            modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
                        )
                    }
                    items(day.entries, key = { it.key }) { entry ->
                        Column {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 56.dp)
                                    .clickable(role = Role.Button) { onTrackClick(entry) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CoverThumb(url = entry.coverUrl, title = entry.title, size = 40.dp)
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(
                                        entry.title,
                                        style = type.rowTitle,
                                        color = colors.primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        entry.subtitle,
                                        style = type.metadata,
                                        color = colors.tertiaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(entry.time, style = type.metadata, color = colors.tertiaryText)
                            }
                            Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))
                        }
                    }
                }
                item(key = "foot") {
                    Text(
                        "One database, on this phone. Erase it any time in Settings.",
                        style = type.metadata,
                        color = colors.tertiaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    )
                }
            }
        }
    }
}

/**
 * Screen 29: the Forgotten Shelf.
 *
 * Rediscovery worked out entirely on this phone from the listener's own play
 * log. No algorithm, no feed, nothing sent anywhere, and the copy says so
 * because that is the point rather than a caveat.
 */
@Composable
fun ForgottenShelfScreen(
    albums: List<ForgottenAlbum>,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Box(modifier.fillMaxSize()) {
        AmbientGlow(tone = GlowTone.Rose)
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
            IconButton(
                icon = MeedwellIcons.Back,
                contentDescription = "Back",
                onClick = onBack,
                size = 25.dp,
                tint = colors.primaryText,
                edge = IconEdge.Start,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text("Forgotten shelf", style = type.largeHeading, color = colors.primaryText)
            Text(
                "Bought, loved, and quietly waiting",
                style = type.voiceSmall,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (albums.isEmpty()) {
                EmptyNote(
                    "Nothing is waiting yet. This fills in as you listen, and as records go quiet for a while."
                )
                return@Column
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(top = 18.dp, bottom = 120.dp),
            ) {
                items(albums, key = { it.album.id }) { forgotten ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .combinedClickableCompat(
                                onClick = { onAlbumClick(forgotten.album) },
                                onLongClick = { onAlbumLongClick(forgotten.album) },
                            )
                            .semantics {
                                contentDescription =
                                    "${forgotten.album.name} by ${forgotten.album.artist}, ${forgotten.reason}"
                            }
                    ) {
                        CoverSquare(
                            url = forgotten.album.coverUrl,
                            title = forgotten.album.name,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            forgotten.album.name,
                            style = type.metadata,
                            color = colors.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            forgotten.album.artist,
                            style = type.metadata,
                            color = colors.tertiaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        // The reason, in the serif voice. It is the whole point
                        // of the screen: not "here is a record" but "here is
                        // why you might have forgotten it".
                        Text(
                            forgotten.reason,
                            style = type.provenance.copy(fontStyle = FontStyle.Italic),
                            color = colors.secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                item(key = "foot", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Worked out on this phone from your own listening. No algorithm, no feed, nothing " +
                            "sent anywhere.",
                        style = type.metadata,
                        color = colors.tertiaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                    )
                }
            }
        }
    }
}

/**
 * Screens 21 and 23: Lists, with Loved pinned at the top.
 *
 * Lists are **local**, because Bandcamp's API implements no way to create,
 * change or delete a playlist. Anything the account already had is shown and
 * marked as not editable here. The screen says so rather than implying that
 * edits travel.
 */
@Composable
fun ListsScreen(
    state: ListsState,
    onOpenLoved: () -> Unit,
    onOpenList: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Box(modifier.fillMaxSize()) {
        AmbientGlow(tone = GlowTone.Teal)
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Lists", style = type.largeHeading, color = colors.primaryText)
                    Text(
                        // Honest. The old line said "Kept in step with your
                        // Bandcamp collection", which the API cannot support.
                        "Kept on this phone",
                        style = type.voiceSmall,
                        color = colors.secondaryText,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                // No "make a list" button, on purpose.
                //
                // Making lists is not built. The button was here, it did
                // nothing, and a plus sign that does nothing is the app
                // telling somebody they did something wrong. The screen says
                // where lists are instead, which is a real answer.
            }

            LazyColumn(contentPadding = PaddingValues(top = 14.dp, bottom = 120.dp)) {
                // Loved is pinned at the top, always.
                item(key = "loved") {
                    ListRow(
                        title = "Loved",
                        subtitle = state.lovedSubtitle,
                        coverUrl = null,
                        icon = MeedwellIcons.Heart,
                        onClick = onOpenLoved,
                    )
                }
                items(state.lists, key = { it.id }) { list ->
                    ListRow(
                        title = list.name,
                        subtitle = list.subtitle,
                        coverUrl = list.coverUrl,
                        icon = null,
                        onClick = { onOpenList(list.id) },
                    )
                }
                if (state.lists.isEmpty()) {
                    item(key = "empty") {
                        Column(Modifier.padding(top = 22.dp)) {
                            Text(
                                "Making your own lists is not built yet.",
                                style = type.body,
                                color = colors.secondaryText,
                            )
                            Text(
                                // Says why rather than just no. Bandcamp's API
                                // has no way to create or change a playlist, so
                                // whatever gets built lives on this phone only,
                                // and that is worth knowing before you invest
                                // an evening in a list.
                                "Bandcamp's API has no way to make or change a playlist, so lists will " +
                                    "live on this phone when they arrive. Loved above is real, and it " +
                                    "does reach your account.",
                                style = type.metadata,
                                color = colors.tertiaryText,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screen 23: Loved.
 *
 * One-way, and the screen says so. `star` works and reaches the Bandcamp
 * account; `unstar` is broken on their side and returns an error whatever is
 * sent. Stating that here is better than a heart that silently refuses to come
 * off, and the line disappears by itself if Bandcamp fixes the endpoint.
 */
@Composable
fun LovedScreen(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Box(modifier.fillMaxSize()) {
        AmbientGlow(tone = GlowTone.Rose)
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
            IconButton(
                icon = MeedwellIcons.Back,
                contentDescription = "Back",
                onClick = onBack,
                size = 25.dp,
                tint = colors.primaryText,
                edge = IconEdge.Start,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text("Loved", style = type.largeHeading, color = colors.primaryText)
            Text(
                "Hearts that live in your account, not this app",
                style = type.voiceSmall,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (tracks.isEmpty()) {
                EmptyNote("Nothing loved yet. The heart is on every track, and it travels with your account.")
                return@Column
            }

            LazyColumn(contentPadding = PaddingValues(top = 14.dp, bottom = 120.dp)) {
                items(tracks, key = { it.id }) { track ->
                    Column {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .combinedClickableCompat(
                                    onClick = { onTrackClick(track) },
                                    onLongClick = { onTrackLongClick(track) },
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CoverThumb(url = null, title = track.title, size = 40.dp)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(track.title, style = type.rowTitle, color = colors.primaryText, maxLines = 1)
                                Text(track.artist, style = type.metadata, color = colors.tertiaryText, maxLines = 1)
                            }
                            Text(
                                formatDuration(track.durationSeconds),
                                style = type.metadata,
                                color = colors.tertiaryText,
                            )
                        }
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))
                    }
                }
                item(key = "foot") {
                    Text(
                        "A heart set here reaches your Bandcamp account. Taking one off is broken on their " +
                            "side at the moment, so that has to be done on their website.",
                        style = type.metadata,
                        color = colors.tertiaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
                    )
                }
            }
        }
    }
}

/**
 * Screen 19: the artist page.
 *
 * The "yours" markers in serif italic, in-rotation figures read from the local
 * play log, and a prominent link to their Bandcamp page with copy noting that
 * the money goes to them and Meedwell takes no cut. That last line is not
 * decoration: it is the whole argument of the app in one sentence.
 */
@Composable
fun ArtistScreen(
    state: ArtistState,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit,
    onOpenBandcamp: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Box(modifier.fillMaxSize()) {
        AmbientGlow(tone = GlowTone.Teal)
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
            IconButton(
                icon = MeedwellIcons.Back,
                contentDescription = "Back",
                onClick = onBack,
                size = 25.dp,
                tint = colors.primaryText,
                edge = IconEdge.Start,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(state.name, style = type.sectionHeading, color = colors.primaryText)
            Text(
                state.voiceLine,
                style = type.voiceSmall,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 8.dp),
            )

            LazyColumn(contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp), modifier = Modifier.weight(1f)) {
                items(state.albums, key = { it.id }) { album ->
                    Column {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .combinedClickableCompat(
                                    onClick = { onAlbumClick(album) },
                                    onLongClick = { onAlbumLongClick(album) },
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CoverThumb(url = album.coverUrl, title = album.name, size = 48.dp)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(album.name, style = type.rowTitle, color = colors.primaryText, maxLines = 1)
                                Row {
                                    if (album.year > 0) {
                                        Text("${album.year}", style = type.metadata, color = colors.tertiaryText)
                                    }
                                    if (album.isFullyPresent) {
                                        Text(" · ", style = type.metadata, color = colors.tertiaryText)
                                        Text(
                                            "yours",
                                            style = type.provenance.copy(fontStyle = FontStyle.Italic),
                                            color = colors.secondaryText,
                                        )
                                    }
                                }
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))
                    }
                }
            }

            Text(
                "Their full discography, merch, and whatever's next live on their page. Money spent there " +
                    "goes to them. Meedwell never takes a cut of anything.",
                style = type.metadata,
                color = colors.secondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            )
            PillButton(
                label = "Their Bandcamp page ↗",
                onClick = onOpenBandcamp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 26.dp),
            )
        }
    }
}

@Composable
private fun ListRow(
    title: String,
    subtitle: String,
    coverUrl: String?,
    icon: MeedwellIcons?,
    onClick: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.surfacePanel),
                    contentAlignment = Alignment.Center,
                ) {
                    com.kamsiob.meedwell.ui.components.MeedwellIcon(
                        icon = icon,
                        size = 18.dp,
                        tint = colors.primaryText,
                    )
                }
            } else {
                CoverThumb(url = coverUrl, title = title, size = 44.dp)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, style = type.rowTitle, color = colors.primaryText, maxLines = 1)
                Text(subtitle, style = type.metadata, color = colors.tertiaryText, maxLines = 1)
            }
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text,
        style = MeedwellTheme.typography.metadata,
        color = MeedwellTheme.colors.secondaryText,
        modifier = Modifier.padding(top = 26.dp),
    )
}

// ---------- State ----------

data class HistoryEntry(
    val key: String,
    val trackId: String,
    val albumId: String,
    val title: String,
    val subtitle: String,
    val time: String,
    val coverUrl: String?,
)

data class HistoryDay(val label: String, val entries: List<HistoryEntry>)

data class ForgottenAlbum(val album: Album, val reason: String)

data class ListSummary(val id: String, val name: String, val subtitle: String, val coverUrl: String?)

data class ListsState(
    val lists: List<ListSummary> = emptyList(),
    val lovedCount: Int = 0,
) {
    val lovedSubtitle: String
        get() = when (lovedCount) {
            0 -> "Nothing loved yet"
            1 -> "1 track · lives in your account"
            else -> "$lovedCount tracks · lives in your account"
        }
}

data class ArtistState(
    val id: String = "",
    val name: String = "",
    val albums: List<Album> = emptyList(),
    val ownedCount: Int = 0,
) {
    val voiceLine: String
        get() = when {
            albums.isEmpty() -> "Nothing of theirs on your shelf yet"
            ownedCount == albums.size && albums.size > 1 -> "All ${albums.size} of their records live on your shelf"
            albums.size == 1 -> "One record of theirs lives on your shelf"
            else -> "${albums.size} records of theirs live on your shelf"
        }
}
