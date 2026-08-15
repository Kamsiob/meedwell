package com.kamsiob.meedwell.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.kamsiob.meedwell.core.backup.BackupFile
import com.kamsiob.meedwell.core.backup.BackupFolder
import com.kamsiob.meedwell.core.backup.BackupList
import com.kamsiob.meedwell.core.backup.BackupLocalFile
import com.kamsiob.meedwell.core.backup.BackupPlay
import com.kamsiob.meedwell.core.backup.BackupReader
import com.kamsiob.meedwell.core.backup.BackupResume
import com.kamsiob.meedwell.core.backup.BackupSettings
import com.kamsiob.meedwell.data.db.MeedwellDatabase
import com.kamsiob.meedwell.data.db.PlayEventEntity
import com.kamsiob.meedwell.data.db.PlaylistEntity
import com.kamsiob.meedwell.data.db.PlaylistTrackEntity
import com.kamsiob.meedwell.data.db.WatchedFolderEntity
import com.kamsiob.meedwell.ui.theme.ThemeChoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Export and restore.
 *
 * **Restore is the half that gets shipped untested**, and a restore that
 * half-works is worse than one that refuses: somebody believes their history is
 * back and finds the gaps months later. So this restores inside a single
 * database transaction and rolls the whole thing back on any failure. There is
 * no half import.
 *
 * **Replace, not merge.** Merging two divergent listening histories is
 * genuinely ambiguous, and guessing at it would produce a history that is
 * neither. The confirmation says so in words before anything is touched.
 *
 * The credentials are not written and are not read. A restored install asks for
 * them again, which is what lets an export be mailed to yourself without a
 * second thought.
 */
