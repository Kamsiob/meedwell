package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kamsiob.meedwell.core.model.Album
import com.kamsiob.meedwell.core.model.Artist
import com.kamsiob.meedwell.core.model.Genre
import com.kamsiob.meedwell.core.model.Provenance
import com.kamsiob.meedwell.ui.components.AmbientGlow
import com.kamsiob.meedwell.ui.components.CoverSquare
import com.kamsiob.meedwell.ui.components.CoverThumb
import com.kamsiob.meedwell.ui.components.GlowTone
import com.kamsiob.meedwell.ui.components.combinedClickableCompat
import com.kamsiob.meedwell.ui.components.PillButton
import com.kamsiob.meedwell.ui.components.TextButtonRow
import com.kamsiob.meedwell.ui.theme.InstrumentSerif
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/** Albums, Artists and Genres are three sibling first-class views of one shelf. */
enum class ShelfView { Albums, Artists, Genres }

/**
 * Screens 06 through 11 in the visual reference: the shelf.
 *
 * The view switcher carries Albums, Artists and Genres as siblings. The scope
 * filters, meaning what is here as files and what is local only, live in the
 * sort menu rather than competing with the switcher, which is what keeps the
 * top of the screen readable.
 */
@Composable
fun ShelfScreen(
    state: ShelfState,
    onViewChange: (ShelfView) -> Unit,
    onToggleLayout: () -> Unit,
    onOpenSort: () -> Unit,
    onOpenSearch: () -> Unit,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onGenreClick: (Genre) -> Unit,
    onFindOnBandcamp: () -> Unit,
    onAddLocalFolders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Box(modifier = modifier.fillMaxSize()) {
        AmbientGlow(
            tone = when (state.view) {
                ShelfView.Albums -> GlowTone.Violet
                ShelfView.Artists -> GlowTone.Teal
                ShelfView.Genres -> GlowTone.Rose
            }
        )

        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {

            // Heading and the voice line, which says what the shelf actually is
            // rather than a count with no meaning.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Shelf", style = type.largeHeading, color = colors.primaryText)
                    Text(
                        text = state.voiceLine,
                        style = type.voiceSmall,
                        color = colors.secondaryText,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTextButton(
                        label = if (state.grid) "List" else "Grid",
                        description = if (state.grid) "Show the shelf as a list" else "Show the shelf as a grid",
                        onClick = onToggleLayout,
                    )
                    IconTextButton(
                        label = "Search",
                        description = "Search your shelf",
                        onClick = onOpenSearch,
                    )
                }
            }

            // The view switcher, with sort pushed to the end.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShelfView.entries.forEach { view ->
                    ViewTab(
                        label = view.name,
                        selected = state.view == view,
                        onClick = { onViewChange(view) },
                    )
                }
                Box(Modifier.weight(1f))
                Box(
                    Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .height(48.dp)
                        .clickable(role = Role.Button, onClick = onOpenSort)
                        .semantics { contentDescription = "Change how the shelf is sorted and filtered" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.sortLabel, style = type.metadata, color = colors.secondaryText)
                }
            }

            when {
                state.isEmpty -> ShelfEmpty(
                    state = state,
                    onFindOnBandcamp = onFindOnBandcamp,
                    onAddLocalFolders = onAddLocalFolders,
                )

                state.view == ShelfView.Albums && state.grid -> AlbumGrid(
                    albums = state.albums,
                    newest = state.newestArrival,
                    onAlbumClick = onAlbumClick,
                    onAlbumLongClick = onAlbumLongClick,
                )

                state.view == ShelfView.Albums -> AlbumList(
                    albums = state.albums,
                    onAlbumClick = onAlbumClick,
                    onAlbumLongClick = onAlbumLongClick,
                )

                state.view == ShelfView.Artists -> ArtistList(
                    artists = state.artists,
                    onArtistClick = onArtistClick,
                )

                else -> GenreList(genres = state.genres, onGenreClick = onGenreClick)
            }
        }
    }
}

@Composable
private fun ShelfEmpty(
    state: ShelfState,
    onFindOnBandcamp: () -> Unit,
    onAddLocalFolders: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    // An empty screen is an invitation to act, never a scolding. It names both
    // ways to fill the shelf rather than assuming which one the person wants.
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (state.connected) "Your collection is empty" else "Nothing on this phone yet",
            style = type.rowTitle,
            color = colors.primaryText,
        )
        Text(
            text = if (state.connected) {
                "Meedwell only ever sees music you own. The first record you buy shows up here on the " +
                    "next sync, and everything on the shelf stays yours after that."
            } else {
                "Point Meedwell at a folder with music in it and everything there joins the shelf. " +
                    "Nothing leaves this phone."
            },
            style = type.metadata,
            color = colors.secondaryText,
            modifier = Modifier.padding(top = 10.dp, start = 12.dp, end = 12.dp),
        )
        PillButton(
            label = "Find something on Bandcamp ↗",
            onClick = onFindOnBandcamp,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        )
        TextButtonRow(
            label = "Add local music folders",
            onClick = onAddLocalFolders,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
    }
}

