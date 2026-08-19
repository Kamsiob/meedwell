package com.kamsiob.meedwell.playback

import android.media.audiofx.Equalizer
import com.kamsiob.meedwell.core.library.Voicing
import kotlin.math.roundToInt

/**
 * Tone: the five voicings, applied to the real audio session.
 *
 * **The curve is defined once and mapped onto whatever bands the device has.**
 * A phone's equalizer offers whatever its chip felt like: usually five bands,
 * sometimes ten, and rarely at the frequencies the curve was drawn at. Reading
 * the device's own band centres and interpolating onto them is the difference
 * between a tone control that works on every phone and one that works on the
 * one it was written on.
 *
 * **`As Recorded` releases the effect entirely** rather than setting every band
 * to zero. A flat equalizer still sits in the signal path, still costs battery,
 * and still turns off audio offload. Nothing applied has to mean nothing there.
 *
 * Every failure here is swallowed on purpose. `Equalizer` is an optional
 * platform effect: it throws on devices that do not have one, inside a
 * `MediaPlayer`-less session, and occasionally for no reason at all. A tone
 * control that crashes the music is far worse than one that quietly does
 * nothing, and the screen already states that the phone's own processing sits
 * outside Meedwell.
 */
class ToneController {

    private var equalizer: Equalizer? = null
    private var sessionId: Int = 0

    /** The voicing in force. */
    var voicing: Voicing = Voicing.AsRecorded
        private set

    /**
     * Whether the effect could actually be attached.
     *
     * False on a device with no equalizer. The interface uses it to say so
     * rather than showing a control that silently does nothing.
     */
    var available: Boolean = true
        private set

    /** Called when the player's audio session changes, including on first play. */
    fun onSessionChanged(newSessionId: Int) {
        if (newSessionId == sessionId) return
        release()
        sessionId = newSessionId
        apply(voicing)
    }

    fun apply(newVoicing: Voicing) {
        voicing = newVoicing

        if (newVoicing.isFlat) {
            // Nothing applied means nothing in the path.
            release()
            return
        }
        if (sessionId == 0) return

        val eq = equalizer ?: runCatching {
            // Priority 0: Meedwell is not more important than anything else the
            // user has running, and a higher priority would let it take the
            // effect away from another app.
            Equalizer(0, sessionId).also { it.enabled = true }
        }.onFailure { available = false }.getOrNull() ?: return

        equalizer = eq
        available = true

        runCatching {
            val bands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minMilli = range[0].toInt()
            val maxMilli = range[1].toInt()

            for (band in 0 until bands) {
                val centreHz = eq.getCenterFreq(band.toShort()) / 1000
                val wanted = Voicing.gainAt(newVoicing.curve, centreHz)
                val milli = (wanted * 100).roundToInt().coerceIn(minMilli, maxMilli)
                eq.setBandLevel(band.toShort(), milli.toShort())
            }
        }.onFailure {
            // The effect exists but would not take the settings. Release it
            // rather than leaving a half-applied curve on the music.
            release()
            available = false
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
    }
}
