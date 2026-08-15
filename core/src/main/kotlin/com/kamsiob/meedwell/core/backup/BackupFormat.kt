package com.kamsiob.meedwell.core.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The Meedwell export file, version one.
 *
 * **Versioned from release one**, so a file written today can be migrated
 * cleanly by an app written years from now. The version is the first field and
 * is checked before anything else is read.
 *
 * **What it carries:** the listening history, hearts and lists made on this
 * phone, resume points on long pieces, every setting, and where local files
 * were found. That last one is a map rather than the files themselves, so a
 * restore can re-find the music instead of re-fetching it.
 *
 * **What it does not carry:** the audio, and the credentials.
 *
 * The audio is left out because it is already the user's, sitting in a folder
 * any app can read; copying gigabytes into a backup of a play log would be an
 * odd thing to do to somebody's storage. The app says so on the screen rather
 * than leaving it to be discovered.
 *
 * The credentials are left out because they are the one genuinely sensitive
 * thing the app holds. They live in encrypted storage, are never written to the
 * database, and must not be laundered into a plain file by way of a backup. A
 * restored install asks for them again, which is a small cost for the guarantee
 * that an export can be emailed to yourself without thinking about it.
 *
 * The file is **not encrypted**, deliberately, and says as much. It carries no
 * secrets, and encrypting it would break the portability that is the entire
 * reason it exists.
 */
@Serializable
data class BackupFile(
    /** Always 1 for files this version writes. Checked before anything else. */
    @SerialName("format_version") val formatVersion: Int = FORMAT_VERSION,
    /** Which app wrote it, for a human reading the file in a text editor. */
    @SerialName("written_by") val writtenBy: String = "",
    /** Seconds since the epoch. */
    @SerialName("written_at") val writtenAt: Long = 0,
    /**
     * A plain sentence at the top of the file saying what is and is not in it.
     *
     * Written into the data rather than only shown in the app, because the file
     * outlives the screen that made it and somebody may open it years later in
     * something that is not Meedwell.
     */
    @SerialName("note") val note: String = DEFAULT_NOTE,

    @SerialName("plays") val plays: List<BackupPlay> = emptyList(),
    @SerialName("loved_tracks") val lovedTracks: List<String> = emptyList(),
    @SerialName("loved_albums") val lovedAlbums: List<String> = emptyList(),
    @SerialName("resume_points") val resumePoints: List<BackupResume> = emptyList(),
    @SerialName("lists") val lists: List<BackupList> = emptyList(),
    @SerialName("local_files") val localFiles: List<BackupLocalFile> = emptyList(),
    @SerialName("watched_folders") val watchedFolders: List<BackupFolder> = emptyList(),
    @SerialName("settings") val settings: BackupSettings = BackupSettings(),
) {
    companion object {
        const val FORMAT_VERSION = 1

        const val DEFAULT_NOTE =
            "This is a Meedwell export. It holds your listening history, your hearts and lists, " +
                "your resume points, your settings, and where your local music files were found. " +
                "It does not hold the music itself, which is already yours and stays where it is. " +
                "It does not hold your Bandcamp credentials."
    }
}

/** One play, from the append-only log the forgotten shelf is built on. */
@Serializable
data class BackupPlay(
    @SerialName("track_id") val trackId: String = "",
    @SerialName("album_id") val albumId: String = "",
    @SerialName("played_at") val playedAt: Long = 0,
    @SerialName("played_seconds") val playedSeconds: Long = 0,
    @SerialName("completed") val completed: Boolean = false,
)

/** Where a long piece was left. */
@Serializable
data class BackupResume(
    @SerialName("track_id") val trackId: String = "",
    @SerialName("position_seconds") val positionSeconds: Long = 0,
)

@Serializable
data class BackupList(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0,
    /** Track ids in order. Order is the whole point of a list. */
    @SerialName("track_ids") val trackIds: List<String> = emptyList(),
)

/**
 * Where one track's file was found.
 *
 * The path rather than the file. A restore uses it to re-link music that is
 * still on the phone, and simply leaves the link empty where it is not, which
 * is what makes a restore onto a different device sensible rather than broken.
 */
@Serializable
data class BackupLocalFile(
    @SerialName("track_id") val trackId: String = "",
    @SerialName("path") val path: String = "",
)

@Serializable
data class BackupFolder(
    @SerialName("uri") val uri: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("added_at") val addedAt: Long = 0,
)

@Serializable
data class BackupSettings(
    @SerialName("theme") val theme: String = "",
    @SerialName("shelf_grid") val shelfGrid: Boolean = true,
    @SerialName("gapless") val gapless: Boolean = true,
    @SerialName("remember_long_track_position") val rememberLongTrackPosition: Boolean = true,
    @SerialName("wifi_only_downloads") val wifiOnlyDownloads: Boolean = true,
    @SerialName("surroundings_volume") val surroundingsVolume: Float = 0.6f,
)
