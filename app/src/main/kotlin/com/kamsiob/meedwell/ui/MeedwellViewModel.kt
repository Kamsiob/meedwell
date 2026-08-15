package com.kamsiob.meedwell.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kamsiob.meedwell.AppContainer
import com.kamsiob.meedwell.core.subsonic.SubsonicCredentials
import com.kamsiob.meedwell.core.subsonic.SubsonicOutcome
import com.kamsiob.meedwell.data.CredentialStore
import com.kamsiob.meedwell.data.ShelfScope
import com.kamsiob.meedwell.data.ShelfSort
import com.kamsiob.meedwell.data.SyncFailure
import com.kamsiob.meedwell.data.SyncResult
import com.kamsiob.meedwell.core.model.Album
import com.kamsiob.meedwell.core.model.Track
import com.kamsiob.meedwell.playback.PlaybackState
import com.kamsiob.meedwell.playback.PlayerController
import com.kamsiob.meedwell.data.LocalScanner
import com.kamsiob.meedwell.data.db.WatchedFolderEntity
import com.kamsiob.meedwell.ui.screens.SettingsState
import com.kamsiob.meedwell.ui.screens.YourFilesState
import com.kamsiob.meedwell.ui.theme.ThemeChoice
import android.net.Uri
import com.kamsiob.meedwell.ui.screens.ConnectError
import com.kamsiob.meedwell.ui.screens.ConnectState
import com.kamsiob.meedwell.ui.screens.CoverUrls
import com.kamsiob.meedwell.ui.screens.ShelfState
import com.kamsiob.meedwell.core.surroundings.LicenseGroup
import com.kamsiob.meedwell.ui.screens.SearchState
import com.kamsiob.meedwell.ui.screens.ShelfView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * The one view model for the shelf side of the app.
 *
 * Screens observe the database and never wait on the network, so this holds the
 * interface's own state, meaning which view is showing and how it is sorted,
 * and lets the data flow through from the repository.
 */
class MeedwellViewModel(private val container: AppContainer) : ViewModel() {

    private val settings = container.settings

    private val _shelf = MutableStateFlow(
        ShelfState(
            grid = settings.shelfGrid,
            connected = container.credentials.isConnected,
        )
    )
    val shelf: StateFlow<ShelfState> = _shelf.asStateFlow()

    private val _connect = MutableStateFlow(ConnectState(server = CredentialStore.DEFAULT_SERVER))
    val connect: StateFlow<ConnectState> = _connect.asStateFlow()

    private val _sort = MutableStateFlow(ShelfSort.Artist)
    private val _scope = MutableStateFlow(ShelfScope.Everything)

    /**
     * Playback, bound to the service so that the app, the notification and the
     * lock screen are all driving one player rather than two that can disagree.
     */
    val player = PlayerController(container.appContext, container)

    val playback: StateFlow<PlaybackState> get() = player.state

    private val _albumDetail = MutableStateFlow<AlbumDetail?>(null)
    val albumDetail: StateFlow<AlbumDetail?> = _albumDetail.asStateFlow()

    private val scanner = LocalScanner(container.appContext, container.database)

    private val _search = MutableStateFlow(SearchState())
    val search: StateFlow<SearchState> = _search.asStateFlow()

    private val _settings = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settings.asStateFlow()

    private val _credits = MutableStateFlow(CreditsState())
    val credits: StateFlow<CreditsState> = _credits.asStateFlow()

    private val _yourFiles = MutableStateFlow(YourFilesState())
    val yourFiles: StateFlow<YourFilesState> = _yourFiles.asStateFlow()

