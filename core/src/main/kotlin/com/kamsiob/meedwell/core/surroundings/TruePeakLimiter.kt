package com.kamsiob.meedwell.core.surroundings

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * A look-ahead limiter that holds the true peak under a ceiling.
 *
 * Required by R2: the app applies up to 18 dB of makeup gain, and gain that
 * large will push some transients past full scale. Without a limiter the
 * alternative is clipping, which on a close-miked fire is not subtle.
 *
 * **Why a true peak and not a sample peak.** A signal whose every sample sits
 * under full scale can still exceed it *between* samples, and the reconstruction
 * filter in a converter will render that overshoot. The textbook case is a run
 * of alternating samples near full scale: each one is legal, and the waveform
 * they describe reaches roughly 3 dB higher. A limiter watching only samples
 * lets through exactly the peaks that actually clip, which is why the library
 * was mastered to a true-peak ceiling in the first place. Measuring the same way
 * here is the same promise kept at the other end.
 *
 * So the detector reconstructs the signal at four times the sample rate through
 * a windowed-sinc polyphase filter, which is the method ITU-R BS.1770 specifies,
 * and takes the largest magnitude it finds.
 *
 * **Why the gain is a sliding minimum rather than a follower.** The gain applied
 * to a sample must already account for every peak still inside the look-ahead
 * window, or a loud sample emerges from the delay line after the gain has
 * recovered and clips anyway. Holding the minimum across the window makes that
 * impossible by construction rather than by tuning an attack time.
 *
 * This runs on every frame of audio, so it allocates nothing after construction.
 */
class TruePeakLimiter(
    private val sampleRate: Int,
    private val channels: Int,
    /** The ceiling, in dBFS. Peaks are held at or below this. */
    ceilingDb: Double = LoopMix.CEILING_DBTP,
    /** How far ahead peaks are seen. Long enough to catch a transient's rise. */
    lookAheadMs: Double = 5.0,
    /** How quickly gain returns after a peak passes. */
    releaseMs: Double = 120.0,
) {
    private val ceiling: Float = LoopMix.dbToLinear(ceilingDb).toFloat()

    /** The delay line, in frames. Audio comes out this many frames late. */
    val lookAheadFrames: Int = max(2, (sampleRate * lookAheadMs / 1000.0).toInt())

    private val delay = FloatArray(lookAheadFrames * channels)
    private var delayPos = 0

    private val detector = TruePeakDetector(channels)
    private val window = SlidingMinimum(lookAheadFrames)

    /**
     * The per-frame recovery factor.
     *
     * Exponential, so recovery is brisk where the reduction was small and
     * unhurried where it was large, which is what keeps it inaudible. Release
     * only ever slows the *rise* back to unity; it can never hold gain above
     * what the look-ahead window permits.
     */
    private val releaseCoefficient: Float =
        0.001.pow(1.0 / max(1.0, sampleRate * releaseMs / 1000.0)).toFloat()

    private var gain = 1f

    /** The largest true peak seen since the last reset, for reporting. */
    var observedPeak: Float = 0f
        private set

    /** Whether the limiter has actually reduced gain since the last reset. */
    var everEngaged: Boolean = false
        private set

    fun reset() {
        delay.fill(0f)
        delayPos = 0
        gain = 1f
        observedPeak = 0f
        everEngaged = false
        detector.reset()
        window.reset()
    }

    /**
     * Limits a block of interleaved float frames in place.
     *
     * The block comes back delayed by the look-ahead, which is the cost of
     * seeing peaks before they arrive. At five milliseconds it is inaudible and
     * constant, so it does not drift against anything.
     */
    fun process(buffer: FloatArray, frames: Int) {
        for (frame in 0 until frames) {
            val base = frame * channels

            val truePeak = detector.push(buffer, base)
            if (truePeak > observedPeak) observedPeak = truePeak

            // What this incoming frame alone would allow, then the strictest
            // allowance anywhere in the look-ahead window.
            val allowed = if (truePeak > ceiling) ceiling / truePeak else 1f
            val target = window.push(allowed)

            gain = if (target < gain) {
                target
            } else {
                // Release, but never above what the window permits.
                min(target, gain + (1f - gain) * (1f - releaseCoefficient))
            }
            if (gain < 1f) everEngaged = true

            // Swap the frame about to be written for the one coming out of the
            // delay line, then apply the gain chosen with that one in view.
            for (c in 0 until channels) {
                val slot = delayPos + c
                val delayed = delay[slot]
                delay[slot] = buffer[base + c]
                buffer[base + c] = delayed * gain
            }
            delayPos += channels
            if (delayPos >= delay.size) delayPos = 0
        }
    }
}

