package dev.mouya.phiraforge.chart

import dev.mouya.phiraforge.audio.OnsetDetector
import org.junit.Assert.*
import org.junit.Test

class ChartGeneratorTest {

    private fun makeMockAnalysis(
        onsetCount: Int = 50,
        bpm: Double = 120.0,
        durationSeconds: Double = 30.0
    ): OnsetDetector.AnalysisResult {
        val onsets = (0 until onsetCount).map { i ->
            OnsetDetector.Onset(
                timeSeconds = i * durationSeconds / onsetCount,
                strength = 0.5f + (i % 3) * 0.15f
            )
        }
        return OnsetDetector.AnalysisResult(
            onsets = onsets,
            bpm = bpm,
            bpmConfidence = 0.8,
            energy = FloatArray(onsetCount) { 1f },
            spectralFlux = FloatArray(onsetCount) { 0.5f }
        )
    }

    @Test
    fun testGenerate_producesValidChart() {
        val generator = ChartGenerator()
        val analysis = makeMockAnalysis()
        val result = generator.generate(
            analysis = analysis,
            durationSeconds = 30.0,
            params = ChartGenerator.GenerationParams(
                difficulty = ChartGenerator.Difficulty.NORMAL,
                bpm = null,
                offsetMs = 0,
                seed = 42L
            )
        )

        assertNotNull(result.chart)
        assertTrue(result.noteCount > 0)
        assertEquals(120.0, result.bpm, 0.1)
        assertTrue(result.durationSeconds > 0)
    }

    @Test
    fun testGenerate_easyDifficultyFewerNotes() {
        val generator = ChartGenerator()
        val analysis = makeMockAnalysis(onsetCount = 100)

        val easyResult = generator.generate(
            analysis = analysis,
            durationSeconds = 30.0,
            params = ChartGenerator.GenerationParams(
                difficulty = ChartGenerator.Difficulty.EASY,
                seed = 42L
            )
        )

        val extraResult = generator.generate(
            analysis = analysis,
            durationSeconds = 30.0,
            params = ChartGenerator.GenerationParams(
                difficulty = ChartGenerator.Difficulty.EXTRA,
                seed = 42L
            )
        )

        // Easy should have fewer or equal notes than Extra
        assertTrue("Easy (${easyResult.noteCount}) should have <= Extra (${extraResult.noteCount})",
            easyResult.noteCount <= extraResult.noteCount)
    }

    @Test
    fun testGenerate_chartHasValidStructure() {
        val generator = ChartGenerator()
        val analysis = makeMockAnalysis()
        val result = generator.generate(
            analysis = analysis,
            durationSeconds = 30.0,
            params = ChartGenerator.GenerationParams(
                difficulty = ChartGenerator.Difficulty.HARD,
                seed = 42L
            )
        )

        val chart = result.chart
        // META
        assertEquals(160, chart.meta.rpeVersion)
        // BPMList
        assertEquals(1, chart.bpmList.size)
        assertEquals(120.0, chart.bpmList[0].bpm, 0.1)
        // JudgeLineList
        assertTrue(chart.judgeLineList.isNotEmpty())
        // Each line has event layers
        chart.judgeLineList.forEach { line ->
            assertTrue(line.eventLayers.isNotEmpty())
            line.eventLayers.forEach { layer ->
                assertNotNull(layer)
                assertTrue(layer!!.alphaEvents.isNotEmpty())
                assertTrue(layer.speedEvents.isNotEmpty())
            }
        }
    }

    @Test
    fun testGenerate_noteTypesValid() {
        val generator = ChartGenerator()
        val analysis = makeMockAnalysis(onsetCount = 200)
        val result = generator.generate(
            analysis = analysis,
            durationSeconds = 60.0,
            params = ChartGenerator.GenerationParams(
                difficulty = ChartGenerator.Difficulty.EXTRA,
                seed = 42L
            )
        )

        result.chart.judgeLineList.forEach { line ->
            line.notes.forEach { note ->
                assertTrue("Note type ${note.type} should be 1-4", note.type in 1..4)
            }
        }
    }

    @Test
    fun testGenerate_positionXInRange() {
        val generator = ChartGenerator()
        val analysis = makeMockAnalysis(onsetCount = 100)
        val result = generator.generate(
            analysis = analysis,
            durationSeconds = 30.0,
            params = ChartGenerator.GenerationParams(
                difficulty = ChartGenerator.Difficulty.HARD,
                seed = 42L
            )
        )

        result.chart.judgeLineList.forEach { line ->
            line.notes.forEach { note ->
                assertTrue("positionX ${note.positionX} should be in [-675, 675]",
                    note.positionX >= -675f && note.positionX <= 675f)
            }
        }
    }

    @Test
    fun testGenerate_holdNotesHaveValidEndTime() {
        val generator = ChartGenerator()
        val analysis = makeMockAnalysis(onsetCount = 100)
        val result = generator.generate(
            analysis = analysis,
            durationSeconds = 30.0,
            params = ChartGenerator.GenerationParams(
                difficulty = ChartGenerator.Difficulty.NORMAL,
                seed = 42L
            )
        )

        result.chart.judgeLineList.forEach { line ->
            line.notes.filter { it.type == RPEConstants.NOTE_TYPE_HOLD }.forEach { note ->
                assertTrue("Hold note endTime should be > startTime",
                    note.endTime.beats > note.startTime.beats)
            }
        }
    }

    @Test
    fun testGenerate_reproducibleWithSameSeed() {
        val generator = ChartGenerator()
        val analysis = makeMockAnalysis(onsetCount = 50)

        val result1 = generator.generate(
            analysis = analysis,
            durationSeconds = 30.0,
            params = ChartGenerator.GenerationParams(
                difficulty = ChartGenerator.Difficulty.NORMAL,
                seed = 123L
            )
        )

        val result2 = generator.generate(
            analysis = analysis,
            durationSeconds = 30.0,
            params = ChartGenerator.GenerationParams(
                difficulty = ChartGenerator.Difficulty.NORMAL,
                seed = 123L
            )
        )

        assertEquals(result1.noteCount, result2.noteCount)
    }
}