package com.kamsiob.meedwell.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.kamsiob.meedwell.ui.components.MeedwellIcon
import com.kamsiob.meedwell.ui.components.Hairline
import com.kamsiob.meedwell.ui.components.MeedwellIcons
import com.kamsiob.meedwell.ui.components.ActionSheet
import com.kamsiob.meedwell.ui.components.ActionTarget
import com.kamsiob.meedwell.ui.components.ConfirmSheet
import com.kamsiob.meedwell.ui.components.Notice
import com.kamsiob.meedwell.ui.components.QueueSheet
import com.kamsiob.meedwell.ui.components.SheetAction
import com.kamsiob.meedwell.ui.components.SortSheet
import com.kamsiob.meedwell.ui.components.SyncTroubleSheet
import com.kamsiob.meedwell.ui.components.PendingConfirm
import com.kamsiob.meedwell.ui.components.MiniPlayer
import com.kamsiob.meedwell.ui.components.rememberWashColor
import com.kamsiob.meedwell.ui.screens.AlbumScreen
import com.kamsiob.meedwell.ui.screens.AboutScreen
import com.kamsiob.meedwell.ui.screens.ArtworkViewer
import com.kamsiob.meedwell.ui.screens.MoreDestination
import com.kamsiob.meedwell.ui.screens.MoreScreen
import com.kamsiob.meedwell.ui.screens.ArtistScreen
import com.kamsiob.meedwell.ui.screens.CreditsScreen
import com.kamsiob.meedwell.ui.components.CreditSheet
import com.kamsiob.meedwell.ui.screens.ExportScreen
import com.kamsiob.meedwell.ui.screens.ForgottenShelfScreen
import com.kamsiob.meedwell.ui.screens.SurroundingsScreen
import com.kamsiob.meedwell.ui.screens.HistoryScreen
import com.kamsiob.meedwell.ui.screens.ListsScreen
import com.kamsiob.meedwell.ui.screens.LovedScreen
import com.kamsiob.meedwell.ui.screens.SoftwareNotice
import com.kamsiob.meedwell.ui.screens.PrivacyScreen
import com.kamsiob.meedwell.ui.screens.SearchScreen
import com.kamsiob.meedwell.ui.screens.bandcampSearchUrl
import com.kamsiob.meedwell.ui.screens.SettingsScreen
import com.kamsiob.meedwell.ui.screens.WhatsAheadScreen
import com.kamsiob.meedwell.ui.screens.YourFilesScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.kamsiob.meedwell.BuildConfig
import com.kamsiob.meedwell.ui.components.PlayerPage
import com.kamsiob.meedwell.ui.components.SurroundingsCard
import com.kamsiob.meedwell.ui.components.surroundingsCardHeight
import com.kamsiob.meedwell.ui.screens.PlayerSpread
import com.kamsiob.meedwell.ui.screens.SurroundingsPlayingState
import com.kamsiob.meedwell.ui.screens.ConnectScreen
import com.kamsiob.meedwell.ui.screens.ShelfScreen
import com.kamsiob.meedwell.ui.screens.coverUrl
import com.kamsiob.meedwell.ui.screens.WelcomeScreen
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/**
 * The four tabs: an icon **and** a label, as the reference draws them.
 *
 * Labels rather than icons alone, because an icon-only bar makes people guess,
 * and icons rather than labels alone, because a row of four words is harder to
 * hit accurately than a shape with a word under it.
 */
enum class Tab(val label: String, val icon: MeedwellIcons) {
    Shelf("Shelf", MeedwellIcons.TabShelf),
    Search("Search", MeedwellIcons.TabSearch),

    /**
     * Surroundings is a **first-class destination**, not a row inside More.
     *
     * It was buried under More, which made a headline feature something you had
     * to already know about. The width for it comes from folding Lists into the
     * Shelf's own view switcher as "Shelves", which is where a list of albums
     * belongs anyway.
     */
    Surroundings("Surroundings", MeedwellIcons.TabSurroundings),
    More("More", MeedwellIcons.TabMore),
}

/**
 * Where the app is.
 *
 * A sealed hierarchy rather than a navigation library, because the app has few
 * destinations and the person maintaining it does not write code. Fewer moving
 * parts is worth more here than the flexibility a navigation graph would buy.
 */
sealed interface Destination {
    data object Welcome : Destination
    data object Connect : Destination
    data class Main(val tab: Tab = Tab.Shelf) : Destination
    data class AlbumDetail(val albumId: String) : Destination
    data object NowPlaying : Destination
    data class Artwork(val uri: String?, val title: String, val subtitle: String) : Destination
    data object Settings : Destination
    data object Privacy : Destination
    data object WhatsAhead : Destination
    data object About : Destination
    data object YourFiles : Destination
    data object Credits : Destination
    data object History : Destination
    data object Forgotten : Destination
    data object Surroundings : Destination
    data object Export : Destination
    data object Loved : Destination
    data class ArtistDetail(val artistId: String) : Destination
    data class GenreFilter(val genre: String) : Destination
}

