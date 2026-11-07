package dev.mouya.phiraforge.chart

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class BeatTimeConverterTest {

    @Test
    fun testSecondsToTriple_basicConversion() {
        val bpm = 120.0
        val seconds = 1.0 // 1 second at 120 BPM = 2 beats
        val triple = BeatTimeConverter.secondsToTriple(seconds, bpm)
        assertEquals(2.0, triple.beats, 0.01)
        assertEquals(192, triple.den)
        assertEquals(384, triple.num) // 2 * 192
    }

    @Test
    fun testTripleToSeconds_roundTrip() {
        val bpm = 140.0
        val originalSeconds = 3.5
        val triple = BeatTimeConverter.secondsToTripleOptimal(originalSeconds, bpm)
        val backToSeconds = BeatTimeConverter.tripleToSeconds(triple, bpm)
        assertTrue(abs(backToSeconds - originalSeconds) < 0.01)
    }

    @Test
    fun testBestDenominator_wholeNumber() {
        val den = BeatTimeConverter.bestDenominator(4.0)
        assertEquals(1, den)
    }

    @Test
    fun testBestDenominator_halfBeat() {
        val den = BeatTimeConverter.bestDenominator(0.5)
        assertEquals(2, den)
    }

    @Test
    fun testBestDenominator_quarterBeat() {
        val den = BeatTimeConverter.bestDenominator(0.25)
        assertEquals(4, den)
    }

    @Test
    fun testSecondsToTripleOptimal_precision() {
        val bpm = 120.0
        // 0.5 seconds at 120 BPM = 1 beat, denominator should be 1
        val triple = BeatTimeConverter.secondsToTripleOptimal(0.5, bpm)
        assertEquals(1, triple.den)
        assertEquals(1, triple.num)
        assertEquals(1.0, triple.beats, 0.001)
    }

    @Test
    fun testSecondsToTriple_zeroSeconds() {
        val triple = BeatTimeConverter.secondsToTriple(0.0, 120.0)
        assertEquals(0, triple.num)
        assertEquals(0.0, triple.beats, 0.001)
    }
}