class BackupRepository(
    private val context: Context,
    private val db: MeedwellDatabase,
    private val settings: SettingsStore,
    private val versionName: String,
    private val clock: () -> Long = { System.currentTimeMillis() / 1000 },
) {

    /** What a restore did, in words the interface can show without editing. */
    sealed interface RestoreResult {
        data class Restored(
            val plays: Int,
            val loved: Int,
            val lists: Int,
            val relinked: Int,
            /** Sections in the file this version does not understand. */
            val notUnderstood: List<String>,
        ) : RestoreResult

        data class Refused(val message: String) : RestoreResult
    }

    // ---------- Writing ----------

    /** Everything this phone holds that is genuinely the user's, as a file. */
    suspend fun buildExport(): BackupFile = withContext(Dispatchers.IO) {
        val playlists = db.playlists().allWithTracks()
        BackupFile(
            writtenBy = "Meedwell $versionName",
            writtenAt = clock(),
            plays = db.playEvents().all().map {
                BackupPlay(it.trackId, it.albumId, it.playedAt, it.playedSeconds, it.completed)
            },
            lovedTracks = db.tracks().starredIds(),
            lovedAlbums = db.albums().starredIds(),
            resumePoints = db.tracks().resumePoints().mapNotNull { row ->
                row.resumePositionSeconds?.let { BackupResume(row.id, it) }
            },
            lists = playlists.map { (playlist, trackIds) ->
                BackupList(playlist.id, playlist.name, playlist.createdAt, playlist.updatedAt, trackIds)
            },
            localFiles = db.tracks().localPaths().mapNotNull { row ->
                row.localPath?.let { BackupLocalFile(row.id, it) }
            },
            watchedFolders = db.watchedFolders().all().map {
                BackupFolder(it.uri, it.displayName, it.addedAt)
            },
            settings = BackupSettings(
                theme = settings.theme.name,
                shelfGrid = settings.shelfGrid,
                gapless = settings.gapless,
                rememberLongTrackPosition = settings.rememberLongTrackPosition,
                wifiOnlyDownloads = settings.wifiOnlyDownloads,
                surroundingsVolume = settings.surroundingsVolume,
            ),
        )
    }

    /** Writes an export to a document the user chose. */
    suspend fun writeTo(uri: Uri): String? = withContext(Dispatchers.IO) {
        val text = BackupReader.write(buildExport())
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
                ?: error("no stream")
        }.exceptionOrNull()?.let {
            return@withContext "That file could not be written. Try somewhere else on this phone."
        }
        settings.lastBackupAt = clock()
        null
    }

    // ---------- Reading ----------

    /**
     * Replaces this phone's data with what is in a file.
     *
     * One transaction. Either all of it lands or none of it does, so an
     * interruption partway through cannot leave somebody with half a history
     * and no way to tell which half.
     */
    suspend fun restoreFrom(uri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
            ?: return@withContext RestoreResult.Refused(
                "That file could not be opened. It may have been moved or deleted."
            )

        val parsed = when (val result = BackupReader.read(text)) {
            is BackupReader.Result.Unreadable -> return@withContext RestoreResult.Refused(result.message)
            is BackupReader.Result.Ok -> result
        }
        val file = parsed.file

        var relinked = 0
        val failure = runCatching {
            db.withTransaction {
                // Replace, not merge. Stated in the confirmation before this
                // point is ever reached.
                db.playEvents().eraseAll()
                db.playEvents().insertAll(
                    file.plays.map {
                        PlayEventEntity(
                            trackId = it.trackId,
                            albumId = it.albumId,
                            playedAt = it.playedAt,
                            playedSeconds = it.playedSeconds,
                            completed = it.completed,
                        )
                    }
                )

                db.tracks().clearAllStarred()
                db.albums().clearAllStarred()
                file.lovedTracks.forEach { db.tracks().setStarred(it, true) }
                file.lovedAlbums.forEach { db.albums().setStarred(it, true) }

                db.tracks().clearAllResumePositions()
                file.resumePoints.forEach { db.tracks().setResumePosition(it.trackId, it.positionSeconds) }

                db.playlists().deleteAllLocal()
                file.lists.forEach { list ->
                    db.playlists().upsert(
                        PlaylistEntity(
                            id = list.id,
                            name = list.name,
                            createdAt = list.createdAt,
                            updatedAt = list.updatedAt,
                            fromBandcamp = false,
                        )
                    )
                    db.playlists().insertTracks(
                        list.trackIds.mapIndexed { index, trackId ->
                            PlaylistTrackEntity(playlistId = list.id, trackId = trackId, position = index)
                        }
                    )
                }

                // The folder is restored, but not its scan results: a scan
                // count from another phone would be a claim about this one.
                // Meedwell rescans and finds out for itself.
                db.watchedFolders().upsertAll(
                    file.watchedFolders.map {
                        WatchedFolderEntity(
                            uri = it.uri,
                            displayName = it.displayName,
                            addedAt = it.addedAt,
                            lastScannedAt = null,
                            trackCount = 0,
                        )
                    }
                )

                // Re-link files that are still where the export said they were.
                // A path that no longer resolves is simply left unlinked, which
                // is what makes restoring onto a different phone sensible
                // rather than broken: the shelf comes back, the music re-links
                // where it can, and a later scan finds the rest.
                file.localFiles.forEach { entry ->
                    if (entry.path.isNotBlank() && java.io.File(entry.path).isFile) {
                        db.tracks().setLocalPath(entry.trackId, entry.path)
                        relinked++
                    }
                }
            }
        }.exceptionOrNull()

        if (failure != null) {
            return@withContext RestoreResult.Refused(
                "Something went wrong partway through, so nothing was changed. Your data is as it was."
            )
        }

        applySettings(file.settings)

        RestoreResult.Restored(
            plays = file.plays.size,
            loved = file.lovedTracks.size + file.lovedAlbums.size,
            lists = file.lists.size,
            relinked = relinked,
            notUnderstood = parsed.unknownSections,
        )
    }

    /**
     * Settings are applied after the transaction, not inside it.
     *
     * They live in shared preferences rather than the database, so they cannot
     * take part in its transaction and pretending otherwise would be a lie
     * about atomicity. They are also the least costly thing to lose: a theme
     * that did not come back is a two-second fix, unlike a history.
     */
    private fun applySettings(from: BackupSettings) {
        runCatching { ThemeChoice.valueOf(from.theme) }.getOrNull()?.let { settings.theme = it }
        settings.shelfGrid = from.shelfGrid
        settings.gapless = from.gapless
        settings.rememberLongTrackPosition = from.rememberLongTrackPosition
        settings.wifiOnlyDownloads = from.wifiOnlyDownloads
        settings.surroundingsVolume = from.surroundingsVolume
    }

    /** A suggested filename, dated so successive exports do not overwrite. */
    fun suggestedFileName(): String {
        val stamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(clock() * 1000))
        return "meedwell-$stamp.json"
    }
}
