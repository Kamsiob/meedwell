package com.kamsiob.meedwell.ui

import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
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
import com.kamsiob.meedwell.ui.components.OutputSheet
import com.kamsiob.meedwell.ui.components.SortSheet
import com.kamsiob.meedwell.ui.components.SyncTroubleSheet
import com.kamsiob.meedwell.ui.components.PendingConfirm
import com.kamsiob.meedwell.ui.components.TimePickSheet
import com.kamsiob.meedwell.ui.components.halfHoursBetween
import com.kamsiob.meedwell.ui.components.DaySpan
import com.kamsiob.meedwell.ui.components.AddToListSheet
import com.kamsiob.meedwell.data.SurroundingsDownloads
import com.kamsiob.meedwell.ui.components.CellularSheet
import com.kamsiob.meedwell.ui.components.NameSheet
import com.kamsiob.meedwell.ui.screens.PlaylistScreen
import com.kamsiob.meedwell.ui.components.MiniPlayer
import com.kamsiob.meedwell.ui.components.rememberWashColor
import com.kamsiob.meedwell.ui.screens.AlbumScreen
import com.kamsiob.meedwell.ui.screens.AboutScreen
import com.kamsiob.meedwell.ui.screens.ArtworkViewer
import com.kamsiob.meedwell.ui.screens.MoreDestination
import com.kamsiob.meedwell.ui.screens.MoreScreen
import com.kamsiob.meedwell.ui.screens.NotPlannedScreen
import com.kamsiob.meedwell.ui.screens.SleepTimerScreen
import com.kamsiob.meedwell.ui.screens.ToneScreen
import com.kamsiob.meedwell.ui.screens.ArtistScreen
import com.kamsiob.meedwell.ui.screens.CreditsScreen
import com.kamsiob.meedwell.ui.components.CreditSheet
import com.kamsiob.meedwell.ui.screens.ExportScreen
import com.kamsiob.meedwell.ui.screens.ForgottenShelfScreen
import com.kamsiob.meedwell.ui.screens.SurroundingsGroupScreen
import com.kamsiob.meedwell.ui.screens.SurroundingsStorageScreen
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
import com.kamsiob.meedwell.ui.screens.WhereMusicScreen
import com.kamsiob.meedwell.ui.screens.ToneIntroScreen
import com.kamsiob.meedwell.ui.theme.MeedwellTheme
import com.kamsiob.meedwell.ui.theme.Motion

/**
 * The four tabs: an icon **and** a label, as the reference draws them.
 *
 * Labels rather than icons alone, because an icon-only bar makes people guess,
 * and icons rather than labels alone, because a row of four words is harder to
 * hit accurately than a shape with a word under it.
 */
/**
 * How far a swipe has to travel to change tab.
 *
 * Deliberately long. A tab that changed on a short flick would fire while
 * somebody was aiming at a row, and being moved to another screen by accident is
 * far worse than a gesture that occasionally has to be repeated.
 */
private const val TAB_SWIPE_PX = 140f

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
    /** Grid 01, the declaration. */
    data object Welcome : Destination

    /** Grid 02, two ways in. */
    data object WhereMusic : Destination

    /**
     * Grid 03, the tone disclosure.
     *
     * The last step of onboarding rather than the first, because it is the one
     * screen that only makes sense once somebody has decided to stay.
     */
    data object ToneIntro : Destination
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

    /** One group of recordings, which used to be an accordion section. */
    data class SoundGroup(val id: String) : Destination

    /** What is stored, and the only place a recording can be removed. */
    data object SoundStorage : Destination
    data object Export : Destination
    data object NotPlanned : Destination
    data object SleepTimer : Destination
    data object Tone : Destination
    data object Loved : Destination

    /** One list, with its own order. */
    data class Playlist(val id: String) : Destination
    data class ArtistDetail(val artistId: String) : Destination
    data class GenreFilter(val genre: String) : Destination
}

