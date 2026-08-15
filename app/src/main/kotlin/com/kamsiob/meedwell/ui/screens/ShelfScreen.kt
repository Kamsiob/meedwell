package com.kamsiob.meedwell.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.kamsiob.meedwell.ui.components.CoverSquare
import com.kamsiob.meedwell.ui.components.CoverThumb
import com.kamsiob.meedwell.ui.components.DayLine
import com.kamsiob.meedwell.ui.components.SectionHead
import com.kamsiob.meedwell.ui.components.IconButton
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.components.combinedClickableCompat
import com.kamsiob.meedwell.ui.components.PillButton
import com.kamsiob.meedwell.ui.components.TextButtonRow
import com.kamsiob.meedwell.ui.theme.InstrumentSerif
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/** Albums, Artists and Genres are three sibling first-class views of one shelf. */
/**
 * The three views of the shelf.
 *
 * **Composers, not Artists.** The word is the whole positioning in one tab: it
 * means everything to one listener and nothing to another, and this app is for
 * the first one. **Shelves** is where Lists went when it left the tab bar, so
 * the switcher carries all three ways of looking at the same records.
 */
enum class ShelfView(val label: String) {
    Albums("Albums"),
    Composers("Composers"),
    Shelves("Shelves"),
}

/**
 * Screens 06 through 11 in the visual reference: the shelf.
 *
 * The view switcher carries Albums, Artists and Genres as siblings. The scope
 * filters, meaning what is here as files and what is local only, live in the
 * sort menu rather than competing with the switcher, which is what keeps the
 * top of the screen readable.
 */
/**
 * How much room the mini player and the tab bar actually take.
 *
 * The shelf used a flat 120dp, while the real obstruction is the mini player
 * (58dp plus its 20dp inset), the gap, and the tab bar (58dp plus its own
 * navigation inset), which is nearer 160dp. The third album was sliced in half
 * by the player on a three album shelf. It also has to shrink when nothing is
 * playing, or the shelf ends in 120dp of nothing.
 */
private val BottomInsetPlaying = 168.dp
private val BottomInsetIdle = 96.dp

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
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {

            // The header, from the grid: title and a labelled search pill on
            // one line, then the day line, then the switcher, then the voice.
            //
            // The search pill is **fixed and never scrolls away**, and it is a
            // labelled pill rather than a bare magnifier: the word is what
            // makes it findable by somebody who does not already know the
            // icon.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Shelf", style = type.h1, color = colors.primaryText)
                Box(Modifier.weight(1f))
                SearchPill(onClick = onOpenSearch)
            }

            // The day line, with the copper sun at the real time of day.
            DayLine(Modifier.padding(top = 9.dp))

            // Albums, Composers, Shelves. Icon over label, and the selected one
            // is carried by ink weight rather than by a filled pill.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(21.dp),
            ) {
                ShelfView.entries.forEach { view ->
                    ViewTab(
                        view = view,
                        selected = state.view == view,
                        onClick = { onViewChange(view) },
                    )
                }
            }

            // One voice line per screen, and this is the Shelf's.
            Text(
                text = state.voiceLine,
                style = type.voice,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 13.dp),
            )

            when {
                state.isEmpty -> ShelfEmpty(
                    state = state,
                    onFindOnBandcamp = onFindOnBandcamp,
                    onAddLocalFolders = onAddLocalFolders,
                )

                state.view == ShelfView.Albums && state.grid -> AlbumGrid(
                    albums = state.albums,
                    newest = state.newestArrival,
                    bottomInset = state.bottomInset,
                    onAlbumClick = onAlbumClick,
                    onAlbumLongClick = onAlbumLongClick,
                )

                state.view == ShelfView.Albums -> AlbumList(
                    albums = state.albums,
                    bottomInset = state.bottomInset,
                    onAlbumClick = onAlbumClick,
                    onAlbumLongClick = onAlbumLongClick,
                )

                state.view == ShelfView.Composers -> ArtistList(
                    artists = state.artists,
                    bottomInset = state.bottomInset,
                    onArtistClick = onArtistClick,
                )

                else -> GenreList(
                    genres = state.genres,
                    bottomInset = state.bottomInset,
                    onGenreClick = onGenreClick,
                )
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
            style = type.meta,
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
    bottomInset: androidx.compose.ui.unit.Dp,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 14.dp, bottom = bottomInset),
        modifier = Modifier.fillMaxSize(),
    ) {
        // `NEWLY SHELVED` as a section head with its staff. The grid opens the
        // shelf this way; there is no hero card in it at all, and the one that
        // was here was a rounded filled panel, which is the exact habit this
        // correction removes.
        item(span = { GridItemSpan(maxLineSpan) }, key = "head") {
            SectionHead("Newly shelved", Modifier.padding(bottom = 12.dp))
        }
        items(albums, key = { it.id }) { album ->
            AlbumCard(album, onClick = { onAlbumClick(album) }, onLongClick = { onAlbumLongClick(album) })
        }
    }
}

