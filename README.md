# PhiraForge

> Android 端 Phira 音游谱面自动生成器 — 导入音频，自动分析节奏，生成可玩的 Phira 谱面。

[![Build and Test](https://github.com/mouya-q/PhiraForge/actions/workflows/build.yml/badge.svg)](https://github.com/mouya-q/PhiraForge/actions/workflows/build.yml)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)

## 功能特性

- 🎵 **音频导入**：支持 MP3、WAV、OGG、FLAC、M4A、AAC 等格式
- 🎯 **节奏分析**：纯 Kotlin 实现的 FFT 频谱通量 onset 检测 + 自相关 BPM 估计
- 🎮 **谱面生成**：自动将 onset 映射为 Tap/Hold/Drag/Flick 音符，按判定线排布
- 📊 **难度分级**：Easy / Normal / Hard / Extra 四档难度，密度与复杂度分层
- 👁️ **预览试听**：时间轴可视化，音符类型分布统计
- 📤 **谱面导出**：生成 Phira 兼容的 RPE JSON 格式
- ✅ **兼容性验证**：内置校验器检查谱面结构合法性
- 🤖 **AI 增强（可选）**：支持 OpenAI 兼容 API，失败自动回退离线生成
- 🎨 **Miuix UI**：HyperOS 风格视觉，深浅色适配，玻璃质感组件
- 🔒 **隐私保护**：音频全程本地处理，API Key 加密存储

## 快速开始

### 构建

```bash
# 克隆仓库
git clone https://github.com/mouya-q/PhiraForge.git
cd PhiraForge

# 构建 Debug APK
./gradlew assembleDebug

# 运行单元测试
./gradlew testDebugUnitTest
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

### 使用流程

1. 安装 PhiraForge APK
2. 点击「导入音频」选择本地音频文件
3. 选择难度等级（Easy/Normal/Hard/Extra）
4. （可选）输入 BPM 或留空自动检测
5. （可选）启用 AI 增强并在设置中配置 API
6. 点击「开始生成」
7. 生成完成后可预览谱面时间轴和统计
8. 点击「导出谱面」保存为 JSON 文件
9. 将导出的 JSON 导入 Phira 即可游玩

### 在 Phira 中导入

1. 将导出的 `.json` 谱面文件和音频文件放入同一文件夹
2. 在 Phira 中选择「本地谱面」→ 导入该文件夹
3. 选择对应难度开始游玩

## 技术架构

```
PhiraForge/
├── app/src/main/java/dev/mouya/phiraforge/
│   ├── PhiraForgeApp.kt          # Application 入口
│   ├── MainActivity.kt            # Activity
│   ├── GenerationOrchestrator.kt # 生成流水线编排
│   ├── audio/
│   │   ├── AudioDecoder.kt        # MediaExtractor + MediaCodec 解码
│   │   └── OnsetDetector.kt       # FFT 频谱通量 onset 检测
│   ├── chart/
│   │   ├── RPEChartModel.kt       # RPE 谱面数据模型
│   │   ├── BeatTimeConverter.kt   # 秒↔节拍 Triple 转换
│   │   ├── ChartGenerator.kt      # 谱面生成引擎
│   │   └── ChartExporter.kt       # JSON 序列化 + 验证
│   ├── ai/
│   │   └── AIService.kt           # OpenAI 兼容 API 客户端
│   ├── data/
│   │   └── SettingsManager.kt     # 加密设置存储
│   ├── util/
│   │   └── PFLog.kt               # 隐私安全日志
│   └── ui/
│       ├── PhiraForgeApp.kt       # 主 Composable
│       ├── theme/Theme.kt         # Miuix 主题
│       ├── components/            # 可复用组件
│       └── screens/               # 生成/预览/设置/关于页面
├── app/src/test/                  # 单元测试
├── docs/                          # 文档与样例
└── .github/workflows/             # CI 构建
```

## 技术选型

| 组件 | 选型 | 理由 |
|------|------|------|
| UI 框架 | Miuix KMP 0.9.4 | HyperOS 风格，原生 Compose 支持 |
| 音频解码 | Android MediaExtractor + MediaCodec | 零外部依赖，系统级硬件加速 |
| DSP 分析 | 纯 Kotlin FFT | 无原生依赖，跨架构兼容 |
| JSON | Kotlin Serialization | 类型安全，编译期生成 |
| HTTP | OkHttp 4 | 成熟稳定，连接池管理 |
| 加密存储 | EncryptedSharedPreferences | AndroidX 官方方案 |
| 导航 | Navigation Compose | 官方推荐方案 |

## 许可证

GPL-3.0

## 致谢

- [Phira](https://github.com/TeamFlos/phira) — Phigros 社区重制版，谱面格式参考来源
- [Miuix KMP](https://github.com/compose-miuix-ui/miuix) — UI 组件库
- [phichain](https://github.com/Ivan-1F/phichain) — Phigros 制谱工具链，格式参考

## 免责声明

PhiraForge 是一款辅助工具，与 Phigros 官方及 Phira 开发团队无关。用户应确保对所使用的音频文件拥有合法使用权。生成的谱面质量取决于音频特征和算法能力。

## 开发者

mouya ([GitHub](https://github.com/mouya-q))