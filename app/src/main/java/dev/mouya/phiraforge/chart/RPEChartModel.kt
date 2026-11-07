package dev.mouya.phiraforge.chart

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * RPE (Re:PhiEdit) chart format — the primary chart format consumed by Phira.
 *
 * Derived from the official Phira source code (prpr/src/parse/rpe.rs).
 *
 * Key format details:
 * - META: contains offset (ms) and RPEVersion (default 160)
 * - BPMList: array of { bpm, startTime: [beatNum, beatDen, beatValue] }
 * - judgeLineList: array of judge lines, each with eventLayers and notes
 * - Triple (startTime/endTime): [numerator, denominator, beats] — a rational beat number
 * - Note types: 1=Click(Tap), 2=Hold, 3=Flick, 4=Drag
 * - Coordinate system: RPE_WIDTH=1350, RPE_HEIGHT=900
 * - positionX is in RPE pixels (0=center, range -675..675)
 *
 * Reference: https://github.com/TeamFlos/phira/blob/main/prpr/src/parse/rpe.rs
 */

@Serializable
data class RPEChart(
    @kotlinx.serialization.SerialName("META")
    val meta: RPEMetadata,
    @kotlinx.serialization.SerialName("BPMList")
    val bpmList: List<RPEBpmItem>,
    val judgeLineList: List<RPEJudgeLine>
)

@Serializable
data class RPEMetadata(
    val offset: Int = 0,
    @kotlinx.serialization.SerialName("RPEVersion")
    val rpeVersion: Int = 160
)

@Serializable
data class RPEBpmItem(
    val bpm: Double,
    val startTime: Triple
)

@Serializable
data class Triple(
    val num: Int = 0,
    val den: Int = 1,
    val beats: Double = 0.0
)

@Serializable
data class RPEJudgeLine(
    @kotlinx.serialization.SerialName("Name")
    val name: String = "JudgeLine",
    @kotlinx.serialization.SerialName("Texture")
    val texture: String = "line.png",
    @kotlinx.serialization.SerialName("father")
    val father: Int = -1,
    val rotateWithFather: Int = 0,
    val eventLayers: List<RPEEventLayer?> = listOf(RPEEventLayer()),
    val extended: RPEExtendedEvents? = null,
    val notes: List<RPENote> = emptyList(),
    val isCover: Int = 1,
    val zOrder: Int = 0,
    @kotlinx.serialization.SerialName("attachUI")
    val attachUI: String? = null,
    val posControl: List<RPECtrlEvent> = emptyList(),
    val sizeControl: List<RPECtrlEvent> = emptyList(),
    val alphaControl: List<RPECtrlEvent> = emptyList(),
    val yControl: List<RPECtrlEvent> = emptyList()
)

@Serializable
data class RPEEventLayer(
    val alphaEvents: List<RPEEvent> = listOf(RPEEvent(start = 255f, end = 255f)),
    val moveXEvents: List<RPEEvent> = listOf(RPEEvent(start = 0f, end = 0f)),
    val moveYEvents: List<RPEEvent> = listOf(RPEEvent(start = 0f, end = 0f)),
    val rotateEvents: List<RPEEvent> = listOf(RPEEvent(start = 0f, end = 0f)),
    val speedEvents: List<RPEEvent> = listOf(RPEEvent(start = 1f, end = 1f))
)

@Serializable
data class RPEEvent(
    val easingLeft: Float = 0f,
    val easingRight: Float = 1f,
    val bezier: Int = 0,
    val bezierPoints: List<Float> = listOf(0f, 0f, 0f, 0f),
    val easingType: Int = 1,
    val start: Float = 0f,
    val end: Float = 0f,
    val startTime: Triple = Triple(),
    val endTime: Triple = Triple()
)

@Serializable
data class RPEExtendedEvents(
    val colorEvents: JsonElement? = null,
    val textEvents: JsonElement? = null,
    val scaleXEvents: List<RPEEvent>? = null,
    val scaleYEvents: List<RPEEvent>? = null,
    val inclineEvents: List<RPEEvent>? = null,
    val paintEvents: JsonElement? = null,
    val gifEvents: JsonElement? = null
)

@Serializable
data class RPENote(
    @kotlinx.serialization.SerialName("type")
    val type: Int = 1,
    val above: Int = 1,
    val startTime: Triple = Triple(),
    val endTime: Triple = Triple(),
    val positionX: Float = 0f,
    val yOffset: Float = 0f,
    val alpha: Int = 255,
    val hitsound: String? = null,
    val size: Float = 1f,
    val speed: Float = 1f,
    val isFake: Int = 0,
    val visibleTime: Double = 1e9,
    val tint: List<Int>? = null,
    val tintHitEffects: List<Int>? = null,
    val judgeArea: Float? = null
)

@Serializable
data class RPECtrlEvent(
    val easing: Int = 1,
    val x: Double = 0.0,
    val y: Float = 0f
)

object RPEConstants {
    const val RPE_WIDTH = 1350f
    const val RPE_HEIGHT = 900f
    const val NOTE_TYPE_CLICK = 1
    const val NOTE_TYPE_HOLD = 2
    const val NOTE_TYPE_FLICK = 3
    const val NOTE_TYPE_DRAG = 4
}