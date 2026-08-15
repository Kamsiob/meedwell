package com.kamsiob.meedwell.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * One SQLite database, plain and portable.
 *
 * **Deliberately not encrypted.** The standing Kamsiob template specifies an
 * encrypted database with a Keystore-held key, which is right for an app
 * holding personal records and wrong here. This holds an album catalog and a
 * play log, and it must stay readable as plain portable SQLite so a future
 * Linux desktop or web build can import it. Encrypting it would break the
 * portability contract in order to protect data that is not sensitive.
 *
 * Credentials are the one genuinely sensitive item and they are not in here at
 * all. See `CredentialStore`.
 *
 * The schema is exported to `app/schemas` and committed, so every migration is
 * reviewable in a diff rather than only inside the binary.
 */
@Database(
    entities = [
        AlbumEntity::class,
        TrackEntity::class,
        ArtistEntity::class,
        GenreEntity::class,
        PlayEventEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        WatchedFolderEntity::class,
        QueueItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MeedwellDatabase : RoomDatabase() {
    abstract fun albums(): AlbumDao
    abstract fun tracks(): TrackDao
    abstract fun artists(): ArtistDao
    abstract fun genres(): GenreDao
    abstract fun playEvents(): PlayEventDao
    abstract fun playlists(): PlaylistDao
    abstract fun queue(): QueueDao
    abstract fun watchedFolders(): WatchedFolderDao

    companion object {
        const val NAME = "meedwell.db"

        @Volatile
        private var instance: MeedwellDatabase? = null

        fun get(context: Context): MeedwellDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MeedwellDatabase::class.java,
                    NAME,
                )
                    // No fallbackToDestructiveMigration, ever. Losing a
                    // listening history to a schema change would be exactly the
                    // kind of quiet data loss this app exists to avoid. A
                    // missing migration should fail loudly in development
                    // rather than silently wipe a user's shelf.
                    .build()
                    .also { instance = it }
            }
    }
}
