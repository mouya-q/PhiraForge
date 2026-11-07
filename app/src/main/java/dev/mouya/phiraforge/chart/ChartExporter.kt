package dev.mouya.phiraforge.chart

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Chart serializer and exporter for Phira-compatible RPE JSON format.
 *
 * Produces a .json file that can be directly imported into Phira.
 * The JSON structure matches what Phira's rpe.rs parser expects:
 * camelCase keys, META/BPMList/judgeLineList top-level structure.
 */
class ChartExporter {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Serialize chart to RPE JSON string.
     */
    fun toJson(chart: RPEChart): String {
        return json.encodeToString(chart)
    }

    /**
     * Validate a chart for Phira compatibility.
     * Returns a list of issues; empty list means valid.
     */
    fun validate(chart: RPEChart): List<String> {
        val issues = mutableListOf<String>()

        // META validation
        if (chart.meta.rpeVersion < 100) {
            issues.add("RPEVersion is too low (${chart.meta.rpeVersion}), recommend >= 160")
        }

        // BPMList validation
        if (chart.bpmList.isEmpty()) {
            issues.add("BPMList is empty")
        } else {
            for ((i, bpmItem) in chart.bpmList.withIndex()) {
                if (bpmItem.bpm <= 0 || bpmItem.bpm > 500) {
                    issues.add("BPM at index $i is out of range: ${bpmItem.bpm}")
                }
            }
        }

        // JudgeLine validation
        if (chart.judgeLineList.isEmpty()) {
            issues.add("judgeLineList is empty")
            return issues
        }

        for ((lineIdx, line) in chart.judgeLineList.withIndex()) {
            if (line.eventLayers.isEmpty()) {
                issues.add("JudgeLine $lineIdx has no event layers")
            }

            // Check that event layers have speed events
            for ((layerIdx, layer) in line.eventLayers.withIndex()) {
                layer?.let {
                    if (it.speedEvents.isEmpty()) {
                        issues.add("JudgeLine $lineIdx layer $layerIdx has no speed events")
                    }
                    if (it.alphaEvents.isEmpty()) {
                        issues.add("JudgeLine $lineIdx layer $layerIdx has no alpha events")
                    }
                }
            }

            // Validate notes
            for ((noteIdx, note) in line.notes.withIndex()) {
                if (note.type !in 1..4) {
                    issues.add("JudgeLine $lineIdx note $noteIdx has invalid type: ${note.type}")
                }
                if (note.type == 2) { // Hold
                    val startBeats = note.startTime.beats
                    val endBeats = note.endTime.beats
                    if (endBeats <= startBeats) {
                        issues.add("JudgeLine $lineIdx note $noteIdx (Hold) has endTime <= startTime")
                    }
                }
                if (note.positionX < -700 || note.positionX > 700) {
                    issues.add("JudgeLine $lineIdx note $noteIdx positionX out of range: ${note.positionX}")
                }
            }
        }

        return issues
    }

    /**
     * Create a Phira info.txt file content for the chart package.
     * Phira expects an info.txt with chart metadata.
     */
    fun createInfoJson(
        chartName: String,
        artist: String,
        charter: String,
        difficulty: String,
        difficultyLevel: Int,
        musicFile: String,
        chartFile: String
    ): String {
        // Phira uses a JSON-based info file
        val info = mapOf(
            "name" to chartName,
            "artist" to artist,
            "charter" to charter,
            "difficulty" to mapOf(
                "rating" to difficultyLevel,
                "name" to difficulty
            ),
            "music" to musicFile,
            "chart" to chartFile
        )
        return json.encodeToString(info)
    }
}