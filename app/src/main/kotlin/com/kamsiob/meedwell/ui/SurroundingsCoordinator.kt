package com.kamsiob.meedwell.ui

import androidx.media3.common.util.UnstableApi
import com.kamsiob.meedwell.AppContainer
import com.kamsiob.meedwell.core.surroundings.Credits
import com.kamsiob.meedwell.core.surroundings.Downloads
import com.kamsiob.meedwell.core.surroundings.SurroundingsSound
import com.kamsiob.meedwell.data.SurroundingsDownloadService
import com.kamsiob.meedwell.data.SurroundingsDownloads
import com.kamsiob.meedwell.data.SurroundingsDownloader
import com.kamsiob.meedwell.playback.SurroundingsBed
import com.kamsiob.meedwell.playback.SurroundingsService
import com.kamsiob.meedwell.playback.SurroundingsPlayer
import com.kamsiob.meedwell.ui.components.SurroundingsCardItem
import com.kamsiob.meedwell.ui.components.SurroundingsCardState
import com.kamsiob.meedwell.ui.screens.RowState
import com.kamsiob.meedwell.ui.screens.SurroundingsGroup
import com.kamsiob.meedwell.ui.screens.SurroundingsRow
import com.kamsiob.meedwell.ui.screens.SurroundingsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

/**
 * Everything Surroundings does, in one place.
 *
 * Kept out of `MeedwellViewModel` because it shares nothing with the shelf: a
 * different library, a different player, a different idea of what a file is.
 * Folding it in would have meant one class holding two apps.
 *
 * **One download at a time, in a queue.** Not because the network could not
 * take more, but because the alternative is a phone fetching a hundred and
 * eight files at once, every one of them a partial that must be resumed if
 * anything interrupts it, and a progress display nobody can read. A queue also
 * means cancelling a pack stops it after the recording in flight rather than
 * abandoning nine half-files.
 */
