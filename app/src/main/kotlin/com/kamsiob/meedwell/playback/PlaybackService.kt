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
 * Three behaviours here are release blockers rather than polish, and each is
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
            // information in its Xing or LAME header, and ExoPlayer honours it.
            .setSkipSilenceEnabled(false)
            .build()

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

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
    private inner class LibraryCallback : MediaLibrarySession.Callback

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
