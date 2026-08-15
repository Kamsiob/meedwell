package com.kamsiob.meedwell.playback

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.kamsiob.meedwell.core.surroundings.LoopMix
import com.kamsiob.meedwell.core.surroundings.TruePeakLimiter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The Surroundings audio path: makeup gain, then a true-peak limiter.
 *
 * This exists because **ExoPlayer's own volume cannot go above 1**, and R2
 * requires up to 18 dB of gain. A recording whose transients are loud runs out
 * of headroom before its average reaches the library's bed target, so it is
 * encoded quieter and has to be brought back up at playback. Attenuation alone
 * cannot make this library loud enough, which is the whole reason the mastering
 * left a ceiling to work into.
 *
 * Gain that large will push some transients past full scale, so it is followed
 * immediately by the limiter, in the same processor, with nothing in between
 * that could reorder them.
 *
 * @param gainDb the total gain to apply right now, read fresh on every block so
 *   a volume change takes effect within one buffer rather than at the next
 *   track. There is no next track: this loops for hours.
 */
@UnstableApi
class SurroundingsGainProcessor(
    private val gainDb: () -> Double,
) : BaseAudioProcessor() {

    private var limiter: TruePeakLimiter? = null
    private var scratch = FloatArray(0)
    private var channels = 2
    private var encoding = C.ENCODING_PCM_16BIT

    /** Whether the limiter has had to act, for the interface to report honestly. */
    @Volatile
    var limiterEngaged: Boolean = false
        private set

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // Sixteen bit and float are the two the decoder actually produces here.
        // Anything else is passed through untouched rather than mangled: a bed
        // at the wrong level is a disappointment, and a bed rendered as noise
        // is a bug report.
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        channels = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding
        limiter = TruePeakLimiter(
            sampleRate = inputAudioFormat.sampleRate,
            channels = inputAudioFormat.channelCount,
        )
        return inputAudioFormat
    }

    override fun onFlush() {
        limiter?.reset()
        limiterEngaged = false
    }

    override fun onReset() {
        limiter = null
        scratch = FloatArray(0)
        limiterEngaged = false
    }

    override fun queueInput(input: ByteBuffer) {
        val limiter = this.limiter ?: return
        val bytesPerSample = if (encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        val samples = input.remaining() / bytesPerSample
        if (samples == 0) return
        val frames = samples / channels

        val gain = LoopMix.dbToLinear(gainDb()).toFloat()

        if (scratch.size < samples) scratch = FloatArray(samples)

        // Decode into float, because gain and limiting both need headroom above
        // full scale to work in and integers have none.
        val source = input.order(ByteOrder.nativeOrder())
        if (encoding == C.ENCODING_PCM_FLOAT) {
            for (i in 0 until samples) scratch[i] = source.float * gain
        } else {
            for (i in 0 until samples) scratch[i] = (source.short / 32768f) * gain
        }

        limiter.process(scratch, frames)
        if (limiter.everEngaged) limiterEngaged = true

        val output = replaceOutputBuffer(samples * bytesPerSample).order(ByteOrder.nativeOrder())
        if (encoding == C.ENCODING_PCM_FLOAT) {
            for (i in 0 until samples) output.putFloat(scratch[i])
        } else {
            for (i in 0 until samples) {
                // The limiter guarantees this is inside range, so the clamp is
                // belt and braces rather than the thing doing the work.
                val v = (scratch[i] * 32767f).coerceIn(-32768f, 32767f)
                output.putShort(v.toInt().toShort())
            }
        }
        input.position(input.limit())
        output.flip()
    }
}

/**
 * An ExoPlayer renderer stack with the Surroundings gain path in it.
 *
 * Float output is asked for so the gain is applied before any rounding to
 * integers. Where the device will not give float, the processor works in
 * sixteen bit instead and the only cost is that the makeup gain raises the
 * quantization floor with everything else, which is inaudible under a field
 * recording's own noise floor.
 */
@UnstableApi
class SurroundingsRenderersFactory(
    context: Context,
    private val gainDb: () -> Double,
) : DefaultRenderersFactory(context) {

    val gainProcessor = SurroundingsGainProcessor(gainDb)

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink = DefaultAudioSink.Builder(context)
        .setAudioProcessors(arrayOf(gainProcessor))
        .setEnableFloatOutput(true)
        .build()
}
