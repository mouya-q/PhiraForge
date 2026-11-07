package dev.mouya.phiraforge.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.mouya.phiraforge.PhiraForgeApp
import dev.mouya.phiraforge.R
import dev.mouya.phiraforge.GenerationOrchestrator
import dev.mouya.phiraforge.chart.ChartGenerator
import dev.mouya.phiraforge.util.PFLog
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GenerateScreen(
    onGenerationComplete: (ChartGenerator.GenerationResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { PhiraForgeApp.instance.settingsManager }

    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var audioFileName by remember { mutableStateOf("") }
    var selectedDifficulty by remember {
        mutableStateOf(settingsManager.getDifficulty())
    }
    var bpmInput by remember { mutableStateOf("") }
    var useAI by remember { mutableStateOf(settingsManager.aiEnabled) }
    var isGenerating by remember { mutableStateOf(false) }
    var progressStage by remember { mutableStateOf("") }
    var progressValue by remember { mutableStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successResult by remember { mutableStateOf<ChartGenerator.GenerationResult?>(null) }

    val orchestrator = remember {
        GenerationOrchestrator(context, settingsManager)
    }

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedAudioUri = it
            audioFileName = it.lastPathSegment?.substringAfterLast('/') ?: "audio"
            errorMessage = null
            PFLog.i("Audio selected: $audioFileName")
        }
    }

    val fileSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { outputUri ->
            successResult?.let { result ->
                scope.launch {
                    val success = orchestrator.exportChart(
                        result = result,
                        outputUri = outputUri,
                        chartName = audioFileName,
                        artist = "Unknown"
                    )
                    if (!success) {
                        errorMessage = "导出失败"
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.generate_title)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Audio import section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.generate_import_audio),
                        style = MiuixTheme.textStyles.title2
                    )
                    Text(
                        text = stringResource(R.string.generate_import_audio_desc),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    if (selectedAudioUri != null) {
                        Text(
                            text = "✓ $audioFileName",
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.primary
                        )
                    }
                    Button(
                        text = stringResource(R.string.generate_import_audio),
                        onClick = {
                            audioPicker.launch(arrayOf("audio/*"))
                        }
                    )
                }
            }

            // Difficulty selection
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.generate_difficulty),
                        style = MiuixTheme.textStyles.title2
                    )
                    ChartGenerator.Difficulty.values().forEach { difficulty ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDifficulty == difficulty,
                                onClick = { selectedDifficulty = difficulty }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = difficulty.label,
                                    style = MiuixTheme.textStyles.body1
                                )
                            }
                        }
                    }
                }
            }

            // BPM input
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.generate_bpm_hint),
                        style = MiuixTheme.textStyles.title2
                    )
                    TextField(
                        text = bpmInput,
                        onTextChange = { bpmInput = it.filter { c -> c.isDigit() || c == '.' } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // AI enhancement toggle
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.generate_ai_enhance),
                        style = MiuixTheme.textStyles.title2
                    )
                    Text(
                        text = stringResource(R.string.generate_ai_enhance_desc),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = useAI,
                            onCheckedChange = {
                                useAI = it
                                if (it && !settingsManager.aiEnabled) {
                                    errorMessage = "请在设置中配置 AI API"
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (useAI) "已启用" else "未启用",
                            style = MiuixTheme.textStyles.body1
                        )
                    }
                }
            }

            // Progress display
            if (isGenerating) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = progressStage,
                            style = MiuixTheme.textStyles.body1
                        )
                        LinearProgressIndicator(
                            progress = progressValue,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            text = stringResource(R.string.generate_cancel),
                            onClick = {
                                orchestrator.cancel()
                            },
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }

            // Error display
            errorMessage?.let { msg ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = msg,
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Success display
            successResult?.let { result ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.generate_success),
                            style = MiuixTheme.textStyles.title2,
                            color = MiuixTheme.colorScheme.primary
                        )
                        Text("物量：${result.noteCount}")
                        Text("BPM：${result.bpm.toInt()}")
                        Text("判定线：${result.judgeLineCount}")
                        Text("NPS：${"%.1f".format(result.nps)}")
                        if (result.usedAI) {
                            Text("AI 增强：已使用")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                text = stringResource(R.string.generate_export),
                                onClick = {
                                    fileSaver.launch("phiraforge_${System.currentTimeMillis()}.json")
                                }
                            )
                        }
                    }
                }
            }

            // Generate button
            if (!isGenerating) {
                Button(
                    text = stringResource(R.string.generate_start),
                    onClick = {
                        if (selectedAudioUri == null) {
                            errorMessage = context.getString(R.string.generate_no_audio)
                            return@Button
                        }
                        errorMessage = null
                        successResult = null
                        isGenerating = true
                        scope.launch {
                            try {
                                val bpmOverride = bpmInput.toDoubleOrNull()
                                orchestrator.generate(
                                    audioUri = selectedAudioUri!!,
                                    difficulty = selectedDifficulty,
                                    bpm = bpmOverride,
                                    useAI = useAI,
                                    onProgress = { progress ->
                                        when (progress) {
                                            is GenerationOrchestrator.Progress.Update -> {
                                                progressStage = progress.stage
                                                progressValue = progress.progress
                                            }
                                            is GenerationOrchestrator.Progress.Complete -> {
                                                isGenerating = false
                                                successResult = progress.result
                                                onGenerationComplete(progress.result)
                                            }
                                            is GenerationOrchestrator.Progress.Failed -> {
                                                isGenerating = false
                                                errorMessage = progress.message
                                            }
                                            is GenerationOrchestrator.Progress.Cancelled -> {
                                                isGenerating = false
                                            }
                                        }
                                    }
                                )
                            } catch (e: Exception) {
                                isGenerating = false
                                errorMessage = e.message ?: "Unknown error"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}