@Composable
fun MeedwellApp(viewModel: MeedwellViewModel) {
    val context = LocalContext.current
    val shelf by viewModel.shelf.collectAsState()
    val connect by viewModel.connect.collectAsState()
    val playback by viewModel.playback.collectAsState()
    val albumDetail by viewModel.albumDetail.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()
    val yourFiles by viewModel.yourFiles.collectAsState()
    val search by viewModel.search.collectAsState()
    val credits by viewModel.credits.collectAsState()
    val history by viewModel.history.collectAsState()
    val forgotten by viewModel.forgotten.collectAsState()
    val lists by viewModel.lists.collectAsState()
    val loved by viewModel.loved.collectAsState()
    val artist by viewModel.artist.collectAsState()
    val syncFailure by viewModel.syncFailure.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val surroundings by viewModel.surroundings.state.collectAsState()
    val surroundingsDetail by viewModel.surroundings.detail.collectAsState()
    val surroundingsCard by viewModel.surroundings.card.collectAsState()
    val exportState by viewModel.export.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val scope by viewModel.scope.collectAsState()
    var pendingConfirm by remember { mutableStateOf<PendingConfirm?>(null) }

    // The three sheets that any screen can raise. Held here rather than inside
    // each screen so that a long-press on a track means the same thing on the
    // shelf, in search, in an album and on an artist page.
    var sheetTarget by remember { mutableStateOf<ActionTarget?>(null) }
    var sortSheetOpen by remember { mutableStateOf(false) }
    var queueSheetOpen by remember { mutableStateOf(false) }

    // Which page of the player spread is showing. Held here rather than inside
    // the player so that reopening it lands on the page you left.
    var playerPage by remember { mutableStateOf(PlayerPage.Music) }

    // The Storage Access Framework picker. A tree grant with persistable
    // permission, so the folder survives a reboot rather than being asked for
    // again every launch.
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) viewModel.addWatchedFolder(uri) }

    // Export writes to a document the user names, and restore reads one they
    // pick. Both go through the system picker rather than a path this app
    // chooses, so the file lands somewhere they can actually find it again.
    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.exportTo(uri) }

    val restorePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.restoreFrom(uri) }

    var destination by remember {
        mutableStateOf<Destination>(
            // A returning user lands on their shelf. Only somebody who has
            // never chosen a path sees the Welcome screen.
            if (viewModel.hasChosenPath) Destination.Main() else Destination.Welcome
        )
    }

    fun open(url: String) {
        // Links go to the user's own browser through an Intent. Meedwell never
        // fetches them itself, which is what the Privacy screen promises under
        // "Sharing and outside links".
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }
    }

    // System back, handled everywhere.
    //
    // Without this, Android's back gesture drops out of the app from any
    // sub-screen, which on a phone is the single most common way to feel lost.
    // Each destination names where back goes rather than relying on a stack,
    // which keeps the model small enough to hold in your head.
    val backTarget: Destination? = when (val current = destination) {
        Destination.Welcome -> null
        Destination.Connect -> Destination.Welcome
        is Destination.Main -> if (current.tab == Tab.Shelf) null else Destination.Main(Tab.Shelf)
        is Destination.AlbumDetail -> Destination.Main()
        Destination.NowPlaying -> Destination.Main()
        is Destination.Artwork -> Destination.NowPlaying
        Destination.Settings, Destination.Privacy, Destination.WhatsAhead,
        Destination.About, Destination.YourFiles, Destination.Credits,
        Destination.History, Destination.Forgotten,
        Destination.Surroundings -> Destination.Main(Tab.More)
        Destination.Export -> Destination.Settings
        Destination.Loved -> Destination.Main(Tab.Shelf)
        is Destination.ArtistDetail -> Destination.Main()
        is Destination.GenreFilter -> Destination.Main()  // clearGenreFilter runs on leaving, below
    }
    // A sheet is on top of everything, so back closes the sheet before it
    // moves anywhere. Back navigating out from under an open sheet is the
    // classic way to end up somewhere you did not ask for.
    val sheetOpen = sheetTarget != null || sortSheetOpen || queueSheetOpen ||
        pendingConfirm != null || surroundingsDetail != null
    BackHandler(enabled = sheetOpen || backTarget != null) {
        if (sheetOpen) {
            sheetTarget = null
            sortSheetOpen = false
            queueSheetOpen = false
            pendingConfirm = null
            viewModel.surroundings.closeDetail()
            return@BackHandler
        }
        // Leaving a genre view drops the filter. Without this the shelf stays
        // narrowed to a tag with nothing on screen saying why.
        if (destination is Destination.GenreFilter) viewModel.clearGenreFilter()
        backTarget?.let { destination = it }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MeedwellTheme.colors.background)
    ) {
        when (val current = destination) {
            Destination.Welcome -> WelcomeScreen(
                onConnect = { destination = Destination.Connect },
                onLocalOnly = {
                    viewModel.continueLocalOnly()
                    destination = Destination.Main()
                },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )

            Destination.Connect -> ConnectScreen(
                state = connect,
                onServerChange = viewModel::onServerChange,
                onUsernameChange = viewModel::onUsernameChange,
                onPasswordChange = viewModel::onPasswordChange,
                onConnect = { viewModel.connect { destination = Destination.Main() } },
                onOpenBandcampSettings = { open("https://bandcamp.com/settings/subsonic") },
                onBack = { destination = Destination.Welcome },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )

            is Destination.Main -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
                // The card floats over the content rather than sitting in the
                // flow beneath it, which is what its 88 percent tint is for:
                // whatever is behind ghosts through. So the content keeps its
                // full height and takes bottom padding equal to the card
                // instead, recomputed whenever the card appears, expands,
                // collapses or leaves.
                val cardRoom = if (current.tab == Tab.Surroundings) 0.dp
                    else surroundingsCardHeight(surroundingsCard)

                Box(Modifier.weight(1f)) {
                    when (current.tab) {
                        Tab.Shelf -> ShelfScreen(
                            state = shelf.copy(
                                playerVisible = playback.hasQueue,
                                cardRoom = cardRoom,
                            ),
                            onViewChange = viewModel::setView,
                            onToggleLayout = viewModel::toggleLayout,
                            onOpenSort = { sortSheetOpen = true },
                            onOpenSearch = { destination = Destination.Main(Tab.Search) },
                            onAlbumClick = { destination = Destination.AlbumDetail(it.id) },
                            onAlbumLongClick = { sheetTarget = it.asTarget() },
                            onArtistClick = {
                                viewModel.openArtist(it.id)
                                destination = Destination.ArtistDetail(it.id)
                            },
                            onGenreClick = { destination = Destination.GenreFilter(it.name) },
                            onFindOnBandcamp = { open("https://bandcamp.com/discover") },
                            onAddLocalFolders = { destination = Destination.YourFiles },
                        )
                        Tab.Search -> SearchScreen(
                            state = search,
                            onQueryChange = viewModel::onSearchQueryChange,
                            onAlbumClick = { destination = Destination.AlbumDetail(it.id) },
                            onTrackClick = { track -> viewModel.playTrack(track) },
                            onArtistClick = {
                                viewModel.openArtist(it.id)
                                destination = Destination.ArtistDetail(it.id)
                            },
                            // The only thing about a search that ever leaves
                            // the phone, and only when this is tapped.
                            onSearchBandcamp = { query -> open(bandcampSearchUrl(query)) },
                        )
                        Tab.Surroundings -> SurroundingsScreen(
                            state = surroundings,
                            onPlay = viewModel.surroundings::play,
                            onPause = viewModel.surroundings::pause,
                            onStop = viewModel.surroundings::stop,
                            onVolume = viewModel.surroundings::setVolume,
                            onDownload = viewModel.surroundings::download,
                            onCancelDownload = viewModel.surroundings::cancelDownload,
                            onDownloadGroup = viewModel.surroundings::downloadGroup,
                            onDownloadEverything = viewModel.surroundings::downloadEverything,
                            onCheckForNew = viewModel.surroundings::checkForNew,
                            onRemove = viewModel.surroundings::remove,
                            onOpenDetail = viewModel.surroundings::openDetail,
                            onToggleGroup = viewModel.surroundings::toggleGroup,
                            onOpenCredits = { destination = Destination.Credits },
                            onBack = { destination = Destination.Main() },
                        )
                        Tab.More -> MoreScreen(
                            connected = shelf.connected,
                            onOpen = { where ->
                                destination = when (where) {
                                    MoreDestination.Settings -> Destination.Settings
                                    MoreDestination.Privacy -> Destination.Privacy
                                    MoreDestination.WhatsAhead -> Destination.WhatsAhead
                                    MoreDestination.About -> Destination.About
                                    MoreDestination.YourFiles -> Destination.YourFiles
                                    MoreDestination.Credits -> Destination.Credits
                                    MoreDestination.History -> Destination.History
                                    MoreDestination.Forgotten -> Destination.Forgotten
                                    MoreDestination.Surroundings -> Destination.Surroundings
                                }
                            },
                            onConnectBandcamp = { destination = Destination.Connect },
                        )
                    }

                    // Suppressed on the Surroundings tab, where the whole
                    // library is already on screen, and on the player spread,
                    // whose Surroundings page carries its own volume control.
                    // Duplicating either would be clutter rather than help.
                    if (current.tab != Tab.Surroundings) {
                        SurroundingsCard(
                            state = surroundingsCard,
                            onToggleExpanded = viewModel.surroundings::toggleCard,
                            onVolume = viewModel.surroundings::setVolume,
                            onPick = viewModel.surroundings::play,
                            onStop = viewModel.surroundings::stop,
                            onOpenAll = { destination = Destination.Main(Tab.Surroundings) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                        )
                    }
                }
                MiniPlayer(
                    state = playback,
                    onPlayPause = viewModel.player::playPause,
                    onOpen = { destination = Destination.NowPlaying },
                )
                TabBar(
                    selected = current.tab,
                    onSelect = { destination = Destination.Main(it) },
                    playing = surroundings.isPlaying,
                )
            }

            Destination.Settings -> SettingsScreen(
                state = settingsState,
                onThemeChange = viewModel::setTheme,
                onToggleGapless = viewModel::toggleGapless,
                onToggleLongResume = viewModel::toggleLongResume,
                onOpenLocalFolders = { destination = Destination.YourFiles },
                onOpenExport = {
                    viewModel.refreshExportState()
                    destination = Destination.Export
                },
                onToggleShelfView = viewModel::toggleLayout,
                onSyncNow = viewModel::refresh,
                onToggleWifiOnly = viewModel::toggleWifiOnly,
                onEraseHistory = { pendingConfirm = PendingConfirm.EraseHistory },
                onDisconnect = { pendingConfirm = PendingConfirm.Disconnect },
                onSupport = { open(SUPPORT_URL) },
                onBack = { destination = Destination.Main(Tab.More) },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )

            Destination.Privacy -> PrivacyScreen(
                onOpenSource = { open(SOURCE_URL) },
                onBack = { destination = Destination.Main(Tab.More) },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )

            Destination.WhatsAhead -> WhatsAheadScreen(
                onBack = { destination = Destination.Main(Tab.More) },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )

            Destination.About -> AboutScreen(
                versionName = BuildConfig.VERSION_NAME,
                onOpenSource = { open(SOURCE_URL) },
                onOpenSite = { open("https://kamsiob.com") },
                onSupport = { open(SUPPORT_URL) },
                onBack = { destination = Destination.Main(Tab.More) },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )

            Destination.History -> WithMiniPlayer(
                playback = playback,
                onPlayPause = viewModel.player::playPause,
                onOpen = { destination = Destination.NowPlaying },
            ) {
                HistoryScreen(
                    days = history,
                    onTrackClick = { viewModel.playAlbum(it.albumId) },
                    onBack = { destination = Destination.Main(Tab.More) },
                    modifier = Modifier.statusBarsPadding(),
                )
            }

            Destination.Forgotten -> WithMiniPlayer(
                playback = playback,
                onPlayPause = viewModel.player::playPause,
                onOpen = { destination = Destination.NowPlaying },
            ) {
                ForgottenShelfScreen(
                    albums = forgotten,
                    onAlbumClick = { destination = Destination.AlbumDetail(it.id) },
                    onAlbumLongClick = { sheetTarget = it.asTarget() },
                    onBack = { destination = Destination.Main(Tab.More) },
                    modifier = Modifier.statusBarsPadding(),
                )
            }

            Destination.Loved -> WithMiniPlayer(
                playback = playback,
                onPlayPause = viewModel.player::playPause,
                onOpen = { destination = Destination.NowPlaying },
            ) {
                LovedScreen(
                    tracks = loved,
                    onTrackClick = { viewModel.playTrack(it) },
                    onTrackLongClick = { sheetTarget = it.asTarget() },
                    onBack = { destination = Destination.Main(Tab.Shelf) },
                    modifier = Modifier.statusBarsPadding(),
                )
            }

            is Destination.ArtistDetail -> WithMiniPlayer(
                playback = playback,
                onPlayPause = viewModel.player::playPause,
                onOpen = { destination = Destination.NowPlaying },
            ) {
                ArtistScreen(
                    state = artist,
                    onAlbumClick = { destination = Destination.AlbumDetail(it.id) },
                    onAlbumLongClick = { sheetTarget = it.asTarget() },
                    onOpenBandcamp = {
                        // No response from the API carries an artist page URL,
                        // so this is the constructed search deep link, which is
                        // the documented fallback rather than a guess.
                        open("https://bandcamp.com/search?q=" + artist.name.trim().replace(" ", "+") + "&item_type=b")
                    },
                    onBack = { destination = Destination.Main() },
                    modifier = Modifier.statusBarsPadding(),
                )
            }

            is Destination.GenreFilter -> {
                LaunchedEffect(current.genre) { viewModel.filterByGenre(current.genre) }

                // Everything in this view leaves the filter behind, because a
                // genre is a lens on the shelf rather than a place. Tapping a
                // view tab, tapping the filter chip, or going back all put the
                // whole shelf back.
                fun leaveFilter() {
                    viewModel.clearGenreFilter()
                    destination = Destination.Main()
                }

                Column(Modifier.fillMaxSize().statusBarsPadding()) {
                    Box(Modifier.weight(1f)) {
                        ShelfScreen(
                            state = shelf.copy(playerVisible = playback.hasQueue),
                            onViewChange = { leaveFilter() },
                            onToggleLayout = viewModel::toggleLayout,
                            // In this view the label slot is the filter chip,
                            // so tapping it takes the filter off.
                            onOpenSort = { leaveFilter() },
                            onOpenSearch = { destination = Destination.Main(Tab.Search) },
                            onAlbumClick = { destination = Destination.AlbumDetail(it.id) },
                            onAlbumLongClick = { sheetTarget = it.asTarget() },
                            onArtistClick = {
                                viewModel.clearGenreFilter()
                                viewModel.openArtist(it.id)
                                destination = Destination.ArtistDetail(it.id)
                            },
                            onGenreClick = { destination = Destination.GenreFilter(it.name) },
                            onFindOnBandcamp = { open("https://bandcamp.com/discover") },
                            onAddLocalFolders = { destination = Destination.YourFiles },
                        )
                        // The mini player was missing here, so starting
                        // something from a genre view left no way back to it
                        // short of leaving the view.
                        MiniPlayer(
                            state = playback,
                            onPlayPause = viewModel.player::playPause,
                            onOpen = { destination = Destination.NowPlaying },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                                .navigationBarsPadding(),
                        )
                    }
                }
            }

            Destination.Export -> WithMiniPlayer(
                playback = playback,
                onPlayPause = viewModel.player::playPause,
                onOpen = { destination = Destination.NowPlaying },
            ) {
                ExportScreen(
                    state = exportState,
                    onExport = { exportPicker.launch(viewModel.suggestedBackupName()) },
                    // Restore replaces rather than merges, so it is confirmed
                    // before the picker rather than after: choosing a file
                    // should not be the moment somebody learns what it does.
                    onRestore = { pendingConfirm = PendingConfirm.Restore },
                    onBack = { destination = Destination.Settings },
                    modifier = Modifier.statusBarsPadding(),
                )
            }

            Destination.Surroundings -> WithMiniPlayer(
                playback = playback,
                onPlayPause = viewModel.player::playPause,
                onOpen = { destination = Destination.NowPlaying },
            ) {
                SurroundingsScreen(
                    state = surroundings,
                    onPlay = viewModel.surroundings::play,
                    onPause = viewModel.surroundings::pause,
                    onStop = viewModel.surroundings::stop,
                    onVolume = viewModel.surroundings::setVolume,
                    onDownload = viewModel.surroundings::download,
                    onCancelDownload = viewModel.surroundings::cancelDownload,
                    onDownloadGroup = viewModel.surroundings::downloadGroup,
                    onDownloadEverything = viewModel.surroundings::downloadEverything,
                    onCheckForNew = viewModel.surroundings::checkForNew,
                    onRemove = viewModel.surroundings::remove,
                    onOpenDetail = viewModel.surroundings::openDetail,
                    onToggleGroup = viewModel.surroundings::toggleGroup,
                    onOpenCredits = { destination = Destination.Credits },
                    onBack = { destination = Destination.Main(Tab.More) },
                    modifier = Modifier.statusBarsPadding(),
                )
            }

            Destination.Credits -> WithMiniPlayer(
                playback = playback,
                onPlayPause = viewModel.player::playPause,
                onOpen = { destination = Destination.NowPlaying },
            ) {
                CreditsScreen(
                    summary = credits.summary,
                    groups = credits.groups,
                    loadError = credits.loadError,
                    softwareNotices = SOFTWARE_NOTICES,
                    onOpenUrl = { open(it) },
                    onBack = { destination = Destination.Main(Tab.More) },
                    modifier = Modifier.statusBarsPadding(),
                )
            }

            Destination.YourFiles -> YourFilesScreen(
                state = yourFiles,
                onAddFolder = { folderPicker.launch(null) },
                onRemoveFolder = viewModel::removeWatchedFolder,
                onRescan = viewModel::rescanFolders,
                onGetFromBandcamp = { open("https://bandcamp.com/collection") },
                onAlbumClick = { destination = Destination.AlbumDetail(it.id) },
                onBack = { destination = Destination.Main(Tab.More) },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )

            Destination.NowPlaying -> PlayerSpread(
                page = playerPage,
                onPageChange = { playerPage = it },
                state = playback,
                surroundings = SurroundingsPlayingState(
                    title = surroundings.playingTitle,
                    description = "",
                    credit = surroundings.playingCredit,
                    isPlaying = surroundings.isPlaying,
                    volume = surroundings.volume,
                    hasSound = surroundings.playingId != null,
                ),
                onCollapse = { destination = Destination.Main() },
                onMenu = { viewModel.openSheetForCurrentTrack { sheetTarget = it } },
                onPlayPause = viewModel.player::playPause,
                onNext = { viewModel.player.next() },
                onPrevious = viewModel.player::previous,
                onSeek = viewModel.player::seekTo,
                onOpenArtwork = {
                    destination = Destination.Artwork(
                        uri = playback.artworkUri,
                        title = playback.title,
                        subtitle = playback.artist,
                    )
                },
                onOpenQueue = { queueSheetOpen = true },
                onLove = { viewModel.loveCurrentTrack() },
                onSleepTimer = { viewModel.showSleepTimerComing() },
                onTone = { viewModel.showToneComing() },
                onSurroundingsPlayPause = {
                    if (surroundings.isPlaying) viewModel.surroundings.pause()
                    else surroundings.playingId?.let { viewModel.surroundings.play(it) }
                },
                onSurroundingsVolume = viewModel.surroundings::setVolume,
                onSurroundingsCredit = {
                    surroundings.playingId?.let { viewModel.surroundings.openDetail(it) }
                },
                onBrowseSurroundings = { destination = Destination.Main(Tab.Surroundings) },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )

            is Destination.Artwork -> ArtworkViewer(
                artworkUri = current.uri,
                title = current.title,
                subtitle = current.subtitle,
                onClose = { destination = Destination.NowPlaying },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )

            is Destination.AlbumDetail -> {
                LaunchedEffect(current.albumId) { viewModel.openAlbum(current.albumId) }
                val detail = albumDetail
                if (detail == null) {
                    // No spinner. The album is in the database already, so this
                    // is a frame or two, and a spinner would be theatre.
                    Box(Modifier.fillMaxSize())
                } else {
                    Column(Modifier.fillMaxSize().statusBarsPadding()) {
                        Box(Modifier.weight(1f)) {
                            AlbumScreen(
                                album = detail.album,
                                tracks = detail.tracks,
                                playingTrackId = playback.trackId,
                                onBack = { destination = Destination.Main() },
                                onPlay = { viewModel.playAlbum(detail.album.id) },
                                onShuffle = { viewModel.shuffleAlbum(detail.album.id) },
                                onTrackClick = { index -> viewModel.playAlbum(detail.album.id, index) },
                                onTrackLongClick = { sheetTarget = it.asTarget() },
                                onAlbumMenu = { sheetTarget = detail.album.asTarget() },
                                onOpenArtwork = {
                                    destination = Destination.Artwork(
                                        uri = detail.album.coverUrl,
                                        title = detail.album.name,
                                        subtitle = detail.album.artist,
                                    )
                                },
                            )
                        }
                        MiniPlayer(
                            state = playback,
                            onPlayPause = viewModel.player::playPause,
                            onOpen = { destination = Destination.NowPlaying },
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .navigationBarsPadding(),
                        )
                    }
                }
            }
        }

        // Connection trouble, which was fully written in strings.xml, populated
        // correctly by the view model, and collected by nothing. On a beta API
        // a failed sync is not rare, and until now it showed as an empty shelf
        // saying "your collection is empty".
        syncFailure?.let { failure ->
            SyncTroubleSheet(
                failure = failure,
                lastSyncAt = viewModel.lastSyncAt,
                onRetry = { viewModel.refresh() },
                onFreshCredentials = { open("https://bandcamp.com/settings/subsonic") },
                onDismiss = { viewModel.dismissSyncFailure() },
            )
        }

        if (sortSheetOpen) {
            SortSheet(
                sort = sort,
                scope = scope,
                onPick = { newSort, newScope -> viewModel.setSort(newSort, newScope) },
                onDismiss = { sortSheetOpen = false },
            )
        }

        if (queueSheetOpen) {
            // Read off the player rather than mirrored, and re-read whenever
            // the queue or the current track changes. One copy, no drift.
            val queueItems = remember(playback.trackId, playback.queueSize) {
                viewModel.player.queueSnapshot()
            }
            QueueSheet(
                items = queueItems,
                onPlay = { viewModel.player.playQueueItem(it) },
                onRemove = { viewModel.player.removeQueueItem(it) },
                onDismiss = { queueSheetOpen = false },
            )
        }

        sheetTarget?.let { target ->
            ActionSheet(
                target = target,
                onAction = { action ->
                    when (action) {
                        SheetAction.PlayNext -> viewModel.playNext(target)
                        SheetAction.AddToQueue -> viewModel.addToQueue(target)
                        SheetAction.Love -> viewModel.love(target)
                        // Already loved. The row exists to state the limit, so
                        // tapping it repeats that limit rather than doing
                        // nothing or pretending to remove the heart.
                        SheetAction.AlreadyLoved -> viewModel.showLoveLimit()
                        SheetAction.ViewArtwork -> destination = Destination.Artwork(
                            uri = target.coverUrl,
                            title = target.title,
                            subtitle = target.subtitle,
                        )
                        SheetAction.GoToArtist -> target.artistId?.let {
                            viewModel.openArtist(it)
                            destination = Destination.ArtistDetail(it)
                        }
                        // Shares a Bandcamp search rather than a page URL. No
                        // API response carries a real page link, and a share
                        // that lands somewhere wrong is worse than a search.
                        SheetAction.Share -> shareText(
                            context,
                            "${target.title} by ${target.subtitle}\n" + bandcampSearchUrl(target.title),
                        )
                    }
                },
                onDismiss = { sheetTarget = null },
            )
        }

        surroundingsDetail?.let { detail ->
            CreditSheet(
                title = detail.title,
                originalTitle = detail.originalTitle,
                credit = detail.credit,
                onOpenUrl = { open(it) },
                onDismiss = viewModel.surroundings::closeDetail,
            )
        }

        // How much sits at the bottom of the current screen, so a notice lands
        // above it rather than across a track title. Measured in the same
        // numbers the layouts use rather than guessed at.
        val noticeLift = run {
            var lift = 0.dp
            if (playback.hasQueue) lift += 64.dp
            if (destination is Destination.Main) lift += 58.dp
            if (destination == Destination.Surroundings && surroundings.playingId != null) lift += 126.dp
            lift
        }
        Notice(text = notice, liftedBy = noticeLift, onDismiss = viewModel::dismissNotice)

        pendingConfirm?.let { pending ->
            when (pending) {
                PendingConfirm.EraseHistory -> ConfirmSheet(
                    title = "Erase your listening history?",
                    body = "This clears every play Meedwell has recorded on this phone, which is what " +
                        "the forgotten shelf and your resume points are built from. Your music and your " +
                        "shelf are untouched. This one cannot be undone.",
                    confirmLabel = "Erase it",
                    onConfirm = viewModel::eraseHistory,
                    onDismiss = { pendingConfirm = null },
                )
                PendingConfirm.Restore -> ConfirmSheet(
                    title = "Replace everything on this phone?",
                    body = "Restoring puts the file's listening history, hearts, lists and settings " +
                        "in place of the ones here. It does not merge them: two histories that have " +
                        "drifted apart cannot be joined into one true answer. Your music and your " +
                        "shelf are untouched. This one cannot be undone.",
                    confirmLabel = "Choose a file",
                    onConfirm = { restorePicker.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    onDismiss = { pendingConfirm = null },
                )
                PendingConfirm.Disconnect -> ConfirmSheet(
                    title = "Disconnect from Bandcamp?",
                    body = "Meedwell forgets your credentials and stops syncing. Your shelf, your lists " +
                        "and your listening history all stay. To connect again you will need to paste " +
                        "both lines from Bandcamp's Subsonic settings.",
                    confirmLabel = "Disconnect",
                    onConfirm = viewModel::disconnect,
                    onDismiss = { pendingConfirm = null },
                )
            }
        }
    }
}

