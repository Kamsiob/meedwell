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

        /**
         * Set when the database on disk could not be opened and was set aside.
         *
         * Read by the interface so it can say what happened rather than leaving
         * somebody with an app that opens onto an empty shelf and no
         * explanation. Null in the ordinary case, which is every case.
         */
        @Volatile
        var setAsideFileName: String? = null
            private set

        fun get(context: Context): MeedwellDatabase =
            instance ?: synchronized(this) {
                instance ?: open(context.applicationContext).also { instance = it }
            }

        /**
         * Opens the database, and survives the one case where it cannot.
         *
         * **Still no `fallbackToDestructiveMigration`.** Losing a listening
         * history to a schema change is exactly the quiet data loss this app
         * exists to avoid, and that flag deletes and rebuilds without asking or
         * telling.
         *
         * But refusing to open is not enough on its own. Room throws on the
         * first query, which on this app is during startup, so a missing
         * migration means an install that crashes every time it is opened and
         * cannot be used, exported from, or fixed. There is no way out of that
         * from inside the app, and "clear app data" both loses everything and
         * is not something most people know to do. That is issue #49.
         *
         * So the failure is caught, and the old file is **renamed rather than
         * deleted**. The app starts on a fresh database, says plainly what
         * happened, and the old data is still sitting in the app's own folder
         * where a later version can read it. Nothing is destroyed to make the
         * app run again.
         */
        private fun open(context: Context): MeedwellDatabase {
            // Probed before Room is allowed near it. Android's own error
            // handler **deletes** a database it finds corrupt and carries on,
            // which is the silent data loss this whole design exists to
            // prevent, and it happens before any Room exception could be
            // caught. Opening read-only first does not trigger it.
            if (!isReadable(context)) return freshAfterSettingAside(context)

            val built = build(context)
            val opened = runCatching { built.openHelper.writableDatabase }
            if (opened.isSuccess) return built

            runCatching { built.close() }
            return freshAfterSettingAside(context)
        }

        /**
         * Whether the file on disk can be opened at all.
         *
         * Read-only and cheap: one pragma. A file that answers this is a
         * database, whatever Room later thinks of its schema.
         *
         * **The error handler is the entire point of this function.** Android's
         * default one deletes a database it finds corrupt, immediately, without
         * telling anybody, and it runs inside `openDatabase` itself. The first
         * version of this probe therefore destroyed the very file it existed to
         * rescue, and did it before a single line of recovery code ran. Passing
         * a handler that does nothing is what makes "check whether this is
         * readable" a question rather than an execution.
         */
        private fun isReadable(context: Context): Boolean {
            val file = context.getDatabasePath(NAME)
            if (!file.exists() || file.length() == 0L) return true
            return runCatching {
                android.database.sqlite.SQLiteDatabase.openDatabase(
                    file.path,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY,
                    // Deliberately does nothing. Never deletes.
                    { /* corrupt is a fact to report, not a file to remove */ },
                ).use { db ->
                    db.rawQuery("PRAGMA schema_version", null).use { it.moveToFirst() }
                }
                true
            }.getOrDefault(false)
        }

        /** Renames the unreadable database out of the way and starts a new one. */
        private fun freshAfterSettingAside(context: Context): MeedwellDatabase {
            val stamp = System.currentTimeMillis() / 1000
            val setAside = "$NAME.unreadable-$stamp"
            listOf("", "-wal", "-shm").forEach { suffix ->
                val from = context.getDatabasePath(NAME + suffix)
                if (from.exists()) {
                    // Renamed, never deleted. This is somebody's listening
                    // history, and the app failing to read it today does not
                    // make it worthless tomorrow.
                    from.renameTo(context.getDatabasePath(setAside + suffix))
                }
            }
            setAsideFileName = setAside
            return build(context)
        }

        private fun build(context: Context): MeedwellDatabase =
            Room.databaseBuilder(context, MeedwellDatabase::class.java, NAME).build()
    }
}
