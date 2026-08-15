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
import com.kamsiob.meedwell.ui.screens.ConnectError
import com.kamsiob.meedwell.ui.screens.ConnectState
import com.kamsiob.meedwell.ui.screens.CoverUrls
import com.kamsiob.meedwell.ui.screens.ShelfState
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

    init {
        installCoverResolver()
        observeLibrary()
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

    val lastSyncAt: Long get() = settings.lastSyncAt

    val isConnected: Boolean get() = container.credentials.isConnected

    val hasChosenPath: Boolean get() = settings.hasChosenPath

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MeedwellViewModel(container) as T
    }
}
