package dev.mouya.phiraforge.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.mouya.phiraforge.R
import dev.mouya.phiraforge.chart.BeatTimeConverter
import dev.mouya.phiraforge.chart.ChartGenerator
import dev.mouya.phiraforge.chart.RPEConstants
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PreviewScreen(
    generationResult: ChartGenerator.GenerationResult?
) {
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.preview_title)
            )
        }
    ) { paddingValues ->
        if (generationResult == null) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.preview_empty),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Stats card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.preview_stats),
                            style = MiuixTheme.textStyles.title2
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.preview_note_count),
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                                Text(
                                    text = "${generationResult.noteCount}",
                                    style = MiuixTheme.textStyles.title1
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.preview_judge_line_count),
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                                Text(
                                    text = "${generationResult.judgeLineCount}",
                                    style = MiuixTheme.textStyles.title1
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.preview_bpm),
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                                Text(
                                    text = "${generationResult.bpm.toInt()}",
                                    style = MiuixTheme.textStyles.title1
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.preview_duration),
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                                Text(
                                    text = "${"%.1f".format(generationResult.durationSeconds)}s",
                                    style = MiuixTheme.textStyles.title1
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.preview_nps),
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                                Text(
                                    text = "${"%.1f".format(generationResult.nps)}",
                                    style = MiuixTheme.textStyles.title1
                                )
                            }
                        }
                    }
                }

                // Timeline visualization
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.preview_timeline),
                            style = MiuixTheme.textStyles.title2
                        )
                        ChartTimeline(
                            result = generationResult,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }

                // Note type distribution
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "音符类型分布",
                            style = MiuixTheme.textStyles.title2
                        )
                        val noteTypeCounts = remember(generationResult) {
                            val counts = mutableMapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0)
                            generationResult.chart.judgeLineList.forEach { line ->
                                line.notes.forEach { note ->
                                    counts[note.type] = counts.getOrDefault(note.type, 0) + 1
                                }
                            }
                            counts
                        }
                        Text("Tap (Click): ${noteTypeCounts[1]}")
                        Text("Hold: ${noteTypeCounts[2]}")
                        Text("Flick: ${noteTypeCounts[3]}")
                        Text("Drag: ${noteTypeCounts[4]}")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ChartTimeline(
    result: ChartGenerator.GenerationResult,
    modifier: Modifier = Modifier
) {
    val primaryColor = MiuixTheme.colorScheme.primary
    val surfaceColor = MiuixTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val durationSeconds = result.durationSeconds.toFloat()

        // Draw background
        drawRect(surfaceColor)

        // Draw center line (judge line position)
        for (lineIdx in 0 until result.judgeLineCount) {
            val y = height * (lineIdx + 1) / (result.judgeLineCount + 1)
            drawLine(
                color = Color.Gray,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 2f
            )
        }

        // Draw notes as dots
        result.chart.judgeLineList.forEachIndexed { lineIdx, line ->
            val y = height * (lineIdx + 1) / (result.judgeLineCount + 1)
            val bpm = result.bpm.toFloat()

            line.notes.forEach { note ->
                val timeSeconds = BeatTimeConverter.tripleToSeconds(note.startTime, result.bpm).toFloat()
                val x = if (durationSeconds > 0) timeSeconds / durationSeconds * width else 0f

                val color = when (note.type) {
                    RPEConstants.NOTE_TYPE_CLICK -> Color(0xFF4CAF50)
                    RPEConstants.NOTE_TYPE_HOLD -> Color(0xFF2196F3)
                    RPEConstants.NOTE_TYPE_FLICK -> Color(0xFFFF9800)
                    RPEConstants.NOTE_TYPE_DRAG -> Color(0xFF9C27B0)
                    else -> primaryColor
                }

                val radius = when (note.type) {
                    RPEConstants.NOTE_TYPE_HOLD -> 6f
                    else -> 4f
                }

                drawCircle(
                    color = color,
                    radius = radius,
                    center = Offset(x, y)
                )

                // Draw hold line
                if (note.type == RPEConstants.NOTE_TYPE_HOLD) {
                    val endTimeSeconds = BeatTimeConverter.tripleToSeconds(note.endTime, result.bpm).toFloat()
                    val endX = if (durationSeconds > 0) endTimeSeconds / durationSeconds * width else 0f
                    drawLine(
                        color = color,
                        start = Offset(x, y),
                        end = Offset(endX, y),
                        strokeWidth = 4f
                    )
                }
            }
        }
    }
}