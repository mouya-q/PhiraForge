package dev.mouya.phiraforge.chart

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Utility for converting between wall-clock seconds and RPE beat-based Triple time.
 *
 * In RPE/Phira, time is expressed as a Triple [num, den, beats] where beats = num/den.
 * A BpmList maps beat positions to wall-clock time: t = sum over BPM segments of
 * (beats_in_segment / bpm) * 60.
 *
 * For generated charts, we use a single constant BPM, so:
 *   beats = time_seconds * bpm / 60
 *   num = round(beats * den)
 */
object BeatTimeConverter {

    /**
     * Convert seconds to a Triple given a constant BPM.
     * Uses denominator 192 (LCM of 2,3,4,6,8,12,16,24,32,48,64,96,192) for max precision.
     */
    fun secondsToTriple(seconds: Double, bpm: Double, denominator: Int = 192): Triple {
        val beats = seconds * bpm / 60.0
        val num = (beats * denominator).roundToInt()
        return Triple(
            num = num,
            den = denominator,
            beats = num.toDouble() / denominator
        )
    }

    /**
     * Convert a Triple beat position back to seconds given a constant BPM.
     */
    fun tripleToSeconds(triple: Triple, bpm: Double): Double {
        val beats = if (triple.beats != 0.0) triple.beats else triple.num.toDouble() / triple.den
        return beats * 60.0 / bpm
    }

    /**
     * Find the best-fit denominator for a given beat value, minimizing rounding error
     * while keeping the denominator small (1, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64, 96, 192).
     */
    fun bestDenominator(beats: Double): Int {
        val candidates = listOf(1, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64, 96, 192)
        for (den in candidates) {
            val num = (beats * den).roundToInt()
            if (abs(num.toDouble() / den - beats) < 1e-6) return den
        }
        return 192
    }

    /**
     * Convert seconds to a Triple with optimized denominator.
     */
    fun secondsToTripleOptimal(seconds: Double, bpm: Double): Triple {
        val beats = seconds * bpm / 60.0
        val den = bestDenominator(beats)
        val num = (beats * den).roundToInt()
        return Triple(num = num, den = den, beats = num.toDouble() / den)
    }
}