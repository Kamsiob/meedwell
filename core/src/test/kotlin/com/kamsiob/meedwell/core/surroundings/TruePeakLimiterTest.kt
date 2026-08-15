package com.kamsiob.meedwell.core.surroundings

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * The limiter, tested on signals whose correct answer is known rather than on
 * audio. It only ever runs when the app is pushing 18 dB of gain into a field
 * recording, which is exactly when nobody is in a position to notice it going
 * wrong until the speaker crackles.
 */
class TruePeakLimiterTest {

    private val ceiling = LoopMix.dbToLinear(LoopMix.CEILING_DBTP).toFloat()

    private fun limiter(channels: Int = 2, releaseMs: Double = 120.0) =
        TruePeakLimiter(sampleRate = 48_000, channels = channels, releaseMs = releaseMs)

    /** Runs a signal through in blocks and returns everything that came out. */
    private fun run(limiter: TruePeakLimiter, signal: FloatArray, channels: Int = 2): FloatArray {
        val out = signal.copyOf()
        var offset = 0
        val blockFrames = 1024
        while (offset < out.size) {
            val frames = minOf(blockFrames, (out.size - offset) / channels)
            if (frames == 0) break
            val block = out.copyOfRange(offset, offset + frames * channels)
            limiter.process(block, frames)
            block.copyInto(out, offset)
            offset += frames * channels
        }
        return out
    }

    private fun tone(seconds: Double, amplitude: Float, channels: Int = 2, hz: Double = 220.0): FloatArray {
        val frames = (48_000 * seconds).toInt()
        val buffer = FloatArray(frames * channels)
        for (f in 0 until frames) {
            val v = (amplitude * sin(2 * PI * hz * f / 48_000)).toFloat()
            for (c in 0 until channels) buffer[f * channels + c] = v
        }
        return buffer
    }

    private fun peak(buffer: FloatArray, from: Int = 0): Float {
        var m = 0f
        for (i in from until buffer.size) {
            val a = abs(buffer[i])
            if (a > m) m = a
        }
        return m
    }

    /**
     * The one thing that must never happen. Whatever goes in, nothing comes out
     * above the ceiling.
     */
    @Test
    fun `nothing exceeds the ceiling however hard it is driven`() {
        val l = limiter()
        // Well past full scale, which is what 18 dB on a loud transient does.
        val out = run(l, tone(seconds = 0.5, amplitude = 4.0f))
        assertThat(peak(out)).isAtMost(ceiling * 1.001f)
    }

    /**
     * Just as important, and the reason the manifest states each file's true
     * peak: a signal under the ceiling must come out untouched, not merely
     * unclipped. A limiter that quietly attenuates everything is a limiter that
     * makes the whole library sound flat.
     */
    @Test
    fun `a signal under the ceiling passes through unchanged`() {
        val l = limiter()
        val signal = tone(seconds = 0.3, amplitude = 0.5f)
        val out = run(l, signal)

        assertThat(l.everEngaged).isFalse()
        // Compare past the look-ahead delay, which is the only difference.
        val delayFrames = (48_000 * 5.0 / 1000.0).toInt()
        val skip = delayFrames * 2
        for (i in skip until out.size) {
            assertThat(out[i]).isWithin(1e-6f).of(signal[i - skip])
        }
    }

    /**
     * The point of the look-ahead. A limiter that reacts when a peak arrives
     * has already passed the front edge of it, and that escaped edge is the
     * click. The very first sample of a hard transient must already be down.
     */
    @Test
    fun `a transient is caught before it arrives, not after`() {
        val l = limiter()
        val channels = 2
        val frames = 48_000
        val signal = FloatArray(frames * channels)
        // Silence, then a hard step to well over the ceiling.
        val edge = 20_000
        for (f in edge until frames) {
            for (c in 0 until channels) signal[f * channels + c] = 3.0f
        }
        val out = run(l, signal)

        val delayFrames = (48_000 * 5.0 / 1000.0).toInt()
        // Where the transient emerges from the delay line, and every sample
        // after it, is already under the ceiling. Nothing escapes the front.
        val emerges = (edge + delayFrames) * channels
        assertThat(peak(out, from = emerges)).isAtMost(ceiling * 1.001f)
    }