/*
 * The newest arrival card is gone.
 *
 * It was a rounded, filled panel with the cover inside it, which is the single
 * habit that most made this app look like every other one. The grid puts a
 * section head and a staff at the top of the shelf instead, and the newest
 * record is simply the first one in the grid because the grid is sorted that
 * way.
 */

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
            style = type.gridTitle,
            color = colors.primaryText,
            // Two lines, as the grid sets them. A record called "Nocturnes for
            // a Small Room" is not a record called "Nocturnes for a Sm…".
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp),
        )
        // The plate line, set the way a printed plate line is set: small caps,
        // letterspaced, tertiary. "Marren · piano" in the grid.
        Text(
            // `.plate { text-transform: uppercase }`. Compose has no such
            // property, so the transform happens here. Doing it in the style
            // is not possible; forgetting it is how a plate line ends up as
            // ordinary small text with odd letterspacing.
            text = album.artist.uppercase(),
            style = type.plate,
            color = colors.tertiaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/**
 * The line under an album title.
 *
 * The "yours" marker is Instrument Serif italic, and it is paired with the
 * artist name rather than standing alone, because color and style are never
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
                style = type.meta.copy(fontStyle = FontStyle.Italic),
                color = colors.secondaryText,
            )
            is Provenance.PartlyHere -> Text(
                // Honest about being partial rather than rounding up.
                text = "${p.found} of ${p.total} here",
                style = type.meta.copy(fontStyle = FontStyle.Italic),
                color = colors.secondaryText,
            )
            Provenance.OnThisPhone -> Text(
                text = "on this phone",
                style = type.meta.copy(fontStyle = FontStyle.Italic),
                color = colors.secondaryText,
            )
            Provenance.Streaming -> Unit
        }
        if (album.provenance != Provenance.Streaming) {
            Text(" · ", style = type.meta, color = colors.tertiaryText)
        }
        Text(
            text = album.artist,
            style = type.meta,
            color = colors.tertiaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AlbumList(
    albums: List<Album>,
    bottomInset: androidx.compose.ui.unit.Dp,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = bottomInset),
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
                        style = type.meta,
                        color = colors.tertiaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                // The presence dot is paired with the text in the subtitle, so
                // color and shape are never the only carrier of meaning.
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
private fun ArtistList(
    artists: List<Artist>,
    bottomInset: androidx.compose.ui.unit.Dp,
    onArtistClick: (Artist) -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 6.dp, bottom = bottomInset),
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
                        style = type.meta,
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
private fun GenreList(
    genres: List<Genre>,
    bottomInset: androidx.compose.ui.unit.Dp,
    onGenreClick: (Genre) -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 6.dp, bottom = bottomInset),
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
                        style = type.meta,
                        color = colors.tertiaryText,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                MeedwellIcon(icon = MeedwellIcons.ChevronRight, size = 14.dp, tint = colors.tertiaryText)
            }
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))
        }
    }
}

@Composable
private fun ViewTab(view: ShelfView, selected: Boolean, onClick: () -> Unit) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    val tint = if (selected) colors.primaryText else colors.tertiaryText

    Column(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics {
                contentDescription = if (selected) "${view.label}, showing" else view.label
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MeedwellIcon(
            icon = when (view) {
                ShelfView.Albums -> MeedwellIcons.AlbumsView
                ShelfView.Composers -> MeedwellIcons.Composers
                // The same shelf-with-spines the tab bar uses, because it is
                // the same idea: a shelf you put records on.
                ShelfView.Shelves -> MeedwellIcons.TabShelf
            },
            size = 18.dp,
            tint = tint,
        )
        Text(
            text = view.label,
            style = type.tabLabel,
            // Selection is carried by weight and ink rather than by a filled
            // pill, because this design has no filled containers at all.
            color = tint,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/**
 * The search control on the Shelf: a labelled pill, fixed at the top.
 *
 * ```
 * .spill{...border:1px solid var(--hair-2);border-radius:999px;padding:6px 12px;}
 * ```
 *
 * An outline rather than a fill, and it carries the word "Search" rather than
 * only a magnifier. It never scrolls away.
 */
@Composable
private fun SearchPill(onClick: () -> Unit) {
    val colors = MeedwellTheme.colors
    Row(
        Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Search your shelf" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .border(1.dp, colors.hairline2, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MeedwellIcon(MeedwellIcons.Search, size = 13.dp, tint = colors.primaryText)
            Text("Search", style = MeedwellTheme.typography.chip, color = colors.primaryText)
        }
    }
}

