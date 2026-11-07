package dev.mouya.phiraforge.chart

import dev.mouya.phiraforge.audio.OnsetDetector
import kotlin.math.*
import kotlin.random.Random

/**
 * Chart generator that converts onset detection results into playable RPE charts.
 *
 * Generation strategy:
 * 1. Snap onsets to beat grid (quantization)
 * 2. Assign notes to judge lines based on musical phrases
 * 3. Choose note types based on onset strength and musical context
 * 4. Position notes horizontally with variety and anti-clustering
 * 5. Apply difficulty scaling via density, note type variety, and positioning complexity
 *
 * Difficulty tiers:
 * - Easy:   1 judge line, sparse taps only, slow speed
 * - Normal: 1 judge line, taps + occasional holds, moderate density
 * - Hard:   2 judge lines, taps + holds + drags + flicks, higher density
 * - Extra:  2-3 judge lines, full variety, high density, complex patterns
 */
class ChartGenerator {

    enum class Difficulty(val label: String, val densityMultiplier: Float) {
        EASY("Easy", 0.35f),
        NORMAL("Normal", 0.6f),
        HARD("Hard", 0.85f),
        EXTRA("Extra", 1.0f)
    }

    data class GenerationParams(
        val difficulty: Difficulty = Difficulty.NORMAL,
        val bpm: Double? = null, // null = auto-detect
        val offsetMs: Int = 0,
        val seed: Long = System.currentTimeMillis()
    )

    data class GenerationResult(
        val chart: RPEChart,
        val noteCount: Int,
        val judgeLineCount: Int,
        val bpm: Double,
        val durationSeconds: Double,
        val nps: Double, // notes per second
        val difficulty: Difficulty,
        val usedAI: Boolean
    )

    /**
     * Generate a complete RPE chart from onset analysis.
     */
    fun generate(
        analysis: OnsetDetector.AnalysisResult,
        durationSeconds: Double,
        params: GenerationParams,
        onProgress: ((Float) -> Unit)? = null
    ): GenerationResult {
        val bpm = params.bpm ?: analysis.bpm
        val rng = Random(params.seed)

        // Quantize onsets to beat grid
        val quantizedOnsets = quantizeOnsets(analysis.onsets, bpm, params.difficulty)
        onProgress?.invoke(0.3f)

        // Determine judge line count based on difficulty
        val lineCount = when (params.difficulty) {
            Difficulty.EASY -> 1
            Difficulty.NORMAL -> 1
            Difficulty.HARD -> 2
            Difficulty.EXTRA -> if (analysis.onsets.size > 200) 3 else 2
        }

        // Assign onsets to judge lines and generate notes
        val allNotes = mutableListOf<RPENote>()
        val lines = mutableListOf<RPEJudgeLine>()

        for (lineIdx in 0 until lineCount) {
            val lineOnsets = assignOnsetsToLine(quantizedOnsets, lineIdx, lineCount, rng)
            val notes = generateNotesForLine(lineOnsets, bpm, params.difficulty, lineIdx, rng)
            allNotes.addAll(notes)

            val line = RPEJudgeLine(
                name = "JudgeLine ${lineIdx + 1}",
                texture = "line.png",
                father = -1,
                rotateWithFather = 0,
                eventLayers = listOf(createEventLayer(bpm, durationSeconds)),
                notes = notes,
                isCover = if (lineIdx == 0) 1 else 0,
                zOrder = lineIdx
            )
            lines.add(line)
        }

        onProgress?.invoke(0.7f)

        // Create BPM list (single constant BPM for generated charts)
        val bpmList = listOf(
            RPEBpmItem(bpm = bpm, startTime = Triple(num = 0, den = 1, beats = 0.0))
        )

        val chart = RPEChart(
            meta = RPEMetadata(offset = params.offsetMs, rpeVersion = 160),
            bpmList = bpmList,
            judgeLineList = lines
        )

        onProgress?.invoke(1.0f)

        val nps = if (durationSeconds > 0) allNotes.size / durationSeconds else 0.0

        return GenerationResult(
            chart = chart,
            noteCount = allNotes.size,
            judgeLineCount = lineCount,
            bpm = bpm,
            durationSeconds = durationSeconds,
            nps = nps,
            difficulty = params.difficulty,
            usedAI = false
        )
    }

