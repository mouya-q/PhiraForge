package dev.mouya.phiraforge.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.mouya.phiraforge.BuildConfig
import dev.mouya.phiraforge.R
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AboutScreen() {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.about_title)
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

            // App info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PhiraForge",
                        style = MiuixTheme.textStyles.title1
                    )
                    Text(
                        text = stringResource(R.string.about_description),
                        style = MiuixTheme.textStyles.body1
                    )
                    Text(
                        text = "${stringResource(R.string.about_version)}: ${BuildConfig.VERSION_NAME}",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    Text(
                        text = stringResource(R.string.about_developer),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }

            // GitHub
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about_github),
                        style = MiuixTheme.textStyles.title2
                    )
                    Button(
                        text = "GitHub",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mouya-q/PhiraForge"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // Disclaimer
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about_disclaimer),
                        style = MiuixTheme.textStyles.title2
                    )
                    Text(
                        text = "PhiraForge 是一款辅助工具，用于将音频自动转换为 Phira 兼容谱面。" +
                            "本应用与 Phigros 官方及 Phira 开发团队无关，不提供任何官方授权或背书。" +
                            "用户应确保对所使用的音频文件拥有合法使用权。" +
                            "生成的谱面质量取决于音频特征和算法能力，不保证可玩性。",
                        style = MiuixTheme.textStyles.body2
                    )
                }
            }

            // Privacy policy
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about_privacy),
                        style = MiuixTheme.textStyles.title2
                    )
                    Text(
                        text = "PhiraForge 不收集任何用户数据。" +
                            "音频文件仅在本地解码和分析，不上传至任何服务器。" +
                            "AI API Key 使用 EncryptedSharedPreferences 加密存储，不会泄露。" +
                            "AI 增强功能仅在用户明确启用时才会向用户配置的 API 地址发送节奏分析摘要（不含音频原始数据）。",
                        style = MiuixTheme.textStyles.body2
                    )
                }
            }

            // AI data usage
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AI 数据使用声明",
                        style = MiuixTheme.textStyles.title2
                    )
                    Text(
                        text = "当 AI 增强功能启用时，以下数据将发送至用户配置的 AI API 地址：" +
                            "\n• BPM（每分钟节拍数）" +
                            "\n• 节拍点数量" +
                            "\n• 音频时长" +
                            "\n• 目标难度等级" +
                            "\n\n不发送的数据：" +
                            "\n• 音频原始数据或文件" +
                            "\n• 文件名或路径" +
                            "\n• 设备信息" +
                            "\n• 用户身份信息" +
                            "\n\nAI 返回的谱面设计建议（密度曲线、段落边界、音符类型分布等）仅用于优化谱面生成算法。",
                        style = MiuixTheme.textStyles.body2
                    )
                }
            }

            // Open source licenses
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about_open_source),
                        style = MiuixTheme.textStyles.title2
                    )
                    Text(
                        text = "本应用使用以下开源组件：" +
                            "\n• Miuix KMP (Apache-2.0) — UI 组件库" +
                            "\n• Kotlin Serialization (Apache-2.0) — JSON 序列化" +
                            "\n• OkHttp (Apache-2.0) — HTTP 客户端" +
                            "\n• AndroidX Security Crypto (Apache-2.0) — 加密存储" +
                            "\n• AndroidX Compose (Apache-2.0) — UI 框架" +
                            "\n• AndroidX DocumentFile (Apache-2.0) — 文件访问" +
                            "\n• AndroidX Navigation (Apache-2.0) — 导航" +
                            "\n\nPhira 谱面格式参考自 Phira 开源项目 (GPL-3.0)。" +
                            "\nPhiraForge 本身以 GPL-3.0 许可证开源。",
                        style = MiuixTheme.textStyles.body2
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}