private fun pluralAlbums(count: Int) = if (count == 1) "1 album" else "$count albums"

private fun plural(count: Int, word: String) = if (count == 1) word else word + "s"

/**
 * Numbers under twenty spelled out, which is the register the reference's own
 * voice lines use: "Forty-one names behind 148 records".
 */
private fun spelled(n: Int): String = when (n) {
    2 -> "Two"; 3 -> "Three"; 4 -> "Four"; 5 -> "Five"; 6 -> "Six"; 7 -> "Seven"
    8 -> "Eight"; 9 -> "Nine"; 10 -> "Ten"; 11 -> "Eleven"; 12 -> "Twelve"
    13 -> "Thirteen"; 14 -> "Fourteen"; 15 -> "Fifteen"; 16 -> "Sixteen"
    17 -> "Seventeen"; 18 -> "Eighteen"; 19 -> "Nineteen"
    else -> n.toString()
}

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
    /**
     * Whether `sortLabel` is naming a genre the shelf is narrowed to, rather
     * than naming an order. Changes what the label slot is: a filter you can
     * take off rather than a setting you can change.
     */
    val filtering: Boolean = false,
    val syncing: Boolean = false,
    /** Whether the mini player is on screen, which changes how much room the list needs. */
    val playerVisible: Boolean = false,
    /**
     * How much room the floating Surroundings card needs.
     *
     * Passed in rather than worked out here, because the card can expand and
     * collapse while the shelf is untouched, and the last row has to stay
     * reachable in every one of those states.
     */
    val cardRoom: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val bottomInset: androidx.compose.ui.unit.Dp
        get() = (if (playerVisible) BottomInsetPlaying else BottomInsetIdle) + cardRoom

    val isEmpty: Boolean
        get() = when (view) {
            ShelfView.Albums -> albums.isEmpty()
            ShelfView.Composers -> artists.isEmpty()
            ShelfView.Shelves -> genres.isEmpty()
        }

    /**
     * The voice line, which is the app's quiet editorial register.
     *
     * A user who never connects an account must never meet sync language the
     * app cannot honor, so the local-only line is genuinely different rather
     * than the same sentence with a number swapped.
     */
    /**
     * The editorial line under the heading, and it changes with the view.
     *
     * One string repeated across three sibling views is the tell that the voice
     * is a constant rather than a voice, and the serif italic makes the
     * repetition louder rather than quieter, because the typeface promises a
     * person wrote it.
     *
     * Every line here is derived from what is actually on the shelf, so none of
     * them can be wrong.
     */
    val voiceLine: String
        get() = when (view) {
            ShelfView.Composers -> when (artists.size) {
                0 -> "No names yet"
                1 -> "One name behind everything here"
                // Both numbers spelled or both numerals. "Two names behind 3
                // records" mixes registers in one sentence and reads as careless.
                else -> "${spelled(artists.size)} names behind ${spelled(albumCount).lowercase()} records"
            }
            ShelfView.Shelves -> when (genres.size) {
                0 -> "Nothing here carries a genre tag"
                1 -> "One tag, so far"
                else -> "Every tag Bandcamp knows your records by"
            }
            ShelfView.Albums -> when {
                !connected && albumCount > 0 ->
                    "$albumCount ${plural(albumCount, "album")} on this phone, no account involved"
                albumCount == 0 -> if (connected) "Connected, and nothing here yet" else "Nothing here yet"
                presentCount > 0 -> "$albumCount albums, $presentCount of them living here"
                else -> "$albumCount ${plural(albumCount, "album")} on your shelf"
            }
        }
}

private fun Album.rowSubtitle(): String = buildList {
    add(artist)
    if (year > 0) add(year.toString())
    // "Streaming" is the default state of almost every row, so saying it on
    // every row is noise. Only a record that is actually here says so, which is
    // what makes the marker mean something when it appears.
    when (val p = provenance) {
        Provenance.Yours -> add("yours")
        is Provenance.PartlyHere -> add("${p.found} of ${p.total} here")
        Provenance.OnThisPhone -> add("on this phone")
        Provenance.Streaming -> Unit
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