/**
 * Puts the mini player under a screen that is not the shelf.
 *
 * The player used to exist only inside the four tabs and the album screen, so
 * starting something and then opening History, Loved, an artist or the credits
 * made it disappear: no pause, no title, no way back to what was playing short
 * of retracing your steps. A music player losing its transport when you walk
 * around it is the app forgetting what it is for.
 *
 * In flow rather than floating over the content, so nothing has to guess at how
 * much room to leave and no screen can ever have its last row covered.
 */
@Composable
private fun WithMiniPlayer(
    playback: com.kamsiob.meedwell.playback.PlaybackState,
    onPlayPause: () -> Unit,
    onOpen: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { content() }
        if (playback.hasQueue) {
            MiniPlayer(
                state = playback,
                onPlayPause = onPlayPause,
                onOpen = onOpen,
                modifier = Modifier.padding(bottom = 6.dp).navigationBarsPadding(),
            )
        } else {
            // The gesture bar still has to be cleared when nothing is playing,
            // and the wrapped screen no longer pads for it itself.
            Box(Modifier.navigationBarsPadding())
        }
    }
}

/**
 * Hands text to the system share sheet.
 *
 * Meedwell never posts anything anywhere itself. This opens Android's own
 * chooser and stops, which is what the Privacy screen promises under "Sharing
 * and outside links".
 */
