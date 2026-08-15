package com.kamsiob.meedwell.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.kamsiob.meedwell.core.surroundings.LoopMix
import com.kamsiob.meedwell.core.surroundings.SurroundingsSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * The ambience under the music.
 *
 * **Two players, not one on repeat.** A single player set to loop plays a file
 * to its last sample and restarts from its first, which joins two unrelated
 * points in a waveform and clicks. R1 requires a two to three second crossfade
 * instead, and a crossfade needs both ends audible at once, which needs two
 * players. The second one starts before the first finishes and they trade
 * places, over and over, for as long as somebody wants a fire going.
 *
 * **Separate from the music player on purpose.** This runs alongside whatever
 * is on the shelf rather than instead of it, so it is a second ExoPlayer with
 * its own audio attributes. It does not take audio focus and it does not
 * duck: a bed that ducks under the thing it is meant to sit under is a bed that
 * pulses with the music. When something else takes focus properly, such as a
 * call, the system pauses it with everything else.
 *
 * The fade is driven from a coroutine ticking every 50 ms rather than from a
 * sample-accurate processor. The fade is two and a half seconds long and both
 * curves are smooth, so a 50 ms granularity is a fiftieth of the shortest
 * segment and inaudible, and it keeps the whole mechanism in ordinary code
 * where it can be read.
 */