    /**
     * Quantize onset times to beat grid positions.
     */
    private fun quantizeOnsets(
        onsets: List<OnsetDetector.Onset>,
        bpm: Double,
        difficulty: Difficulty
    ): List<QuantizedOnset> {
        val beatDuration = 60.0 / bpm
        // Difficulty determines quantization granularity
        val division = when (difficulty) {
            Difficulty.EASY -> 2 // half beats
            Difficulty.NORMAL -> 4 // quarter beats
            Difficulty.HARD -> 8 // eighth beats
            Difficulty.EXTRA -> 16 // sixteenth beats
        }
        val gridStep = beatDuration / division

        // Apply density filtering
        val densityThreshold = when (difficulty) {
            Difficulty.EASY -> 0.4f
            Difficulty.NORMAL -> 0.25f
            Difficulty.HARD -> 0.12f
            Difficulty.EXTRA -> 0.0f
        }

        val filtered = onsets.filter { it.strength > densityThreshold }

        // Snap to grid
        val result = mutableListOf<QuantizedOnset>()
        val usedSlots = mutableSetOf<Int>()

        for (onset in filtered) {
            val gridIdx = (onset.timeSeconds / gridStep).roundToInt()
            if (gridIdx !in usedSlots) {
                val snappedTime = gridIdx * gridStep
                val beatPos = snappedTime / beatDuration
                result.add(QuantizedOnset(snappedTime, beatPos, onset.strength, gridIdx))
                usedSlots.add(gridIdx)
            }
        }

        return result.sortedBy { it.timeSeconds }
    }

    /**
     * Assign onsets to a specific judge line.
     */
    private fun assignOnsetsToLine(
        onsets: List<QuantizedOnset>,
        lineIdx: Int,
        lineCount: Int,
        rng: Random
    ): List<QuantizedOnset> {
        if (lineCount == 1) return onsets

        // Distribute by alternating pattern with some randomness
        return onsets.filterIndexed { index, _ ->
            if (lineCount == 2) {
                index % 2 == lineIdx && rng.nextFloat() > 0.15f
            } else {
                index % lineCount == lineIdx && rng.nextFloat() > 0.1f
            }
        }
    }

    /**
     * Generate RPENotes for a judge line from quantized onsets.
     */
    private fun generateNotesForLine(
        onsets: List<QuantizedOnset>,
        bpm: Double,
        difficulty: Difficulty,
        lineIdx: Int,
        rng: Random
    ): List<RPENote> {
        val notes = mutableListOf<RPENote>()
        var lastPositionX = 0f
        val positionRange = RPEConstants.RPE_WIDTH / 2 * 0.8f // ±540 RPE pixels

        for ((index, onset) in onsets.withIndex()) {
            val triple = BeatTimeConverter.secondsToTripleOptimal(onset.timeSeconds, bpm)

            // Determine note type
            val noteType = selectNoteType(onset, index, onsets, difficulty, rng)

            // Calculate position with anti-clustering
            val positionX = calculatePosition(
                onset, index, onsets, difficulty, lastPositionX, positionRange, rng
            )
            lastPositionX = positionX

            // Determine above (note side)
            val above = if (rng.nextFloat() > 0.5f) 1 else 0

            // Note speed
            val speed = when (difficulty) {
                Difficulty.EASY -> 1.0f
                Difficulty.NORMAL -> 1.0f + rng.nextFloat() * 0.3f
                Difficulty.HARD -> 1.2f + rng.nextFloat() * 0.5f
                Difficulty.EXTRA -> 1.5f + rng.nextFloat() * 0.8f
            }

            val note = if (noteType == RPEConstants.NOTE_TYPE_HOLD) {
                // Hold note: calculate end time based on onset strength
                val holdBeats = when (difficulty) {
                    Difficulty.EASY -> 1.0
                    Difficulty.NORMAL -> 0.5 + rng.nextDouble() * 0.5
                    Difficulty.HARD -> 0.25 + rng.nextDouble() * 0.5
                    Difficulty.EXTRA -> 0.125 + rng.nextDouble() * 0.375
                }
                val endTimeSeconds = onset.timeSeconds + holdBeats * 60.0 / bpm
                val endTriple = BeatTimeConverter.secondsToTripleOptimal(endTimeSeconds, bpm)
                RPENote(
                    type = RPEConstants.NOTE_TYPE_HOLD,
                    above = above,
                    startTime = triple,
                    endTime = endTriple,
                    positionX = positionX,
                    speed = speed,
                    size = 1.0f
                )
            } else {
                RPENote(
                    type = noteType,
                    above = above,
                    startTime = triple,
                    endTime = triple,
                    positionX = positionX,
                    speed = speed,
                    size = 1.0f
                )
            }
            notes.add(note)
        }

        // Sort by time
        return notes.sortedBy { BeatTimeConverter.tripleToSeconds(it.startTime, bpm) }
    }

