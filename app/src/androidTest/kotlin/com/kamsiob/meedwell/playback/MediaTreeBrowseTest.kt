package com.kamsiob.meedwell.playback

import android.content.ComponentName
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kamsiob.meedwell.MeedwellApplication
import com.kamsiob.meedwell.data.db.AlbumEntity
import com.kamsiob.meedwell.data.db.TrackEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Walks the browse tree the way Android Auto walks it.
 *
 * **This is the test that means the car works**, and it needs no car. Auto is a
 * `MediaBrowser` connecting to this app's `MediaLibraryService` and asking for
 * children by id; so is this. What the head unit adds on top is Google's own
 * chrome, which is not this app's code and not this app's to prove.
 *
 * It runs against whatever is on the device, so it asserts the shape of the
 * tree rather than particular records: a shelf with nothing in it is a real
 * state and the test still has to pass on it.
 */
@RunWith(AndroidJUnit4::class)
class MediaTreeBrowseTest {

    private lateinit var browser: MediaBrowser

    @Before
    fun connect() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        seedOneRecord(context)
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        // Built on the main thread, because every MediaController method has to
        // be called from the thread the controller was made on, and Media3
        // throws rather than tolerating it.
        browser = onMain { MediaBrowser.Builder(context, token).buildAsync() }.get()
    }

    @After
    fun disconnect() {
        if (::browser.isInitialized) onMain { browser.release() }
    }

    /**
     * Puts one record on the shelf before anything is browsed.
     *
     * **Without this the interesting tests pass by doing nothing.** They walk
     * the albums node, find it empty, and return, which is green and proves
     * nothing at all. A test device has no Bandcamp account and no synced
     * library, so the fixture has to come from here. It is written straight to
     * the database, which is also what proves the tree reads locally rather
     * than reaching for the network.
     *
     * The track carries a `localPath`, so resolution has a real uri to produce
     * without a server: the file does not have to exist for the item to be
     * built, only for it to make sound.
     */
    private fun seedOneRecord(context: android.content.Context) {
        val container = (context as MeedwellApplication).container
        runBlocking {
            container.database.albums().upsertAll(
                listOf(
                    AlbumEntity(
                        id = "test-album",
                        name = "Nocturnes for a Small Room",
                        artist = "Bride Callanan",
                        artistId = "test-artist",
                        coverArtId = "test-cover",
                        year = 2026,
                        trackCount = 2,
                        durationSeconds = 600,
                        addedAt = 1_750_000_000_000,
                        genres = "",
                        origin = "Local",
                        isStarred = false,
                        localTrackCount = 2,
                        sortArtist = "callanan bride",
                        sortName = "nocturnes for a small room",
                        lastSeenAt = 1_750_000_000_000,
                    )
                )
            )
            container.database.tracks().upsertAll(
                (1..2).map { number ->
                    TrackEntity(
                        id = "test-track-$number",
                        albumId = "test-album",
                        albumName = "Nocturnes for a Small Room",
                        title = "Nocturne $number",
                        artist = "Bride Callanan",
                        artistId = "test-artist",
                        trackNumber = number,
                        discNumber = 1,
                        durationSeconds = 300,
                        year = 2026,
                        suffix = "flac",
                        bitRate = 0,
                        sizeBytes = 0,
                        coverArtId = "test-cover",
                        localPath = "/storage/emulated/0/Music/nocturne-$number.flac",
                        isStarred = false,
                        resumePositionSeconds = null,
                    )
                }
            )
        }
    }

    /** Runs a browser call on the main thread and hands back what it returned. */
    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result = runCatching(block)
        }
        return result!!.getOrThrow()
    }

    @Test
    fun theRootOffersTheFourWaysIn() {
        val root = onMain { browser.getLibraryRoot(null) }.get().value!!
        val children = onMain { browser.getChildren(root.mediaId, 0, 50, null) }.get().value!!

        assertEquals(
            listOf("Recent", "Albums", "Composers", "Lists"),
            children.map { it.mediaMetadata.title.toString() },
        )
        // Every root node is a folder to open, never something to play. A tap
        // on one of these must go somewhere rather than start sound.
        assertTrue(children.all { it.mediaMetadata.isBrowsable == true })
        assertTrue(children.all { it.mediaMetadata.isPlayable == false })
    }

    @Test
    fun everyAlbumCanBeOpenedAndPlayed() {
        val albums = onMain { browser.getChildren("meedwell:albums", 0, 50, null) }.get().value!!
        albums.forEach { album ->
            assertTrue("an album must be openable", album.mediaMetadata.isBrowsable == true)
            assertTrue("an album must be playable", album.mediaMetadata.isPlayable == true)
            assertTrue(album.mediaId.startsWith("meedwell:album:"))
        }
    }

    @Test
    fun openingAnAlbumGivesItsMovements() {
        val albums = onMain { browser.getChildren("meedwell:albums", 0, 50, null) }.get().value!!
        val first = albums.first { it.mediaId == "meedwell:album:test-album" }

        val tracks = onMain { browser.getChildren(first.mediaId, 0, 200, null) }.get().value!!
        assertEquals("both movements must be listed", 2, tracks.size)
        tracks.forEach { track ->
            assertTrue(track.mediaId.startsWith("meedwell:track:"))
            assertTrue("a movement must be playable", track.mediaMetadata.isPlayable == true)
            assertTrue("a movement is not a folder", track.mediaMetadata.isBrowsable == false)
        }
    }

    /**
     * The one that catches the failure nobody sees.
     *
     * A browse item carries an id and no uri. If the session cannot turn that
     * id back into something with audio behind it, the car looks like it
     * accepted the tap and then plays nothing at all, with no error anywhere.
     */
    @Test
    fun anAlbumIdResolvesToItemsThatCanActuallyPlay() {
        val albums = onMain { browser.getChildren("meedwell:albums", 0, 50, null) }.get().value!!
        val first = albums.first { it.mediaId == "meedwell:album:test-album" }

        val resolved = resolveOnMainThread(first)
        assertEquals("a tapped record must resolve to its whole programme", 2, resolved.size)
        assertTrue(
            "resolved items must carry playable audio",
            resolved.all { it.localConfiguration != null },
        )
    }

    @Test
    fun anUnknownIdIsAnErrorRatherThanACrash() {
        val result = onMain { browser.getItem("meedwell:album:no-such-record") }.get()
        assertTrue("an unknown id must report an error", result.resultCode != 0)
    }

    /**
     * Media3 requires the player to be driven from the main thread, so the
     * resolution is asked for there and waited on here.
     */
    private fun resolveOnMainThread(item: MediaItem): List<MediaItem> {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val done = java.util.concurrent.CountDownLatch(1)
        var got: List<MediaItem> = emptyList()
        handler.post {
            browser.setMediaItem(item)
            browser.prepare()
            handler.postDelayed({
                got = (0 until browser.mediaItemCount).map { browser.getMediaItemAt(it) }
                browser.stop()
                browser.clearMediaItems()
                done.countDown()
            }, 1_500)
        }
        done.await(15, java.util.concurrent.TimeUnit.SECONDS)
        return got
    }
}