@UnstableApi
class SurroundingsPlayer(
    private val context: Context,
    private val settings: com.kamsiob.meedwell.data.SettingsStore,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var fadeJob: Job? = null

    private var playerA: ExoPlayer? = null
    private var playerB: ExoPlayer? = null

    /** Which of the two is currently the one in front. */
    private var leadIsA = true

    private var current: SurroundingsSound? = null
    private var currentFile: File? = null

    /**
     * The recording this player currently holds, or null for none.
     *
     * Exposed because the interface remembers a bed across a restart and shows
     * it paused, while the player itself starts empty. Without this the caller
     * cannot tell "paused" from "never loaded", and asking a player with
     * nothing in it to resume does nothing at all: the bar's play button was
     * dead every time the app was reopened.
     */
    val loadedId: String? get() = current?.id

    private val _state = MutableStateFlow(SurroundingsState())
    val state: StateFlow<SurroundingsState> = _state.asStateFlow()

    /**
     * Starts a recording, or swaps to a different one.
     *
     * There is no queue and no skip: this is one sound at a time, held for as
     * long as it is wanted.
     */
    fun play(sound: SurroundingsSound, file: File) {
        if (current?.id == sound.id && _state.value.isPlaying) return
        stopInternal()

        current = sound
        currentFile = file
        leadIsA = true

        val lead = newPlayer().also { playerA = it }
        playerB = newPlayer()

        lead.setMediaItem(MediaItem.fromUri(file.toURI().toString()))
        lead.prepare()
        lead.volume = 1f
        lead.play()

        settings.surroundingsSoundId = sound.id
        _state.value = SurroundingsState(
            soundId = sound.id,
            displayName = sound.displayName,
            recordist = sound.attribution.recordistName,
            isPlaying = true,
            volume = settings.surroundingsVolume,
            limiterEngaged = LoopMix.limiterEngages(
                sound,
                LoopMix.playbackGainDb(sound, settings.surroundingsVolume.toDouble()),
            ),
        )
        startFadeLoop(sound)
    }

    fun pause() {
        playerA?.pause()
        playerB?.pause()
        _state.value = _state.value.copy(isPlaying = false)
    }

    fun resume() {
        val sound = current ?: return
        val file = currentFile ?: return
        if (playerA == null) {
            play(sound, file)
            return
        }
        if (leadIsA) playerA?.play() else playerB?.play()
        _state.value = _state.value.copy(isPlaying = true)
        startFadeLoop(sound)
    }

    fun stop() {
        stopInternal()
        settings.surroundingsSoundId = null
        _state.value = SurroundingsState(volume = settings.surroundingsVolume)
    }

    /**
     * Sets the volume, 0 through 1.
     *
     * The recording's own makeup gain is applied underneath, so the same
     * setting is the same loudness on every recording whatever its file needed.
     */
    fun setVolume(volume: Float) {
        settings.surroundingsVolume = volume
        val sound = current
        _state.value = _state.value.copy(
            volume = volume,
            limiterEngaged = sound != null && LoopMix.limiterEngages(
                sound,
                LoopMix.playbackGainDb(sound, volume.toDouble()),
            ),
        )
        // The fade loop picks the new volume up on its next tick, which is at
        // most 50 ms away, so there is nothing to do here but record it.
    }

    fun release() {
        stopInternal()
        scope.cancel()
    }

    // ---------- The loop ----------

    /**
     * Watches the leading player and runs the crossfade when it nears the end.
     *
     * The trailing player is started `fade` seconds before the leader ends, and
     * the two are weighted against each other for exactly that long. When the
     * fade completes the roles swap and the old leader is stopped and rewound,
     * ready to be the next trailer.
     */
    private fun startFadeLoop(sound: SurroundingsSound) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val fadeMs = (LoopMix.effectiveFadeSeconds(sound.durationSeconds.toDouble()) * 1000).toLong()
            val mismatch = LoopMix.loopMismatchDb(sound)

            /**
             * Whether the incoming loop has been started for this fade.
             *
             * **Not `trail.isPlaying`.** A player that has just been told to
             * play reports false until it is ready, so testing that restarted
             * the incoming loop on every tick: it was prepared and seeked forty
             * times over two and a half seconds and never once got far enough
             * to make a sound. The outgoing loop faded out on schedule and the
             * incoming one faded in silent, so the bed simply stopped at the
             * first loop point. A flag says what was asked for rather than what
             * has happened yet.
             */
            var armed = false

            /**
             * Whether the incoming loop has been loaded but not yet started.
             *
             * Preparing a file takes a moment, and a player told to play at the
             * instant the fade begins arrives a few hundred milliseconds late,
             * so the first part of the fade is quieter than it should be.
             * Loading it a second early and holding it silent means it starts
             * on the sample it was asked for.
             */
            var prepared = false

            while (true) {
                delay(TICK_MS)
                val lead = if (leadIsA) playerA else playerB
                val trail = if (leadIsA) playerB else playerA
                if (lead == null || trail == null) return@launch
                if (!_state.value.isPlaying) continue

                val duration = lead.duration
                if (duration == C.TIME_UNSET || duration <= 0) continue
                val remaining = duration - lead.currentPosition

                // Load the incoming loop a second before it is needed, and
                // hold it silent until the fade actually starts.
                if (!prepared && remaining <= fadeMs + PREPARE_LEAD_MS) {
                    prepared = true
                    trail.setMediaItem(MediaItem.fromUri(currentFile!!.toURI().toString()))
                    trail.volume = 0f
                    trail.prepare()
                    trail.seekTo(0)
                }

                if (remaining > fadeMs) {
                    // Well inside the file. Leader at full, trailer silent.
                    lead.volume = 1f
                    continue
                }

                if (!armed) {
                    armed = true
                    trail.play()
                }

                val t = ((fadeMs - remaining).toDouble() / fadeMs).coerceIn(0.0, 1.0)
                val (outWeight, inWeight) = LoopMix.crossfadeWeights(t)
                // R3: the incoming loop arrives at the level the outgoing one
                // left, then is released back over the rest of the fade.
                val compensation = LoopMix.dbToLinear(LoopMix.loopCompensationDb(mismatch, t))

                lead.volume = outWeight.toFloat()
                // The compensation is at or below unity for a file that starts
                // louder than it ends, which is the common direction. Where a
                // file ends louder it would exceed unity, which ExoPlayer will
                // not take, so it is clamped and the residual is left alone: a
                // few decibels short of perfect compensation is still far
                // better than the step it replaces.
                trail.volume = (inWeight * compensation).toFloat().coerceIn(0f, 1f)

                if (t >= 1.0) {
                    lead.pause()
                    lead.seekTo(0)
                    lead.volume = 0f
                    leadIsA = !leadIsA
                    armed = false
                    prepared = false
                }
            }
        }
    }

    /**
     * The gain the processor applies, in decibels.
     *
     * **The split between here and ExoPlayer's own volume is not arbitrary.**
     * ExoPlayer's volume is a plain multiplier that is not allowed above 1, so
     * it can carry the crossfade weights, which are always at or below unity,
     * and nothing else. The makeup gain that R2 requires goes up to 18 dB and
     * has to be applied where there is headroom to apply it, which is the float
     * audio processor, immediately in front of the limiter.
     */
    private fun gainDb(): Double {
        val sound = current ?: return 0.0
        return LoopMix.playbackGainDb(sound, settings.surroundingsVolume.toDouble())
    }

    private fun newPlayer(): ExoPlayer =
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                // Explicitly not handling audio focus. This is a bed that plays
                // under the music player, and two players in one app fighting
                // over focus would silence each other.
                /* handleAudioFocus = */ false,
            )
            .setRenderersFactory(SurroundingsRenderersFactory(context, ::gainDb))
            .build()
            .apply { repeatMode = ExoPlayer.REPEAT_MODE_OFF }

    private fun stopInternal() {
        fadeJob?.cancel()
        fadeJob = null
        playerA?.release()
        playerB?.release()
        playerA = null
        playerB = null
        current = null
        currentFile = null
    }

    private companion object {
        /**
         * How often the fade is stepped.
         *
         * A fiftieth of the shortest fade segment, which is far below the
         * threshold at which a level change is heard as a step rather than a
         * slope, and cheap enough to run for hours.
         */
        const val TICK_MS = 50L

        /**
         * How far ahead the incoming loop is loaded.
         *
         * Long enough for a local Opus file to be decoded and positioned, short
         * enough that a second player is not sitting prepared for minutes.
         */
        const val PREPARE_LEAD_MS = 1_200L
    }
}

/** What the ambience is doing, for the interface. */
data class SurroundingsState(
    val soundId: String? = null,
    val displayName: String = "",
    val recordist: String = "",
    val isPlaying: Boolean = false,
    val volume: Float = 0.6f,
    /**
     * Whether the limiter is in the signal path at this volume. Shown nowhere
     * prominent, but it is the honest answer to "why does this not get louder".
     */
    val limiterEngaged: Boolean = false,
) {
    val hasSound: Boolean get() = soundId != null
}