@Composable
fun MeedwellApp(viewModel: MeedwellViewModel) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
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
    val playlist by viewModel.playlist.collectAsState()
    val askCellular by viewModel.surroundings.askCellular.collectAsState()
    val downloadQueue by SurroundingsDownloads.state.collectAsState()
    val downloadsBusy = downloadQueue.busy
    val loved by viewModel.loved.collectAsState()
    val artist by viewModel.artist.collectAsState()
    val syncFailure by viewModel.syncFailure.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val surroundings by viewModel.surroundings.state.collectAsState()
    val surroundingsDetail by viewModel.surroundings.detail.collectAsState()
    val surroundingsCard by viewModel.surroundings.card.collectAsState()
    val surroundingsSlices by viewModel.surroundings.slices.collectAsState()
    val exportState by viewModel.export.collectAsState()
    val moreState by viewModel.more.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val scope by viewModel.scope.collectAsState()
    var pendingConfirm by remember { mutableStateOf<PendingConfirm?>(null) }

    // The three sheets that any screen can raise. Held here rather than inside
    // each screen so that a long-press on a track means the same thing on the
    // shelf, in search, in an album and on an artist page.
    var sheetTarget by remember { mutableStateOf<ActionTarget?>(null) }
    var sortSheetOpen by remember { mutableStateOf(false) }
    var queueSheetOpen by remember { mutableStateOf(false) }
    var outputSheetOpen by remember { mutableStateOf(false) }

    // Which page of the player spread is showing.
    //
    // **It opens on the music, every time.** It used to keep whatever page you
    // last swiped to, for the whole life of the composition, so somebody who had
    // glanced at Surroundings once found the player opening there for the rest
    // of the session, including when they came back to the app with music
    // playing. Whatever else the player is, it is a music player first.
    var playerPage by remember { mutableStateOf(PlayerPage.Music) }

    /**
     * The tab you were last actually on.
     *
     * Back used to be a flat table: everything under an album or the player
     * mapped to `Destination.Main()`, and that default is the Shelf. So opening
     * a record from Search and pressing back put you on the Shelf, which is not
     * where you were and is exactly the inconsistency the owner reported. This
     * remembers the real answer.
     */
    var lastMainTab by remember { mutableStateOf(Tab.Shelf) }

    /**
     * Where the sleep timer and Tone give you back to.
     *
     * Their back target was hardcoded to the More tab, so setting a timer from
     * the player and pressing back stranded you in a menu with the player gone,
     * at night, half asleep. The screens now return you to wherever you came
     * from.
     */
    var toolReturn by remember { mutableStateOf<Destination>(Destination.Main(Tab.More)) }

    /**
     * The Surroundings card's measured height, plus the gap under it.
     *
     * Null until the card has been laid out once. Null rather than a default so
     * the estimate is used for exactly one frame instead of being silently
     * relied on forever.
     */
    var measuredCardHeight by remember { mutableStateOf<Dp?>(null) }

    /** Which of the two hours is being set, or null when neither is. */
    var timePick by remember { mutableStateOf<TimePick?>(null) }

    /** A list being named or renamed, or null when none is. */
    var naming by remember { mutableStateOf<Naming?>(null) }

    /** A track or album waiting to be put in a list. */
    var addingToList by remember { mutableStateOf<ActionTarget?>(null) }

    /**
     * What to drop into the list that is about to be made.
     *
     * Somebody who taps "Add to list" and then "New list" has said twice what
     * they want; asking them to add the track again afterwards would be the app
     * forgetting mid-sentence.
     */
    var pendingAdd by remember { mutableStateOf<ActionTarget?>(null) }

    var destination by remember {
        mutableStateOf<Destination>(
            // A returning user lands on their shelf. Only somebody who has
            // never chosen a path sees the Welcome screen.
            if (viewModel.hasChosenPath) Destination.Main() else Destination.Welcome
        )
    }

    // The Storage Access Framework picker. A tree grant with persistable
    // permission, so the folder survives a reboot rather than being asked for
    // again every launch.
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.addWatchedFolder(uri)
            // Picking a folder during onboarding is the local-only path's
            // equivalent of connecting, so it advances the same way. Outside
            // onboarding it just adds a folder and stays put.
            if (destination == Destination.WhereMusic) destination = Destination.ToneIntro
        }
    }

    // Export writes to a document the user names, and restore reads one they
    // pick. Both go through the system picker rather than a path this app
    // chooses, so the file lands somewhere they can actually find it again.
    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.exportTo(uri) }

    val restorePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.restoreFrom(uri) }

    /**
     * The notification permission, asked for at the first note and never again.
     *
     * It was never asked for at all, so on Android 13 and up the playback
     * notification simply did not exist: no lock screen controls, no shade
     * player, no way to pause without reopening the app. That is not a small
     * omission in a music player.
     *
     * **Asked at the first play rather than at launch.** A permission dialog on
     * a screen that has never made a sound has to be answered on trust; one that
     * arrives as the music starts is asking about a thing that is happening.
     * Refusing costs nothing but the shade controls, so it is never asked twice.
     */
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.markNotificationsAsked() }

    /**
     * And again as the first download starts.
     *
     * **A refusal at the first play must not silently decide this too.** A
     * download runs as a foreground service, and a foreground service whose
     * notification the system will not draw is work happening with nothing to
     * show for it: no name, no progress, no way to stop it without coming back
     * into the app. A phone here was in exactly that state, and a whole group
     * arrived start to finish with no sign of it anywhere.
     *
     * Asked after the download has started rather than before, because it is not
     * a condition of downloading and holding the queue hostage to a dialog would
     * imply that it is. It arrives over a thing that is already happening, which
     * is the only moment this question explains itself.
     */
    val downloadNotificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.markDownloadNotificationsAsked() }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(playback.isPlaying) {
            if (playback.isPlaying && viewModel.shouldAskForNotifications()) {
                notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        LaunchedEffect(downloadsBusy) {
            if (downloadsBusy && viewModel.shouldAskForDownloadNotifications()) {
                downloadNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
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
    LaunchedEffect(destination) {
        (destination as? Destination.Main)?.let { lastMainTab = it.tab }
        // Opening the player always starts on the music page.
        if (destination == Destination.NowPlaying) playerPage = PlayerPage.Music
    }

    val backTarget: Destination? = when (val current = destination) {
        Destination.Welcome -> null
        Destination.WhereMusic -> Destination.Welcome
        // Back from the tone disclosure goes to the fork rather than out of
        // onboarding, so the last screen is never a dead end.
        Destination.ToneIntro -> Destination.WhereMusic
        Destination.Connect -> Destination.WhereMusic
        is Destination.Main -> if (current.tab == Tab.Shelf) null else Destination.Main(Tab.Shelf)
        is Destination.AlbumDetail -> Destination.Main(lastMainTab)
        Destination.NowPlaying -> Destination.Main(lastMainTab)
        is Destination.Artwork -> Destination.NowPlaying
        Destination.Settings, Destination.Privacy, Destination.WhatsAhead,
        Destination.About, Destination.YourFiles, Destination.Credits,
        Destination.History, Destination.Forgotten,
        Destination.Surroundings -> Destination.Main(Tab.More)
        is Destination.SoundGroup -> Destination.Main(Tab.Surroundings)
        Destination.SoundStorage -> Destination.Main(Tab.Surroundings)
        Destination.Export -> Destination.Settings
        Destination.NotPlanned -> Destination.Main(Tab.More)
        Destination.SleepTimer -> toolReturn
        Destination.Tone -> toolReturn
        Destination.Loved -> Destination.Main(lastMainTab)
        is Destination.Playlist -> Destination.Main(lastMainTab)
        is Destination.ArtistDetail -> Destination.Main(lastMainTab)
        is Destination.GenreFilter -> Destination.Main(lastMainTab)  // clearGenreFilter runs on leaving, below
    }
    // A sheet is on top of everything, so back closes the sheet before it
    // moves anywhere. Back navigating out from under an open sheet is the
    // classic way to end up somewhere you did not ask for.
    val sheetOpen = sheetTarget != null || sortSheetOpen || queueSheetOpen || outputSheetOpen ||
        pendingConfirm != null || surroundingsDetail != null
    BackHandler(enabled = sheetOpen || backTarget != null) {
        if (sheetOpen) {
            sheetTarget = null
            sortSheetOpen = false
            queueSheetOpen = false
            outputSheetOpen = false
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
            .background(MeedwellTheme.colors.background),
        contentAlignment = Alignment.TopCenter,
    ) {
      // A ceiling on how wide the page gets, and the only concession the layout
      // makes to screen size.
      //
      // Every measurement in this app comes from a grid drawn at phone width. On
      // a tablet or an unfolded foldable, letting that stretch would give body
      // copy lines of well over a hundred characters, which is past the point
      // where the eye reliably finds the start of the next one. Capping the
      // column and centering it means a big screen shows the same well-set page
      // with more paper around it, rather than the same page pulled out of shape.
      //
      // The cap sits above any phone in portrait, so on a phone this modifier
      // does nothing at all and the grid's numbers are untouched.
      Box(Modifier.widthIn(max = READABLE_WIDTH).fillMaxSize()) {
        when (val current = destination) {
            // Both buttons go the same way. The second one refuses nobody: it
            // is there so somebody who listens to something else is told what
            // this was built for and then waved straight through.
            Destination.Welcome -> WelcomeScreen(
                onAgree = { destination = Destination.WhereMusic },
                onCarryOn = { destination = Destination.WhereMusic },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )

            Destination.WhereMusic -> WhereMusicScreen(
                onJustListen = {
                    viewModel.chooseListeningOnly()
                    destination = Destination.Main(Tab.Surroundings)
                },
                onConnect = { destination = Destination.Connect },
                // Local-only still passes through the tone disclosure, because
                // the default applies to local files exactly as it does to
                // streamed ones.
                onChooseFolders = { folderPicker.launch(null) },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )

            Destination.ToneIntro -> ToneIntroScreen(
                voicing = playback.voicing,
                onPick = viewModel.player::setVoicing,
                onContinue = {
                    // The path is marked chosen here, at the end, rather than
                    // at the first tap. Onboarding abandoned halfway starts
                    // again instead of dropping somebody onto an empty shelf
                    // they never agreed to.
                    viewModel.finishOnboarding()
                    destination = Destination.Main()
                },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )

            Destination.Connect -> ConnectScreen(
                state = connect,
                onServerChange = viewModel::onServerChange,
                onUsernameChange = viewModel::onUsernameChange,
                onPasswordChange = viewModel::onPasswordChange,
                onConnect = { viewModel.connect { destination = Destination.ToneIntro } },
                onOpenBandcampSettings = { open(BANDCAMP_SETTINGS_URL) },
                onBack = { destination = Destination.WhereMusic },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )

            // **Swipe between the tabs.**
            //
            // The bar at the foot was the only way across, which on a tall phone
            // means reaching for the bottom of the screen to change what you are
            // looking at. The gesture sits on the whole tab body, and anything
            // with its own horizontal drag inside it, the mini player's scrub and
            // the bed's level line, consumes first and is untouched.
            // **Not on the Shelf.** There the swipe belongs to the upper
            // switcher, Albums through Lists, which is the row a person on
            // that screen is actually choosing between; ShelfScreen owns that
            // gesture itself. The tab bar remains one tap away.
            is Destination.Main -> Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .then(
                        if (current.tab == Tab.Shelf) Modifier
                        else Modifier.pointerInput(current.tab) {
                            var travelled = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { travelled = 0f },
                                onDragEnd = {
                                    val order = Tab.entries
                                    val at = order.indexOf(current.tab)
                                    if (travelled <= -TAB_SWIPE_PX && at < order.lastIndex) {
                                        destination = Destination.Main(order[at + 1])
                                    } else if (travelled >= TAB_SWIPE_PX && at > 0) {
                                        destination = Destination.Main(order[at - 1])
                                    }
                                },
                            ) { change, delta ->
                                change.consume()
                                travelled += delta
                            }
                        }
                    )
            ) {
                // The card floats over the content rather than sitting in the
                // flow beneath it, which is what its 88 percent tint is for:
                // whatever is behind ghosts through. So the content keeps its
                // full height and takes bottom padding equal to the card
                // instead, recomputed whenever the card appears, expands,
                // collapses or leaves.
                // The card's measured height, not an estimate of it. The
                // estimate is kept only as the first frame's value, before the
                // card has had a chance to report.
                val cardRoom = if (current.tab == Tab.Surroundings || !surroundingsCard.visible) {
                    0.dp
                } else {
                    measuredCardHeight ?: surroundingsCardHeight(surroundingsCard)
                }

                Box(Modifier.weight(1f)) {
                    // **Crossing tabs is a small page turn too.** The pane
                    // used to snap, which read as the screen being replaced;
                    // it now arrives from the side you travelled toward, 42px
                    // on the Settle curve with a short fade, and the old pane
                    // leaves fast. Same grammar as the player spread and the
                    // shelf views, so the whole app turns pages one way.
                    val tabReduced = MeedwellTheme.reducedMotion
                    AnimatedContent(
                        targetState = current.tab,
                        transitionSpec = {
                            if (tabReduced) {
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
                        label = "tab",
                    ) { tab ->
                    when (tab) {
                        Tab.Shelf -> ShelfScreen(
                            state = shelf.copy(
                                playerVisible = playback.hasQueue,
                                cardRoom = cardRoom,
                                daySpan = DaySpan(
                                    settingsState.dawnMinute,
                                    settingsState.duskMinute,
                                ),
                                lists = lists.lists,
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
                            onRefresh = viewModel::refresh,
                            onOpenList = { id ->
                                viewModel.openList(id)
                                destination = Destination.Playlist(id)
                            },
                            onNewList = { naming = Naming.NewList },
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
                            onOpenGroup = { destination = Destination.SoundGroup(it) },
                            onOpenStorage = { destination = Destination.SoundStorage },
                            onSearch = viewModel.surroundings::setSearch,
                            onOpenCredits = { destination = Destination.Credits },
                            onBack = { destination = Destination.Main() },
                        )
                        Tab.More -> MoreScreen(
                            state = moreState,
                            onOpen = { where ->
                                destination = when (where) {
                                    MoreDestination.Settings -> Destination.Settings
                                    MoreDestination.Privacy -> Destination.Privacy
                                    MoreDestination.WhatsAhead -> Destination.WhatsAhead
                                    MoreDestination.NotPlanned -> Destination.NotPlanned
                                    MoreDestination.About -> Destination.About
                                    MoreDestination.YourFiles -> Destination.YourFiles
                                    MoreDestination.Credits -> Destination.Credits
                                    MoreDestination.History -> Destination.History
                                    MoreDestination.Forgotten -> Destination.Forgotten
                                    MoreDestination.Loved -> Destination.Loved
                                    MoreDestination.Tone -> Destination.Tone.also { toolReturn = Destination.Main(Tab.More) }
                                    MoreDestination.SleepTimer -> Destination.SleepTimer.also { toolReturn = Destination.Main(Tab.More) }
                                }
                            },
                            onConnectBandcamp = { destination = Destination.Connect },
                        )
                    }
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
                            onPlayPause = {
                                if (surroundings.isPlaying) viewModel.surroundings.pause()
                                else surroundings.playingId?.let { viewModel.surroundings.play(it) }
                            },
                            onPick = viewModel.surroundings::play,
                            onStop = viewModel.surroundings::stop,
                            onOpenAll = { destination = Destination.Main(Tab.Surroundings) },
                            onHeightChanged = { measuredCardHeight = it + 8.dp },
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
                    onSeek = viewModel.player::seekTo,
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
                onEditDawn = { timePick = TimePick.Dawn },
                onEditDusk = { timePick = TimePick.Dusk },
                onSyncNow = viewModel::refresh,
                onToggleWifiOnly = viewModel::toggleWifiOnly,
                onToggleResumeQueue = viewModel::toggleResumeQueue,
                onEraseHistory = { pendingConfirm = PendingConfirm.EraseHistory },
                onDisconnect = { pendingConfirm = PendingConfirm.Disconnect },
                onOpenAppSettings = { openAppSettings(context) },
                onOpenLicenses = { destination = Destination.Credits },
                onSendFeedback = { sendFeedback(context, BuildConfig.VERSION_NAME) },
                onSupport = { open(SUPPORT_URL) },
                onBack = { destination = Destination.Main(Tab.More) },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )

            Destination.Privacy -> PrivacyScreen(
                onOpenSource = { open(SOURCE_URL) },
                onBack = { destination = Destination.Main(Tab.More) },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )

            Destination.SleepTimer -> SleepTimerScreen(
                secondsRemaining = playback.sleepSecondsRemaining,
                atEndOfPiece = playback.sleepAtEndOfPiece,
                currentPieceTitle = playback.title,
                secondsLeftInPiece = ((playback.durationMs - playback.positionMs) / 1000).coerceAtLeast(0),
                onSetMinutes = viewModel.player::setSleepTimer,
                onSetEndOfPiece = viewModel.player::setSleepAtEndOfPiece,
                // Back to wherever this was opened from: the player at night, or
                    // the More list by day. A hardcoded target stranded people in
                    // a menu with the player gone.
                    onBack = { destination = toolReturn },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )

            Destination.Tone -> ToneScreen(
                voicing = playback.voicing,
                available = playback.toneAvailable,
                onPick = viewModel.player::setVoicing,
                // Back to wherever this was opened from: the player at night, or
                    // the More list by day. A hardcoded target stranded people in
                    // a menu with the player gone.
                    onBack = { destination = toolReturn },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )

            is Destination.Playlist -> WithMiniPlayer(
                playback = playback,
                onPlayPause = viewModel.player::playPause,
                onOpen = { destination = Destination.NowPlaying },
                onSeek = viewModel.player::seekTo,
            ) {
                PlaylistScreen(
                    state = playlist,
                    onPlay = { index -> viewModel.playList(current.id, index) },
                    onShuffle = { viewModel.shuffleList(current.id) },
                    onMove = { from, to -> viewModel.moveInList(current.id, from, to) },
                    onRemove = { index -> viewModel.removeFromList(current.id, index) },
                    onRename = { naming = Naming.RenameList(current.id, playlist.name) },
                    onDelete = { pendingConfirm = PendingConfirm.DeleteList(current.id) },
                    onBack = {
                        viewModel.closeList()
                        destination = Destination.Main(Tab.Shelf)
                    },
                    modifier = Modifier.statusBarsPadding(),
                )
            }

            Destination.NotPlanned -> NotPlannedScreen(
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
                onOpenVideos = { open(VIDEOS_URL) },
                onOpenSite = { open(SITE_URL) },
                onFeedback = { sendFeedback(context, BuildConfig.VERSION_NAME) },
                onOpenLicenses = { destination = Destination.Credits },
                onSupport = { open(SUPPORT_URL) },
                onBack = { destination = Destination.Main(Tab.More) },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )

            Destination.History -> WithMiniPlayer(
                playback = playback,
                onPlayPause = viewModel.player::playPause,
                onOpen = { destination = Destination.NowPlaying },
                onSeek = viewModel.player::seekTo,
            ) {
                HistoryScreen(
                    days = history,
                    onTrackClick = { viewModel.playHistoryEntry(it.trackId, it.albumId) },
                    onBack = { destination = Destination.Main(Tab.More) },
                    modifier = Modifier.statusBarsPadding(),
                )
            }

            Destination.Forgotten -> WithMiniPlayer(
                playback = playback,
                onPlayPause = viewModel.player::playPause,
                onOpen = { destination = Destination.NowPlaying },
                onSeek = viewModel.player::seekTo,
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
                onSeek = viewModel.player::seekTo,
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
                onSeek = viewModel.player::seekTo,
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
                            onRefresh = viewModel::refresh,
                            onOpenList = { id ->
                                viewModel.openList(id)
                                destination = Destination.Playlist(id)
                            },
                            onNewList = { naming = Naming.NewList },
                        )
                        // The mini player was missing here, so starting
                        // something from a genre view left no way back to it
                        // short of leaving the view.
                        MiniPlayer(
                            state = playback,
                            onPlayPause = viewModel.player::playPause,
                            onOpen = { destination = Destination.NowPlaying },
                    onSeek = viewModel.player::seekTo,
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
                onSeek = viewModel.player::seekTo,
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
                onSeek = viewModel.player::seekTo,
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
                    onOpenGroup = { destination = Destination.SoundGroup(it) },
                    onOpenStorage = { destination = Destination.SoundStorage },
                    onSearch = viewModel.surroundings::setSearch,
                    onOpenCredits = { destination = Destination.Credits },
                    onBack = { destination = Destination.Main(Tab.More) },
                    modifier = Modifier.statusBarsPadding(),
                )
            }

            is Destination.SoundGroup -> WithMiniPlayer(
                playback = playback,
                onPlayPause = viewModel.player::playPause,
                onOpen = { destination = Destination.NowPlaying },
                onSeek = viewModel.player::seekTo,
            ) {
                SurroundingsGroupScreen(
                    group = surroundings.groups.firstOrNull { it.id == current.id },
                    playingId = surroundings.playingId,
                    isPlaying = surroundings.isPlaying,
                    onBack = { destination = Destination.Main(Tab.Surroundings) },
                    onPlay = viewModel.surroundings::play,
                    onPause = viewModel.surroundings::pause,
                    onOpenDetail = viewModel.surroundings::openDetail,
                    onDownloadAll = { viewModel.surroundings.downloadGroup(current.id) },
                    modifier = Modifier.statusBarsPadding(),
                )
            }

            Destination.SoundStorage -> WithMiniPlayer(
                playback = playback,
                onPlayPause = viewModel.player::playPause,
                onOpen = { destination = Destination.NowPlaying },
                onSeek = viewModel.player::seekTo,
            ) {
                SurroundingsStorageScreen(
                    state = surroundings,
                    onBack = { destination = Destination.Main(Tab.Surroundings) },
                    onRemove = viewModel.surroundings::remove,
                    onOpenDetail = viewModel.surroundings::openDetail,
                    modifier = Modifier.statusBarsPadding(),
                )
            }

            Destination.Credits -> WithMiniPlayer(
                playback = playback,
                onPlayPause = viewModel.player::playPause,
                onOpen = { destination = Destination.NowPlaying },
                onSeek = viewModel.player::seekTo,
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

            // **Entering the player is an arrival, not a cut.**
            //
            // Opening the player, the actual moment of entering something, had
            // no motion of any kind. It rises a twelfth of its height and fades
            // up while its own parts are still being set inside it, so the
            // room is being lit as you walk in. The exit stays instant, because
            // nobody watches an exit.
            Destination.NowPlaying -> {
                var entered by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { entered = true }
                val arrive by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (entered) 1f else 0f,
                    animationSpec = if (MeedwellTheme.reducedMotion) {
                        androidx.compose.animation.core.snap()
                    } else {
                        androidx.compose.animation.core.tween(300, easing = com.kamsiob.meedwell.ui.theme.Motion.Settle)
                    },
                    label = "player arrival",
                )
                Box(
                    Modifier.graphicsLayer {
                        alpha = (arrive * 2.2f).coerceAtMost(1f)
                        translationY = (1f - arrive) * size.height / 12f
                    }
                ) {
                PlayerSpread(
                page = playerPage,
                onPageChange = { playerPage = it },
                state = playback,
                surroundings = SurroundingsPlayingState(
                    soundId = surroundings.playingId.orEmpty(),
                    group = surroundings.playingGroup,
                    title = surroundings.playingTitle,
                    // Was hardcoded blank, so the grid's descriptive line under
                    // the title ("a suburban street, before dawn") could never
                    // render even though the manifest carries it.
                    description = surroundings.playingDescription,
                    credit = surroundings.playingCredit,
                    isPlaying = surroundings.isPlaying,
                    volume = surroundings.volume,
                    hasSound = surroundings.playingId != null,
                    hereByGroup = surroundingsSlices,
                    sleepLabel = playback.sleepLabel,
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
                onSleepTimer = { toolReturn = Destination.NowPlaying; destination = Destination.SleepTimer },
                onTone = { toolReturn = Destination.NowPlaying; destination = Destination.Tone },
                onSurroundingsPlayPause = {
                    if (surroundings.isPlaying) viewModel.surroundings.pause()
                    else surroundings.playingId?.let { viewModel.surroundings.play(it) }
                },
                onSurroundingsVolume = viewModel.surroundings::setVolume,
                onSurroundingsCredit = {
                    surroundings.playingId?.let { viewModel.surroundings.openDetail(it) }
                },
                onBrowseSurroundings = { destination = Destination.Main(Tab.Surroundings) },
                onPickSurroundings = { viewModel.surroundings.play(it) },
                onSurroundingsStop = { viewModel.surroundings.stop() },
                // A single tick when a mode changes: the tactile version of the
                // moss rule drawing under the mark.
                onShuffle = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.player.setShuffle(!playback.shuffle)
                },
                onRepeat = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.player.cycleRepeat()
                },
                // **Our own sheet, listing the system's real routes.**
                //
                // This first opened `android.settings.panel.action.MEDIA_OUTPUT`,
                // which is documented and does not resolve on every phone. On
                // the test device it resolved to nothing, so the control existed
                // and did nothing while Bluetooth was connected. A button that
                // lies is worse than no button.
                onOutput = { outputSheetOpen = true },
                modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
            )
                }
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
                    onSeek = viewModel.player::seekTo,
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
                onFreshCredentials = { open(BANDCAMP_SETTINGS_URL) },
                onDismiss = { viewModel.dismissSyncFailure() },
            )
        }

        if (outputSheetOpen) {
            OutputSheet(onDismiss = { outputSheetOpen = false })
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
                onMove = { from, to -> viewModel.player.moveQueueItem(from, to) },
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
                        SheetAction.AddToList -> addingToList = target
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
                canRemove = detail.isHere && !detail.isBundled,
                onRemove = { viewModel.surroundings.remove(detail.id) },
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
                is PendingConfirm.DeleteList -> ConfirmSheet(
                    title = "Delete this list?",
                    // Says what is lost and what is not, because the fear is
                    // that deleting a list deletes the music in it.
                    body = "The list goes. Every track in it stays exactly where it was, " +
                        "on your shelf and in your account.",
                    confirmLabel = "Delete",
                    onConfirm = {
                        viewModel.deleteList(pending.id)
                        viewModel.closeList()
                        destination = Destination.Main(Tab.Shelf)
                    },
                    onDismiss = { pendingConfirm = null },
                )

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

        if (askCellular) {
            CellularSheet(
                onAnswer = viewModel.surroundings::answerCellular,
                onDismiss = viewModel.surroundings::dismissCellularQuestion,
            )
        }

        naming?.let { what ->
            when (what) {
                Naming.NewList -> NameSheet(
                    title = "Name this list",
                    initial = "",
                    confirmLabel = "Make it",
                    onConfirm = { name ->
                        viewModel.createList(name) { id ->
                            pendingAdd?.let { waiting ->
                                if (waiting.kind == ActionTarget.Kind.Album) {
                                    viewModel.addAlbumToList(id, waiting.id)
                                } else {
                                    viewModel.addTrackToList(id, waiting.id)
                                }
                                pendingAdd = null
                            }
                            // Straight into the new list, because an empty list
                            // sitting in a pane is not the thing anybody wanted:
                            // they wanted somewhere to put music.
                            viewModel.openList(id)
                            destination = Destination.Playlist(id)
                        }
                    },
                    onDismiss = { naming = null },
                )

                is Naming.RenameList -> NameSheet(
                    title = "Rename this list",
                    initial = what.current,
                    confirmLabel = "Rename",
                    onConfirm = { viewModel.renameList(what.id, it) },
                    onDismiss = { naming = null },
                )
            }
        }

        addingToList?.let { target ->
            AddToListSheet(
                lists = lists.lists.filter { it.editable },
                onPick = { listId ->
                    if (target.kind == ActionTarget.Kind.Album) {
                        viewModel.addAlbumToList(listId, target.id)
                    } else {
                        viewModel.addTrackToList(listId, target.id)
                    }
                    addingToList = null
                },
                onNew = {
                    val pending = target
                    addingToList = null
                    naming = Naming.NewList
                    // Remembered so the new list is the one it lands in, rather
                    // than making somebody add the track a second time.
                    pendingAdd = pending
                },
                onDismiss = { addingToList = null },
            )
        }

        timePick?.let { which ->
            val dawn = which == TimePick.Dawn
            TimePickSheet(
                title = if (dawn) "Dawn" else "Dusk",
                note = if (dawn) {
                    "Where the day line starts. Meedwell uses your phone's clock and never your location."
                } else {
                    "Where the day line ends. Meedwell uses your phone's clock and never your location."
                },
                // Sensible bands rather than the whole twenty four hours: a dawn
                // at nine at night is not a setting anybody is reaching for, and
                // a shorter list is a faster answer.
                minutes = if (dawn) halfHoursBetween(3, 11) else halfHoursBetween(15, 23),
                selectedMinute = if (dawn) settingsState.dawnMinute else settingsState.duskMinute,
                onPick = { minute ->
                    if (dawn) viewModel.setDawn(minute) else viewModel.setDusk(minute)
                    timePick = null
                },
                onDismiss = { timePick = null },
            )
        }
      }
    }
}

/** Which hour a picker is open for. */
private enum class TimePick { Dawn, Dusk }

/** What a naming sheet is naming. */
private sealed interface Naming {
    data object NewList : Naming
    data class RenameList(val id: String, val current: String) : Naming
}

/**
 * The widest the page is ever allowed to get.
 *
 * Chosen to sit above any phone in portrait, so on a phone the cap never binds
 * and every measurement stays exactly as the grid drew it. It only takes effect
 * on a tablet or an unfolded foldable, where the alternative is a line of body
 * copy long enough to lose your place in.
 */
private val READABLE_WIDTH = 600.dp

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
    onSeek: (Float) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { content() }
        if (playback.hasQueue) {
            MiniPlayer(
                state = playback,
                onPlayPause = onPlayPause,
                onOpen = onOpen,
                onSeek = onSeek,
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
/**
 * Opens this app's own page in the system settings.
 *
 * Permissions are granted and revoked there, not here. Meedwell cannot re-ask
 * for one it has already been refused twice, so the honest move is to take
 * somebody to the only place the answer can actually be changed.
 */
private fun openAppSettings(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.fromParts("package", context.packageName, null))
        )
    }
}

/**
 * Opens a mail draft to Kamsiob, carrying the version and nothing else.
 *
 * `ACTION_SENDTO` with a `mailto:` URI rather than `ACTION_SEND`, so only mail
 * apps can answer it. `ACTION_SEND` would offer the whole share sheet, and a
 * bug report is not something to hand to the first messaging app on the list.
 *
 * The body is left empty on purpose. Nothing is gathered, nothing is attached,
 * and the draft is sitting in a mail app where it can be read before it goes,
 * which is what the row underneath the title promises.
 */
private fun sendFeedback(context: android.content.Context, versionName: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_SENDTO)
                .setData("mailto:$FEEDBACK_EMAIL".toUri())
                .putExtra(Intent.EXTRA_SUBJECT, "Meedwell $versionName")
        )
    }
}

private const val FEEDBACK_EMAIL = "hello@kamsiob.com"
private const val VIDEOS_URL = "https://youtube.com/@kamsiob"
private const val SITE_URL = "https://kamsiob.com"
/**
 * Bandcamp's fan settings, where the Subsonic credentials live.
 *
 * The obvious looking `/settings/subsonic` is not a real page and resolved to
 * nothing useful. This is the address Bandcamp's own account menu links to, and
 * it **only works for a signed in browser**: signed out it lands somewhere that
 * looks nothing like the instructions beside the button. The Connect screen says
 * so before offering to open it.
 */
private const val BANDCAMP_SETTINGS_URL =
    "https://bandcamp.com/settings?ui_context=usernav&pane=fan"

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