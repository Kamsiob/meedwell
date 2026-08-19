package com.kamsiob.meedwell.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.common.MediaItem
import androidx.media3.session.LibraryResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.kamsiob.meedwell.MainActivity
import com.kamsiob.meedwell.MeedwellApplication
import com.kamsiob.meedwell.data.OkHttpSubsonicEngine
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * The playback service.
 *
 * A `MediaLibraryService` rather than a plain `MediaSessionService`, which
 * costs nothing today and is what makes Android Auto a small change later
 * rather than a rewrite.
 *
 * Three behaviors here are release blockers rather than polish, and each is
 * the kind of thing that is invisible when it works and infuriating when it
 * does not:
 *
 *  - **Audio focus, handled completely.** Playback pauses when headphones are
 *    unplugged, yields to calls, ducks for a navigation prompt, and resumes
 *    only after a transient loss, never after a permanent one.
 *  - **Media buttons work** on wired and Bluetooth headsets.
 *  - **Meedwell never auto-plays on Bluetooth connect.** A car or a speaker
 *    grabbing the session unprompted is the opposite of restraint, and it is
 *    the single most common way a music player annoys somebody. This is
 *    achieved by deliberately **not** implementing playback resumption; see
 *    the callback below.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private var player: ExoPlayer? = null
    private var session: MediaLibrarySession? = null
    private var tree: MediaTree? = null

    /**
     * Where the browse callbacks do their work.
     *
     * Every `MediaLibrarySession.Callback` method returns a future, and every
     * answer this app can give involves reading the database. Doing that on the
     * calling thread would block the car's browser; this scope is where it
     * happens instead, and it is cancelled with the service.
     */
    private val treeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        val container = (application as MeedwellApplication).container

        val httpFactory = OkHttpDataSource.Factory(streamingClient())
            .setUserAgent(OkHttpSubsonicEngine.USER_AGENT)

        // A DefaultDataSource wrapping the HTTP one, so the same player reads
        // a local file and a stream without the caller caring which it got.
        // That indifference is the whole point of Tier C: a track the user
        // downloaded from Bandcamp themselves plays exactly like a streamed one.
        val dataSourceFactory = DataSource.Factory {
            DefaultDataSource.Factory(this, httpFactory).createDataSource()
        }

        val exo = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                // true: let the player own audio focus. This is what yields to
                // a call, ducks for a navigation prompt, and resumes after a
                // transient loss but not a permanent one.
                true,
            )
            // The becoming-noisy broadcast: unplugging headphones pauses rather
            // than blasting the track out of the phone speaker.
            .setHandleAudioBecomingNoisy(true)
            // Gapless. Bandcamp streams are MP3, which carries gapless
            // information in its Xing or LAME header, and ExoPlayer honors it.
            .setSkipSilenceEnabled(false)
            .build()

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        tree = MediaTree(container)

        session = MediaLibrarySession.Builder(this, exo, LibraryCallback())
            .setSessionActivity(openApp)
            .build()

        player = exo
        QueuePersistence.attach(exo, container)
        // The play log. Everything that looks like intelligence on the shelf
        // reads this one table, and without it History, the Forgotten Shelf and
        // the most-played sort are all permanently empty.
        PlayLogger(container).attach(exo)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    /**
     * Deliberately minimal.
     *
     * `onPlaybackResumption` is **not** implemented. That callback is what
     * Android asks when a Bluetooth device connects or a media button is
     * pressed with no active session, and implementing it is what makes an app
     * start playing on its own when a car connects. Leaving it unimplemented is
     * the feature, not an omission, and this comment exists so that a future
     * session does not helpfully add it.
     */
    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val tree = tree ?: return errorFuture()
            val rootParams = LibraryParams.Builder().setExtras(tree.rootExtras()).build()
            return Futures.immediateFuture(LibraryResult.ofItem(tree.root(), rootParams))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = answering {
            LibraryResult.ofItemList(tree!!.children(parentId, page, pageSize), params)
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = answering {
            tree!!.item(mediaId)
                ?.let { LibraryResult.ofItem(it, null) }
                ?: LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
        }

        /**
         * Voice search. The results are computed here and announced, which is
         * the two step shape the API asks for: this call says how many there
         * are, and the browser then asks for them.
         */
        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> = answering {
            val found = tree!!.search(query, page = 0, pageSize = Int.MAX_VALUE)
            session.notifySearchResultChanged(browser, query, found.size, params)
            LibraryResult.ofVoid()
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = answering {
            LibraryResult.ofItemList(tree!!.search(query, page, pageSize), params)
        }

        /**
         * **What turns a tap in the car into sound.**
         *
         * The items arriving here carry a media id and nothing else: no uri, no
         * duration, no artwork. Without this the player is handed items it
         * cannot play and the car goes quiet with no error anybody sees. A
         * browsable id resolves to everything inside it, so tapping a record
         * plays the record rather than doing nothing.
         */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            val future = SettableFuture.create<MutableList<MediaItem>>()
            treeScope.launch {
                runCatching {
                    mediaItems.flatMap { item ->
                        // An item that already knows where its audio is came
                        // from this app rather than from the car, and is left
                        // exactly as it is.
                        if (item.localConfiguration != null) listOf(item)
                        else tree?.resolve(item.mediaId).orEmpty()
                    }.toMutableList()
                }.onSuccess(future::set).onFailure(future::setException)
            }
            return future
        }

        /** Runs a suspending answer and hands back the future the API wants. */
        private fun <T : Any> answering(block: suspend () -> LibraryResult<T>):
            ListenableFuture<LibraryResult<T>> {
            val future = SettableFuture.create<LibraryResult<T>>()
            treeScope.launch {
                runCatching { block() }
                    .onSuccess(future::set)
                    // A browse error is reported as one rather than thrown into
                    // the car's process, where it reads as this app crashing.
                    .onFailure { future.set(LibraryResult.ofError<T>(LibraryResult.RESULT_ERROR_UNKNOWN)) }
            }
            return future
        }

        private fun <T : Any> errorFuture(): ListenableFuture<LibraryResult<T>> =
            Futures.immediateFuture(
                LibraryResult.ofError<T>(LibraryResult.RESULT_ERROR_SESSION_DISCONNECTED)
            )
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping the app away with nothing playing should not leave a service
        // running. With something playing, the notification is the user's
        // control and the service stays.
        val current = player
        if (current == null || !current.playWhenReady || current.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        treeScope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        player = null
        super.onDestroy()
    }

    /**
     * A separate OkHttp client for audio.
     *
     * Streams are long-lived reads with different timeout needs from the short
     * metadata calls, and sharing one client would mean either metadata waits
     * too long or audio times out mid-track.
     */
    private fun streamingClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        // No call timeout: a call here is a whole track being read.
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()
}
