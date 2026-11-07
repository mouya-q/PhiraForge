package dev.mouya.phiraforge.audio

import kotlin.math.*

/**
 * Onset detection using spectral flux analysis.
 *
 * Algorithm:
 * 1. Compute short-time Fourier transform (STFT) with Hann window
 * 2. Calculate spectral flux (sum of positive magnitude changes between frames)
 * 3. Apply adaptive thresholding (moving window mean + k * std)
 * 4. Peak-pick to get onset times
 *
 * This is a lightweight pure-Kotlin implementation — no external DSP library needed.
 * Optimized for 22050 Hz mono input, 1024-sample FFT frames with 512-hop.
 */
class OnsetDetector(
    private val sampleRate: Int = AudioDecoder.TARGET_SAMPLE_RATE,
    private val fftSize: Int = 1024,
    private val hopSize: Int = 512
) {

    data class Onset(
        val timeSeconds: Double,
        val strength: Float
    )

    data class AnalysisResult(
        val onsets: List<Onset>,
        val bpm: Double,
        val bpmConfidence: Double,
        val energy: FloatArray,
        val spectralFlux: FloatArray
    )

    /**
     * Perform full onset detection and BPM estimation.
     */
    fun analyze(samples: FloatArray, onProgress: ((Float) -> Unit)? = null): AnalysisResult {
        val numFrames = max(1, (samples.size - fftSize) / hopSize)
        val window = hannWindow(fftSize)

        // Compute magnitude spectrum for each frame
        val magnitudes = Array(numFrames) { FloatArray(fftSize / 2) }
        val real = FloatArray(fftSize)
        val imag = FloatArray(fftSize)

        for (frame in 0 until numFrames) {
            val start = frame * hopSize
            for (i in 0 until fftSize) {
                val sampleIndex = start + i
                real[i] = if (sampleIndex < samples.size) samples[sampleIndex] * window[i] else 0f
                imag[i] = 0f
            }
            fft(real, imag)
            for (k in 0 until fftSize / 2) {
                magnitudes[frame][k] = sqrt(real[k] * real[k] + imag[k] * imag[k])
            }
            if (frame % 50 == 0) onProgress?.invoke(frame.toFloat() / numFrames * 0.5f)
        }

        // Spectral flux
        val flux = FloatArray(numFrames)
        for (frame in 1 until numFrames) {
            var sum = 0f
            for (k in 0 until fftSize / 2) {
                val diff = magnitudes[frame][k] - magnitudes[frame - 1][k]
                if (diff > 0) sum += diff
            }
            flux[frame] = sum
        }

        // Energy
        val energy = FloatArray(numFrames)
        for (frame in 0 until numFrames) {
            var e = 0f
            for (k in 0 until fftSize / 2) {
                e += magnitudes[frame][k] * magnitudes[frame][k]
            }
            energy[frame] = sqrt(e)
        }

        // Adaptive threshold peak picking
        val onsets = peakPick(flux, onProgress)

        // BPM estimation via autocorrelation of onset times
        val (bpm, confidence) = estimateBpm(onsets)

        onProgress?.invoke(1.0f)
        return AnalysisResult(onsets, bpm, confidence, energy, flux)
    }

    /**
     * Hann window function.
     */
    private fun hannWindow(n: Int): FloatArray {
        return FloatArray(n) { i ->
            (0.5 - 0.5 * cos(2.0 * PI * i / (n - 1))).toFloat()
        }
    }

    /**
     * In-place FFT (Cooley-Tukey radix-2). Size must be power of 2.
     */
    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        if (n <= 1) return

        // Bit reversal
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                val tr = real[i]
                real[i] = real[j]
                real[j] = tr
                val ti = imag[i]
                imag[i] = imag[j]
                imag[j] = ti
            }
        }

        // FFT
        var len = 2
        while (len <= n) {
            val halfLen = len / 2
            val angle = -2.0 * PI / len
            val wReal = cos(angle).toFloat()
            val wImag = sin(angle).toFloat()
            var i = 0
            while (i < n) {
                var currentWReal = 1f
                var currentWImag = 0f
                for (k in 0 until halfLen) {
                    val idx1 = i + k
                    val idx2 = i + k + halfLen
                    val tReal = currentWReal * real[idx2] - currentWImag * imag[idx2]
                    val tImag = currentWReal * imag[idx2] + currentWImag * real[idx2]
                    real[idx2] = real[idx1] - tReal
                    imag[idx2] = imag[idx1] - tImag
                    real[idx1] = real[idx1] + tReal
                    imag[idx1] = imag[idx1] + tImag
                    val newWReal = currentWReal * wReal - currentWImag * wImag
                    currentWImag = currentWReal * wImag + currentWImag * wReal
                    currentWReal = newWReal
                }
                i += len
            }
            len *= 2
        }
    }

    /**
     * Adaptive threshold peak picking.
     * Uses a moving window to compute local mean and picks peaks above mean + k*std.
     */
    private fun peakPick(flux: FloatArray, onProgress: ((Float) -> Unit)?): List<Onset> {
        val onsets = mutableListOf<Onset>()
        val windowSize = 20 // ~0.46s at 1024/512/22050

        // Normalize flux
        val maxFlux = flux.maxOrNull() ?: 1f
        if (maxFlux <= 0f) return emptyList()
        val normFlux = flux.map { it / maxFlux }

        for (i in windowSize until flux.size - windowSize) {
            if (normFlux[i] <= 0f) continue

            // Local mean
            var sum = 0.0
            var count = 0
            for (j in (i - windowSize)..(i + windowSize)) {
                if (j >= 0 && j < flux.size) {
                    sum += normFlux[j]
                    count++
                }
            }
            val localMean = if (count > 0) sum / count else 0.0
            val threshold = localMean + 0.3

            if (normFlux[i] > threshold && normFlux[i] >= normFlux[i - 1] && normFlux[i] > normFlux[i + 1]) {
                // Check minimum distance from last onset (50ms)
                val timeSeconds = i.toDouble() * hopSize / sampleRate
                if (onsets.isEmpty() || timeSeconds - onsets.last().timeSeconds > 0.05) {
                    onsets.add(Onset(timeSeconds, normFlux[i].toFloat()))
                }
            }
        }

        onProgress?.invoke(0.9f)
        return onsets
    }

    /**
     * BPM estimation via autocorrelation of inter-onset intervals.
     * Tests BPM range 60-200, finds the period with highest autocorrelation.
     */
    private fun estimateBpm(onsets: List<Onset>): Pair<Double, Double> {
        if (onsets.size < 4) return Pair(120.0, 0.0)

        // Build onset envelope at 100Hz resolution
        val resolution = 100.0 // Hz
        val maxTime = onsets.last().timeSeconds
        val envelopeLength = (maxTime * resolution).toInt() + 1
        val envelope = FloatArray(envelopeLength)

        for (onset in onsets) {
            val idx = (onset.timeSeconds * resolution).toInt()
            if (idx < envelopeLength) {
                envelope[idx] = max(envelope[idx], onset.strength)
            }
        }

        // Autocorrelation for BPM range 60-200
        var bestBpm = 120.0
        var bestScore = 0.0
        val minLag = (resolution * 60.0 / 200.0).toInt() // 200 BPM
        val maxLag = (resolution * 60.0 / 60.0).toInt() // 60 BPM

        for (lag in minLag..maxLag) {
            var score = 0.0
            for (i in 0 until envelopeLength - lag) {
                score += envelope[i] * envelope[i + lag]
            }
            score /= (envelopeLength - lag)

            if (score > bestScore) {
                bestScore = score
                bestBpm = 60.0 * resolution / lag
            }
        }

        // Try halving/doubling
        val alternatives = listOf(bestBpm / 2, bestBpm * 2)
        for (alt in alternatives) {
            if (alt in 60.0..200.0) {
                val altLag = (resolution * 60.0 / alt).toInt()
                if (altLag in minLag..maxLag) {
                    var altScore = 0.0
                    for (i in 0 until envelopeLength - altLag) {
                        altScore += envelope[i] * envelope[i + altLag]
                    }
                    altScore /= (envelopeLength - altLag)
                    if (altScore > bestScore * 1.2) {
                        bestScore = altScore
                        bestBpm = alt
                    }
                }
            }
        }

        val confidence = bestScore / (envelope.map { it * it }.sum() / envelopeLength + 1e-10)
        return Pair(bestBpm.roundToInt().toDouble(), min(1.0, confidence))
    }
}