private fun shareText(context: android.content.Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, null)) }
}

/** An album as a sheet target. */
private fun com.kamsiob.meedwell.core.model.Album.asTarget() = ActionTarget(
    id = id,
    title = name,
    subtitle = artist,
    coverUrl = coverUrl,
    kind = ActionTarget.Kind.Album,
    isStarred = isStarred,
    artistId = artistId.takeIf { it.isNotBlank() },
)

/** A track as a sheet target. */
private fun com.kamsiob.meedwell.core.model.Track.asTarget() = ActionTarget(
    id = id,
    title = title,
    subtitle = artist,
    coverUrl = com.kamsiob.meedwell.ui.screens.CoverUrls.of(coverArtId),
    kind = ActionTarget.Kind.Track,
    isStarred = isStarred,
    artistId = artistId.takeIf { it.isNotBlank() },
)

@Composable
private fun TabBar(selected: Tab, onSelect: (Tab) -> Unit, playing: Boolean) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Column {
        // `.tabs { border-top: 1px solid var(--hair) }`. A full-width hairline,
        // 1dp, not the 0.5dp it used to be.
        Hairline()
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.background)
                .navigationBarsPadding()
                // `.tabs { padding: 9px 4px 18px }`.
                .padding(start = 4.dp, end = 4.dp, top = 9.dp, bottom = 18.dp),
        ) {
            Tab.entries.forEach { tab ->
                val isSelected = tab == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable(role = Role.Tab) { onSelect(tab) }
                        .semantics {
                            contentDescription = when {
                                isSelected -> "${tab.label}, showing"
                                tab == Tab.Surroundings && playing -> "${tab.label}, a sound is playing"
                                else -> tab.label
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        MeedwellIcon(
                            icon = tab.icon,
                            // `.tabs .tb i { height: 17px }`.
                            size = 17.dp,
                            tint = if (isSelected) colors.primaryText else colors.tertiaryText,
                        )
                        // `.livedot`: a 4px moss dot beside the Surroundings
                        // icon while something is playing, so the fact is
                        // readable from anywhere in the app without a banner.
                        if (tab == Tab.Surroundings && playing) {
                            Box(
                                Modifier
                                    .padding(start = 3.dp)
                                    .offset(x = 6.dp, y = (-1).dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(colors.moss)
                            )
                        }
                    }
                    Text(
                        text = tab.label,
                        style = type.tabLabel,
                        color = if (isSelected) colors.primaryText else colors.tertiaryText,
                        // `.tabs .tb i { margin: 0 auto 4px }`.
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Placeholder(title: String, body: String, onBack: (() -> Unit)? = null) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography
    Column(
        Modifier.fillMaxSize().padding(26.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(title, style = type.h2, color = colors.primaryText)
        Text(body, style = type.body, color = colors.secondaryText, modifier = Modifier.padding(top = 10.dp))
        if (onBack != null) {
            Text(
                "Back",
                style = type.meta,
                color = colors.secondaryText,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .height(48.dp)
                    .clickable(role = Role.Button, onClick = onBack),
            )
        }
    }
}

/** The support link. One label, one place, never a coffee cliche. */
private const val SUPPORT_URL = "https://buymeacoffee.com/kamsiob"
private const val SOURCE_URL = "https://github.com/Kamsiob/meedwell"

/**
 * The software this app is built on.
 *
 * Listed alongside the recording credits rather than on a separate screen,
 * because a reader looking for "what is in here and under what terms" wants one
 * answer, not two.
 */
private val SOFTWARE_NOTICES = listOf(
    SoftwareNotice("Jetpack Compose, Media3, Room", "Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
    SoftwareNotice("Kotlin and kotlinx", "Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
    SoftwareNotice("OkHttp", "Apache 2.0", "https://square.github.io/okhttp/"),
    SoftwareNotice("Coil", "Apache 2.0", "https://coil-kt.github.io/coil/"),
    SoftwareNotice("Instrument Sans and Instrument Serif", "SIL Open Font License 1.1", "https://openfontlicense.org"),
    SoftwareNotice("Meedwell itself", "AGPL-3.0", "https://github.com/Kamsiob/meedwell"),
)