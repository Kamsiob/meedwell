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
import com.kamsiob.meedwell.ui.screens.ArtworkViewer
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
}

@Composable
fun MeedwellApp(viewModel: MeedwellViewModel) {
    val context = LocalContext.current
    val shelf by viewModel.shelf.collectAsState()
    val connect by viewModel.connect.collectAsState()
    val playback by viewModel.playback.collectAsState()
    val albumDetail by viewModel.albumDetail.collectAsState()

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
                        Tab.Search -> Placeholder("Search", "Your shelf, and Bandcamp's site one tap away.")
                        Tab.Lists -> Placeholder("Lists", "Lists live on this phone.")
                        Tab.More -> Placeholder("More", "Privacy, What's ahead, Settings and About.")
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