    init {
        installCoverResolver()
        observeLibrary()
        player.connect()
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

    /** Loads one album and its tracks for the album screen. */
    fun openAlbum(albumId: String) {
        _albumDetail.value = null
        viewModelScope.launch {
            combine(
                container.library.observeAlbum(albumId),
                container.library.observeTracks(albumId),
            ) { album, tracks -> album?.let { AlbumDetail(it, tracks) } }
                .collect { _albumDetail.value = it }
        }
    }

    fun playAlbum(albumId: String, startIndex: Int = 0) = player.playAlbum(albumId, startIndex)

    /**
     * Plays one specific track, from within its own album.
     *
     * Search used to call `playAlbum(track.albumId)`, which started at index
     * zero: you searched for a song, tapped it, and a different song played.
     * Music did start, so nothing looked broken, which is worse than a crash.
     */
    fun playTrack(track: Track) {
        viewModelScope.launch {
            val tracks = container.library.tracksForAlbum(track.albumId)
            val index = tracks.indexOfFirst { it.id == track.id }
            if (index >= 0) {
                player.playTracks(tracks, index)
            } else {
                // A track with no album context, such as a local loose file.
                player.playTracks(listOf(track))
            }
        }
    }

    fun shuffleAlbum(albumId: String) {
        viewModelScope.launch {
            // Honestly random, which is what the queue sheet promises. No
            // weighting, no recency avoidance, no cleverness.
            val tracks = container.library.tracksForAlbum(albumId).shuffled()
            player.playTracks(tracks)
        }
    }

    /**
     * Cover URLs need a client, and rows should not each carry one. Installed
     * once here and cleared on disconnect, so a local-files-only session never
     * builds a URL pointing at a server it is not talking to.
     */
    private fun installCoverResolver() {
        val client = container.client()
        if (client != null) CoverUrls.install { id -> client.coverArtUrl(id) } else CoverUrls.clear()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeLibrary() {
        combine(_sort, _scope) { sort, scope -> sort to scope }
            .flatMapLatest { (sort, scope) -> container.library.observeAlbums(sort, scope) }
            .onEach { albums -> _shelf.value = _shelf.value.copy(albums = albums) }
            .launchIn(viewModelScope)

        container.library.observeArtists()
            .onEach { artists -> _shelf.value = _shelf.value.copy(artists = artists) }
            .launchIn(viewModelScope)

        container.library.observeGenres()
            .onEach { genres -> _shelf.value = _shelf.value.copy(genres = genres) }
            .launchIn(viewModelScope)

        container.library.observeNewestArrival()
            .onEach { newest -> _shelf.value = _shelf.value.copy(newestArrival = newest) }
            .launchIn(viewModelScope)

        container.library.observeAlbumCount()
            .onEach { count -> _shelf.value = _shelf.value.copy(albumCount = count) }
            .launchIn(viewModelScope)

        container.library.observePresentCount()
            .onEach { count -> _shelf.value = _shelf.value.copy(presentCount = count) }
            .launchIn(viewModelScope)

        container.database.watchedFolders().observeAll()
            .onEach { folders ->
                _yourFiles.value = _yourFiles.value.copy(
                    folders = folders,
                    connected = container.credentials.isConnected,
                )
                _settings.value = _settings.value.copy(watchedFolderCount = folders.size)
            }
            .launchIn(viewModelScope)

        // Records that actually have files here, which is what "Your files"
        // exists to show.
        container.library.observeAlbums(ShelfSort.Artist, ShelfScope.OnThisPhone)
            .onEach { albums -> _yourFiles.value = _yourFiles.value.copy(matched = albums) }
            .launchIn(viewModelScope)

        refreshSettings()
        loadCredits()
    }

    // ---------- Credits ----------

    /**
     * Loads the Surroundings credits from the bundled manifest.
     *
     * Not lazy and not gated on anybody opening an ambience player: 21 of the
     * recordings are CC BY, where the credit is a condition of use, so the
     * credits screen has to render whether or not the feature has been touched.
     */
    private fun loadCredits() {
        viewModelScope.launch {
            val rejected = container.surroundings.rejected()
            _credits.value = CreditsState(
                summary = container.surroundings.creditsSummary(),
                groups = container.surroundings.creditsByLicense(),
                // Anything with incomplete attribution is never offered, and is
                // named here rather than silently dropped.
                withheld = rejected.map { it.filename },
                loadError = container.surroundings.loadError,
            )
        }
    }

    // ---------- Search ----------

    /**
     * Searches the local database rather than the API.
     *
     * `search3` exists and works, but it searches the same collection the
     * database already holds, so calling it would mean a round trip and a
     * spinner to learn something already known. Searching locally is instant,
     * works offline, and is literally true when the screen says nothing about
     * the search leaves the phone.
     */
    fun onSearchQueryChange(query: String) {
        _search.value = _search.value.copy(query = query)
        if (query.isBlank()) {
            _search.value = SearchState(query = query)
            return
        }
        viewModelScope.launch {
            val results = container.library.search(query)
            // Guard against a slow query landing after the user has typed on.
            if (_search.value.query != query) return@launch
            _search.value = SearchState(
                query = query,
                albums = results.albums,
                tracks = results.tracks,
                artists = results.artists,
            )
        }
    }

    // ---------- Settings ----------

    fun refreshSettings() {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(
                theme = settings.theme,
                shelfGrid = settings.shelfGrid,
                gapless = settings.gapless,
                rememberLongTrackPosition = settings.rememberLongTrackPosition,
                connected = container.credentials.isConnected,
                historyEventCount = container.database.playEvents().count(),
            )
        }
    }

    fun setTheme(choice: ThemeChoice) {
        settings.theme = choice
        refreshSettings()
    }

    fun toggleGapless() {
        settings.gapless = !settings.gapless
        refreshSettings()
    }

    fun toggleLongResume() {
        settings.rememberLongTrackPosition = !settings.rememberLongTrackPosition
        refreshSettings()
    }

    fun eraseHistory() {
        viewModelScope.launch {
            container.database.playEvents().eraseAll()
            refreshSettings()
        }
    }

    /**
     * Disconnecting removes the credentials and nothing else.
     *
     * The shelf, the history and the lists stay, because losing them is not
     * what "disconnect" means to anybody. Deleting one's own data is a
     * separate, explicit act.
     */
    fun disconnect() {
        container.credentials.clear()
        CoverUrls.clear()
        _shelf.value = _shelf.value.copy(connected = false)
        refreshSettings()
    }

    // ---------- Your files ----------

    fun addWatchedFolder(uri: Uri) {
        viewModelScope.launch {
            if (scanner.addFolder(uri)) rescanFolders()
        }
    }

    fun removeWatchedFolder(folder: WatchedFolderEntity) {
        viewModelScope.launch {
            scanner.removeFolder(folder.uri)
            container.library.refreshLocalCounts()
        }
    }

    fun rescanFolders() {
        if (_yourFiles.value.scanning) return
        _yourFiles.value = _yourFiles.value.copy(scanning = true, lastResult = null)
        viewModelScope.launch {
            val result = scanner.scan()
            _yourFiles.value = _yourFiles.value.copy(
                scanning = false,
                // Says exactly what happened, including the part nobody likes.
                lastResult = buildString {
                    append("Looked at ${result.filesFound} ${if (result.filesFound == 1) "file" else "files"}. ")
                    append("${result.matched} matched your collection")
                    if (result.localOnly > 0) append(", ${result.localOnly} became local albums")
                    if (result.wentMissing > 0) append(", ${result.wentMissing} went missing since last time")
                    append(".")
                },
            )
        }
    }

    // ---------- Shelf ----------

    fun setView(view: ShelfView) {
        _shelf.value = _shelf.value.copy(view = view)
    }

    fun toggleLayout() {
        val grid = !_shelf.value.grid
        settings.shelfGrid = grid
        _shelf.value = _shelf.value.copy(grid = grid)
    }

    fun setSort(sort: ShelfSort, scope: ShelfScope) {
        _sort.value = sort
        _scope.value = scope
        _shelf.value = _shelf.value.copy(sortLabel = sortLabel(sort, scope))
    }

    private fun sortLabel(sort: ShelfSort, scope: ShelfScope): String {
        val sortText = when (sort) {
            ShelfSort.Artist -> "Artist A to Z"
            ShelfSort.Recent -> "Recently added"
            ShelfSort.Title -> "Title A to Z"
            ShelfSort.MostPlayed -> "Most played"
        }
        val scopeText = when (scope) {
            ShelfScope.Everything -> null
            ShelfScope.OnThisPhone -> "here as files"
            ShelfScope.LocalOnly -> "local only"
        }
        return listOfNotNull(sortText, scopeText).joinToString(" · ")
    }

    // ---------- Connecting ----------

    fun onServerChange(value: String) {
        _connect.value = _connect.value.copy(server = value, error = null)
    }

    fun onUsernameChange(value: String) {
        _connect.value = _connect.value.copy(username = value.trim(), error = null)
    }

    fun onPasswordChange(value: String) {
        _connect.value = _connect.value.copy(password = value.trim(), error = null)
    }

    /**
     * Checks the credentials, then saves them.
     *
     * **Validated with `getArtists`, never `ping`.** `ping` returns ok for a
     * wrong password, verified 15 August 2026, so validating with it would
     * accept anything typed and fail later during sync, which is exactly the
     * kind of dishonest interface this app exists to avoid.
     */
    fun connect(onConnected: () -> Unit) {
        val state = _connect.value
        if (!state.canSubmit || state.checking) return

        _connect.value = state.copy(checking = true, error = null)
        viewModelScope.launch {
            val credentials = SubsonicCredentials(
                serverUrl = state.server.trim(),
                username = state.username.trim(),
                password = state.password.trim(),
            )
            when (val outcome = container.clientFor(credentials).validateCredentials()) {
                is SubsonicOutcome.Success -> {
                    container.credentials.save(credentials)
                    settings.hasChosenPath = true
                    installCoverResolver()
                    _connect.value = ConnectState(server = CredentialStore.DEFAULT_SERVER)
                    _shelf.value = _shelf.value.copy(connected = true)
                    onConnected()
                    syncNow()
                }
                // HTTP 500 with an empty body. This is Bandcamp's way of
                // rejecting a login; there is no error code to show.
                is SubsonicOutcome.AuthRejected ->
                    _connect.value = state.copy(checking = false, error = ConnectError.Rejected)
                is SubsonicOutcome.Unreachable ->
                    _connect.value = state.copy(checking = false, error = ConnectError.Unreachable)
                // A server that answers but does not implement getArtists is
                // not a Bandcamp Subsonic endpoint, whatever it is.
                is SubsonicOutcome.EndpointAbsent ->
                    _connect.value = state.copy(checking = false, error = ConnectError.NotSubsonic)
                is SubsonicOutcome.Unreadable ->
                    _connect.value = state.copy(checking = false, error = ConnectError.NotSubsonic)
                is SubsonicOutcome.ServerError ->
                    _connect.value = state.copy(checking = false, error = ConnectError.ServerSaid(outcome.message))
                is SubsonicOutcome.XmlFailure ->
                    _connect.value = state.copy(checking = false, error = ConnectError.ServerSaid(outcome.message))
            }
        }
    }

    /** Chosen "just play my local files". No account, and no sync language anywhere after this. */
    fun continueLocalOnly() {
        settings.hasChosenPath = true
        _shelf.value = _shelf.value.copy(connected = false)
    }

    // ---------- Syncing ----------

    private val _syncFailure = MutableStateFlow<SyncFailure?>(null)
    val syncFailure: StateFlow<SyncFailure?> = _syncFailure.asStateFlow()

    fun syncNow() {
        val client = container.client() ?: return
        if (_shelf.value.syncing) return
        _shelf.value = _shelf.value.copy(syncing = true)
        viewModelScope.launch {
            when (val result = container.library.sync(client)) {
                is SyncResult.Completed -> {
                    settings.lastSyncAt = result.at
                    _syncFailure.value = null
                    container.library.refreshLocalCounts()
                }
                is SyncResult.Failed -> _syncFailure.value = result.reason
            }
            _shelf.value = _shelf.value.copy(syncing = false)
        }
    }

    /** Dismissing the trouble sheet clears the failure but changes nothing else. */
    fun dismissSyncFailure() {
        _syncFailure.value = null
    }

    val lastSyncAt: Long get() = settings.lastSyncAt

    /**
     * Syncs when the app comes back and the last one is old enough.
     *
     * Sync previously ran once, inside `connect()`, and never again. The only
     * way to see a record bought that morning was to disconnect and re-paste
     * both credentials. For an app built around Bandcamp Friday that is the
     * product not working.
     *
     * Still no background worker and still nothing on a timer: this runs when
     * somebody opens the app, which is the only moment the answer matters.
     */
    fun syncIfStale(now: Long = System.currentTimeMillis() / 1000) {
        if (!isConnected) return
        if (now - settings.lastSyncAt < STALE_AFTER_SECONDS) return
        syncNow()
    }

    /** Pull to refresh, and the manual override for "check now". */
    fun refresh() = syncNow()

    val isConnected: Boolean get() = container.credentials.isConnected

    val hasChosenPath: Boolean get() = settings.hasChosenPath

    private companion object {
        /**
         * Half an hour. Long enough that reopening the app repeatedly does not
         * hammer a service in open beta, short enough that a record bought this
         * morning is there by lunchtime.
         */
        const val STALE_AFTER_SECONDS = 30 * 60L
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MeedwellViewModel(container) as T
    }
}

/** An album together with its tracks, which is what the album screen needs. */
data class AlbumDetail(val album: Album, val tracks: List<Track>)

/** The Surroundings credits, generated from the manifest rather than typed. */
data class CreditsState(
    val summary: String = "",
    val groups: List<LicenseGroup> = emptyList(),
    val withheld: List<String> = emptyList(),
    /** Set when the credits could not be read at all. Shown, never swallowed. */
    val loadError: String? = null,
)
