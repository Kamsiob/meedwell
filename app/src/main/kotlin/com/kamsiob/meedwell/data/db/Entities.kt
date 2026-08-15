package com.kamsiob.meedwell.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The database schema, which is the app's **public data contract** from version
 * one and is documented in `ARCHITECTURE.md` rather than left to be reverse
 * engineered. The app is AGPL-3.0, so the schema is public regardless;
 * documenting it deliberately is what lets a future desktop or web build
 * interoperate.
 *
 * **Not encrypted, deliberately.** This holds an album catalog and a play
 * log. Encrypting it would break the portability contract in order to protect
 * data that is not sensitive. Credentials are the one genuinely sensitive item
 * and they never come near this file: they live in EncryptedSharedPreferences
 * alone. See `DECISIONS.md`.
 *
 * Schema files are exported to `app/schemas` and committed, so a migration is
 * reviewable in a diff.
 */

@Entity(
    tableName = "album",
    indices = [Index("artistId"), Index("sortArtist"), Index("addedAt")],
)
data class AlbumEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artist: String,
    val artistId: String,
    val coverArtId: String,
    val year: Int,
    val trackCount: Int,
    val durationSeconds: Long,
    /** Purchase date on Bandcamp. Null when the API sent a date we could not read. */
    val addedAt: Long?,
    /** Comma separated, already deduplicated by the mapper. */
    val genres: String,
    /** `Bandcamp`, `Local` or `Owned`. See `Origin` in `:core`. */
    val origin: String,
    val isStarred: Boolean,
    /** How many of this album's tracks were found as files. Never rounded up. */
    val localTrackCount: Int,
    /**
     * The artist name lowercased with a leading article removed, so that
     * "The Long Static" sorts under L and the A to Z rail agrees with what a
     * person expects. Stored rather than computed, because the fast scroller
     * builds its index once and must not recompute on scroll.
     */
    val sortArtist: String,
    val sortName: String,
    /** When this row was last confirmed present in the collection during a sync. */
    val lastSeenAt: Long,
)

@Entity(
    tableName = "track",
    indices = [Index("albumId"), Index("artistId"), Index("localPath")],
)
data class TrackEntity(
    @PrimaryKey val id: String,
    val albumId: String,
    val albumName: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val trackNumber: Int,
    val discNumber: Int,
    val durationSeconds: Long,
    val year: Int,
    val suffix: String,
    val bitRate: Int,
    val sizeBytes: Long,
    val coverArtId: String,
    /**
     * Where the file for this track sits, when one has been found. Playback
     * prefers it over streaming. Null is the normal case, not an error.
     */
    val localPath: String?,
    val isStarred: Boolean,
    /**
     * Where the listener got to in a long piece. Only kept for pieces over
     * twenty minutes, which is what the Settings toggle governs.
     */
    val resumePositionSeconds: Long?,
)

@Entity(tableName = "artist", indices = [Index("sortName")])
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val albumCount: Int,
    val coverArtId: String,
    val imageUrl: String,
    val sortName: String,
    val lastSeenAt: Long,
)

@Entity(tableName = "genre")
data class GenreEntity(
    @PrimaryKey val name: String,
    val albumCount: Int,
    val songCount: Int,
    val durationSeconds: Long,
)

/**
 * The append-only play log.
 *
 * Append-only is the whole point. It powers History, the Forgotten Shelf and
 * the most-played sort, all computed on device with no network call and no
 * algorithm. Rows are never updated in place, so a play is a recorded fact
 * rather than a counter. Erase listening history deletes rows; nothing else
 * ever writes to them.
 */
@Entity(
    tableName = "play_event",
    indices = [Index("trackId"), Index("albumId"), Index("playedAt")],
)
data class PlayEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val albumId: String,
    val playedAt: Long,
    /** How far the listener actually got, so a skip is not counted as a play. */
    val playedSeconds: Long,
    val completed: Boolean,
)

/**
 * A list.
 *
 * Local because Bandcamp's API offers no way to create, edit or delete a
 * playlist. [fromBandcamp] marks the ones the account already had, which are
 * shown and not editable.
 */
@Entity(tableName = "playlist")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val fromBandcamp: Boolean,
)

@Entity(
    tableName = "playlist_track",
    primaryKeys = ["playlistId", "position"],
    indices = [Index("playlistId"), Index("trackId")],
)
data class PlaylistTrackEntity(
    val playlistId: String,
    val trackId: String,
    val position: Int,
)

/**
 * A folder the user asked Meedwell to watch.
 *
 * Load bearing rather than a convenience: verification found Bandcamp's API
 * will not release purchased files, so this is how owned music reaches the
 * shelf at all. Stored as a Storage Access Framework tree URI with persisted
 * permission, so it survives reboots without asking again.
 */
@Entity(tableName = "watched_folder")
data class WatchedFolderEntity(
    @PrimaryKey val uri: String,
    val displayName: String,
    val addedAt: Long,
    val lastScannedAt: Long?,
    val trackCount: Int,
)

/**
 * The queue, persisted.
 *
 * A release blocker rather than polish: the queue, the current track and the
 * position have to survive process death and reboot, landing back on the same
 * queue paused where it left off, with no spinner and no re-sync first.
 */
@Entity(tableName = "queue_item", indices = [Index("position")])
data class QueueItemEntity(
    @PrimaryKey val position: Int,
    val trackId: String,
)