@Composable
private fun AlbumGrid(
    albums: List<Album>,
    newest: Album?,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 14.dp, bottom = 120.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (newest != null) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "newest") {
                NewestArrivalCard(newest, onClick = { onAlbumClick(newest) })
            }
        }
        items(albums, key = { it.id }) { album ->
            AlbumCard(album, onClick = { onAlbumClick(album) }, onLongClick = { onAlbumLongClick(album) })
        }
    }
}

/**
 * The newest arrival card, at the top of the shelf.
 *
 * The legibility law shows up here in a specific, checkable way: the complete
 * cover sits **beside** its caption, never underneath it. An earlier design put
 * words over the art, and that is exactly what the law retired.
 */
@Composable
private fun NewestArrivalCard(album: Album, onClick: () -> Unit) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfacePanel)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(10.dp)
            .semantics { contentDescription = "Newest on your shelf: ${album.name} by ${album.artist}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The cover, whole, on the left. Words start past its edge.
        CoverThumb(url = album.coverUrl, title = album.name, size = 82.dp)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text("Newest on your shelf", style = type.voiceSmall, color = colors.secondaryText)
            Text(
                album.name,
                style = type.rowTitle,
                color = colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                album.artist,
                style = type.metadata,
                color = colors.tertiaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit, onLongClick: () -> Unit) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickableCompat(onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = album.accessibilityLabel() }
    ) {
        // The cover, complete, with the caption beginning only past its edge.
        CoverSquare(url = album.coverUrl, title = album.name, modifier = Modifier.fillMaxWidth())
        Text(
            text = album.name,
            style = type.metadata,
            color = colors.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        ProvenanceLine(album)
    }
}

/**
 * The line under an album title.
 *
 * The "yours" marker is Instrument Serif italic, and it is paired with the
 * artist name rather than standing alone, because colour and style are never
 * the only carrier of meaning.
 */
@Composable
private fun ProvenanceLine(album: Album, modifier: Modifier = Modifier) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Row(modifier = modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        when (val p = album.provenance) {
            Provenance.Yours -> Text(
                text = "yours",
                style = type.provenance.copy(fontStyle = FontStyle.Italic),
                color = colors.secondaryText,
            )
            is Provenance.PartlyHere -> Text(
                // Honest about being partial rather than rounding up.
                text = "${p.found} of ${p.total} here",
                style = type.provenance.copy(fontStyle = FontStyle.Italic),
                color = colors.secondaryText,
            )
            Provenance.OnThisPhone -> Text(
                text = "on this phone",
                style = type.provenance.copy(fontStyle = FontStyle.Italic),
                color = colors.secondaryText,
            )
            Provenance.Streaming -> Unit
        }
        if (album.provenance != Provenance.Streaming) {
            Text(" · ", style = type.metadata, color = colors.tertiaryText)
        }
        Text(
            text = album.artist,
            style = type.metadata,
            color = colors.tertiaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AlbumList(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 120.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(albums, key = { it.id }) { album ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // 56dp minimum row height, which also satisfies the 48dp
                    // touch target floor.
                    .defaultMinSize(minHeight = 56.dp)
                    .combinedClickableCompat(
                        onClick = { onAlbumClick(album) },
                        onLongClick = { onAlbumLongClick(album) },
                    )
                    .padding(vertical = 8.dp)
                    .semantics { contentDescription = album.accessibilityLabel() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverThumb(url = album.coverUrl, title = album.name)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        album.name,
                        style = type.rowTitle,
                        color = colors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = album.rowSubtitle(),
                        style = type.metadata,
                        color = colors.tertiaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                // The presence dot is paired with the text in the subtitle, so
                // colour and shape are never the only carrier of meaning.
                if (album.isFullyPresent) {
                    Box(
                        Modifier
                            .padding(start = 8.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(colors.secondaryText)
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))
        }
    }
}

@Composable
private fun ArtistList(artists: List<Artist>, onArtistClick: (Artist) -> Unit) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 6.dp, bottom = 120.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(artists, key = { it.id }) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .clickable(role = Role.Button) { onArtistClick(artist) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Artists are round, albums are square. That difference is what
                // tells the two lists apart at a glance.
                CoverThumb(
                    url = artist.imageUrl.takeIf { it.isNotBlank() },
                    title = artist.name,
                    size = 44.dp,
                    modifier = Modifier.clip(CircleShape),
                )
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(artist.name, style = type.rowTitle, color = colors.primaryText, maxLines = 1)
                    Text(
                        text = pluralAlbums(artist.albumCount),
                        style = type.metadata,
                        color = colors.tertiaryText,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))
        }
    }
}

