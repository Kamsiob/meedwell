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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.snap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kamsiob.meedwell.core.model.Album
import com.kamsiob.meedwell.core.model.Artist
import com.kamsiob.meedwell.core.model.Genre
import com.kamsiob.meedwell.core.model.Provenance
import com.kamsiob.meedwell.ui.components.Hairline
import com.kamsiob.meedwell.ui.components.CoverSquare
import com.kamsiob.meedwell.ui.components.CoverThumb
import com.kamsiob.meedwell.ui.components.DayLine
import com.kamsiob.meedwell.ui.components.DaySpan
import com.kamsiob.meedwell.ui.components.SeedHeadPlate
import com.kamsiob.meedwell.data.ShelfSort
import com.kamsiob.meedwell.ui.components.AlphabetRail
import com.kamsiob.meedwell.ui.components.SectionHead
import com.kamsiob.meedwell.ui.components.railLetterOf
import com.kamsiob.meedwell.ui.components.railLetters
import com.kamsiob.meedwell.ui.components.MarkRefreshIndicator
import com.kamsiob.meedwell.ui.components.MeedwellMark
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.components.combinedClickableCompat
import com.kamsiob.meedwell.ui.components.PillButton
import com.kamsiob.meedwell.ui.components.TextButtonRow
import com.kamsiob.meedwell.ui.theme.InstrumentSerif
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Motion
import kotlinx.coroutines.launch

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

    /**
     * Lists, as a fourth first-class way of looking at the shelf.
     *
     * It had no home at all: `ListsScreen` existed and was routed nowhere, so
     * the one part of the app the listener actually builds themselves was
     * unreachable. Shelves is genres, which is the library's own idea of order;
     * this is the listener's.
     */
    Lists("Lists"),
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
/** How far a swipe travels before it steps the view, same feel as the tabs. */
private const val VIEW_SWIPE_PX = 140f

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
    onRefresh: () -> Unit,
    onOpenList: (String) -> Unit,
    onNewList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    // One state, shared by the box and its indicator. Two calls to
    // `rememberPullToRefreshState` would give the indicator a state nothing
    // drives, so it would sit still through the whole gesture.
    val pullState = rememberPullToRefreshState()

    // **A swipe crosses the switcher, not the tab bar.** On the shelf the
    // upper row is the thing you are choosing between, so a horizontal swipe
    // anywhere on the page steps Albums, Composers, Shelves, Lists, the same
    // travelled-distance-on-release judgment every other swipe in the app
    // uses. The tab bar keeps its swipe on the other tabs, where there is no
    // second row to argue with.
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.view) {
                var travelled = 0f
                detectHorizontalDragGestures(
                    onDragStart = { travelled = 0f },
                    onDragEnd = {
                        val views = ShelfView.entries
                        val at = views.indexOf(state.view)
                        if (travelled <= -VIEW_SWIPE_PX && at < views.lastIndex) {
                            onViewChange(views[at + 1])
                        } else if (travelled >= VIEW_SWIPE_PX && at > 0) {
                            onViewChange(views[at - 1])
                        }
                    },
                ) { change, delta ->
                    change.consume()
                    travelled += delta
                }
            },
    ) {
        // The mark, pressed into the paper.
        //
        // This replaced a scatter of specks meant to read as paper fibre. On a
        // real screen it read as noise, which is the exact failure a texture has
        // to avoid: the moment you can see the dots it stops being paper and
        // starts being a pattern.
        //
        // One large shape at four percent works where a thousand small ones did
        // not. It is far too faint to look at and just enough to stop the lower
        // half of the shelf being blank, and because it is the mark rather than
        // an invented ornament it cannot drift off brand. Bare, so no dark tile
        // appears; low and centred, so covers never sit on top of it.
        MeedwellMark(
            size = 300.dp,
            bare = true,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 92.dp)
                .alpha(if (colors.isDark) 0.05f else 0.04f),
        )

        // Pull down to ask Bandcamp again.
        //
        // The gesture everybody already knows, and until now the only manual
        // sync was a row buried in Settings. A comment in the view model has
        // claimed "pull to refresh" since the sync work was done; this is the
        // half that was never built.
        //
        // **Only when connected.** On a local-only shelf there is nothing to
        // ask, and a spinner that resolves to no change would be the app
        // pretending to do something.
        PullToRefreshBox(
            isRefreshing = state.syncing,
            onRefresh = { if (state.connected) onRefresh() },
            modifier = Modifier.fillMaxSize(),
            state = pullState,
            // The mark itself, not a spinner. The coin lifts out of its cradle
            // as you pull and rocks there while the sync runs.
            indicator = {
                if (state.connected) {
                    MarkRefreshIndicator(
                        state = pullState,
                        isRefreshing = state.syncing,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            },
        ) {
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
            DayLine(Modifier.padding(top = 9.dp), span = state.daySpan)

            // Albums, Composers, Shelves, Lists.
            //
            // **Spread across the whole width, and standing on a rule.** They
            // used to sit bunched at the left on a fixed 16dp gap, which left a
            // third of the row empty on a big phone and read as four things
            // nobody had arranged. Given a quarter of the width each they line
            // up with the tab bar at the bottom of the screen, and the taps
            // become the wide targets they always should have been.
            //
            // The polish is the rule underneath rather than anything added
            // around them. It is the staff line this whole design is built on,
            // drawn faintly the whole way across, with the segment under the
            // chosen view inked in and slid into place. Selection stays a matter
            // of weight and ink: still no filled pill anywhere in this app.
            ShelfSwitcher(
                selected = state.view,
                onViewChange = onViewChange,
                modifier = Modifier.padding(top = 10.dp),
            )

            // One voice line per screen, and this is the Shelf's. The order
            // and the view toggle ride at the end of it.
            //
            // **These two had no home and so were never reachable.** Both were
            // built, both were threaded all the way through `MeedwellApp`, and
            // `ShelfScreen` never called either one: sorting and the grid to
            // list switch were dead in the shipping app. The grid draws no
            // control for them anywhere, which is why it went unnoticed, but
            // `DESIGN.md` section 12 names the view toggle and sort outright, so
            // the intent was never in doubt, only the placement.
            //
            // The voice line is where they belong. It is the one row that is
            // always present, it sits directly above the records it describes,
            // and it was carrying a short italic phrase and a great deal of
            // empty paper. The order gets a name rather than an icon, because
            // "ARTIST A TO Z" says what a pair of arrows cannot.
            Row(
                Modifier.fillMaxWidth().padding(top = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.voiceLine,
                    style = type.voice,
                    color = colors.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                Box(
                    Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable(role = Role.Button, onClick = onOpenSort)
                        .padding(start = 10.dp)
                        .semantics {
                            contentDescription = if (state.filtering) {
                                "Showing ${state.sortLabel} only. Tap to see everything."
                            } else {
                                "Sorted by ${state.sortLabel}. Tap to change the order."
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        state.sortLabel.uppercase(),
                        style = type.section,
                        color = colors.tertiaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Only the albums have two ways of being looked at. Composers,
                // shelves and lists are lists by nature, so the control is not
                // shown rather than shown and ignored.
                if (state.view == ShelfView.Albums) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clickable(role = Role.Button, onClick = onToggleLayout)
                            .semantics {
                                contentDescription = if (state.grid) {
                                    "Showing covers. Switch to a list."
                                } else {
                                    "Showing a list. Switch to covers."
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        MeedwellIcon(
                            if (state.grid) MeedwellIcons.ListView else MeedwellIcons.Grid,
                            size = 17.dp,
                            tint = colors.tertiaryText,
                        )
                    }
                }
            }

            // **The pane slides the way the finger went.** With the swipe
            // stepping the switcher, the change of view is a small page turn:
            // the new pane arrives from the side the gesture named, 42px on
            // the Settle curve, and the old one leaves fast the way exits do.
            // The switcher's own underline already travels; this makes the
            // page under it agree. Reduced motion collapses it to a short
            // crossfade.
            val reduced = MeedwellTheme.reducedMotion
            AnimatedContent(
                targetState = state.view,
                transitionSpec = {
                    if (reduced) {
                        fadeIn(tween(90)) togetherWith fadeOut(tween(90))
                    } else {
                        val forward = targetState.ordinal > initialState.ordinal
                        val step = if (forward) 42 else -42
                        (slideInHorizontally(
                            tween(Motion.turn, easing = Motion.Settle)
                        ) { step } + fadeIn(tween(160)))
                            .togetherWith(
                                slideOutHorizontally(
                                    tween(Motion.leave, easing = Motion.Leave)
                                ) { -step } + fadeOut(tween(90))
                            )
                    }.using(SizeTransform(clip = false) { _, _ -> snap() })
                },
                label = "shelf view",
            ) { view ->
                val empty = when (view) {
                    ShelfView.Albums -> state.albums.isEmpty()
                    ShelfView.Composers -> state.artists.isEmpty()
                    ShelfView.Shelves -> state.genres.isEmpty()
                    ShelfView.Lists -> false
                }
                when {
                    empty -> ShelfEmpty(
                        state = state,
                        onFindOnBandcamp = onFindOnBandcamp,
                        onAddLocalFolders = onAddLocalFolders,
                    )

                    view == ShelfView.Albums && state.grid -> AlbumGrid(
                        albums = state.albums,
                        sort = state.sort,
                        newest = state.newestArrival,
                        bottomInset = state.bottomInset,
                        onAlbumClick = onAlbumClick,
                        onAlbumLongClick = onAlbumLongClick,
                    )

                    view == ShelfView.Albums -> AlbumList(
                        albums = state.albums,
                        sort = state.sort,
                        bottomInset = state.bottomInset,
                        onAlbumClick = onAlbumClick,
                        onAlbumLongClick = onAlbumLongClick,
                    )

                    view == ShelfView.Lists -> ListsPane(
                        lists = state.lists,
                        bottomInset = state.bottomInset,
                        onOpen = onOpenList,
                        onNew = onNewList,
                    )

                    view == ShelfView.Composers -> ArtistList(
                        artists = state.artists,
                        heldSeconds = state.albums
                            .groupBy { it.artistId }
                            .mapValues { (_, records) -> records.sumOf { it.durationSeconds } },
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

/**
 * The lists, and the one control that makes a new one.
 *
 * "New list" sits at the top rather than behind a floating button, because this
 * design has no floating buttons and because making a list is the first thing
 * somebody arriving at an empty pane wants to do.
 *
 * An empty pane says what a list is for instead of showing a blank page. The
 * standing rule is that an empty screen is an invitation, never a scolding.
 */
@Composable
private fun ListsPane(
    lists: List<ListSummary>,
    bottomInset: androidx.compose.ui.unit.Dp,
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = bottomInset),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "new") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .clickable(role = Role.Button, onClick = onNew)
                    .padding(vertical = 13.dp)
                    .semantics { contentDescription = "Make a new list" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("New list", style = type.rowTitle, color = colors.mossInk, modifier = Modifier.weight(1f))
                MeedwellIcon(MeedwellIcons.ChevronRight, size = 14.dp, tint = colors.tertiaryText)
            }
            Hairline()
        }

        if (lists.isEmpty()) {
            item(key = "empty") {
                Column(Modifier.fillMaxWidth().padding(top = 28.dp)) {
                    Text(
                        "Nothing here yet.",
                        style = type.serifOpening.copy(fontSize = 22.sp, lineHeight = 28.sp),
                        color = colors.primaryText,
                    )
                    Text(
                        "A list is yours to build: a sequence for a walk, a set for the evening, " +
                            "the pieces you keep coming back to. They live on this phone and go " +
                            "wherever your export goes.",
                        style = type.body,
                        color = colors.secondaryText,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }

        items(lists, key = { it.id }) { list ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 60.dp)
                    .clickable(role = Role.Button) { onOpen(list.id) }
                    .padding(vertical = 11.dp)
                    .semantics { contentDescription = "${list.name}. ${list.subtitle}" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverThumb(
                    url = list.coverUrl,
                    title = list.name,
                    modifier = Modifier.size(44.dp),
                )
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        list.name,
                        style = type.rowTitle,
                        color = colors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        list.subtitle,
                        style = type.rowSub,
                        color = colors.tertiaryText,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                MeedwellIcon(MeedwellIcons.ChevronRight, size = 14.dp, tint = colors.tertiaryText)
            }
            Hairline()
        }
    }
}

@Composable
private fun AlbumGrid(
    albums: List<Album>,
    sort: ShelfSort,
    newest: Album?,
    bottomInset: androidx.compose.ui.unit.Dp,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        // Wide enough that neighboring plate marks never touch.
        horizontalArrangement = Arrangement.spacedBy(19.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 14.dp, bottom = bottomInset),
        modifier = Modifier.fillMaxSize(),
    ) {
        // `NEWLY SHELVED` as a section head with its staff. The grid opens the
        // shelf this way; there is no hero card in it at all, and the one that
        // was here was a rounded filled panel, which is the exact habit this
        // correction removes.
        // The head names the order the grid is actually in. "Newly shelved"
        // stood over an alphabetical grid, which made the first sentence above
        // somebody's own records false.
        item(span = { GridItemSpan(maxLineSpan) }, key = "head") {
            SectionHead(
                when (sort) {
                    ShelfSort.Recent -> "Newly shelved"
                    ShelfSort.MostPlayed -> "Most in hand"
                    ShelfSort.Title -> "By title"
                    else -> "By artist"
                },
                Modifier.padding(bottom = 12.dp),
            )
        }
        items(albums, key = { it.id }) { album ->
            AlbumCard(album, onClick = { onAlbumClick(album) }, onLongClick = { onAlbumLongClick(album) })
        }

        // The foot of the shelf, and the reason the page no longer ends in a
        // stretch of blank paper.
        //
        // `DESIGN.md` asks for engraved plates at rest points and this is the
        // plainest one in the app: the end of everything you own. Drawn at a
        // fraction of its usual ink so it reads as a watermark pressed into the
        // stock rather than as a picture somebody put there, and it never
        // competes with a cover for attention.
        if (albums.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "foot") {
                Box(
                    Modifier.fillMaxWidth().padding(top = 26.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    SeedHeadPlate(Modifier.alpha(0.34f))
                }
            }
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
        // The collector's mark, in the serif italic, exactly where DESIGN.md
        // section 11 puts collector provenance. It was written, correct, and
        // never called: in the default view nothing separated a record you own
        // the files for from one you are streaming.
        ProvenanceLine(album)
    }
}

/**
 * The collector's mark under an album title.
 *
 * The mark is a word, "yours" or "on this phone", never a color or a dot, so
 * meaning has a carrier every eye and every screen reader gets. The artist is
 * not repeated here: the plate line above the mark already names them, and the
 * first grid build printed "Adrian von Ziegler" twice on every tile. A
 * streamed record shows no mark at all, streaming being the unmarked case.
 */
@Composable
private fun ProvenanceLine(album: Album, modifier: Modifier = Modifier) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    val mark = when (val p = album.provenance) {
        Provenance.Yours -> "yours"
        // Honest about being partial rather than rounding up.
        is Provenance.PartlyHere -> "${p.found} of ${p.total} here"
        Provenance.OnThisPhone -> "on this phone"
        Provenance.Streaming -> return
    }
    Text(
        text = mark,
        style = type.meta.copy(fontStyle = FontStyle.Italic),
        color = colors.secondaryText,
        modifier = modifier.padding(top = 2.dp),
    )
}

@Composable
private fun AlbumList(
    albums: List<Album>,
    sort: ShelfSort,
    bottomInset: androidx.compose.ui.unit.Dp,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // The key the shelf is filed under, which is the artist or the title
    // depending on the order, and nothing at all under the two orders that are
    // not alphabetical.
    val keyOf: ((Album) -> String)? = when (sort) {
        ShelfSort.Artist -> { album -> album.artist }
        ShelfSort.Title -> { album -> album.name }
        ShelfSort.Recent, ShelfSort.MostPlayed -> null
    }
    val letters = remember(albums, sort) {
        keyOf?.let { key -> railLetters(albums.map(key)) }.orEmpty()
    }

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
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
                // **Much smaller than the grid's, on purpose.**
                //
                // The list exists to get down a long shelf quickly, so the art
                // is a reminder of which record this is rather than the point of
                // the row. At the old 48dp the list was just a narrower grid.
                CoverThumb(url = album.coverUrl, title = album.name, size = 34.dp)
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
            Hairline()
        }
    }

        if (letters.size > 1 && keyOf != null) {
            AlphabetRail(
                letters = letters,
                onLetter = { letter ->
                    val index = albums.indexOfFirst { railLetterOf(keyOf(it)) == letter }
                    if (index >= 0) scope.launch { listState.scrollToItem(index) }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(bottom = bottomInset),
            )
        }
    }
}


@Composable
private fun ArtistList(
    artists: List<Artist>,
    /** Seconds of music held per artist, summed from their records. */
    heldSeconds: Map<String, Long>,
    bottomInset: androidx.compose.ui.unit.Dp,
    onArtistClick: (Artist) -> Unit,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 6.dp, bottom = bottomInset),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "surname-head") {
            SectionHead("By surname", Modifier.padding(top = 2.dp, bottom = 2.dp))
        }
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
                // **Time held, never play counts.** The grid's own caption for
                // this view. Hours of somebody's music on your shelf is a
                // collector's number; a play count is a platform's.
                heldSeconds[artist.id]?.takeIf { it > 0 }?.let { seconds ->
                    Text(
                        text = if (seconds >= 3600) {
                            "${seconds / 3600} hr ${(seconds % 3600) / 60} min"
                        } else {
                            "${seconds / 60} min"
                        },
                        style = type.meta,
                        color = colors.tertiaryText,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
            Hairline()
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
            Hairline()
        }
    }
}

/**
 * The four ways of looking at the shelf, and the rule they stand on.
 *
 * The indicator is deliberately not a pill, a box or a filled tab: it is one
 * segment of a staff line, the same line the section heads and the day line are
 * drawn from. It slides rather than jumping, because the eye should be able to
 * follow which view it just left.
 */
@Composable
private fun ShelfSwitcher(
    selected: ShelfView,
    onViewChange: (ShelfView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val views = ShelfView.entries
    val index = views.indexOf(selected).coerceAtLeast(0)

    // Slid, not jumped. Long enough to read as movement, short enough that
    // nobody is waiting for it.
    val position by animateFloatAsState(
        targetValue = index.toFloat(),
        animationSpec = tween(durationMillis = Motion.turn, easing = Motion.Rule),
        label = "shelf switcher",
    )

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            views.forEach { view ->
                ViewTab(
                    view = view,
                    selected = selected == view,
                    onClick = { onViewChange(view) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .padding(top = 6.dp)
        ) {
            val slot = size.width / views.size
            val y = size.height / 2f

            // The quiet rule, all the way across.
            drawLine(
                color = colors.hairline,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )

            // The inked segment, under whichever view is showing. Narrower than
            // its slot so it reads as a mark on the line rather than as the line
            // changing color.
            val mark = slot * 0.46f
            val left = slot * position + (slot - mark) / 2f
            drawLine(
                color = colors.mossInk,
                start = Offset(left, y),
                end = Offset(left + mark, y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun ViewTab(
    view: ShelfView,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    val tint = if (selected) colors.primaryText else colors.tertiaryText

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
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
                ShelfView.Lists -> MeedwellIcons.Lists
            },
            // The grid draws these at 21 by 18 and they read as too small on a
            // real phone: this is the primary switcher for the whole shelf and
            // it was quieter than the tab bar underneath it. Raised twice, to 26
            // and then to 30, which is where it finally reads as the main
            // control on the screen rather than as a caption above the records.
            // The labels still carry the meaning for anybody who does not read
            // the glyphs.
            size = 30.dp,
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
    /**
     * The order the albums are in, which decides whether an A to Z rail means
     * anything. Under "Recently added" or "Most played" no letter corresponds to
     * a position, so the rail is not drawn at all.
     */
    val sort: ShelfSort = ShelfSort.Artist,
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
    /** The listener's own dawn and dusk, for the day line. */
    val daySpan: DaySpan = DaySpan.Default,
    val lists: List<ListSummary> = emptyList(),
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
            // Never "empty" in the sense the empty state means: the pane always
            // carries "New list", which is the invitation, so the shelf's own
            // empty screen would be the wrong thing to show over it.
            ShelfView.Lists -> false
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
                else -> if (albumCount == 1) {
                    "${spelled(artists.size)} names behind one record"
                } else {
                    "${spelled(artists.size)} names behind ${spelled(albumCount).lowercase()} records"
                }
            }
            ShelfView.Lists -> when (lists.size) {
                0 -> "Yours to build"
                1 -> "One list, kept on this phone"
                else -> "${lists.size} lists, kept on this phone"
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

    /**
     * One URL per cover, held for as long as the credentials are.
     *
     * **This is what stops the whole shelf reloading its art on every sync.**
     * Subsonic authenticates with a salt and a token, and the client mints a
     * fresh salt on every call, so asking for the same cover twice produced two
     * different URL strings. The image loader keys its cache on the URL, so
     * every refresh looked like a hundred images it had never seen, and the art
     * visibly reloaded even though not a pixel of it had changed.
     *
     * Reusing a salt is not a weakness here: the token is derived from the
     * password rather than issued by the server, so it does not expire and one
     * salt is exactly as good as the next. What matters is that the string
     * stays put.
     *
     * Cleared whenever the resolver is installed or dropped, so a change of
     * credentials never leaves a URL signed with the old ones behind.
     */
    private val urls = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun install(resolver: (String) -> String) {
        this.resolver = resolver
        urls.clear()
    }

    fun clear() {
        resolver = null
        urls.clear()
    }

    fun of(coverArtId: String): String? {
        if (coverArtId.isBlank()) return null
        val make = resolver ?: return null
        return urls.getOrPut(coverArtId) { make(coverArtId) }
    }
}
