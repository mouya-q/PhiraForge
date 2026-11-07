package dev.mouya.phiraforge.chart

import org.junit.Assert.*
import org.junit.Test

class ChartExporterTest {

    private fun makeSimpleChart(): RPEChart {
        return RPEChart(
            meta = RPEMetadata(offset = 0, rpeVersion = 160),
            bpmList = listOf(RPEBpmItem(bpm = 120.0, startTime = Triple())),
            judgeLineList = listOf(
                RPEJudgeLine(
                    name = "TestLine",
                    notes = listOf(
                        RPENote(
                            type = 1,
                            above = 1,
                            startTime = Triple(num = 0, den = 1, beats = 0.0),
                            endTime = Triple(num = 0, den = 1, beats = 0.0),
                            positionX = 0f
                        ),
                        RPENote(
                            type = 2,
                            above = 1,
                            startTime = Triple(num = 4, den = 1, beats = 4.0),
                            endTime = Triple(num = 8, den = 1, beats = 8.0),
                            positionX = 100f
                        )
                    )
                )
            )
        )
    }

    @Test
    fun testToJson_producesValidJson() {
        val exporter = ChartExporter()
        val json = exporter.toJson(makeSimpleChart())
        assertTrue(json.contains("\"META\""))
        assertTrue(json.contains("\"BPMList\""))
        assertTrue(json.contains("\"judgeLineList\""))
        assertTrue(json.contains("\"offset\""))
        assertTrue(json.contains("\"RPEVersion\""))
        assertTrue(json.contains("\"bpm\""))
    }

    @Test
    fun testValidate_validChartNoIssues() {
        val exporter = ChartExporter()
        val issues = exporter.validate(makeSimpleChart())
        assertTrue("Valid chart should have no issues: $issues", issues.isEmpty())
    }

    @Test
    fun testValidate_emptyBpmListFails() {
        val exporter = ChartExporter()
        val chart = RPEChart(
            meta = RPEMetadata(),
            bpmList = emptyList(),
            judgeLineList = listOf(RPEJudgeLine())
        )
        val issues = exporter.validate(chart)
        assertTrue(issues.any { it.contains("BPMList is empty") })
    }

    @Test
    fun testValidate_emptyJudgeLineListFails() {
        val exporter = ChartExporter()
        val chart = RPEChart(
            meta = RPEMetadata(),
            bpmList = listOf(RPEBpmItem(bpm = 120.0, startTime = Triple())),
            judgeLineList = emptyList()
        )
        val issues = exporter.validate(chart)
        assertTrue(issues.any { it.contains("judgeLineList is empty") })
    }

    @Test
    fun testValidate_invalidNoteTypeFails() {
        val exporter = ChartExporter()
        val chart = RPEChart(
            meta = RPEMetadata(),
            bpmList = listOf(RPEBpmItem(bpm = 120.0, startTime = Triple())),
            judgeLineList = listOf(
                RPEJudgeLine(
                    notes = listOf(RPENote(type = 99))
                )
            )
        )
        val issues = exporter.validate(chart)
        assertTrue(issues.any { it.contains("invalid type") })
    }

    @Test
    fun testValidate_holdNoteEndTimeBeforeStartFails() {
        val exporter = ChartExporter()
        val chart = RPEChart(
            meta = RPEMetadata(),
            bpmList = listOf(RPEBpmItem(bpm = 120.0, startTime = Triple())),
            judgeLineList = listOf(
                RPEJudgeLine(
                    notes = listOf(
                        RPENote(
                            type = 2,
                            startTime = Triple(num = 8, den = 1, beats = 8.0),
                            endTime = Triple(num = 4, den = 1, beats = 4.0)
                        )
                    )
                )
            )
        )
        val issues = exporter.validate(chart)
        assertTrue(issues.any { it.contains("endTime <= startTime") })
    }

    @Test
    fun testCreateInfoJson_containsRequiredFields() {
        val exporter = ChartExporter()
        val info = exporter.createInfoJson(
            chartName = "Test",
            artist = "Artist",
            charter = "mouya",
            difficulty = "Normal",
            difficultyLevel = 8,
            musicFile = "song.mp3",
            chartFile = "chart.json"
        )
        assertTrue(info.contains("\"name\""))
        assertTrue(info.contains("\"artist\""))
        assertTrue(info.contains("\"charter\""))
        assertTrue(info.contains("\"difficulty\""))
        assertTrue(info.contains("\"music\""))
        assertTrue(info.contains("\"chart\""))
    }
}