package dev.mouya.phiraforge.audio

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaCodec
import android.util.Log
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.min

/**
 * Audio decoder using Android MediaExtractor + MediaCodec.
 * Decodes any supported audio format (MP3, WAV, OGG, FLAC, M4A, AAC) to mono PCM float array.
 *
 * Designed for chunked processing to avoid OOM on large files.
 */
class AudioDecoder {

    companion object {
        private const val TAG = "AudioDecoder"
        const val TARGET_SAMPLE_RATE = 22050 // Downsample for onset detection efficiency
    }

    data class DecodedAudio(
        val samples: FloatArray,
        val sampleRate: Int,
        val durationMs: Long,
        val channels: Int
    )

    /**
     * Decode audio file to mono PCM float array at TARGET_SAMPLE_RATE.
     * Uses chunked reading to handle large files.
     *
     * @param filePath Path to audio file
     * @param progressCallback Optional callback for progress updates (0.0 to 1.0)
     * @return DecodedAudio containing mono samples
     * @throws Exception if decoding fails
     */
    fun decode(filePath: String, progressCallback: ((Float) -> Unit)? = null): DecodedAudio {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(filePath)
        } catch (e: Exception) {
            throw Exception("Failed to set data source: ${e.message}", e)
        }

        var audioTrackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                break
            }
        }

        if (audioTrackIndex < 0) {
            extractor.release()
            throw Exception("No audio track found in file")
        }

        extractor.selectTrack(audioTrackIndex)
        val inputFormat = extractor.getTrackFormat(audioTrackIndex)

        val sourceSampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val sourceChannels = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        } else 1

        val durationMs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) {
            inputFormat.getLong(MediaFormat.KEY_DURATION)
        } else 0L

        if (durationMs > 0 && durationMs < 5000) {
            extractor.release()
            throw Exception("Audio duration too short (${durationMs}ms, minimum 5000ms required)")
        }

        val mime = inputFormat.getString(MediaFormat.KEY_MIME)!!
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(inputFormat, null, null, 0)
        codec.start()

        val sampleLists = mutableListOf<FloatArray>()
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var totalOutputSamples = 0

        try {
            while (!outputDone) {
                // Feed input
                if (!inputDone) {
                    val inputBufferIndex = codec.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val presentationTimeUs = extractor.sampleTime
                            codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0)
                            extractor.advance()
                        }
                    }
                }

                // Read output
                val outputBufferIndex = codec.dequeueOutputBuffer(info, 10000)
                if (outputBufferIndex >= 0) {
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)!!
                    if (info.size > 0) {
                        val chunk = decodePcmToFloat(
                            outputBuffer,
                            info.offset,
                            info.size,
                            sourceChannels
                        )
                        sampleLists.add(chunk)
                        totalOutputSamples += chunk.size
                    }
                    codec.releaseOutputBuffer(outputBufferIndex, false)

                    if (durationMs > 0) {
                        val progress = min(1.0f, info.presentationTimeUs / 1000.0f / durationMs.toFloat())
                        progressCallback?.invoke(progress)
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.i(TAG, "Output format changed: ${codec.outputFormat}")
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }

        // Merge chunks
        val merged = FloatArray(totalOutputSamples)
        var offset = 0
        for (chunk in sampleLists) {
            System.arraycopy(chunk, 0, merged, offset, chunk.size)
            offset += chunk.size
        }

        // Downsample if needed
        val finalSamples = if (sourceSampleRate != TARGET_SAMPLE_RATE) {
            downsample(merged, sourceSampleRate, TARGET_SAMPLE_RATE)
        } else {
            merged
        }

        val actualDurationMs = if (durationMs > 0) durationMs else (finalSamples.size.toLong() * 1000 / TARGET_SAMPLE_RATE)

        progressCallback?.invoke(1.0f)
        return DecodedAudio(finalSamples, TARGET_SAMPLE_RATE, actualDurationMs, 1)
    }

    /**
     * Decode raw PCM bytes (16-bit signed LE) to float array, mixing to mono.
     */
    private fun decodePcmToFloat(buffer: ByteBuffer, offset: Int, size: Int, channels: Int): FloatArray {
        val bytesPerFrame = channels * 2 // 16-bit per channel
        val frameCount = size / bytesPerFrame
        val result = FloatArray(frameCount)

        buffer.position(offset)
        for (i in 0 until frameCount) {
            var sum = 0
            for (c in 0 until channels) {
                val lo = buffer.get().toInt() and 0xFF
                val hi = buffer.get().toInt()
                val sample = (hi shl 8) or lo
                sum += sample
            }
            // Average across channels and normalize to -1.0..1.0
            result[i] = (sum / channels) / 32768.0f
        }
        return result
    }

    /**
     * Simple linear downsample by integer ratio.
     */
    private fun downsample(input: FloatArray, fromRate: Int, toRate: Int): FloatArray {
        if (fromRate == toRate) return input
        val ratio = fromRate.toDouble() / toRate
        val outLength = (input.size / ratio).toInt()
        val result = FloatArray(outLength)
        for (i in 0 until outLength) {
            val srcPos = (i * ratio).toInt()
            val srcEnd = min(input.size, ((i + 1) * ratio).toInt())
            var sum = 0.0f
            var count = 0
            for (j in srcPos until srcEnd) {
                sum += input[j]
                count++
            }
            result[i] = if (count > 0) sum / count else 0.0f
        }
        return result
    }
}