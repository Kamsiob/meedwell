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
import com.kamsiob.meedwell.ui.components.ActionTarget
import com.kamsiob.meedwell.ui.screens.CoverUrls
import com.kamsiob.meedwell.ui.screens.ShelfState
import com.kamsiob.meedwell.core.surroundings.LicenseGroup
import com.kamsiob.meedwell.ui.screens.ArtistState
import com.kamsiob.meedwell.ui.screens.ForgottenAlbum
import com.kamsiob.meedwell.ui.screens.HistoryDay
import com.kamsiob.meedwell.ui.screens.HistoryEntry
import com.kamsiob.meedwell.ui.screens.ListsState
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
    val sort: StateFlow<ShelfSort> = _sort.asStateFlow()

    private val _scope = MutableStateFlow(ShelfScope.Everything)
    val scope: StateFlow<ShelfScope> = _scope.asStateFlow()

    /**
     * The genre currently narrowing the shelf, or null for the whole shelf.
     *
     * This is a flow rather than a plain field on purpose. It used to be a
     * field, with `filterByGenre` launching its own collector, and nothing
     * cancelled that collector on the way out: leaving a genre view left the
     * old collector alive, still writing its filtered list into the shelf, so
     * the shelf stayed narrowed with no label saying why. Folding the filter
     * into the same `flatMapLatest` below means switching it off cancels the
     * previous query by construction rather than by remembering to.
     */
    private val _genreFilter = MutableStateFlow<String?>(null)

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

    private val _history = MutableStateFlow<List<HistoryDay>>(emptyList())
    val history: StateFlow<List<HistoryDay>> = _history.asStateFlow()

    private val _forgotten = MutableStateFlow<List<ForgottenAlbum>>(emptyList())
    val forgotten: StateFlow<List<ForgottenAlbum>> = _forgotten.asStateFlow()

    private val _lists = MutableStateFlow(ListsState())
    val lists: StateFlow<ListsState> = _lists.asStateFlow()

    private val _loved = MutableStateFlow<List<Track>>(emptyList())
    val loved: StateFlow<List<Track>> = _loved.asStateFlow()

    private val _artist = MutableStateFlow(ArtistState())
    val artist: StateFlow<ArtistState> = _artist.asStateFlow()

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
        combine(_sort, _scope, _genreFilter) { sort, scope, genre -> Triple(sort, scope, genre) }
            .flatMapLatest { (sort, scope, genre) ->
                if (genre != null) container.library.observeAlbumsByGenre(genre)
                else container.library.observeAlbums(sort, scope)
            }
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

        container.library.observeHistory()
            .onEach { rows -> _history.value = groupByDay(rows) }
            .launchIn(viewModelScope)

        container.library.observeForgotten()
            .onEach { albums -> _forgotten.value = albums.map { ForgottenAlbum(it, forgottenReason(it)) } }
            .launchIn(viewModelScope)

        container.library.observeStarredTracks()
            .onEach { tracks ->
                _loved.value = tracks
                _lists.value = _lists.value.copy(lovedCount = tracks.size)
            }
            .launchIn(viewModelScope)

        refreshSettings()
        loadCredits()
    }

    /**
     * Groups the play log by day, with the two nearest days named rather than
     * dated. "Today" and "Yesterday" are what a person actually calls them.
     */
    private fun groupByDay(rows: List<com.kamsiob.meedwell.data.db.HistoryRow>): List<HistoryDay> {
        if (rows.isEmpty()) return emptyList()
        val client = container.client()
        val nowDay = (System.currentTimeMillis() / 1000) / 86_400
        return rows
            .groupBy { it.playedAt / 86_400 }
            .toSortedMap(compareByDescending { it })
            .map { (day, entries) ->
                HistoryDay(
                    label = when (nowDay - day) {
                        0L -> "Today"
                        1L -> "Yesterday"
                        else -> dayLabel(day)
                    },
                    entries = entries.map { row ->
                        HistoryEntry(
                            key = row.eventId.toString(),
                            trackId = row.trackId,
                            albumId = row.albumId,
                            title = row.title,
                            subtitle = row.artist,
                            time = clockLabel(row.playedAt),
                            coverUrl = row.coverArtId.takeIf { it.isNotBlank() }
                                ?.let { id -> client?.coverArtUrl(id) },
                        )
                    },
                )
            }
    }

    /**
     * Why a record is on the Forgotten Shelf.
     *
     * The reason is the whole point of the screen. "Here is a record" is a
     * shelf; "here is why you might have forgotten it" is the feature.
     */
    private fun forgottenReason(album: com.kamsiob.meedwell.core.model.Album): String = "quietly waiting"

    /**
     * Narrows the whole shelf to one tag.
     *
     * Genres were a first-class view whose every row did nothing, which made
     * one of the three sibling views decorative. Tapping one now filters the
     * albums list, which is what "one tap narrows the whole shelf to a tag" in
     * the reference means.
     */
    fun filterByGenre(genre: String) {
        if (_genreFilter.value == genre) return
        _genreFilter.value = genre
        _shelf.value = _shelf.value.copy(view = ShelfView.Albums, sortLabel = genre, filtering = true)
    }

    /** Drops the genre filter and puts the whole shelf back. */
    fun clearGenreFilter() {
        if (_genreFilter.value == null) return
        _genreFilter.value = null
        _shelf.value = _shelf.value.copy(sortLabel = sortLabel(_sort.value, _scope.value), filtering = false)
    }

    fun openArtist(artistId: String) {
        viewModelScope.launch {
            combine(
                container.library.observeArtist(artistId),
                container.library.observeAlbumsByArtist(artistId),
            ) { artist, albums ->
                ArtistState(
                    id = artistId,
                    name = artist?.name.orEmpty(),
                    albums = albums,
                    ownedCount = albums.count { it.isFullyPresent },
                )
            }.collect { _artist.value = it }
        }
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
                lastSyncAt = settings.lastSyncAt,
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
        // Settings shows the same value. Without this the row and the shelf
        // disagree until something else happens to refresh it.
        _settings.value = _settings.value.copy(shelfGrid = grid)
    }

    fun setSort(sort: ShelfSort, scope: ShelfScope) {
        _sort.value = sort
        _scope.value = scope
        // Inside a genre view the label belongs to the tag, and saying
        // "Artist A to Z" there would lose the only thing on screen that
        // explains why most of the shelf is missing.
        if (_genreFilter.value == null) {
            _shelf.value = _shelf.value.copy(sortLabel = sortLabel(sort, scope))
        }
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

    fun syncNow(manual: Boolean = false) {
        val client = container.client() ?: return
        if (_shelf.value.syncing) return
        _shelf.value = _shelf.value.copy(syncing = true)
        _settings.value = _settings.value.copy(syncing = true)
        viewModelScope.launch {
            var found = 0
            when (val result = container.library.sync(client)) {
                is SyncResult.Completed -> {
                    // How many arrived, so that a manual check can answer the
                    // question that prompted it rather than just stopping.
                    found = (result.albumCount - _shelf.value.albumCount).coerceAtLeast(0)
                    settings.lastSyncAt = result.at
                    _syncFailure.value = null
                    container.library.refreshLocalCounts()
                }
                is SyncResult.Failed -> _syncFailure.value = result.reason
            }
            _shelf.value = _shelf.value.copy(syncing = false)
            _settings.value = _settings.value.copy(syncing = false)
            refreshSettings()
            if (_syncFailure.value == null && manual) {
                _notice.value = when (found) {
                    0 -> "Nothing new. Your shelf is up to date."
                    1 -> "One new record on your shelf."
                    else -> "$found new records on your shelf."
                }
            }
        }
    }

    /** Dismissing the trouble sheet clears the failure but changes nothing else. */
    fun dismissSyncFailure() {
        _syncFailure.value = null
    }

    // ---------- The action sheet ----------

    /**
     * A short line confirming something happened, shown once and then gone.
     *
     * Every verb in the action sheet acts on something off screen: a queue you
     * are not looking at, an account somewhere else. Without a word back, the
     * sheet closing is the only feedback, which is indistinguishable from the
     * sheet closing because nothing worked.
     */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    fun dismissNotice() {
        _notice.value = null
    }

    /**
     * The tracks a sheet target stands for: one for a track, the whole record
     * in running order for an album.
     */
    private suspend fun tracksFor(target: ActionTarget): List<Track> =
        if (target.kind == ActionTarget.Kind.Album) {
            container.library.tracksForAlbum(target.id)
        } else {
            container.library.tracks(listOf(target.id))
        }

    /**
     * Repeats the one-way limit on hearts at the moment somebody tries it.
     *
     * `unstar` errors whatever is sent, so this is the truthful answer rather
     * than a control that fails silently.
     */
    fun showLoveLimit() {
        _notice.value = "Bandcamp cannot take a heart off yet. Their website can."
    }

    fun playNext(target: ActionTarget) {
        viewModelScope.launch {
            val tracks = tracksFor(target)
            if (tracks.isEmpty()) return@launch
            player.playNext(tracks)
            _notice.value = "${target.title} plays next"
        }
    }

    fun addToQueue(target: ActionTarget) {
        viewModelScope.launch {
            val tracks = tracksFor(target)
            if (tracks.isEmpty()) return@launch
            player.addToQueue(tracks)
            _notice.value = if (tracks.size > 1) {
                "${tracks.size} tracks added to the queue"
            } else {
                "${target.title} added to the queue"
            }
        }
    }

    /**
     * Sends a heart to the Bandcamp account, and says plainly if it did not go.
     *
     * The failure line names the account rather than blaming the user, because
     * the usual cause is a regenerated password on Bandcamp's side.
     */
    fun love(target: ActionTarget) {
        viewModelScope.launch {
            val landed = container.library.love(
                client = container.client(),
                id = target.id,
                isAlbum = target.kind == ActionTarget.Kind.Album,
            )
            _notice.value = if (landed) {
                "Loved. It is on your Bandcamp account too."
            } else {
                "Bandcamp did not take the heart. Nothing changed."
            }
        }
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
    fun refresh() = syncNow(manual = true)

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

/** "14 March", for history days older than yesterday. */
private fun dayLabel(daysSinceEpoch: Long): String {
    var year = 1970
    var remaining = daysSinceEpoch
    while (true) {
        val length = if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 366 else 365
        if (remaining < length) break
        remaining -= length
        year++
    }
    val leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    val lengths = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val names = listOf("January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December")
    var month = 0
    while (month < 12 && remaining >= lengths[month]) {
        remaining -= lengths[month]
        month++
    }
    return "${remaining + 1} ${names[month.coerceIn(0, 11)]}"
}

/** "22:14", tabular so a column of them does not jitter. */
private fun clockLabel(epochSeconds: Long): String {
    val secondsIntoDay = ((epochSeconds % 86_400) + 86_400) % 86_400
    return "%02d:%02d".format(secondsIntoDay / 3600, (secondsIntoDay % 3600) / 60)
}