    /**
     * Select note type based on onset strength, position in sequence, and difficulty.
     */
    private fun selectNoteType(
        onset: QuantizedOnset,
        index: Int,
        allOnsets: List<QuantizedOnset>,
        difficulty: Difficulty,
        rng: Random
    ): Int {
        val r = rng.nextFloat()
        val strength = onset.strength

        return when (difficulty) {
            Difficulty.EASY -> {
                when {
                    strength > 0.8f && r < 0.15f -> RPEConstants.NOTE_TYPE_HOLD
                    else -> RPEConstants.NOTE_TYPE_CLICK
                }
            }
            Difficulty.NORMAL -> {
                when {
                    strength > 0.8f && r < 0.2f -> RPEConstants.NOTE_TYPE_HOLD
                    r < 0.05f -> RPEConstants.NOTE_TYPE_DRAG
                    else -> RPEConstants.NOTE_TYPE_CLICK
                }
            }
            Difficulty.HARD -> {
                when {
                    strength > 0.7f && r < 0.15f -> RPEConstants.NOTE_TYPE_HOLD
                    r < 0.12f -> RPEConstants.NOTE_TYPE_DRAG
                    r < 0.18f -> RPEConstants.NOTE_TYPE_FLICK
                    else -> RPEConstants.NOTE_TYPE_CLICK
                }
            }
            Difficulty.EXTRA -> {
                when {
                    strength > 0.6f && r < 0.18f -> RPEConstants.NOTE_TYPE_HOLD
                    r < 0.15f -> RPEConstants.NOTE_TYPE_DRAG
                    r < 0.25f -> RPEConstants.NOTE_TYPE_FLICK
                    r < 0.3f -> RPEConstants.NOTE_TYPE_HOLD
                    else -> RPEConstants.NOTE_TYPE_CLICK
                }
            }
        }
    }

    /**
     * Calculate note horizontal position with anti-clustering and variety.
     */
    private fun calculatePosition(
        onset: QuantizedOnset,
        index: Int,
        allOnsets: List<QuantizedOnset>,
        difficulty: Difficulty,
        lastPosition: Float,
        range: Float,
        rng: Random
    ): Float {
        val spread = when (difficulty) {
            Difficulty.EASY -> 0.4f
            Difficulty.NORMAL -> 0.6f
            Difficulty.HARD -> 0.8f
            Difficulty.EXTRA -> 1.0f
        }

        // Generate position that avoids clustering with last note
        val minDistance = range * 0.15f * (1.0f + difficulty.densityMultiplier)
        var attempts = 0
        var pos: Float

        do {
            // Mix of sequential pattern and randomness
            val patternBase = if (index % 2 == 0) -range * spread else range * spread
            val noise = (rng.nextFloat() - 0.5f) * range * spread * 1.5f
            pos = (patternBase + noise).coerceIn(-range, range)
            attempts++
        } while (abs(pos - lastPosition) < minDistance && attempts < 5)

        return pos
    }

    /**
     * Create default event layer with constant speed.
     */
    private fun createEventLayer(bpm: Double, durationSeconds: Double): RPEEventLayer {
        val endTime = BeatTimeConverter.secondsToTripleOptimal(durationSeconds, bpm)
        return RPEEventLayer(
            alphaEvents = listOf(
                RPEEvent(start = 255f, end = 255f, startTime = Triple(), endTime = endTime)
            ),
            moveXEvents = listOf(
                RPEEvent(start = 0f, end = 0f, startTime = Triple(), endTime = endTime)
            ),
            moveYEvents = listOf(
                RPEEvent(start = 0f, end = 0f, startTime = Triple(), endTime = endTime)
            ),
            rotateEvents = listOf(
                RPEEvent(start = 0f, end = 0f, startTime = Triple(), endTime = endTime)
            ),
            speedEvents = listOf(
                RPEEvent(start = 1f, end = 1f, startTime = Triple(), endTime = endTime)
            )
        )
    }

    private data class QuantizedOnset(
        val timeSeconds: Double,
        val beatPos: Double,
        val strength: Float,
        val gridIdx: Int
    )
}