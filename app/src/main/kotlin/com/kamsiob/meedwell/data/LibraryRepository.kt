package com.kamsiob.meedwell.data

import com.kamsiob.meedwell.core.library.SortKeys
import com.kamsiob.meedwell.core.model.Album
import com.kamsiob.meedwell.core.model.Artist
import com.kamsiob.meedwell.core.model.Genre
import com.kamsiob.meedwell.core.model.Origin
import com.kamsiob.meedwell.core.model.Track
import com.kamsiob.meedwell.core.subsonic.SubsonicClient
import com.kamsiob.meedwell.core.subsonic.SubsonicOutcome
import com.kamsiob.meedwell.core.subsonic.toDomain
import com.kamsiob.meedwell.data.db.AlbumEntity
import com.kamsiob.meedwell.data.db.ArtistEntity
import com.kamsiob.meedwell.data.db.GenreEntity
import com.kamsiob.meedwell.data.db.MeedwellDatabase
import com.kamsiob.meedwell.data.db.TrackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The library, read from the database and refreshed from the API.
 *
 * **The database is the source of truth for the interface**, always. Screens
 * observe it and never wait on the network. That is what makes the offline
 * state "a quiet banner and the shelf becomes what is here" rather than a
 * spinner, and what lets the app open on a usable shelf before any sync runs.
 */
