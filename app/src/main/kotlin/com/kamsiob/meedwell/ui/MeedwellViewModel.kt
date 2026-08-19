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
import com.kamsiob.meedwell.ui.screens.ExportState
import com.kamsiob.meedwell.ui.screens.MoreState
import com.kamsiob.meedwell.ui.screens.ForgottenAlbum
import com.kamsiob.meedwell.ui.screens.HistoryDay
import com.kamsiob.meedwell.ui.screens.HistoryEntry
import com.kamsiob.meedwell.ui.screens.ListSummary
import kotlinx.coroutines.flow.map
import com.kamsiob.meedwell.ui.screens.PlaylistState
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    /**
     * A short line confirming something happened, shown once and then gone.
     *
     * Every verb in the action sheet acts on something off screen: a queue you
     * are not looking at, an account somewhere else. Without a word back, the
     * sheet closing is the only feedback, which is indistinguishable from the
     * sheet closing because nothing worked.
     *
     * **Declared here, above `init`, on purpose.** Kotlin initializes
     * properties in declaration order, and `init` announces a database that had
     * to be set aside. With this further down the class it was still null at
     * that moment, so the one path that exists to rescue somebody from an
     * unopenable database crashed instead. That path is by definition the one
     * nobody exercises, which is exactly why it has to be right.
     */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    /**
     * Say something once, in the app's own voice.
     *
     * The interface had no way to raise a notice of its own: every message came
     * from inside the view model, so a screen that learned something the model
     * could not know, such as a system panel refusing to open, had to swallow it.
     */
    fun say(message: String) {
        _notice.value = message
    }

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

    private val _playlist = MutableStateFlow(PlaylistState())
    val playlist: StateFlow<PlaylistState> = _playlist.asStateFlow()
    private var playlistJob: kotlinx.coroutines.Job? = null

    private val _loved = MutableStateFlow<List<Track>>(emptyList())
    val loved: StateFlow<List<Track>> = _loved.asStateFlow()

    private val _artist = MutableStateFlow(ArtistState())
    val artist: StateFlow<ArtistState> = _artist.asStateFlow()

    private val _credits = MutableStateFlow(CreditsState())
    val credits: StateFlow<CreditsState> = _credits.asStateFlow()

    private val _more = MutableStateFlow(MoreState())
    val more: StateFlow<MoreState> = _more.asStateFlow()

    private val _export = MutableStateFlow(ExportState())
    val export: StateFlow<ExportState> = _export.asStateFlow()

    private val _yourFiles = MutableStateFlow(YourFilesState())
    val yourFiles: StateFlow<YourFilesState> = _yourFiles.asStateFlow()

    /**
     * Surroundings: the ambience library, its downloads and its own player.
     *
     * Its own object rather than more methods here, because it shares nothing
     * with the shelf. A different library, a different player, a different idea
     * of what a file is.
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    val surroundings = SurroundingsCoordinator(container, viewModelScope) { message ->
        _notice.value = message
    }

    init {
        installCoverResolver()
        observeLibrary()
        player.connect()
        surroundings.load()
        announceSetAsideDatabase()
        // The timer stops both, or the one that keeps going wakes somebody up.
        player.onSleep = { surroundings.stop() }
        observePlaybackForMore()
        observeLists()
    }

    /**
     * Keeps More's Tone and sleep timer values honest.
     *
     * Both live on the playback state and change while nothing else on More
     * does, so they cannot ride along with `refreshSettings` the way the rest of
     * the right-hand column does. A sleep timer counting down behind a row that
     * still says "Off" is exactly the sort of stale value that makes the whole
     * column untrustworthy.
     */
    private fun observePlaybackForMore() {
        viewModelScope.launch {
            player.state.collect { playback ->
                val tone = playback.voicingName
                val sleep = playback.sleepLabel
                val current = _more.value
                if (current.toneName != tone || current.sleepTimer != sleep) {
                    _more.value = current.copy(toneName = tone, sleepTimer = sleep)
                }
            }
        }
    }

    /**
     * Says so, once, when the previous database could not be read.
     *
     * Without this the app opens onto an empty shelf and the user is left to
     * conclude that it lost everything. It did not: the old file was renamed
     * rather than deleted, and this says where it is.
     */
    private fun announceSetAsideDatabase() {
        viewModelScope.launch {
            // **The database has to be opened before this can be asked.**
            //
            // `setAsideFileName` is written while the database is being opened,
            // and the container opens it lazily, on first use. Reading the flag
            // straight from `init` therefore raced the open and lost every
            // time: it saw null, said nothing, and somebody whose history had
            // just been set aside was shown an empty shelf with no explanation.
            // That is the half of issue #49 that survived the crash fix, and it
            // is worse than the crash in one way, because a crash at least
            // tells you something happened.
            //
            // Touching the database here is what makes the question answerable.
            // It is idempotent, and it happens on the IO dispatcher because
            // opening a database is file work and this runs during startup.
            withContext(Dispatchers.IO) { container.database }

            val name = com.kamsiob.meedwell.data.db.MeedwellDatabase.setAsideFileName
                ?: return@launch
            _notice.value = "Meedwell could not read the data it had, so it started fresh. " +
                "Your old data was not deleted: it is still on this phone as $name. " +
                "Your music and your Bandcamp account are untouched."
        }
    }

    override fun onCleared() {
        player.release()
        surroundings.release()
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
     * Plays one entry out of the history, as itself.
     *
     * The history row used to call `playAlbum`, so tapping the piece you heard
     * on Tuesday started its record from track one. The same bug was found and
     * fixed in Search, with a comment saying that music starting makes it worse
     * than a crash; this is that fix, applied to the screen it was still on.
     */
    /**
     * The third way in: neither Bandcamp nor folders, just listening.
     *
     * Somebody who declined Bandcamp and has no files on the phone could not
     * get past onboarding at all, while the footer promised them a "later" that
     * was unreachable. The person the welcome screen was written for was the
     * one person it locked out. This opens the door onto Surroundings, which
     * works from the first minute with no account and no files.
     */
    fun chooseListeningOnly() {
        settings.hasChosenPath = true
        refreshSettings()
    }

    /**
     * Plays one entry out of the history, as itself, rather than track one of
     * its album. The same bug was found and fixed in Search first.
     */
    fun playHistoryEntry(trackId: String, albumId: String) {
        viewModelScope.launch {
            val tracks = container.library.tracksForAlbum(albumId)
            val index = tracks.indexOfFirst { it.id == trackId }
            if (index >= 0) player.playTracks(tracks, index) else player.playAlbum(albumId)
        }
    }

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
            .onEach { records ->
                _forgotten.value = records.map { ForgottenAlbum(it.album, forgottenReason(it)) }
                _more.value = _more.value.copy(forgottenCount = records.size)
            }
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
                            // Through `CoverUrls` rather than the client, so
                            // history shares the one stable URL per cover with
                            // the shelf instead of minting a fresh salt and
                            // reloading art the loader already holds.
                            coverUrl = CoverUrls.of(row.coverArtId),
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
    private fun forgottenReason(record: com.kamsiob.meedwell.data.ForgottenRecord): String {
        val now = System.currentTimeMillis() / 1000
        val last = record.lastPlayedAt
        val month = java.text.SimpleDateFormat("MMMM", java.util.Locale.US)
        val monthYear = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.US)
        fun name(at: Long, withYear: Boolean) =
            (if (withYear) monthYear else month).format(java.util.Date(at * 1000))
        return when {
            last == null && (record.album.addedAt ?: 0) > 0 &&
                now - (record.album.addedAt ?: 0) > 365L * 86_400 ->
                "Never played. Shelved ${name(record.album.addedAt!!, withYear = true)}."
            last == null -> "Not played yet"
            now - last > 365L * 86_400 -> "Last heard ${name(last, withYear = true)}"
            now - last > 45L * 86_400 -> "Last heard in ${name(last, withYear = false)}"
            record.plays == 1 -> "Played once, then quiet"
            else -> "Played twice, then quiet"
        }
    }

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
        _shelf.value = _shelf.value.copy(sort = _sort.value, sortLabel = sortLabel(_sort.value, _scope.value), filtering = false)
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

    /**
     * Whether a runtime permission is actually held.
     *
     * **The one Settings shows arrived in API 33**, and this app runs from 29.
     * Asking the platform about a permission that does not exist on the device
     * returns denied, so an Android 10 phone would have read "Not allowed.
     * Playback controls will not appear" while the notification sat in its
     * shade. Below 33 notifications are granted at install, so there is nothing
     * to withhold and nothing to report as missing.
     */
    private fun isGranted(permission: String): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return true
        return androidx.core.content.ContextCompat.checkSelfPermission(
            container.appContext,
            permission,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

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
                wifiOnlyDownloads = settings.wifiOnlyDownloads,
                resumeQueueOnOpening = settings.resumeQueueOnOpening,
                dawnMinute = settings.dawnMinute,
                duskMinute = settings.duskMinute,
                lastBackupAt = settings.lastBackupAt,
                notificationsAllowed = isGranted(android.Manifest.permission.POST_NOTIFICATIONS),
                versionName = com.kamsiob.meedwell.BuildConfig.VERSION_NAME,
                versionCode = com.kamsiob.meedwell.BuildConfig.VERSION_CODE,
            )
            // More shows the same facts on the right of its rows, so it is
            // refreshed from the same place rather than kept in step by hand.
            _more.value = _more.value.copy(
                connected = container.credentials.isConnected,
                playCount = container.database.playEvents().count(),
                folderCount = _settings.value.watchedFolderCount,
                lastSyncAt = settings.lastSyncAt,
            )
        }
    }

    // ---------- Lists ----------

    /**
     * The lists, and how many tracks are in each.
     *
     * `ListsState` existed with nothing but a loved count in it: the lists
     * themselves were never loaded, so the screen that reads this had nothing to
     * show and no way to say why.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeLists() {
        container.playlists.observeAll()
            .flatMapLatest { rows ->
                if (rows.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
                else combine(
                    rows.map { row ->
                        container.playlists.observeTracks(row.id).map { tracks -> row to tracks }
                    }
                ) { it.toList() }
            }
            .onEach { pairs ->
                _lists.value = _lists.value.copy(
                    lists = pairs.map { (row, tracks) ->
                        ListSummary(
                            id = row.id,
                            name = row.name,
                            subtitle = buildString {
                                append(
                                    when (tracks.size) {
                                        0 -> "Empty"
                                        1 -> "1 track"
                                        else -> "${tracks.size} tracks"
                                    }
                                )
                                // Said on the row rather than discovered on the
                                // screen behind it, because it is the difference
                                // between a list you can change and one you
                                // cannot.
                                if (row.fromBandcamp) append(" · from Bandcamp")
                            },
                            coverUrl = tracks.firstOrNull()?.let { CoverUrls.of(it.coverArtId) },
                            editable = !row.fromBandcamp,
                        )
                    }
                )
            }
            .launchIn(viewModelScope)
    }

    /**
     * Opens one list and follows it.
     *
     * The previous collector is cancelled first: without that, opening three
     * lists in a row leaves three collectors writing into the same state and the
     * last one to emit wins, which looks like the screen showing the wrong list
     * at random.
     */
    fun openList(id: String) {
        playlistJob?.cancel()
        playlistJob = combine(
            container.playlists.observe(id),
            container.playlists.observeTracks(id),
        ) { row, tracks ->
            PlaylistState(
                id = id,
                name = row?.name.orEmpty(),
                tracks = tracks,
                editable = row?.fromBandcamp != true,
            )
        }
            .onEach { _playlist.value = it }
            .launchIn(viewModelScope)
    }

    fun closeList() {
        playlistJob?.cancel()
        playlistJob = null
        _playlist.value = PlaylistState()
    }

    fun shuffleList(listId: String) {
        viewModelScope.launch {
            val ids = container.playlists.trackIds(listId).shuffled()
            if (ids.isEmpty()) return@launch
            player.playTracks(container.library.tracks(ids), 0)
        }
    }

    fun createList(name: String, onMade: (String) -> Unit = {}) {
        viewModelScope.launch {
            val id = container.playlists.create(name)
            _notice.value = "Made \"${name.trim().ifBlank { "New list" }}\"."
            onMade(id)
        }
    }

    fun renameList(id: String, name: String) {
        viewModelScope.launch { container.playlists.rename(id, name) }
    }

    fun deleteList(id: String) {
        viewModelScope.launch {
            container.playlists.delete(id)
            _notice.value = "List deleted. The music is untouched."
        }
    }

    fun addTrackToList(listId: String, trackId: String) {
        viewModelScope.launch {
            container.playlists.addTrack(listId, trackId)
            _notice.value = "Added."
        }
    }

    fun addAlbumToList(listId: String, albumId: String) {
        viewModelScope.launch {
            val before = container.playlists.trackCount(listId)
            container.playlists.addAlbum(listId, albumId)
            val added = container.playlists.trackCount(listId) - before
            _notice.value = if (added == 1) "Added 1 track." else "Added $added tracks."
        }
    }

    fun removeFromList(listId: String, position: Int) {
        viewModelScope.launch { container.playlists.removeAt(listId, position) }
    }

    fun moveInList(listId: String, from: Int, to: Int) {
        viewModelScope.launch { container.playlists.move(listId, from, to) }
    }

    /** Plays a list from a given position, in its own order. */
    fun playList(listId: String, startIndex: Int = 0) {
        viewModelScope.launch {
            val ids = container.playlists.trackIds(listId)
            if (ids.isEmpty()) return@launch
            player.playTracks(container.library.tracks(ids), startIndex)
        }
    }

    // ---------- Export and restore ----------

    fun refreshExportState() {
        _export.value = _export.value.copy(lastBackupAt = settings.lastBackupAt)
    }

    fun suggestedBackupName(): String = container.backup.suggestedFileName()

    fun exportTo(uri: Uri) {
        _export.value = _export.value.copy(working = true, result = null)
        viewModelScope.launch {
            val problem = container.backup.writeTo(uri)
            _export.value = _export.value.copy(
                working = false,
                lastBackupAt = settings.lastBackupAt,
                result = problem ?: "Exported. Keep the file somewhere you will find it again.",
            )
            refreshSettings()
        }
    }

    fun restoreFrom(uri: Uri) {
        _export.value = _export.value.copy(working = true, result = null)
        viewModelScope.launch {
            val outcome = container.backup.restoreFrom(uri)
            _export.value = _export.value.copy(working = false, result = describe(outcome))
            refreshSettings()
        }
    }

    /**
     * What a restore did, in a sentence.
     *
     * It says what came back **and** what did not, because a restore that
     * reports only its successes is how somebody finds out months later that a
     * section of the file was skipped.
     */
    private fun describe(outcome: com.kamsiob.meedwell.data.BackupRepository.RestoreResult): String =
        when (outcome) {
            is com.kamsiob.meedwell.data.BackupRepository.RestoreResult.Refused -> outcome.message
            is com.kamsiob.meedwell.data.BackupRepository.RestoreResult.Restored -> buildString {
                append("Restored ${outcome.plays} plays")
                if (outcome.loved > 0) append(", ${outcome.loved} hearts")
                if (outcome.lists > 0) append(", ${outcome.lists} lists")
                append(".")
                if (outcome.relinked > 0) {
                    append(" ${outcome.relinked} of your music files were found where the export said ")
                    append("they would be and are linked up again.")
                }
                if (outcome.notUnderstood.isNotEmpty()) {
                    append(" This version did not understand ")
                    append(outcome.notUnderstood.joinToString(", "))
                    append(", so that part was left alone rather than guessed at.")
                }
            }
        }

    fun toggleWifiOnly() {
        settings.wifiOnlyDownloads = !settings.wifiOnlyDownloads
        refreshSettings()
    }

    /**
     * Whether to put the notification permission to somebody now.
     *
     * Once per install, and only while it is still genuinely ungranted. Somebody
     * who granted it in the system settings never sees a dialog they have
     * already answered.
     */
    fun shouldAskForNotifications(): Boolean =
        !settings.hasAskedNotifications &&
            !isGranted(android.Manifest.permission.POST_NOTIFICATIONS)

    fun markNotificationsAsked() {
        settings.hasAskedNotifications = true
        refreshSettings()
    }

    /**
     * Whether to put it again as a download starts.
     *
     * Asked a second time, and only a second time, because this is the occasion
     * where refusing has a real cost: without it the download runs with nothing
     * in the shade, no progress and no way to stop it from outside the app.
     * Somebody who granted it at the first play never sees this, and somebody
     * who refuses here is never asked a third time.
     */
    fun shouldAskForDownloadNotifications(): Boolean =
        !settings.hasAskedDownloadNotifications &&
            !isGranted(android.Manifest.permission.POST_NOTIFICATIONS)

    fun markDownloadNotificationsAsked() {
        settings.hasAskedDownloadNotifications = true
        refreshSettings()
    }

    /**
     * The listener's own dawn and dusk.
     *
     * Kept sane rather than trusted: a dusk at or before dawn would draw a line
     * that runs backwards, so the two are nudged apart by an hour instead of
     * being allowed to cross.
     */
    fun setDawn(minute: Int) {
        settings.dawnMinute = minute
        if (settings.duskMinute <= minute) settings.duskMinute = (minute + 60).coerceAtMost(23 * 60 + 59)
        refreshSettings()
    }

    fun setDusk(minute: Int) {
        settings.duskMinute = minute
        if (settings.dawnMinute >= minute) settings.dawnMinute = (minute - 60).coerceAtLeast(0)
        refreshSettings()
    }

    fun toggleResumeQueue() {
        settings.resumeQueueOnOpening = !settings.resumeQueueOnOpening
        refreshSettings()
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
            _shelf.value = _shelf.value.copy(sort = sort, sortLabel = sortLabel(sort, scope))
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
        if (state.checking) return
        if (!state.canSubmit) {
            // The button used to swallow the tap in silence, which reads as the
            // app being broken rather than the form being incomplete.
            _notice.value = "Both fields are needed. Paste the username and password from Bandcamp's page."
            return
        }

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
    /**
     * The end of onboarding, and the only place the path is marked chosen.
     *
     * Called from the tone disclosure, which is the last of the three screens.
     * Marking it earlier would mean somebody who backed out partway through
     * reopened the app onto a shelf they had never agreed to set up.
     */
    fun finishOnboarding() {
        settings.hasChosenPath = true
        _shelf.value = _shelf.value.copy(connected = container.credentials.isConnected)
    }

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
                is SyncResult.Failed -> if (manual) {
                    _syncFailure.value = result.reason
                } else {
                    // **A background failure must not ambush the app.** The
                    // stale-sync check runs on open, and raising the full
                    // trouble sheet over whatever somebody opened the app to do
                    // put a modal error in front of a person who came to press
                    // play. The sheet keeps its job for the sync they asked
                    // for; the one nobody asked for gets a quiet line.
                    _notice.value = "Bandcamp didn't answer just now. Your music is untouched; sync again any time from Settings."
                }
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

    /**
     * The action sheet for whatever is playing.
     *
     * Built from the database rather than from the player, so it carries the
     * heart state and the artist ID that a media item does not.
     */
    fun openSheetForCurrentTrack(onReady: (ActionTarget) -> Unit) {
        val id = player.state.value.trackId ?: return
        viewModelScope.launch {
            val track = container.library.track(id) ?: return@launch
            onReady(
                ActionTarget(
                    id = track.id,
                    title = track.title,
                    subtitle = track.artist,
                    coverUrl = CoverUrls.of(track.coverArtId),
                    kind = ActionTarget.Kind.Track,
                    isStarred = track.isStarred,
                    artistId = track.artistId.takeIf { it.isNotBlank() },
                )
            )
        }
    }

    /** The heart on the now playing screen. */
    fun loveCurrentTrack() {
        val id = player.state.value.trackId ?: return
        viewModelScope.launch {
            val track = container.library.track(id)
            if (track == null) {
                _notice.value = "That track is not on your shelf, so there is nothing to love."
                return@launch
            }
            if (track.isStarred) {
                showLoveLimit()
                return@launch
            }
            val landed = container.library.love(container.client(), track.id, isAlbum = false)
            _notice.value = if (landed) {
                "Loved. It is on your Bandcamp account too."
            } else {
                if (container.client() == null) {
                    "The heart travels with a Bandcamp account, and nothing is connected yet."
                } else {
                    "Bandcamp did not take the heart. Nothing changed."
                }
            }
        }
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
                if (container.client() == null) {
                    "The heart travels with a Bandcamp account, and nothing is connected yet."
                } else {
                    "Bandcamp did not take the heart. Nothing changed."
                }
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