@UnstableApi
class SurroundingsCoordinator(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    private val onNotice: (String) -> Unit,
) {

    private val store = container.surroundingsStore
    private val downloader = container.surroundingsDownloader

    // Borrowed from the application, not built here. See `AppContainer`.
    val player = container.surroundingsPlayer

    private val _state = MutableStateFlow(SurroundingsUiState())
    val state: StateFlow<SurroundingsUiState> = _state.asStateFlow()

    /**
     * The floating card's own state.
     *
     * Whether it is expanded lives here rather than in the interface, because
     * the card outlives any one screen: it shows on Shelf, Search and More, and
     * an expanded card should not collapse because somebody changed tab.
     */
    private var cardExpanded = false

    /**
     * What is on this phone, by category, for the player's picker.
     *
     * Kept beside the card rather than inside it. The card is a short list on
     * purpose; this is the opposite, the whole of what somebody owns arranged so
     * it can be browsed without leaving the page.
     */
    val slices: StateFlow<List<com.kamsiob.meedwell.ui.screens.SurroundingsSlice>> get() = _slices
    private val _slices =
        MutableStateFlow<List<com.kamsiob.meedwell.ui.screens.SurroundingsSlice>>(emptyList())

    val card: StateFlow<SurroundingsCardState> get() = _card
    private val _card = MutableStateFlow(SurroundingsCardState())

    fun toggleCard() {
        cardExpanded = !cardExpanded
        rebuildCard()
    }

    /**
     * The card's contents: four recordings at most, already on the phone, most
     * recently used first with the playing one at the top.
     *
     * Deliberately not a browser. Anything longer belongs on the Surroundings
     * tab, which is what "All recordings" opens.
     */
    private fun rebuildCard() {
        val playing = _state.value
        val sound = sounds.firstOrNull { it.id == playing.playingId }
        val present = store.presentIds(sounds)

        /** What is on the phone, as rows, playing one first. */
        val plays = container.settings.surroundingsPlays
        fun rows(first: String?): List<SurroundingsCardItem> =
            (listOfNotNull(first) + recentlyUsed +
                present.sortedByDescending { plays[it] ?: 0 })
                .distinct()
                .mapNotNull { id -> sounds.firstOrNull { it.id == id && it.id in present } }
                .take(4)
                .map {
                    SurroundingsCardItem(
                        id = it.id,
                        title = rowTitle(it),
                        duration = Downloads.humanDuration(it.durationSeconds),
                        playing = it.id == first,
                    )
                }

        if (sound == null) {
            // **The list is built even with nothing playing.**
            //
            // The floating card stays hidden, because it exists only while a bed
            // is going. But the player spread's facing page shows the same rows,
            // and it used to short circuit to a blurb and a link whenever no bed
            // was playing: a whole page of nothing, on the surface whose entire
            // job is choosing a bed. With the rows present, starting one from
            // inside the player is a single tap.
            _card.value = SurroundingsCardState(visible = false, others = rows(null))
            return
        }
        // Seeded with what is already on the phone, not only with what has been
        // played this session.
        //
        // `recentlyUsed` starts empty, so on a fresh launch the opened card
        // listed exactly one recording: the one already playing, whose row is a
        // no-op. The one surface built for swapping the bed in two taps was
        // useless until you had already swapped it the six tap way.
        val others = rows(sound.id)
        _card.value = SurroundingsCardState(
            // It exists only while a sound is playing. A paused bed is still a
            // bed; a stopped one is not.
            visible = playing.playingId != null,
            expanded = cardExpanded,
            soundId = sound.id,
            title = rowTitle(sound),
            isPlaying = playing.isPlaying,
            volume = container.settings.surroundingsVolume,
            others = others,
        )
    }

    /** Most recently played first, so the card's short list is the useful one. */
    private val recentlyUsed = ArrayDeque<String>()

    /** The recording whose credit sheet is open, if any. */
    private val _detail = MutableStateFlow<SurroundingsDetail?>(null)
    val detail: StateFlow<SurroundingsDetail?> = _detail.asStateFlow()

    private var sounds: List<SurroundingsSound> = emptyList()
    private var expanded: Set<String> = emptySet()
    private var checking = false

    /** What somebody has typed into the library's search field. */
    private var query: String = ""

    /** The ids that shipped inside the app and therefore cannot be removed. */
    private var bundledIds: Set<String> = emptySet()

    fun setSearch(text: String) {
        query = text
        rebuild()
    }

    /**
     * Set when a download is waiting on the mobile data question.
     *
     * The coordinator cannot show a sheet, so it holds the intent and raises a
     * flag; the interface asks and calls back. Holding the action rather than
     * asking somebody to tap download again is the difference between being
     * asked a question and being sent away.
     */
    private var deferredDownload: (() -> Unit)? = null

    private val _askCellular = MutableStateFlow(false)
    val askCellular: StateFlow<Boolean> = _askCellular.asStateFlow()

    /** True until the question has been put once. */
    private fun needsCellularAnswer(): Boolean = !container.settings.hasAskedCellular

    private fun askThen(action: () -> Unit) {
        deferredDownload = action
        _askCellular.value = true
    }

    /**
     * The answer, and then the download that was waiting on it.
     *
     * Either answer counts as answered: saying "Wi-Fi only" is a real choice and
     * must not mean being asked again tomorrow.
     */
    fun answerCellular(allowCellular: Boolean) {
        container.settings.wifiOnlyDownloads = !allowCellular
        container.settings.hasAskedCellular = true
        _askCellular.value = false
        val waiting = deferredDownload
        deferredDownload = null
        waiting?.invoke()
    }

    fun dismissCellularQuestion() {
        // Dismissing without choosing leaves the default in place and does not
        // count as answered, so the question comes back next time rather than
        // silently deciding for somebody.
        _askCellular.value = false
        deferredDownload = null
    }

    // The queue lives in the download service now, so that leaving the app does
    // not take it with it. These read through to whatever that service is doing,
    // and are the only view of it this class has.
    private val queue: List<String> get() = SurroundingsDownloads.state.value.queued
    private val failures: Map<String, String> get() = SurroundingsDownloads.state.value.failures
    private val progressId: String? get() = SurroundingsDownloads.state.value.workingOn
    private val progress: Float get() = SurroundingsDownloads.state.value.progress

    /**
     * Reads the library and works out what is already here.
     *
     * The three bundled recordings are unpacked from assets on the first run so
     * that a phone with no network has something to play from the moment
     * Surroundings is opened. They go through exactly the same verification and
     * atomic placement as a downloaded one.
     */
    fun load() {
        // The service is the only thing that knows how a download is going, and
        // it is in another component entirely. Without this the rows would read
        // their progress once and then sit still: everything below reads through
        // to that state, so something has to notice when it moves.
        // The shade can pause or stop the bed while no screen is looking, so the
        // interface follows the bed rather than assuming it is the only thing
        // that ever changes it. Without this, pausing from the notification left
        // the card and the player still claiming it was playing.
        SurroundingsBed.state
            .onEach { bed ->
                val here = _state.value
                if (!bed.present && here.playingId != null) {
                    _state.value = here.copy(
                        playingId = null,
                        isPlaying = false,
                        playingTitle = "",
                        playingDescription = "",
                        playingCredit = "",
                    )
                    cardExpanded = false
                    rebuildCard()
                } else if (bed.present && bed.playing != here.isPlaying) {
                    _state.value = here.copy(isPlaying = bed.playing)
                    rebuildCard()
                }
            }
            .launchIn(scope)

        SurroundingsDownloads.state
            .onEach { rebuild() }
            .launchIn(scope)

        scope.launch {
            val manifest = container.surroundings.manifest()
            // The hard rule is applied once, here, and everything downstream
            // works from what comes out. Nothing else reads raw manifest
            // entries, so no future screen can route around it.
            sounds = Downloads.offerable(manifest)

            container.surroundings.loadError?.let {
                _state.value = _state.value.copy(loadError = it)
                return@launch
            }

            store.sweepOrphans(sounds)

            val bundled = manifest.bundled.map { it.id }.toSet()
            bundledIds = bundled
            sounds.filter { it.id in bundled && !store.isPresent(it) }.forEach { sound ->
                store.installBundled(sound)?.let { problem ->
                    // Worth saying out loud: a bundled recording failing means
                    // the install itself is damaged, not the network.
                    onNotice(problem)
                }
            }

            // Anything already playing before a restart is put back, paused.
            val remembered = container.settings.surroundingsSoundId
            rebuild()
            if (remembered != null) {
                sounds.firstOrNull { it.id == remembered && store.isPresent(it) }?.let { sound ->
                    _state.value = _state.value.copy(
                        playingId = sound.id,
                        playingTitle = sound.displayName,
                        playingDescription = sound.description,
                        playingGroup = sound.group,
                        playingCredit = Credits.oneLine(sound),
                        isPlaying = false,
                    )
                    // Without this the remembered bed had no control anywhere
                    // outside the Surroundings tab: the state was restored but
                    // the card was never built, so it stayed at its invisible
                    // default until something else happened to rebuild it.
                    rebuildCard()
                }
            }
        }
    }

    fun toggleGroup(id: String) {
        expanded = if (id in expanded) expanded - id else expanded + id
        rebuild()
    }

    // ---------- Playing ----------

    fun play(id: String) {
        val sound = sounds.firstOrNull { it.id == id } ?: return
        if (!store.isPresent(sound)) {
            download(id)
            return
        }
        // Resume only if this player is genuinely holding this recording.
        // After a restart the interface remembers a bed and shows it paused
        // while the player is empty, and asking an empty player to resume does
        // nothing: it has to be started instead.
        if (player.loadedId == id && !_state.value.isPlaying) {
            player.resume()
        } else {
            player.play(sound, store.fileFor(sound))
        }
        recentlyUsed.remove(sound.id)
        recentlyUsed.addFirst(sound.id)
        container.settings.noteSurroundingsPlay(sound.id)
        _state.value = _state.value.copy(
            playingId = sound.id,
            playingTitle = sound.displayName,
            playingDescription = sound.description,
            playingGroup = sound.group,
            // Generated from the manifest, never typed. The credit on the bar
            // and the credit on the credits screen cannot drift apart because
            // they come out of the same function.
            playingCredit = Credits.oneLine(sound),
            isPlaying = true,
            volume = container.settings.surroundingsVolume,
        )
        // The bed is now a fact the whole app can see, and the service that
        // holds it up and draws its notification follows from that.
        SurroundingsBed.set(sound.id, sound.displayName, playing = true)
        SurroundingsService.start(container.appContext)
        rebuildCard()
    }

    fun pause() {
        player.pause()
        _state.value = _state.value.copy(isPlaying = false)
        SurroundingsBed.setPlaying(false)
    }

    fun stop() {
        player.stop()
        _state.value = _state.value.copy(
            playingId = null,
            isPlaying = false,
            playingTitle = "",
            playingDescription = "",
            playingGroup = "",
            playingCredit = "",
        )
        // Clearing this is what stops the service and takes the notification
        // with it. Nothing else needs telling.
        SurroundingsBed.clear()
        cardExpanded = false
        rebuildCard()
    }

    fun setVolume(volume: Float) {
        player.setVolume(volume)
        _state.value = _state.value.copy(volume = volume)
        rebuildCard()
    }

    // ---------- Getting ----------

    fun download(id: String) {
        if (id in queue || progressId == id) return
        if (needsCellularAnswer()) return askThen { download(id) }
        SurroundingsDownloadService.enqueue(container.appContext, listOf(id))
    }

    /**
     * The whole library, everything not already here.
     *
     * The same queue as any other download, so it can be stopped one recording
     * at a time and picks up where it left off.
     */
    fun downloadEverything() {
        val missing = sounds.filter { !store.isPresent(it) }
        if (missing.isEmpty()) return
        if (needsCellularAnswer()) return askThen { downloadEverything() }
        downloader.networkObjection()?.let {
            onNotice(it.message)
            return
        }
        SurroundingsDownloadService.enqueue(container.appContext, missing.map { it.id })
        onNotice("Queued ${missing.size} recordings. You can stop any of them, and they keep going if you leave.")
    }

    /**
     * Asks the library whether there is anything new.
     *
     * Only ever from a tap. Nothing checks on a timer, on launch, or in the
     * background: a library that adds things to somebody's app unasked has
     * started making decisions on their behalf.
     */
    fun checkForNew() {
        if (checking) return
        checking = true
        rebuild()
        scope.launch {
            val message = container.surroundings.refreshManifest(container.httpClient)
            sounds = Downloads.offerable(container.surroundings.manifest())
            checking = false
            rebuild()
            onNotice(message)
        }
    }

    /** Everything in a group that is not already here, queued in order. */
    fun downloadGroup(groupId: String) {
        val missing = sounds.filter { it.group == groupId && !store.isPresent(it) }
        if (missing.isEmpty()) return
        if (needsCellularAnswer()) return askThen { downloadGroup(groupId) }

        downloader.networkObjection()?.let {
            onNotice(it.message)
            return
        }

        SurroundingsDownloadService.enqueue(container.appContext, missing.map { it.id })
    }

    /**
     * Stops a download.
     *
     * What has already arrived is kept, so starting again continues rather than
     * restarts. That is the whole point of the partial file surviving.
     */
    fun cancelDownload(id: String) {
        SurroundingsDownloadService.cancel(container.appContext, id)
    }

    fun remove(id: String) {
        val sound = sounds.firstOrNull { it.id == id } ?: return
        if (_state.value.playingId == id) stop()
        store.remove(sound)
        SurroundingsDownloads.forget(id)
        rebuild()
    }

    // ---------- The credit sheet ----------

    fun openDetail(id: String) {
        val sound = sounds.firstOrNull { it.id == id } ?: return
        _detail.value = SurroundingsDetail(
            id = sound.id,
            title = rowTitle(sound),
            originalTitle = sound.displayName,
            // Every line here is generated from the manifest. The extra
            // conditions in particular are reproduced word for word, because
            // paraphrasing somebody's credit request is not crediting them.
            credit = Credits.full(sound),
            soundPageUrl = sound.attribution.soundPageUrl,
            licenseUrl = sound.attribution.licenseUrl,
            recordistUrl = sound.attribution.recordistProfileUrl,
            isHere = store.isPresent(sound),
            isBundled = sound.id in bundledIds,
        )
    }

    fun closeDetail() {
        _detail.value = null
    }

    fun release() {
        // **Deliberately does not release the player.**
        //
        // It belongs to the application now, and a bed is often the thing still
        // playing when a screen goes away. Releasing it here was what made
        // ambient audio die on a whim.
    }

    // ---------- Assembling what the screen shows ----------

    /**
     * One recording as a row.
     *
     * **The subtitle is `Credits.oneLine`, on every surface without exception.**
     * That line is the recordist and the license, generated from the manifest
     * and never typed, and it is how the CC BY conditions are met wherever a
     * recording is shown. Any new list that shows recordings uses this, so
     * attribution cannot be lost by adding a screen.
     */
    private fun rowFor(sound: SurroundingsSound, present: Set<String>): SurroundingsRow =
        SurroundingsRow(
            id = sound.id,
            title = rowTitle(sound),
            subtitle = Credits.oneLine(sound),
            state = when {
                sound.id in present -> RowState.Here
                sound.id == progressId || sound.id in queue -> RowState.Downloading
                else -> RowState.Away
            },
            durationLabel = Downloads.humanDuration(sound.durationSeconds),
            progress = if (sound.id == progressId) progress else 0f,
            sizeLabel = Downloads.humanSize(sound.fileSizeBytes),
            failure = failures[sound.id],
        )

    private fun rebuild() {
        val present = store.presentIds(sounds)
        val byGroup = sounds.groupBy { it.group }

        val groups = byGroup.entries.sortedBy { it.key }.map { (groupId, inGroup) ->
            val missing = inGroup.filter { it.id !in present }
            SurroundingsGroup(
                id = groupId,
                title = groupTitle(groupId),
                subtitle = groupSubtitle(inGroup.size, inGroup.size - missing.size),
                costLine = Downloads.costLine(inGroup, present),
                missingCount = missing.size,
                expanded = groupId in expanded,
                sounds = inGroup
                    .sortedWith(compareBy({ it.categoryName }, { rowTitle(it) }))
                    .let { ordered ->
                        var lastCategory: String? = null
                        ordered.map { sound ->
                            val header = sound.categoryName
                                .takeIf { it.isNotBlank() && it != lastCategory }
                            lastCategory = sound.categoryName
                            SurroundingsRow(
                                id = sound.id,
                                title = rowTitle(sound),
                                subtitle = Credits.oneLine(sound),
                                state = when {
                                    sound.id in present -> RowState.Here
                                    sound.id == progressId || sound.id in queue -> RowState.Downloading
                                    else -> RowState.Away
                                },
                                categoryHeader = header,
                                durationLabel = Downloads.humanDuration(sound.durationSeconds),
                                progress = if (sound.id == progressId) progress else 0f,
                                sizeLabel = Downloads.humanSize(sound.fileSizeBytes),
                                failure = failures[sound.id],
                            )
                        }
                    },
            )
        }

        // What can be played right now, most recently used first. This is the
        // answer to "what do I have", and it belongs above nine closed doors.
        val plays = container.settings.surroundingsPlays
        val onPhone = sounds
            .filter { it.id in present }
            .sortedWith(
                compareByDescending<SurroundingsSound> { plays[it.id] ?: 0 }
                    .thenBy { recentlyUsed.indexOf(it.id).let { i -> if (i < 0) Int.MAX_VALUE else i } }
                    .thenBy { rowTitle(it) }
            )
            .map { rowFor(it, present) }

        // Search runs over everything a listener could reasonably remember: what
        // it is, where it was taken, its category, and who recorded it.
        val needle = query.trim().lowercase()
        val results = if (needle.isBlank()) emptyList() else sounds.filter { sound ->
            rowTitle(sound).lowercase().contains(needle) ||
                sound.description.lowercase().contains(needle) ||
                sound.categoryName.lowercase().contains(needle) ||
                Credits.oneLine(sound).lowercase().contains(needle)
        }.sortedBy { rowTitle(it) }.map { rowFor(it, present) }

        val here = sounds.filter { it.id in present }.sortedBy { rowTitle(it) }

        _state.value = _state.value.copy(
            groups = groups,
            query = query,
            results = results,
            onPhone = onPhone,
            bundledRows = here.filter { it.id in bundledIds }.map { rowFor(it, present) },
            downloadedRows = here.filter { it.id !in bundledIds }.map { rowFor(it, present) },
            hereCount = present.size,
            totalCount = sounds.size,
            storageLine = storageLine(present.size),
            everythingCostLine = Downloads.costLine(sounds, present),
            missingCount = sounds.size - present.size,
            checking = checking,
            volume = container.settings.surroundingsVolume,
        )

        // The player's picker: the same facts, arranged by kind of place.
        _slices.value = sounds
            .filter { it.id in present }
            .groupBy { it.group }
            .entries
            .sortedBy { it.key }
            .map { (groupId, inGroup) ->
                com.kamsiob.meedwell.ui.screens.SurroundingsSlice(
                    id = groupId,
                    title = groupTitle(groupId),
                    items = inGroup
                        .sortedBy { rowTitle(it) }
                        .map {
                            SurroundingsCardItem(
                                id = it.id,
                                title = rowTitle(it),
                                duration = Downloads.humanDuration(it.durationSeconds),
                                playing = it.id == _state.value.playingId,
                            )
                        },
                )
            }

        // **The card is rebuilt whenever anything else is.**
        //
        // It was rebuilt only when a bed started, stopped, or was remembered at
        // launch. So on a phone with 83 recordings and nothing playing, the list
        // of what is here was never built at all, and the player's Surroundings
        // page had nothing to show and rendered an empty screen. The page was
        // right; the data behind it had never been asked for.
        rebuildCard()
    }

    /**
     * What a recording is called in the list.
     *
     * The catalog description, because the uploader's own title is a filename
     * as often as a name and reads as noise in a row. The original title is
     * never lost: it is on the credit sheet, which is where somebody tracing
     * the source will look.
     *
     * Where no description was written, the uploader's title is used rather
     * than the category. The category is already the heading these rows sit
     * under, and a row that repeats its own heading tells the reader nothing.
     */
    private fun rowTitle(sound: SurroundingsSound): String {
        val description = sound.description.trim()
        val useful = description.isNotBlank() &&
            !description.equals(sound.displayName.trim(), ignoreCase = true)
        val chosen = if (useful) description else sound.displayName.trim()
        return chosen.ifBlank { sound.categoryName }
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun storageLine(here: Int): String {
        val used = Downloads.humanSize(store.bytesUsed())
        return when (here) {
            0 -> "Nothing stored yet."
            1 -> "One recording on this phone, $used."
            else -> "$here recordings on this phone, $used."
        }
    }

    /**
     * How much of a group is on the phone, said in every case.
     *
     * A group with nothing downloaded used to read "18 recordings" and stop
     * there, so the signal for "none of this is here" was the *absence* of a
     * phrase. Nobody can scan for something that is not printed. Grid 13 words
     * it "7 recordings, none yet", and now so does this.
     */
    private fun groupSubtitle(total: Int, here: Int): String = when {
        here == 0 -> "$total recordings, none yet"
        here == total -> "$total recordings, all here"
        here == 1 -> "$total recordings, 1 on this phone"
        else -> "$total recordings, $here on this phone"
    }

    /**
     * The name a group is shown under.
     *
     * The manifest's ids are ordered with a numeric prefix so the pipeline can
     * sort directories. That ordering is genuinely useful, so it is kept, and
     * the prefix is stripped for display rather than the folders being renamed.
     */
    private fun groupTitle(id: String): String = when (id) {
        "01_water_and_weather" -> "Water and weather"
        "02_rainforest_and_jungle" -> "Rainforest and jungle"
        "03_water_bodies" -> "Rivers, lakes and sea"
        "04_wind_and_air" -> "Wind and air"
        "05_fire" -> "Fire"
        "06_forest_and_countryside" -> "Forest and countryside"
        "07_human_spaces" -> "Rooms with people in them"
        "08_transit" -> "Trains, boats and planes"
        "09_mechanical_and_steady" -> "Machines and steady hum"
        // A group the app has not been taught about yet, which is possible
        // because the library can grow without the app being updated.
        else -> id.substringAfter("_").replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}

/** One recording's credit, as the sheet shows it. */
data class SurroundingsDetail(
    val id: String,
    val title: String,
    val originalTitle: String,
    val credit: com.kamsiob.meedwell.core.surroundings.CreditBlock?,
    val soundPageUrl: String,
    val licenseUrl: String,
    val recordistUrl: String,
    val isHere: Boolean,
    /** Bundled recordings stay: they are why this works with no connection. */
    val isBundled: Boolean = false,
)
