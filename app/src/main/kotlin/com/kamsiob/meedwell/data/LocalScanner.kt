package com.kamsiob.meedwell.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.kamsiob.meedwell.core.library.Matching
import com.kamsiob.meedwell.core.library.SortKeys
import com.kamsiob.meedwell.core.model.Origin
import com.kamsiob.meedwell.data.db.AlbumEntity
import com.kamsiob.meedwell.data.db.MeedwellDatabase
import com.kamsiob.meedwell.data.db.TrackEntity
import com.kamsiob.meedwell.data.db.WatchedFolderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans watched folders and matches what it finds onto the shelf.
 *
 * **This is the app's ownership story now.** Verification found that Bandcamp's
 * API will not release purchased files to any client, so a user downloads them
 * from Bandcamp themselves and this is what recognizes them. If this is wrong,
 * the whole positioning has nothing behind it.
 *
 * Two directions, both of which have to work:
 *
 *  - Files arriving after a collection sync, matched onto records already here.
 *  - Files that were on the phone **before** Meedwell was installed, which is
 *    likelier than it sounds: somebody who has been buying from Bandcamp for
 *    years already has a folder full of them.
 *
 * Anything that matches nothing becomes a local album, which is a first-class
 * thing rather than a failure.
 */
class LocalScanner(
    private val context: Context,
    private val db: MeedwellDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() / 1000 },
) {

    /**
     * Adds a folder and takes persistable permission on it.
     *
     * Persistable is the point: without it the grant dies with the process and
     * the user is asked again after every reboot, which would make watched
     * folders useless for the one job they have.
     */
    suspend fun addFolder(treeUri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            val name = DocumentFile.fromTreeUri(context, treeUri)?.name ?: treeUri.lastPathSegment.orEmpty()
            db.watchedFolders().upsert(
                WatchedFolderEntity(
                    uri = treeUri.toString(),
                    displayName = name,
                    addedAt = clock(),
                    lastScannedAt = null,
                    trackCount = 0,
                )
            )
            true
        }.getOrDefault(false)
    }

    suspend fun removeFolder(uri: String) = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(uri),
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        // Files under a folder that is no longer watched stop counting as here.
        val paths = db.tracks().allLocalPaths().filter { it.startsWith(uri) }
        if (paths.isNotEmpty()) db.tracks().clearLocalPaths(paths)
        db.watchedFolders().remove(uri)
    }

    /**
     * Walks every watched folder, matches, and writes the result.
     *
     * Reconciliation happens first and unconditionally: a file the database
     * believes in that is no longer there stops being counted before anything
     * new is added. The property that makes owned files real also makes them
     * deletable in the Files app, so this is a normal code path rather than an
     * error path.
     */
    suspend fun scan(onProgress: (ScanProgress) -> Unit = {}): ScanResult = withContext(Dispatchers.IO) {
        val folders = db.watchedFolders().all()
        if (folders.isEmpty()) return@withContext ScanResult(0, 0, 0, 0)

        onProgress(ScanProgress(ScanStage.Reconciling, 0, null))
        val missing = reconcile()

        onProgress(ScanProgress(ScanStage.Reading, 0, null))
        val found = mutableListOf<LocalAudioFile>()
        folders.forEach { folder ->
            val root = DocumentFile.fromTreeUri(context, Uri.parse(folder.uri))
            if (root != null && root.isDirectory) {
                collectAudio(root, found, onFound = { onProgress(ScanProgress(ScanStage.Reading, found.size, null)) })
            }
        }

        onProgress(ScanProgress(ScanStage.Matching, 0, found.size))
        val collection = db.tracks().let { dao ->
            db.albums().all().flatMap { album ->
                dao.forAlbum(album.id).map { track ->
                    Matching.CollectionTrack(
                        id = track.id,
                        albumId = track.albumId,
                        artist = track.artist,
                        album = track.albumName.ifBlank { album.name },
                        title = track.title,
                    )
                }
            }
        }

        val result = Matching.match(
            collection = collection,
            files = found.map {
                Matching.LocalFile(
                    artist = it.artist,
                    album = it.album,
                    title = it.title,
                    payload = it,
                )
            },
        )

        result.matches.forEach { match ->
            db.tracks().setLocalPath(match.track.id, match.file.payload.uri)
        }

        val localAlbums = createLocalAlbums(result.unmatchedFiles.map { it.payload })

        // Counts are recomputed rather than incremented, so a scan that runs
        // twice cannot inflate them.
        LibraryRepository(db, clock).refreshLocalCounts()

        folders.forEach { folder ->
            db.watchedFolders().upsert(folder.copy(lastScannedAt = clock(), trackCount = found.size))
        }

        ScanResult(
            filesFound = found.size,
            matched = result.matches.size,
            localOnly = localAlbums,
            wentMissing = missing,
        )
    }

    /**
     * Detects files the database believes in that are no longer readable.
     *
     * Checked by actually opening each one rather than by trusting the
     * database, because a card can be unmounted and a file can be deleted in
     * another app at any moment. Meedwell marks those albums as not present and
     * changes nothing else.
     */
    private suspend fun reconcile(): Int {
        val known = db.tracks().allLocalPaths()
        if (known.isEmpty()) return 0
        val gone = known.filter { path ->
            runCatching {
                val uri = Uri.parse(path)
                val doc = DocumentFile.fromSingleUri(context, uri)
                doc == null || !doc.exists()
            }.getOrDefault(true)
        }
        if (gone.isNotEmpty()) db.tracks().clearLocalPaths(gone)
        return gone.size
    }

    private fun collectAudio(
        dir: DocumentFile,
        into: MutableList<LocalAudioFile>,
        onFound: () -> Unit,
        depth: Int = 0,
    ) {
        // A depth guard, because a symlinked or self-referential tree would
        // otherwise walk forever.
        if (depth > MAX_DEPTH) return
        dir.listFiles().forEach { entry ->
            when {
                entry.isDirectory -> collectAudio(entry, into, onFound, depth + 1)
                entry.isFile && entry.name?.substringAfterLast('.', "")?.lowercase() in AUDIO_EXTENSIONS -> {
                    readTags(entry)?.let {
                        into += it
                        onFound()
                    }
                }
            }
        }
    }

    /**
     * Reads tags, preferring `albumArtist` over `artist`.
     *
     * That preference is load bearing rather than a nicety: reading `artist`
     * first shatters a compilation into one album per track, which is the
     * single most common way a local library looks broken.
     *
     * A file with no usable tags falls back to its folder name and file name,
     * which is what a person would call it anyway.
     */
    private fun readTags(file: DocumentFile): LocalAudioFile? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, file.uri)
            fun tag(key: Int) = retriever.extractMetadata(key)?.trim().orEmpty()

            val albumArtist = tag(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            val artist = tag(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = tag(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val title = tag(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val fileName = file.name?.substringBeforeLast('.').orEmpty()

            LocalAudioFile(
                uri = file.uri.toString(),
                // albumArtist first. This is the compilation rule.
                artist = albumArtist.ifBlank { artist }.ifBlank { UNKNOWN_ARTIST },
                trackArtist = artist.ifBlank { albumArtist },
                album = album.ifBlank { file.parentFile?.name.orEmpty() }.ifBlank { LOOSE_TRACKS },
                title = title.ifBlank { fileName },
                trackNumber = tag(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                    .substringBefore('/').toIntOrNull() ?: 0,
                discNumber = tag(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
                    .substringBefore('/').toIntOrNull() ?: 1,
                year = tag(MediaMetadataRetriever.METADATA_KEY_YEAR).take(4).toIntOrNull() ?: 0,
                durationSeconds = (tag(MediaMetadataRetriever.METADATA_KEY_DURATION).toLongOrNull() ?: 0L) / 1000,
                suffix = file.name?.substringAfterLast('.', "")?.lowercase().orEmpty(),
                sizeBytes = file.length(),
            )
        } catch (t: Throwable) {
            // A corrupt or truncated file is skipped rather than failing the
            // scan. On a folder somebody has been filling for years there will
            // be at least one.
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** Builds albums for files that matched nothing in the collection. */
    private suspend fun createLocalAlbums(files: List<LocalAudioFile>): Int {
        if (files.isEmpty()) return 0
        val grouped = files.groupBy { it.artist to it.album }
        grouped.forEach { (key, tracks) ->
            val (artist, album) = key
            val albumId = localAlbumId(artist, album)
            val now = clock()
            db.albums().upsertAll(
                listOf(
                    AlbumEntity(
                        id = albumId,
                        name = album,
                        artist = artist,
                        artistId = "",
                        coverArtId = "",
                        year = tracks.firstNotNullOfOrNull { it.year.takeIf { y -> y > 0 } } ?: 0,
                        trackCount = tracks.size,
                        durationSeconds = tracks.sumOf { it.durationSeconds },
                        addedAt = now,
                        genres = "",
                        origin = Origin.Local.name,
                        isStarred = false,
                        localTrackCount = tracks.size,
                        sortArtist = SortKeys.sortKey(artist),
                        sortName = SortKeys.sortKey(album),
                        lastSeenAt = now,
                    )
                )
            )
            db.tracks().upsertAll(
                tracks.sortedWith(compareBy({ it.discNumber }, { it.trackNumber })).map { file ->
                    TrackEntity(
                        id = localTrackId(file.uri),
                        albumId = albumId,
                        albumName = album,
                        title = file.title,
                        // The performer, not the album artist, so a compilation
                        // still shows who played each track.
                        artist = file.trackArtist.ifBlank { artist },
                        artistId = "",
                        trackNumber = file.trackNumber,
                        discNumber = file.discNumber,
                        durationSeconds = file.durationSeconds,
                        year = file.year,
                        suffix = file.suffix,
                        bitRate = 0,
                        sizeBytes = file.sizeBytes,
                        coverArtId = "",
                        localPath = file.uri,
                        isStarred = false,
                        resumePositionSeconds = null,
                    )
                }
            )
        }
        return grouped.size
    }

    private companion object {
        val AUDIO_EXTENSIONS = setOf("flac", "mp3", "m4a", "aac", "ogg", "opus", "wav", "aiff", "aif", "wma")
        const val MAX_DEPTH = 12
        const val UNKNOWN_ARTIST = "Unknown artist"

        /**
         * Loose singles go to a plainly labeled bucket rather than having an
         * album name invented for them.
         */
        const val LOOSE_TRACKS = "Loose tracks"

        fun localAlbumId(artist: String, album: String) = "local:album:${(artist + "/" + album).hashCode()}"
        fun localTrackId(uri: String) = "local:track:${uri.hashCode()}"
    }
}

data class LocalAudioFile(
    val uri: String,
    val artist: String,
    val trackArtist: String,
    val album: String,
    val title: String,
    val trackNumber: Int,
    val discNumber: Int,
    val year: Int,
    val durationSeconds: Long,
    val suffix: String,
    val sizeBytes: Long,
)

enum class ScanStage { Reconciling, Reading, Matching }

data class ScanProgress(val stage: ScanStage, val done: Int, val total: Int?)

data class ScanResult(
    val filesFound: Int,
    val matched: Int,
    val localOnly: Int,
    val wentMissing: Int,
)
