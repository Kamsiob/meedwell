package com.kamsiob.meedwell.core.surroundings

import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * The arithmetic behind looping a field recording without anyone hearing it
 * loop.
 *
 * All of it lives here, as plain functions on numbers, because these are the
 * decisions that are either right or audible and they should be provable
 * without a device, a file, or an ear.
 *
 * Three separate problems, from `REQUIREMENTS.md`:
 *
 *  - **R1, the join clicks.** Every file has only a 50 ms fade at each edge, on
 *    purpose: a longer one would dip toward silence at every loop and produce
 *    exactly the periodicity the library exists to avoid. So a file sits at
 *    essentially full level 50 ms from each end, and cutting from the last
 *    sample to the first joins two unrelated points in a waveform.
 *  - **R2, some files are quiet.** The library is matched to a -43.5 dB bed
 *    with a -2 dBTP ceiling. A recording with loud transients runs out of
 *    headroom before its average reaches the target, so it is encoded quieter
 *    and needs makeup at playback. No file is compressed to avoid this.
 *  - **R3, some files drift.** Fourteen shipped files end more than 3 dB away
 *    from where they began, so every loop is a step change in level.
 */
object LoopMix {

    /** The crossfade length. Inside R1's 2 to 3 second window, near the middle. */
    const val CROSSFADE_SECONDS = 2.5

    /** The most makeup any shipped file needs is 16.89 dB. R2 allows 18. */
    const val MAX_MAKEUP_DB = 18.0

    /**
     * What the top of the volume control is worth, above the matched bed level.
     *
     * **Without this the control had nowhere to go.** The library is normalised
     * to a bed level of -43.5 dB and almost every file needs no further makeup,
     * so full volume applied a gain of exactly zero and played the bed at the
     * level it was mastered to sit *under music* at. On its own, in a room, that
     * is barely there, which is precisely what it was reported as.
     *
     * The bed target still does its real job, which is making every recording
     * match every other one. This rides on top of all of them equally, so two
     * beds at the same volume stay equally loud.
     *
     * Twelve rather than more, measured against the shipped library: true peaks
     * run from -31.5 to -2.0 dBTP with a median of -14.0, so twelve puts the
     * median at -2 and brings 45 of the 111 files into contact with the limiter
     * on their transients alone. That is a true-peak limiter with look-ahead
     * catching a crackle or a thunderclap, not compression riding the whole
     * bed, and it is what the limiter is there for. Eighteen would have put 80
     * of them into it, which starts squashing the thing worth listening to.
     */
    const val BED_HEADROOM_DB = 12.0

    /**
     * The true-peak ceiling at playback.
     *
     * A decibel under full scale, rather than at it. Opus is decoded to float
     * and rounded to the output format, and a signal sitting exactly at full
     * scale clips on that rounding.
     */
    const val CEILING_DBTP = -1.0

    /**
     * The outgoing and incoming weights partway through a crossfade.
     *
     * **Equal power, not equal gain.** Two successive loops of ambience are
     * uncorrelated, so a linear crossfade sums to about 3 dB below either end
     * in the middle and is heard as a dip once per loop. `cos` and `sin` of the
     * same quarter turn satisfy `cos² + sin² = 1`, which holds the summed power
     * constant instead. Verified on 60 files: level through the fade lands
     * within 3 dB of the power average of its ends, with zero clicks.
     *
     * @param t position through the fade, 0 at the start and 1 at the end.
     */
    fun crossfadeWeights(t: Double): Pair<Double, Double> {
        val clamped = t.coerceIn(0.0, 1.0)
        val quarterTurn = clamped * Math.PI / 2
        return cos(quarterTurn) to sin(quarterTurn)
    }

    /**
     * The gain to apply to a recording so it plays at the level it was matched
     * for, in decibels.
     *
     * Read from the manifest rather than measured, because the manifest figure
     * was measured on the encoded audio by the pipeline that made the file.
     * Clamped to R2's range: negative makeup is not a thing, and nothing shipped
     * needs more than 18 dB.
     */
    fun makeupGainDb(sound: SurroundingsSound): Double {
        val stated = sound.loudness.makeupGainDb.toDoubleOrNull() ?: return 0.0
        return stated.coerceIn(0.0, MAX_MAKEUP_DB)
    }

    /**
     * The level difference across the loop point, in decibels.
     *
     * Positive means the file starts louder than it ends, so an uncompensated
     * loop jumps up. Fourteen shipped files exceed 3 dB and none exceeds 7,
     * which is the reason this can be corrected inside the crossfade at all: a
     * larger step would need a ramp long enough to be an audible event itself,
     * and files above 8 dB were dropped from the library for that reason.
     */
    fun loopMismatchDb(sound: SurroundingsSound): Double {
        val head = sound.loop.headLevelDb.toDoubleOrNull() ?: return 0.0
        val tail = sound.loop.tailLevelDb.toDoubleOrNull() ?: return 0.0
        return head - tail
    }