/**
 * The smallest value in the last N pushed, in constant time.
 *
 * A monotonic deque over a ring buffer. The obvious alternative, rescanning the
 * window on every frame, is 240 comparisons per sample at 48 kHz and would cost
 * more than the audio decoding does.
 */
internal class SlidingMinimum(private val size: Int) {
    private val values = FloatArray(size)
    private val deque = IntArray(size)
    private var head = 0
    private var tail = 0
    private var index = 0

    fun reset() {
        values.fill(1f)
        head = 0
        tail = 0
        index = 0
    }

    fun push(value: Float): Float {
        val slot = index % size
        values[slot] = value

        // Anything already queued that is no smaller than this can never be the
        // minimum again, because this one is newer and stays longer.
        while (tail > head && values[deque[(tail - 1) % size] % size] >= value) tail--
        deque[tail % size] = index
        tail++

        // Drop whatever has fallen out of the window.
        while (deque[head % size] <= index - size) head++

        index++
        return values[deque[head % size] % size]
    }
}

/**
 * The true peak of a signal, found by reconstructing it at four times the
 * sample rate.
 *
 * Four times is what ITU-R BS.1770 specifies for rates up to 48 kHz, and it is
 * enough: the residual error against a very high oversampling factor is a small
 * fraction of a decibel, well inside the margin the ceiling already leaves.
 */
internal class TruePeakDetector(private val channels: Int) {

    private val history = Array(channels) { FloatArray(TAPS) }
    private var position = 0

    fun reset() {
        history.forEach { it.fill(0f) }
        position = 0
    }

    /** Pushes one frame and returns the largest reconstructed magnitude in it. */
    fun push(buffer: FloatArray, base: Int): Float {
        var peak = 0f
        for (c in 0 until channels) {
            val ring = history[c]
            ring[position] = buffer[base + c]
            for (phase in 0 until PHASES) {
                val coefficients = KERNEL[phase]
                var sum = 0f
                for (tap in 0 until TAPS) {
                    // Walk backward from the newest sample.
                    val slot = (position - tap + TAPS) % TAPS
                    sum += ring[slot] * coefficients[tap]
                }
                val magnitude = abs(sum)
                if (magnitude > peak) peak = magnitude
            }
        }
        position = (position + 1) % TAPS
        return peak
    }

    private companion object {
        const val PHASES = 4
        const val TAPS = 12

        /**
         * A windowed-sinc interpolator, split into four phases.
         *
         * Phase zero is the sample itself, so an unfiltered signal reads back
         * exactly and a quiet passage cannot be nudged over the ceiling by the
         * detector's own ripple. The other three are the points between.
         */
        val KERNEL: Array<FloatArray> = Array(PHASES) { phase ->
            FloatArray(TAPS) { tap ->
                if (phase == 0) {
                    if (tap == 0) 1f else 0f
                } else {
                    val offset = tap + phase.toDouble() / PHASES
                    val x = offset - TAPS / 2.0
                    val sinc = if (abs(x) < 1e-9) 1.0 else sin(PI * x) / (PI * x)
                    // A Hann window over the kernel, which keeps the stopband
                    // clean enough that the reconstruction does not invent
                    // peaks of its own.
                    val window = 0.5 * (1 - cos(2 * PI * offset / TAPS))
                    (sinc * window).toFloat()
                }
            }
        }
    }
}