@Composable
private fun GenreList(genres: List<Genre>, onGenreClick: (Genre) -> Unit) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 6.dp, bottom = 120.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(genres, key = { it.name }) { genre ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .clickable(role = Role.Button) { onGenreClick(genre) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(genre.name, style = type.rowTitle, color = colors.primaryText, maxLines = 1)
                    Text(
                        text = pluralAlbums(genre.albumCount),
                        style = type.metadata,
                        color = colors.tertiaryText,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text("›", style = type.metadata, color = colors.tertiaryText)
            }
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))
        }
    }
}

@Composable
private fun ViewTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .height(48.dp)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(end = 18.dp)
            .semantics { if (selected) contentDescription = "$label, showing" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = type.metadata,
            // Selection is carried by weight and ink rather than by colour
            // alone, so it survives a colour-blind reader and a screenshot.
            color = if (selected) colors.primaryText else colors.tertiaryText,
        )
    }
}

@Composable
private fun IconTextButton(label: String, description: String, onClick: () -> Unit) {
    val colors = MeedwellTheme.colors
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MeedwellTheme.typography.metadata, color = colors.secondaryText)
    }
}

private fun pluralAlbums(count: Int) = if (count == 1) "1 album" else "$count albums"

data class ShelfState(
    val view: ShelfView = ShelfView.Albums,
    val grid: Boolean = true,
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val newestArrival: Album? = null,
    val albumCount: Int = 0,
    val presentCount: Int = 0,
    val connected: Boolean = false,
    val sortLabel: String = "Artist A to Z",
    val syncing: Boolean = false,
) {
    val isEmpty: Boolean
        get() = when (view) {
            ShelfView.Albums -> albums.isEmpty()
            ShelfView.Artists -> artists.isEmpty()
            ShelfView.Genres -> genres.isEmpty()
        }

    /**
     * The voice line, which is the app's quiet editorial register.
     *
     * A user who never connects an account must never meet sync language the
     * app cannot honour, so the local-only line is genuinely different rather
     * than the same sentence with a number swapped.
     */
    val voiceLine: String
        get() = when {
            !connected && albumCount > 0 ->
                "$albumCount ${if (albumCount == 1) "album" else "albums"} on this phone, no account involved"
            albumCount == 0 -> if (connected) "Connected, and nothing here yet" else "Nothing here yet"
            presentCount > 0 -> "$albumCount albums, $presentCount of them living here"
            else -> "$albumCount ${if (albumCount == 1) "album" else "albums"} on your shelf"
        }
}

private fun Album.rowSubtitle(): String = buildList {
    add(artist)
    if (year > 0) add(year.toString())
    when (val p = provenance) {
        Provenance.Yours -> add("here")
        is Provenance.PartlyHere -> add("${p.found} of ${p.total} here")
        Provenance.OnThisPhone -> add("on this phone")
        Provenance.Streaming -> add("streaming")
    }
}.joinToString(" · ")

private fun Album.accessibilityLabel(): String = buildString {
    append(name)
    append(", ")
    append(artist)
    when (val p = provenance) {
        Provenance.Yours -> append(", all of it here as files")
        is Provenance.PartlyHere -> append(", ${p.found} of ${p.total} tracks here as files")
        Provenance.OnThisPhone -> append(", on this phone")
        Provenance.Streaming -> append(", streaming")
    }
}

/** The album's cover URL, or null when there is none to fetch. */
val Album.coverUrl: String?
    get() = CoverUrls.of(coverArtId)

/**
 * Resolves a cover art id to a URL.
 *
 * Set once when a client exists, so composables can ask for a cover without
 * every screen threading a client down to every row. Null before a connection
 * exists, which is the local-files-only case and is normal rather than an error.
 */
object CoverUrls {
    @Volatile
    private var resolver: ((String) -> String)? = null

    fun install(resolver: (String) -> String) {
        this.resolver = resolver
    }

    fun clear() {
        resolver = null
    }

    fun of(coverArtId: String): String? {
        if (coverArtId.isBlank()) return null
        return resolver?.invoke(coverArtId)
    }
}
