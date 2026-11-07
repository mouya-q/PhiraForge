package dev.mouya.phiraforge.audio

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class OnsetDetectorTest {

    @Test
    fun testAnalyze_sineWaveProducesOnsets() {
        val sampleRate = 22050
        val durationSeconds = 5.0
        val numSamples = (sampleRate * durationSeconds).toInt()

        // Generate a sine wave with abrupt amplitude changes to create onsets
        val samples = FloatArray(numSamples) { i ->
            val timeSeconds = i.toDouble() / sampleRate
            val beatTime = (timeSeconds * 4).toInt() // 4 beats per second = 240 BPM
            val envelope = if (beatTime % 2 == 0) 1.0 else 0.1
            (envelope * sin(2.0 * PI * 440 * timeSeconds)).toFloat()
        }

        val detector = OnsetDetector(sampleRate = sampleRate)
        val result = detector.analyze(samples)

        // Should detect some onsets
        assertTrue("Should detect at least 4 onsets, got ${result.onsets.size}",
            result.onsets.size >= 4)

        // BPM should be reasonable
        assertTrue("BPM ${result.bpm} should be in [60, 300]",
            result.bpm >= 60.0 && result.bpm <= 300.0)
    }

    @Test
    fun testAnalyze_silenceProducesNoOnsets() {
        val sampleRate = 22050
        val samples = FloatArray(sampleRate * 3) { 0f }

        val detector = OnsetDetector(sampleRate = sampleRate)
        val result = detector.analyze(samples)

        assertEquals(0, result.onsets.size)
    }

    @Test
    fun testAnalyze_shortAudioReturnsValidResult() {
        val sampleRate = 22050
        val samples = FloatArray(sampleRate * 5) { i ->
            (sin(2.0 * PI * 220 * i.toDouble() / sampleRate)).toFloat()
        }

        val detector = OnsetDetector(sampleRate = sampleRate)
        val result = detector.analyze(samples)

        // Should not crash and should return a result
        assertNotNull(result)
        assertNotNull(result.onsets)
        assertNotNull(result.energy)
        assertNotNull(result.spectralFlux)
    }

    @Test
    fun testAnalyze_onsetsAreTimeOrdered() {
        val sampleRate = 22050
        val numSamples = sampleRate * 10
        val samples = FloatArray(numSamples) { i ->
            val timeSeconds = i.toDouble() / sampleRate
            val beatTime = (timeSeconds * 2).toInt()
            val envelope = if (beatTime % 2 == 0) 0.8 else 0.05
            (envelope * sin(2.0 * PI * 300 * timeSeconds)).toFloat()
        }

        val detector = OnsetDetector(sampleRate = sampleRate)
        val result = detector.analyze(samples)

        // Onsets should be in ascending time order
        for (i in 1 until result.onsets.size) {
            assertTrue(
                "Onset $i (${result.onsets[i].timeSeconds}) should be after onset ${i-1} (${result.onsets[i-1].timeSeconds})",
                result.onsets[i].timeSeconds >= result.onsets[i - 1].timeSeconds
            )
        }
    }
}