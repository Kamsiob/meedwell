package com.kamsiob.meedwell.ui

import androidx.media3.common.util.UnstableApi
import com.kamsiob.meedwell.AppContainer
import com.kamsiob.meedwell.core.surroundings.Credits
import com.kamsiob.meedwell.core.surroundings.Downloads
import com.kamsiob.meedwell.core.surroundings.SurroundingsSound
import com.kamsiob.meedwell.data.SurroundingsDownloader
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

    val player = SurroundingsPlayer(container.appContext, container.settings)

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
        if (sound == null) {
            _card.value = SurroundingsCardState()
            return
        }
        val present = store.presentIds(sounds)
        val others = (listOf(sound.id) + recentlyUsed.filter { it != sound.id })
            .distinct()
            .mapNotNull { id -> sounds.firstOrNull { it.id == id && it.id in present } }
            .take(4)
            .map {
                SurroundingsCardItem(
                    id = it.id,
                    title = rowTitle(it),
                    duration = Downloads.humanDuration(it.durationSeconds),
                    playing = it.id == sound.id,
                )
            }
        _card.value = SurroundingsCardState(
            // It exists only while a sound is playing. A paused bed is still a
            // bed; a stopped one is not.
            visible = playing.playingId != null,
            expanded = cardExpanded,
            soundId = sound.id,
            title = rowTitle(sound),
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
    private val queue = ArrayDeque<String>()
    private val failures = mutableMapOf<String, String>()
    private var progressId: String? = null
    private var progress: Float = 0f
    private var worker: Job? = null
    private var checking = false

    /**
     * Reads the library and works out what is already here.
     *
     * The three bundled recordings are unpacked from assets on the first run so
     * that a phone with no network has something to play from the moment
     * Surroundings is opened. They go through exactly the same verification and
     * atomic placement as a downloaded one.
     */
    fun load() {
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

            val bundledIds = manifest.bundled.map { it.id }.toSet()
            sounds.filter { it.id in bundledIds && !store.isPresent(it) }.forEach { sound ->
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
                        playingCredit = Credits.oneLine(sound),
                        isPlaying = false,
                    )
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
        _state.value = _state.value.copy(
            playingId = sound.id,
            playingTitle = sound.displayName,
            // Generated from the manifest, never typed. The credit on the bar
            // and the credit on the credits screen cannot drift apart because
            // they come out of the same function.
            playingCredit = Credits.oneLine(sound),
            isPlaying = true,
            volume = container.settings.surroundingsVolume,
        )
        rebuildCard()
    }

    fun pause() {
        player.pause()
        _state.value = _state.value.copy(isPlaying = false)
    }

    fun stop() {
        player.stop()
        _state.value = _state.value.copy(playingId = null, isPlaying = false, playingTitle = "", playingCredit = "")
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
        failures.remove(id)
        queue.addLast(id)
        rebuild()
        startWorker()
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
        downloader.networkObjection()?.let {
            onNotice(it.message)
            return
        }
        missing.forEach { if (it.id !in queue && progressId != it.id) queue.addLast(it.id) }
        onNotice("Queued ${missing.size} recordings. You can stop any of them.")
        rebuild()
        startWorker()
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

        downloader.networkObjection()?.let {
            onNotice(it.message)
            return
        }

        missing.forEach { sound ->
            if (sound.id !in queue && progressId != sound.id) queue.addLast(sound.id)
        }
        rebuild()
        startWorker()
    }

    /**
     * Stops a download.
     *
     * What has already arrived is kept, so starting again continues rather than
     * restarts. That is the whole point of the partial file surviving.
     */
    fun cancelDownload(id: String) {
        queue.remove(id)
        if (progressId == id) {
            worker?.cancel()
            worker = null
            progressId = null
            progress = 0f
        }
        rebuild()
        startWorker()
    }

    fun remove(id: String) {
        val sound = sounds.firstOrNull { it.id == id } ?: return
        if (_state.value.playingId == id) stop()
        store.remove(sound)
        rebuild()
    }

    private fun startWorker() {
        if (worker?.isActive == true) return
        val next = queue.removeFirstOrNull() ?: return
        val sound = sounds.firstOrNull { it.id == next } ?: return startWorker()

        progressId = next
        progress = 0f
        rebuild()

        worker = scope.launch {
            val outcome = downloader.fetch(sound) { got, total ->
                progress = if (total > 0) got.toFloat() / total else 0f
                rebuild()
            }
            progressId = null
            progress = 0f
            when (outcome) {
                is SurroundingsDownloader.Outcome.Done -> Unit
                is SurroundingsDownloader.Outcome.Cancelled -> Unit
                is SurroundingsDownloader.Outcome.Failed -> {
                    failures[sound.id] = outcome.message
                    if (!outcome.canRetry) {
                        // A permanent failure would otherwise take the rest of
                        // a queued pack down with it, one identical error at a
                        // time. Say it once and stop.
                        queue.clear()
                        onNotice(outcome.message)
                    }
                }
            }
            worker = null
            rebuild()
            startWorker()
        }
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
        )
    }

    fun closeDetail() {
        _detail.value = null
    }

    fun release() {
        player.release()
    }

    // ---------- Assembling what the screen shows ----------

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

        _state.value = _state.value.copy(
            groups = groups,
            hereCount = present.size,
            totalCount = sounds.size,
            storageLine = storageLine(present.size),
            everythingCostLine = Downloads.costLine(sounds, present),
            missingCount = sounds.size - present.size,
            checking = checking,
            volume = container.settings.surroundingsVolume,
        )
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

    private fun groupSubtitle(total: Int, here: Int): String = when {
        here == 0 -> "$total recordings"
        here == total -> "$total recordings, all here"
        else -> "$total recordings, $here here"
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
)
