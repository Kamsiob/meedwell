package com.kamsiob.meedwell.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.kamsiob.meedwell.ui.components.MiniPlayer
import com.kamsiob.meedwell.ui.components.rememberWashColor
import com.kamsiob.meedwell.ui.screens.AlbumScreen
import com.kamsiob.meedwell.ui.screens.AboutScreen
import com.kamsiob.meedwell.ui.screens.ArtworkViewer
import com.kamsiob.meedwell.ui.screens.MoreDestination
import com.kamsiob.meedwell.ui.screens.MoreScreen
import com.kamsiob.meedwell.ui.screens.PrivacyScreen
import com.kamsiob.meedwell.ui.screens.SearchScreen
import com.kamsiob.meedwell.ui.screens.bandcampSearchUrl
import com.kamsiob.meedwell.ui.screens.SettingsScreen
import com.kamsiob.meedwell.ui.screens.WhatsAheadScreen
import com.kamsiob.meedwell.ui.screens.YourFilesScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.kamsiob.meedwell.BuildConfig
import com.kamsiob.meedwell.ui.screens.NowPlayingScreen
import com.kamsiob.meedwell.ui.screens.ConnectScreen
import com.kamsiob.meedwell.ui.screens.ShelfScreen
import com.kamsiob.meedwell.ui.screens.coverUrl
import com.kamsiob.meedwell.ui.screens.WelcomeScreen
import com.kamsiob.meedwell.ui.theme.MeedwellTheme

/** The four labelled tabs. Labelled rather than icon-only, on purpose. */
enum class Tab(val label: String) { Shelf("Shelf"), Search("Search"), Lists("Lists"), More("More") }

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

    // The Storage Access Framework picker. A tree grant with persistable
    // permission, so the folder survives a reboot rather than being asked for
    // again every launch.
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) viewModel.addWatchedFolder(uri) }

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
                Box(Modifier.weight(1f)) {
                    when (current.tab) {
                        Tab.Shelf -> ShelfScreen(
                            state = shelf,
                            onViewChange = viewModel::setView,
                            onToggleLayout = viewModel::toggleLayout,
                            onOpenSort = { /* Phase 1: the sort sheet */ },
                            onOpenSearch = { destination = Destination.Main(Tab.Search) },
                            onAlbumClick = { destination = Destination.AlbumDetail(it.id) },
                            onAlbumLongClick = { /* Phase 3: the action sheet */ },
                            onArtistClick = { /* Phase 3: artist pages */ },
                            onGenreClick = { /* Phase 1: genre filtering */ },
                            onFindOnBandcamp = { open("https://bandcamp.com/discover") },
                            onAddLocalFolders = { /* Phase 2: watched folders */ },
                        )
                        Tab.Search -> SearchScreen(
                            state = search,
                            onQueryChange = viewModel::onSearchQueryChange,
                            onAlbumClick = { destination = Destination.AlbumDetail(it.id) },
                            onTrackClick = { track -> viewModel.playAlbum(track.albumId) },
                            onArtistClick = { /* Phase 3: artist pages */ },
                            // The only thing about a search that ever leaves
                            // the phone, and only when this is tapped.
                            onSearchBandcamp = { query -> open(bandcampSearchUrl(query)) },
                        )
                        Tab.Lists -> Placeholder("Lists", "Lists live on this phone.")
                        Tab.More -> MoreScreen(
                            connected = shelf.connected,
                            onOpen = { where ->
                                destination = when (where) {
                                    MoreDestination.Settings -> Destination.Settings
                                    MoreDestination.Privacy -> Destination.Privacy
                                    MoreDestination.WhatsAhead -> Destination.WhatsAhead
                                    MoreDestination.About -> Destination.About
                                    MoreDestination.YourFiles -> Destination.YourFiles
                                    // History and the forgotten shelf arrive in
                                    // Phase 3; both read the same play log.
                                    else -> Destination.Main(Tab.More)
                                }
                            },
                            onConnectBandcamp = { destination = Destination.Connect },
                        )
                    }
                }
                MiniPlayer(
                    state = playback,
                    onPlayPause = viewModel.player::playPause,
                    onOpen = { destination = Destination.NowPlaying },
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                TabBar(
                    selected = current.tab,
                    onSelect = { destination = Destination.Main(it) },
                )
            }

            Destination.Settings -> SettingsScreen(
                state = settingsState,
                onThemeChange = viewModel::setTheme,
                onToggleGapless = viewModel::toggleGapless,
                onToggleLongResume = viewModel::toggleLongResume,
                onOpenLocalFolders = { destination = Destination.YourFiles },
                onOpenExport = { /* Phase 6: export and restore */ },
                onEraseHistory = viewModel::eraseHistory,
                onDisconnect = viewModel::disconnect,
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

            Destination.NowPlaying -> {
                val wash by rememberWashColor(playback.artworkUri)
                NowPlayingScreen(
                    state = playback,
                    washColor = wash,
                    onCollapse = { destination = Destination.Main() },
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
                    onOpenQueue = { /* Phase 3: the queue sheet */ },
                    onMenu = { /* Phase 3: the action sheet */ },
                    modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
                )
            }

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
                                onTrackLongClick = { /* Phase 3: the action sheet */ },
                                onAlbumMenu = { /* Phase 3: the action sheet */ },
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
    }
}

@Composable
private fun TabBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val colors = MeedwellTheme.colors
    val type = MeedwellTheme.typography

    Column {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.hairline))
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.background)
                .navigationBarsPadding()
                .padding(vertical = 6.dp),
        ) {
            Tab.entries.forEach { tab ->
                val isSelected = tab == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        // Comfortably past the 48dp touch target floor.
                        .height(56.dp)
                        .clickable(role = Role.Tab) { onSelect(tab) }
                        .semantics {
                            contentDescription = if (isSelected) "${tab.label}, showing" else tab.label
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.label,
                        style = type.metadata,
                        // Labelled tabs, and selection carried by ink weight
                        // rather than by colour alone.
                        color = if (isSelected) colors.primaryText else colors.tertiaryText,
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
        Text(title, style = type.sectionHeading, color = colors.primaryText)
        Text(body, style = type.body, color = colors.secondaryText, modifier = Modifier.padding(top = 10.dp))
        if (onBack != null) {
            Text(
                "Back",
                style = type.metadata,
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