    /**
     * The interpolated peak is what makes this a *true* peak limiter. A signal
     * whose samples sit under the ceiling but whose midpoints do not must still
     * be brought down, because a DAC will render those midpoints.
     */
    @Test
    fun `a peak between two samples is still caught`() {
        val l = limiter(channels = 1)
        val channels = 1
        // Alternating full-scale-adjacent samples of opposite sign: every
        // sample is legal, every midpoint is not.
        val frames = 4096
        val signal = FloatArray(frames)
        for (f in 0 until frames) {
            signal[f] = if (f % 2 == 0) 0.89f else -0.89f
        }
        run(l, signal, channels)
        // The interpolated peak between +0.89 and -0.89 read as magnitude is
        // above the ceiling, so the limiter has work to do here even though no
        // individual sample breaches it.
        assertThat(l.observedPeak).isGreaterThan(ceiling)
        assertThat(l.everEngaged).isTrue()
    }

    /**
     * Recovery has to be gradual. A limiter that jumps back to unity between a
     * fire's crackles pumps the room tone up and down with them, which is far
     * more noticeable than the peaks it is catching.
     */
    @Test
    fun `gain recovers gradually rather than jumping back`() {
        val l = limiter(releaseMs = 120.0)
        val channels = 2
        val frames = 48_000
        val signal = FloatArray(frames * channels)
        // One loud frame, then a steady quiet tone.
        for (c in 0 until channels) signal[c] = 5.0f
        for (f in 1 until frames) {
            val v = (0.4 * sin(2 * PI * 220.0 * f / 48_000)).toFloat()
            for (c in 0 until channels) signal[f * channels + c] = v
        }
        val out = run(l, signal, channels)

        // Measured from where the transient emerges from the delay line, and
        // deliberately past it: the transient itself is held at the ceiling,
        // which says nothing about how the tone behind it recovers.
        fun toneAt(seconds: Double): Float {
            val frame = l.lookAheadFrames + 2 + (48_000 * seconds).toInt()
            val from = frame * channels
            return peak(out.copyOfRange(from, from + 400 * channels))
        }

        val justAfter = toneAt(0.001)
        val partWay = toneAt(0.05)
        val wellAfter = toneAt(0.5)

        // Still well down immediately after, climbing, and fully back by half
        // a second. A limiter that jumped straight back would fail the first.
        assertThat(justAfter).isLessThan(0.2f)
        assertThat(partWay).isGreaterThan(justAfter)
        assertThat(wellAfter).isGreaterThan(partWay)
        assertThat(wellAfter).isWithin(0.02f).of(0.4f)
    }

    @Test
    fun `mono is handled as well as stereo`() {
        val l = limiter(channels = 1)
        val out = run(l, tone(seconds = 0.2, amplitude = 3.0f, channels = 1), channels = 1)
        assertThat(peak(out)).isAtMost(ceiling * 1.001f)
    }

    @Test
    fun `reset clears the delay line and the reporting`() {
        val l = limiter()
        run(l, tone(seconds = 0.1, amplitude = 3.0f))
        assertThat(l.everEngaged).isTrue()
        l.reset()
        assertThat(l.everEngaged).isFalse()
        assertThat(l.observedPeak).isEqualTo(0f)
    }

    /** Silence in, silence out, and no division by zero on the way. */
    @Test
    fun `silence is survivable`() {
        val l = limiter()
        val out = run(l, FloatArray(4096))
        assertThat(peak(out)).isEqualTo(0f)
        assertThat(l.everEngaged).isFalse()
    }
}