class LibraryRepository(
    private val db: MeedwellDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() / 1000 },
) {

    // ---------- Reading. Always from the database. ----------

    fun observeAlbums(sort: ShelfSort, scope: ShelfScope): Flow<List<Album>> {
        val source = when (scope) {
            ShelfScope.Everything -> when (sort) {
                ShelfSort.Artist -> db.albums().observeByArtist()
                ShelfSort.Recent -> db.albums().observeByRecent()
                ShelfSort.Title -> db.albums().observeByTitle()
                ShelfSort.MostPlayed -> db.albums().observeByMostPlayed()
            }
            // The scope filters live in the sort menu rather than competing
            // with the Albums, Artists and Genres view switcher.
            ShelfScope.OnThisPhone -> db.albums().observePresentLocally()
            ShelfScope.LocalOnly -> db.albums().observeLocalOnly()
        }
        return source.map { rows -> rows.map { it.toDomain() } }
    }

    fun observeAlbum(id: String): Flow<Album?> = db.albums().observeById(id).map { it?.toDomain() }

    fun observeTracks(albumId: String): Flow<List<Track>> =
        db.tracks().observeForAlbum(albumId).map { rows -> rows.map { it.toDomain() } }

    fun observeArtists(): Flow<List<Artist>> =
        db.artists().observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observeAlbumsByArtist(artistId: String): Flow<List<Album>> =
        db.albums().observeByArtistId(artistId).map { rows -> rows.map { it.toDomain() } }

    fun observeGenres(): Flow<List<Genre>> =
        db.genres().observeAll().map { rows -> rows.map { Genre(it.name, it.albumCount, it.songCount, it.durationSeconds) } }

    fun observeAlbumsByGenre(genre: String): Flow<List<Album>> =
        db.albums().observeByGenre(genre).map { rows -> rows.map { it.toDomain() } }

    fun observeNewestArrival(): Flow<Album?> = db.albums().observeNewest().map { it?.toDomain() }

    fun observeAlbumCount(): Flow<Int> = db.albums().observeCount()

    fun observePresentCount(): Flow<Int> = db.albums().observePresentCount()

    suspend fun tracksForAlbum(albumId: String): List<Track> =
        db.tracks().forAlbum(albumId).map { it.toDomain() }

    suspend fun track(id: String): Track? = db.tracks().byId(id)?.toDomain()

    suspend fun tracks(ids: List<String>): List<Track> {
        val byId = db.tracks().byIds(ids).associateBy { it.id }
        // Preserve the caller's order, which matters for a queue.
        return ids.mapNotNull { byId[it]?.toDomain() }
    }

    // ---------- Syncing ----------

    /**
     * Pulls the collection and writes it to the database.
     *
     * Sequential with no artificial delay and no parallel fan-out, which is the
     * pacing decision recorded during verification. Being an unremarkable
     * client of a service in open beta is both correct and in the app's
     * interest, and at roughly 200 ms a call, sequential is fast enough.
     *
     * Resumable by construction rather than by bookkeeping: every write is an
     * upsert keyed on the server's own id, so a sync killed halfway and started
     * again simply rewrites what it already wrote. Nothing duplicates.
     */
    suspend fun sync(client: SubsonicClient, onProgress: (SyncProgress) -> Unit = {}): SyncResult {
        val startedAt = clock()

        onProgress(SyncProgress(stage = SyncStage.Albums, done = 0, total = null))

        // Albums, paged. `offset` paging is confirmed working.
        val albums = mutableListOf<Album>()
        var offset = 0
        while (true) {
            when (val outcome = client.getAlbumList2(type = "newest", size = PAGE_SIZE, offset = offset)) {
                is SubsonicOutcome.Success -> {
                    val page = outcome.value.albumList2?.album.orEmpty().map { it.toDomain() }
                    albums += page
                    onProgress(SyncProgress(SyncStage.Albums, albums.size, null))
                    if (page.size < PAGE_SIZE) break
                    offset += PAGE_SIZE
                }
                else -> return SyncResult.Failed(outcome.describe())
            }
            // A guard against a server that never returns a short page. Without
            // it, a paging bug on their side becomes an infinite loop here.
            if (offset > MAX_ALBUMS) break
        }

        db.albums().upsertAll(albums.map { it.toEntity(lastSeenAt = startedAt) })

        // Tracks, one call per album. This is the expensive part of a sync and
        // the reason first sync on a large collection takes a while.
        var trackCount = 0
        albums.forEachIndexed { index, album ->
            when (val outcome = client.getAlbum(album.id)) {
                is SubsonicOutcome.Success -> {
                    val tracks = outcome.value.album?.song.orEmpty().map { it.toDomain() }
                    if (tracks.isNotEmpty()) {
                        // Local paths already discovered are preserved: the
                        // upsert must not wipe the link between a collection
                        // track and the file the user downloaded themselves.
                        val existing = db.tracks().forAlbum(album.id).associate { it.id to it.localPath }
                        db.tracks().upsertAll(
                            tracks.map { it.toEntity(localPath = existing[it.id]) }
                        )
                        trackCount += tracks.size
                    }
                }
                // One album failing does not abandon the sync. On a beta this
                // will happen, and losing the whole run over one record would
                // be the wrong trade.
                else -> Unit
            }
            onProgress(SyncProgress(SyncStage.Tracks, index + 1, albums.size))
        }

        // Artists.
        onProgress(SyncProgress(SyncStage.Artists, 0, null))
        when (val outcome = client.getArtists()) {
            is SubsonicOutcome.Success -> {
                val artists = outcome.value.artists?.toDomain().orEmpty()
                db.artists().upsertAll(artists.map { it.toEntity(lastSeenAt = startedAt) })
            }
            else -> Unit
        }

        // Genres. Replaced wholesale rather than upserted, because a genre that
        // no longer applies to any album should stop appearing rather than
        // linger with a stale count.
        onProgress(SyncProgress(SyncStage.Genres, 0, null))
        when (val outcome = client.getGenres()) {
            is SubsonicOutcome.Success -> {
                val genres = outcome.value.genres?.genre.orEmpty().map { it.toDomain() }
                db.genres().clear()
                db.genres().upsertAll(
                    genres.map { GenreEntity(it.name, it.albumCount, it.songCount, it.durationSeconds) }
                )
            }
            else -> Unit
        }

        // Loved. `getStarred2` does not exist on Bandcamp; `getStarred` does.
        when (val outcome = client.getStarred()) {
            is SubsonicOutcome.Success -> {
                val starred = outcome.value.starred
                starred?.song?.forEach { db.tracks().setStarred(it.id, true) }
                starred?.album?.forEach { db.albums().setStarred(it.id, true) }
            }
            else -> Unit
        }

        // Anything the collection no longer holds. Local-only albums are
        // excluded by the query: they were never in the collection, so a
        // collection sync has no business deleting them.
        db.tracks().deleteStaleFromCollection(startedAt)
        db.albums().deleteStaleFromCollection(startedAt)
        db.artists().deleteStale(startedAt)

        return SyncResult.Completed(albumCount = albums.size, trackCount = trackCount, at = clock())
    }

    /**
     * Recomputes how much of each album is present as files.
     *
     * Run after a scan and after reconciliation, because both change the answer.
     * Only rows whose count actually changed are written, so this does not wake
     * every observing screen on every pass.
     */
    suspend fun refreshLocalCounts() {
        db.albums().all().forEach { album ->
            val present = db.tracks().countPresentForAlbum(album.id)
            val origin = when {
                // A local-only album stays local-only. It was never in the
                // collection, so finding its files does not make it "owned"
                // in the sense of a purchase.
                album.origin == Origin.Local.name -> Origin.Local.name
                present > 0 -> Origin.Owned.name
                else -> Origin.Bandcamp.name
            }
            if (present != album.localTrackCount || origin != album.origin) {
                db.albums().setLocalTrackCount(album.id, present, origin)
            }
        }
    }

    companion object {
        /**
         * 500 per page. The real collection is three albums, so this is sized
         * for a collection nobody here has yet rather than for the test
         * account. Verification confirmed `offset` paging works.
         */
        const val PAGE_SIZE = 500

        /** A guard against a paging bug on the server becoming an infinite loop. */
        const val MAX_ALBUMS = 100_000
    }
}

