package dev.mouya.phiraforge.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.mouya.phiraforge.PhiraForgeApp
import dev.mouya.phiraforge.R
import dev.mouya.phiraforge.ai.AIService
import dev.mouya.phiraforge.chart.ChartGenerator
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { PhiraForgeApp.instance.settingsManager }

    var aiEnabled by remember { mutableStateOf(settingsManager.aiEnabled) }
    var aiApiUrl by remember { mutableStateOf(settingsManager.aiApiUrl) }
    var aiApiKey by remember { mutableStateOf(settingsManager.aiApiKey) }
    var aiModel by remember { mutableStateOf(settingsManager.aiModel) }
    var aiTimeout by remember { mutableStateOf(settingsManager.aiTimeoutSeconds.toString()) }
    var testStatus by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }

    var themeMode by remember { mutableStateOf(settingsManager.themeMode) }
    var defaultDifficulty by remember { mutableStateOf(settingsManager.defaultDifficulty) }
    var chartOffset by remember { mutableStateOf(settingsManager.chartOffsetMs.toString()) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.settings_title)
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

            // General settings
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_general),
                        style = MiuixTheme.textStyles.title2
                    )

                    // Theme mode
                    Text(
                        text = stringResource(R.string.settings_theme_mode),
                        style = MiuixTheme.textStyles.body1
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "system" to stringResource(R.string.settings_theme_system),
                            "light" to stringResource(R.string.settings_theme_light),
                            "dark" to stringResource(R.string.settings_theme_dark)
                        ).forEach { (mode, label) ->
                            Button(
                                text = label,
                                onClick = {
                                    themeMode = mode
                                    settingsManager.themeMode = mode
                                }
                            )
                        }
                    }

                    // Default difficulty
                    Text(
                        text = stringResource(R.string.settings_default_difficulty),
                        style = MiuixTheme.textStyles.body1
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChartGenerator.Difficulty.values().forEach { diff ->
                            Button(
                                text = diff.label,
                                onClick = {
                                    defaultDifficulty = diff.name
                                    settingsManager.defaultDifficulty = diff.name
                                }
                            )
                        }
                    }

                    // Chart offset
                    Text(
                        text = stringResource(R.string.settings_chart_offset),
                        style = MiuixTheme.textStyles.body1
                    )
                    TextField(
                        text = chartOffset,
                        onTextChange = {
                            chartOffset = it.filter { c -> c.isDigit() || c == '-' }
                            chartOffset.toIntOrNull()?.let { v ->
                                settingsManager.chartOffsetMs = v
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // AI settings
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_ai),
                        style = MiuixTheme.textStyles.title2
                    )

                    // AI enable toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = aiEnabled,
                            onCheckedChange = {
                                aiEnabled = it
                                settingsManager.aiEnabled = it
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_ai_enabled))
                    }

                    // API URL
                    Text(
                        text = stringResource(R.string.settings_ai_api_url),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    TextField(
                        text = aiApiUrl,
                        onTextChange = {
                            aiApiUrl = it
                            settingsManager.aiApiUrl = it
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // API Key
                    Text(
                        text = stringResource(R.string.settings_ai_api_key),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    TextField(
                        text = aiApiKey,
                        onTextChange = {
                            aiApiKey = it
                            settingsManager.aiApiKey = it
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Model
                    Text(
                        text = stringResource(R.string.settings_ai_model),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    TextField(
                        text = aiModel,
                        onTextChange = {
                            aiModel = it
                            settingsManager.aiModel = it
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Timeout
                    Text(
                        text = stringResource(R.string.settings_ai_timeout),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    TextField(
                        text = aiTimeout,
                        onTextChange = {
                            aiTimeout = it.filter { c -> c.isDigit() }
                            aiTimeout.toLongOrNull()?.let { v ->
                                settingsManager.aiTimeoutSeconds = v
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Test connection
                    Button(
                        text = if (isTesting) stringResource(R.string.settings_ai_test_testing)
                               else stringResource(R.string.settings_ai_test),
                        onClick = {
                            if (!isTesting) {
                                isTesting = true
                                testStatus = ""
                                scope.launch {
                                    val aiService = AIService(settingsManager.getAIConfig())
                                    val result = aiService.testConnection()
                                    isTesting = false
                                    testStatus = result.fold(
                                        onSuccess = { context.getString(R.string.settings_ai_test_success) },
                                        onFailure = { context.getString(R.string.settings_ai_test_failed, it.message ?: "") }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (testStatus.isNotEmpty()) {
                        Text(
                            text = testStatus,
                            style = MiuixTheme.textStyles.body2,
                            color = if (testStatus.startsWith("连接成功"))
                                MiuixTheme.colorScheme.primary
                            else
                                MiuixTheme.colorScheme.onError
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}