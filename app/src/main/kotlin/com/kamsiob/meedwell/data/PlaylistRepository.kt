package com.kamsiob.meedwell.data

import com.kamsiob.meedwell.core.model.Track
import com.kamsiob.meedwell.data.db.MeedwellDatabase
import com.kamsiob.meedwell.data.db.PlaylistEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Lists: making them, changing them, and getting rid of them.
 *
 * **Local, and honest about it.** Verification found Bandcamp's Subsonic API
 * implements no way to create, change or delete a playlist, so nothing here
 * travels to the account. Lists that arrived from Bandcamp are shown and played
 * but never edited, which the interface says in words rather than by disabling a
 * button and leaving somebody to work out why.
 *
 * **Every change stamps `updatedAt`.** That field is not decoration: it is the
 * merge key a second device resolves on, and `ARCHITECTURE.md` names last write
 * wins per list as the rule. A rename that forgot to touch it would be a rename
 * that silently lost to an older copy of the same list.
 */
class PlaylistRepository(
    private val db: MeedwellDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() / 1000 },
) {

    fun observeAll(): Flow<List<PlaylistEntity>> = db.playlists().observeAll()

    fun observe(id: String): Flow<PlaylistEntity?> = db.playlists().observeById(id)

    fun observeTracks(id: String): Flow<List<Track>> =
        db.playlists().observeTracks(id).map { rows -> rows.map { it.toDomain() } }

    /**
     * Makes a list and returns its id.
     *
     * The id is a UUID rather than a row number so that two devices can each
     * make a list without agreeing first, which is what makes merging possible
     * at all.
     */
    suspend fun create(name: String): String {
        val now = clock()
        val id = "local:${UUID.randomUUID()}"
        db.playlists().upsert(
            PlaylistEntity(
                id = id,
                name = name.trim().ifBlank { "New list" },
                createdAt = now,
                updatedAt = now,
                fromBandcamp = false,
            )
        )
        return id
    }

    suspend fun rename(id: String, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        db.playlists().rename(id, clean, clock())
    }

    /** Only ever a local list. A Bandcamp one is not this phone's to delete. */
    suspend fun delete(id: String) = db.playlists().deleteLocal(id)

    suspend fun addTrack(playlistId: String, trackId: String) {
        db.playlists().appendTrack(playlistId, trackId)
        db.playlists().touch(playlistId, clock())
    }

    /** Adds a whole album, in running order. */
    suspend fun addAlbum(playlistId: String, albumId: String) {
        db.tracks().forAlbum(albumId).forEach { db.playlists().appendTrack(playlistId, it.id) }
        db.playlists().touch(playlistId, clock())
    }

    suspend fun removeAt(playlistId: String, position: Int) {
        db.playlists().removeAt(playlistId, position)
        db.playlists().touch(playlistId, clock())
    }

    suspend fun move(playlistId: String, from: Int, to: Int) {
        db.playlists().move(playlistId, from, to)
        db.playlists().touch(playlistId, clock())
    }

    suspend fun trackCount(playlistId: String): Int = db.playlists().trackCount(playlistId)

    suspend fun trackIds(playlistId: String): List<String> = db.playlists().trackIdsFor(playlistId)
}