    /**
     * The gain applied to the incoming loop partway through the crossfade, in
     * decibels, before its equal-power weight.
     *
     * R3: ramp by `head − tail` across the fade so the incoming loop arrives at
     * the level the outgoing one left. It therefore starts held down by the
     * full mismatch and is released to nothing by the end of the fade, which
     * turns a step at the join into a gentle swell of at most 7 dB spread over
     * two and a half seconds.
     *
     * Where a file's ends already match, this is zero throughout and the
     * crossfade is the plain equal-power one.
     */
    fun loopCompensationDb(mismatchDb: Double, t: Double): Double {
        if (mismatchDb == 0.0) return 0.0
        val clamped = t.coerceIn(0.0, 1.0)
        return -mismatchDb * (1.0 - clamped)
    }

    /**
     * Where the crossfade begins, in seconds from the start of the file.
     *
     * Never negative, and never so late that the fade would run past the end of
     * a short file: for anything under about six seconds the fade is shortened
     * rather than truncated, which keeps the two halves symmetrical.
     */
    fun crossfadeStartSeconds(durationSeconds: Double, fadeSeconds: Double = CROSSFADE_SECONDS): Double =
        max(0.0, durationSeconds - effectiveFadeSeconds(durationSeconds, fadeSeconds))

    /**
     * The fade actually used, which is the requested one unless the file is too
     * short to hold it.
     *
     * Capped at a third of the file so a loop always has more plain audio than
     * fade. Nothing in the shipped library comes close to this: the shortest is
     * three and a half minutes. It exists so a future library of shorter
     * recordings degrades rather than misbehaves.
     */
    fun effectiveFadeSeconds(durationSeconds: Double, fadeSeconds: Double = CROSSFADE_SECONDS): Double {
        if (durationSeconds <= 0.0) return 0.0
        return min(fadeSeconds, durationSeconds / 3.0)
    }

    /**
     * The linear multiplier for a gain in decibels.
     *
     * Zero decibels is exactly one, which matters: the common
     * `10^(0/20)` round trip returns 0.9999999999999999 on some inputs, and a
     * playback path that never quite reaches unity is a playback path that is
     * always very slightly wrong.
     */
    fun dbToLinear(db: Double): Double = if (db == 0.0) 1.0 else 10.0.pow(db / 20.0)

    /** The decibel value of a linear multiplier. Silence is treated as -160 dB. */
    fun linearToDb(linear: Double): Double =
        if (linear <= 1e-8) -160.0 else 20.0 * log10(linear)

    /**
     * Whether the limiter has anything to do.
     *
     * The library ships with a -2 dBTP ceiling and each entry states its own
     * measured true peak, so the answer is arithmetic rather than a guess:
     * apply the gain to the stated peak and see whether it clears the ceiling.
     * On the great majority of files at their intended level it does, and the
     * limiter stays out of the signal path entirely.
     */
    fun limiterEngages(sound: SurroundingsSound, totalGainDb: Double): Boolean {
        val peak = sound.loudness.truePeakDbtp.toDoubleOrNull() ?: return totalGainDb > 0.0
        return peak + totalGainDb > CEILING_DBTP
    }

    /**
     * The largest gain that keeps a recording's stated true peak under the
     * ceiling, in decibels.
     *
     * Used to bias the volume control rather than to cap it. Somebody who wants
     * a fire louder than this may have it; they get the limiter as well.
     */
    fun headroomDb(sound: SurroundingsSound): Double {
        val peak = sound.loudness.truePeakDbtp.toDoubleOrNull() ?: return 0.0
        return CEILING_DBTP - peak
    }

    /**
     * The gain a recording should start at, in decibels, for a given user
     * volume.
     *
     * The makeup is the recording's own and is not the listener's business:
     * two beds set to the same volume should sound equally loud whatever their
     * files needed. The volume rides on top of it.
     *
     * @param volume 0 for silence through 1 for the recording's intended level.
     */
    fun playbackGainDb(sound: SurroundingsSound, volume: Double): Double {
        val v = volume.coerceIn(0.0, 1.0)
        if (v <= 0.0) return -160.0
        // A perceptual taper rather than a linear one. Halfway up a linear
        // slider is a barely perceptible drop of about 6 dB, which makes the
        // bottom half of the control useless for a bed you want under music.
        val volumeDb = 40.0 * log10(v)
        return makeupGainDb(sound) + volumeDb + BED_HEADROOM_DB
    }
}