enum class ShelfSort { Artist, Recent, Title, MostPlayed }

/**
 * The scope filters, which live in the sort menu rather than competing with the
 * Albums, Artists and Genres switcher.
 */
enum class ShelfScope { Everything, OnThisPhone, LocalOnly }

enum class SyncStage { Albums, Tracks, Artists, Genres }

data class SyncProgress(val stage: SyncStage, val done: Int, val total: Int?)

sealed interface SyncResult {
    data class Completed(val albumCount: Int, val trackCount: Int, val at: Long) : SyncResult
    data class Failed(val reason: SyncFailure) : SyncResult
}

/**
 * Why a sync failed, in terms the interface can speak.
 *
 * Distinguishing these is the difference between the Connection trouble screen
 * saying something true and saying something vague.
 */
sealed interface SyncFailure {
    /** HTTP 500 with an empty body, which is how Bandcamp rejects a login. */
    data object CredentialsRejected : SyncFailure
    data class Unreachable(val reason: String) : SyncFailure
    data class ServerSaid(val code: Int, val message: String) : SyncFailure
    data class Unreadable(val reason: String) : SyncFailure
}

private fun SubsonicOutcome<*>.describe(): SyncFailure = when (this) {
    is SubsonicOutcome.AuthRejected -> SyncFailure.CredentialsRejected
    is SubsonicOutcome.Unreachable -> SyncFailure.Unreachable(reason)
    is SubsonicOutcome.ServerError -> SyncFailure.ServerSaid(code, message)
    is SubsonicOutcome.XmlFailure -> SyncFailure.ServerSaid(code, message)
    is SubsonicOutcome.EndpointAbsent -> SyncFailure.Unreadable("this server does not offer $endpoint")
    is SubsonicOutcome.Unreadable -> SyncFailure.Unreadable(reason)
    is SubsonicOutcome.Success -> SyncFailure.Unreadable("no failure")
}

// ---------- Entity mapping ----------

internal fun Album.toEntity(lastSeenAt: Long) = AlbumEntity(
    id = id,
    name = name,
    artist = artist,
    artistId = artistId,
    coverArtId = coverArtId,
    year = year,
    trackCount = trackCount,
    durationSeconds = durationSeconds,
    addedAt = addedAt,
    genres = genres.joinToString(","),
    origin = origin.name,
    isStarred = isStarred,
    localTrackCount = localTrackCount,
    sortArtist = SortKeys.sortKey(artist),
    sortName = SortKeys.sortKey(name),
    lastSeenAt = lastSeenAt,
)

internal fun AlbumEntity.toDomain() = Album(
    id = id,
    name = name,
    artist = artist,
    artistId = artistId,
    coverArtId = coverArtId,
    year = year,
    trackCount = trackCount,
    durationSeconds = durationSeconds,
    addedAt = addedAt,
    genres = genres.split(",").filter { it.isNotBlank() },
    origin = runCatching { Origin.valueOf(origin) }.getOrDefault(Origin.Bandcamp),
    isStarred = isStarred,
    localTrackCount = localTrackCount,
)

internal fun Track.toEntity(localPath: String? = null) = TrackEntity(
    id = id,
    albumId = albumId,
    title = title,
    artist = artist,
    artistId = artistId,
    trackNumber = trackNumber,
    discNumber = discNumber,
    durationSeconds = durationSeconds,
    year = year,
    suffix = suffix,
    bitRate = bitRate,
    sizeBytes = sizeBytes,
    coverArtId = coverArtId,
    localPath = localPath ?: this.localPath,
    isStarred = isStarred,
    resumePositionSeconds = null,
)

internal fun TrackEntity.toDomain() = Track(
    id = id,
    albumId = albumId,
    title = title,
    artist = artist,
    artistId = artistId,
    trackNumber = trackNumber,
    discNumber = discNumber,
    durationSeconds = durationSeconds,
    year = year,
    suffix = suffix,
    bitRate = bitRate,
    sizeBytes = sizeBytes,
    coverArtId = coverArtId,
    localPath = localPath,
    isStarred = isStarred,
)

internal fun Artist.toEntity(lastSeenAt: Long) = ArtistEntity(
    id = id,
    name = name,
    albumCount = albumCount,
    coverArtId = coverArtId,
    imageUrl = imageUrl,
    sortName = SortKeys.sortKey(name),
    lastSeenAt = lastSeenAt,
)

internal fun ArtistEntity.toDomain() = Artist(
    id = id,
    name = name,
    albumCount = albumCount,
    coverArtId = coverArtId,
    imageUrl = imageUrl,
)
