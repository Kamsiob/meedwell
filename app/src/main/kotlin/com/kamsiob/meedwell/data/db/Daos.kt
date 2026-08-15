package com.kamsiob.meedwell.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Upsert
    suspend fun upsertAll(albums: List<AlbumEntity>)

    /**
     * The shelf, ordered by artist.
     *
     * `sortArtist` is stored rather than computed so the A to Z rail can build
     * its index once from the same order the list is in. Sorting by a computed
     * expression here would mean the rail and the list could disagree.
     */
    @Query("SELECT * FROM album ORDER BY sortArtist, sortName")
    fun observeByArtist(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM album ORDER BY addedAt DESC, sortName")
    fun observeByRecent(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM album ORDER BY sortName")
    fun observeByTitle(): Flow<List<AlbumEntity>>

    /**
     * Most played, computed on device from the play log.
     *
     * Deliberately **not** `getAlbumList2&type=frequent`, which Bandcamp
     * returns empty for. Computing it here is also simply better: it reflects
     * what the listener actually played in Meedwell rather than whatever
     * Bandcamp does or does not track. See issue #48.
     */
    @Query(
        """
        SELECT album.* FROM album
        LEFT JOIN (
            SELECT albumId, COUNT(*) AS plays FROM play_event GROUP BY albumId
        ) counts ON counts.albumId = album.id
        ORDER BY COALESCE(counts.plays, 0) DESC, album.sortArtist, album.sortName
        """
    )
    fun observeByMostPlayed(): Flow<List<AlbumEntity>>

    /** The Downloaded scope filter: albums whose files are all here. */
    @Query("SELECT * FROM album WHERE localTrackCount >= trackCount AND trackCount > 0 ORDER BY sortArtist, sortName")
    fun observePresentLocally(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM album WHERE origin = 'Local' ORDER BY sortArtist, sortName")
    fun observeLocalOnly(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM album WHERE id = :id")
    fun observeById(id: String): Flow<AlbumEntity?>

    @Query("SELECT * FROM album WHERE id = :id")
    suspend fun byId(id: String): AlbumEntity?

    @Query("SELECT * FROM album")
    suspend fun all(): List<AlbumEntity>

    /**
     * Search, done in the database rather than in Kotlin.
     *
     * `search3` exists on Bandcamp's API and searches the same collection this
     * table already holds, so calling it would mean a round trip and a spinner
     * to learn something already known. Local search is instant, works offline,
     * and makes "nothing about the search leaves the phone" literally true.
     */
    @Query("SELECT * FROM album WHERE name LIKE :like OR artist LIKE :like ORDER BY sortArtist, sortName LIMIT :limit")
    suspend fun search(like: String, limit: Int = 20): List<AlbumEntity>

    @Query("SELECT * FROM album WHERE artistId = :artistId ORDER BY year DESC, sortName")
    fun observeByArtistId(artistId: String): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM album ORDER BY addedAt DESC LIMIT 1")
    fun observeNewest(): Flow<AlbumEntity?>

    @Query("SELECT COUNT(*) FROM album")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM album WHERE localTrackCount >= trackCount AND trackCount > 0")
    fun observePresentCount(): Flow<Int>

    /** Genre filtering is a LIKE because genres are a comma separated list. */
    @Query("SELECT * FROM album WHERE ',' || genres || ',' LIKE '%,' || :genre || ',%' ORDER BY sortArtist, sortName")
    fun observeByGenre(genre: String): Flow<List<AlbumEntity>>

    @Query("UPDATE album SET localTrackCount = :count, origin = :origin WHERE id = :id")
    suspend fun setLocalTrackCount(id: String, count: Int, origin: String)

    @Query("UPDATE album SET isStarred = :starred WHERE id = :id")
    suspend fun setStarred(id: String, starred: Boolean)

    /**
     * Fills in an album's artist ID after the fact.
     *
     * Bandcamp's `getAlbumList2` returns an album's artist *name* but no
     * `artistId`, so every album arrived with an empty one. Nothing looked
     * broken, because the shelf sorts and groups on the name: the damage was
     * that `observeByArtistId` matched nothing, so every artist page showed an
     * artist with no records. `getAlbum` does carry the ID on each song, which
     * is where the sync gets it from.
     *
     * Guarded on being empty so a real ID is never overwritten by a stray one.
     */
    @Query("UPDATE album SET artistId = :artistId WHERE id = :id AND artistId = ''")
    suspend fun fillArtistId(id: String, artistId: String)

    /** Restore replaces rather than merges, so the old state is cleared first. */
    @Query("UPDATE album SET isStarred = 0")
    suspend fun clearAllStarred()

    /** Every starred album id, for an export. */
    @Query("SELECT id FROM album WHERE isStarred = 1")
    suspend fun starredIds(): List<String>

    @Query("SELECT * FROM album WHERE artistId = ''")
    suspend fun withoutArtistId(): List<AlbumEntity>

    /**
     * Removes anything the last full sync did not see.
     *
     * Local-only albums are excluded: they were never in the collection, so a
     * collection sync has no business deleting them. Getting this wrong would
     * wipe a local-files-only user's shelf on their first connect.
     */
    @Query("DELETE FROM album WHERE lastSeenAt < :before AND origin != 'Local'")
    suspend fun deleteStaleFromCollection(before: Long)
}

@Dao
interface TrackDao {

    @Upsert
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Query("SELECT * FROM track WHERE albumId = :albumId ORDER BY discNumber, trackNumber")
    fun observeForAlbum(albumId: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM track WHERE albumId = :albumId ORDER BY discNumber, trackNumber")
    suspend fun forAlbum(albumId: String): List<TrackEntity>

    @Query("SELECT * FROM track WHERE id = :id")
    suspend fun byId(id: String): TrackEntity?

    @Query("SELECT * FROM track WHERE id IN (:ids)")
    suspend fun byIds(ids: List<String>): List<TrackEntity>

    @Query("SELECT * FROM track WHERE isStarred = 1 ORDER BY artist, title")
    fun observeStarred(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM track WHERE title LIKE :like ORDER BY artist, title LIMIT :limit")
    suspend fun search(like: String, limit: Int = 50): List<TrackEntity>

    @Query("SELECT COUNT(*) FROM track WHERE albumId = :albumId AND localPath IS NOT NULL")
    suspend fun countPresentForAlbum(albumId: String): Int

    @Query("UPDATE track SET localPath = :path WHERE id = :id")
    suspend fun setLocalPath(id: String, path: String?)

    @Query("UPDATE track SET isStarred = :starred WHERE id = :id")
    suspend fun setStarred(id: String, starred: Boolean)

    /** Restore replaces rather than merges, so the old state is cleared first. */
    @Query("UPDATE track SET isStarred = 0")
    suspend fun clearAllStarred()

    @Query("UPDATE track SET resumePositionSeconds = NULL")
    suspend fun clearAllResumePositions()

    /** Every starred track id, for an export. */
    @Query("SELECT id FROM track WHERE isStarred = 1")
    suspend fun starredIds(): List<String>

    /** Every track that has a resume point, for an export. */
    @Query("SELECT id, resumePositionSeconds FROM track WHERE resumePositionSeconds IS NOT NULL")
    suspend fun resumePoints(): List<ResumeRow>

    /** Every track whose file is on this phone, for an export. */
    @Query("SELECT id, localPath FROM track WHERE localPath IS NOT NULL")
    suspend fun localPaths(): List<LocalPathRow>

    @Query("UPDATE track SET resumePositionSeconds = :seconds WHERE id = :id")
    suspend fun setResumePosition(id: String, seconds: Long?)

    /**
     * Clears local paths for files that are no longer where the database said.
     *
     * Reconciliation is a normal code path rather than an error path: the
     * property that makes owned files real also makes them deletable in the
     * Files app, and cards unmount.
     */
    @Query("UPDATE track SET localPath = NULL WHERE localPath IN (:paths)")
    suspend fun clearLocalPaths(paths: List<String>)

    @Query("SELECT localPath FROM track WHERE localPath IS NOT NULL")
    suspend fun allLocalPaths(): List<String>

    @Query("DELETE FROM track WHERE albumId IN (SELECT id FROM album WHERE lastSeenAt < :before AND origin != 'Local')")
    suspend fun deleteStaleFromCollection(before: Long)
}

@Dao
interface ArtistDao {
    @Upsert
    suspend fun upsertAll(artists: List<ArtistEntity>)

    @Query("SELECT * FROM artist ORDER BY sortName")
    fun observeAll(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artist WHERE id = :id")
    fun observeById(id: String): Flow<ArtistEntity?>

    @Query("SELECT * FROM artist WHERE name LIKE :like ORDER BY sortName LIMIT :limit")
    suspend fun search(like: String, limit: Int = 20): List<ArtistEntity>

    @Query("SELECT COUNT(*) FROM artist")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM artist WHERE lastSeenAt < :before")
    suspend fun deleteStale(before: Long)
}

@Dao
interface GenreDao {
    @Upsert
    suspend fun upsertAll(genres: List<GenreEntity>)

    @Query("SELECT * FROM genre WHERE albumCount > 0 ORDER BY albumCount DESC, name")
    fun observeAll(): Flow<List<GenreEntity>>

    @Query("DELETE FROM genre")
    suspend fun clear()
}

@Dao
interface PlayEventDao {
    /**
     * Append only. There is no update and there never will be: a play is a
     * recorded fact rather than a counter someone can adjust.
     */
    @Insert
    suspend fun record(event: PlayEventEntity)

    @Query("SELECT * FROM play_event ORDER BY playedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 500): Flow<List<PlayEventEntity>>

    /** Every play, oldest first, for an export. */
    @Query("SELECT * FROM play_event ORDER BY playedAt")
    suspend fun all(): List<PlayEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<PlayEventEntity>)

    /**
     * History, joined to the track and album it refers to.
     *
     * Done as one query rather than a play log plus N lookups, because a
     * listening history is exactly the table that grows without bound and a
     * per-row lookup would make the screen slower the longer somebody uses it.
     */
    @Query(
        """
        SELECT play_event.playedAt AS playedAt,
               play_event.id AS eventId,
               track.id AS trackId,
               track.title AS title,
               track.artist AS artist,
               album.id AS albumId,
               album.name AS albumName,
               album.coverArtId AS coverArtId
        FROM play_event
        JOIN track ON track.id = play_event.trackId
        JOIN album ON album.id = play_event.albumId
        ORDER BY play_event.playedAt DESC
        LIMIT :limit
        """
    )
    fun observeHistory(limit: Int = 400): Flow<List<HistoryRow>>

    @Query("SELECT COUNT(*) FROM play_event WHERE albumId = :albumId")
    suspend fun countForAlbum(albumId: String): Int

    @Query("SELECT MAX(playedAt) FROM play_event WHERE albumId = :albumId")
    suspend fun lastPlayedForAlbum(albumId: String): Long?

    /**
     * The Forgotten Shelf, computed entirely here.
     *
     * Never played, played at most twice ever, or quiet for long enough. No
     * algorithm, no feed, and nothing leaves the phone to produce it.
     */
    @Query(
        """
        SELECT album.* FROM album
        LEFT JOIN (
            SELECT albumId, COUNT(*) AS plays, MAX(playedAt) AS lastPlayed
            FROM play_event GROUP BY albumId
        ) counts ON counts.albumId = album.id
        WHERE COALESCE(counts.plays, 0) <= :playThreshold
           OR COALESCE(counts.lastPlayed, 0) < :quietBefore
        ORDER BY COALESCE(counts.lastPlayed, 0), album.sortArtist
        """
    )
    fun observeForgotten(playThreshold: Int, quietBefore: Long): Flow<List<AlbumEntity>>

    /** Erase listening history genuinely empties it rather than hiding it. */
    @Query("DELETE FROM play_event")
    suspend fun eraseAll()

    @Query("SELECT COUNT(*) FROM play_event")
    suspend fun count(): Int
}

/** One row of the history screen, already joined. */
/** One track's resume point, for an export. */
data class ResumeRow(
    val id: String,
    val resumePositionSeconds: Long?,
)

/** Where one track's file was found, for an export. */
data class LocalPathRow(
    val id: String,
    val localPath: String?,
)

data class HistoryRow(
    val eventId: Long,
    val playedAt: Long,
    val trackId: String,
    val title: String,
    val artist: String,
    val albumId: String,
    val albumName: String,
    val coverArtId: String,
)

@Dao
interface PlaylistDao {
    @Upsert
    suspend fun upsert(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlist ORDER BY fromBandcamp, name")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist WHERE id = :id")
    fun observeById(id: String): Flow<PlaylistEntity?>

    @Query("DELETE FROM playlist WHERE id = :id AND fromBandcamp = 0")
    suspend fun deleteLocal(id: String)

    /**
     * Clears every list this phone made, for a restore.
     *
     * Lists that came from Bandcamp are left alone: they are not this phone's
     * to replace, and the next sync would put them back anyway.
     */
    @Query("DELETE FROM playlist WHERE fromBandcamp = 0")
    suspend fun deleteAllLocal()

    @Query("SELECT * FROM playlist WHERE fromBandcamp = 0 ORDER BY name")
    suspend fun allLocal(): List<PlaylistEntity>

    @Query("SELECT trackId FROM playlist_track WHERE playlistId = :playlistId ORDER BY position")
    suspend fun trackIdsFor(playlistId: String): List<String>

    /** Each list with its tracks in order, for an export. */
    @Transaction
    suspend fun allWithTracks(): List<Pair<PlaylistEntity, List<String>>> =
        allLocal().map { it to trackIdsFor(it.id) }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(items: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_track WHERE playlistId = :playlistId")
    suspend fun clearTracks(playlistId: String)

    @Query(
        """
        SELECT track.* FROM track
        JOIN playlist_track ON playlist_track.trackId = track.id
        WHERE playlist_track.playlistId = :playlistId
        ORDER BY playlist_track.position
        """
    )
    fun observeTracks(playlistId: String): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM playlist_track WHERE playlistId = :playlistId")
    suspend fun trackCount(playlistId: String): Int

    @Transaction
    suspend fun replaceTracks(playlistId: String, trackIds: List<String>) {
        clearTracks(playlistId)
        insertTracks(trackIds.mapIndexed { i, id -> PlaylistTrackEntity(playlistId, id, i) })
    }
}

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue_item ORDER BY position")
    suspend fun all(): List<QueueItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QueueItemEntity>)

    @Query("DELETE FROM queue_item")
    suspend fun clear()

    @Transaction
    suspend fun replace(trackIds: List<String>) {
        clear()
        insertAll(trackIds.mapIndexed { i, id -> QueueItemEntity(i, id) })
    }
}

@Dao
interface WatchedFolderDao {
    @Upsert
    suspend fun upsert(folder: WatchedFolderEntity)

    @Query("SELECT * FROM watched_folder ORDER BY addedAt")
    fun observeAll(): Flow<List<WatchedFolderEntity>>

    @Query("SELECT * FROM watched_folder ORDER BY addedAt")
    suspend fun all(): List<WatchedFolderEntity>

    @Query("DELETE FROM watched_folder WHERE uri = :uri")
    suspend fun remove(uri: String)

    @Upsert
    suspend fun upsertAll(folders: List<WatchedFolderEntity>)
}
