# 架构说明

## 总体架构

PhiraForge 采用单模块 Android 应用架构，按职责分层：

```
┌─────────────────────────────────────────────┐
│                   UI Layer                    │
│  (Miuix Compose — Screens + Components)       │
├─────────────────────────────────────────────┤
│              Orchestration Layer              │
│  (GenerationOrchestrator — pipeline control)  │
├─────────────────────────────────────────────┤
│                  Core Layer                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────────┐ │
│  │  Audio    │ │  Chart   │ │    AI         │ │
│  │  Decoder  │ │ Generator│ │   Service     │ │
│  │  + Onset  │ │ + Export │ │   (Optional)  │ │
│  └──────────┘ └──────────┘ └──────────────┘ │
├─────────────────────────────────────────────┤
│                Data Layer                     │
│  (SettingsManager — Encrypted Prefs)          │
└─────────────────────────────────────────────┘
```

## 模块职责

### AudioDecoder (`audio/AudioDecoder.kt`)
- 输入：音频文件路径（通过 URI 复制到 cache）
- 输出：`DecodedAudio(samples: FloatArray, sampleRate, durationMs)`
- 实现：Android `MediaExtractor` → `MediaCodec` → PCM 16-bit → float 归一化 → 降采样至 22050Hz
- 特点：分块读取避免 OOM；多声道混缩为单声道

### OnsetDetector (`audio/OnsetDetector.kt`)
- 输入：`FloatArray` PCM 样本
- 输出：`AnalysisResult(onsets, bpm, bpmConfidence, energy, spectralFlux)`
- 算法流程：
  1. STFT：1024 点 FFT，512 hop，Hann 窗
  2. 频谱通量：相邻帧正幅度差之和
  3. 自适应阈值：移动窗口均值 + 0.3
  4. 峰值拾取：局部最大值 + 最小间距 50ms
  5. BPM 估计：onset 包络自相关，搜索 60-200 BPM 范围

### ChartGenerator (`chart/ChartGenerator.kt`)
- 输入：`AnalysisResult` + `GenerationParams(difficulty, bpm, offset, seed)`
- 输出：`GenerationResult(chart: RPEChart, noteCount, ...)`
- 生成策略：
  1. **量化**：onset 时间对齐到节拍网格（精度随难度提升）
  2. **密度过滤**：弱 onset 按难度阈值剔除
  3. **判定线分配**：按交替模式分配到 1-3 条判定线
  4. **音符类型选择**：基于 onset 强度 + 随机权重
  5. **位置计算**：交替方向 + 噪声 + 反聚集约束
  6. **Hold 时长**：基于难度确定拍数

### BeatTimeConverter (`chart/BeatTimeConverter.kt`)
- 秒 ↔ RPE Triple 转换
- Triple = [num, den, beats]，其中 beats = num/den
- 使用 192 分母（2,3,4,6,8,12,16,24,32,48,64,96,192 的 LCM）
- 最优分母选择：从小到大尝试，找到精确匹配

### ChartExporter (`chart/ChartExporter.kt`)
- JSON 序列化：kotlinx.serialization，camelCase，encodeDefaults
- 验证规则：
  - RPEVersion ≥ 100
  - BPMList 非空且 BPM 在 0-500
  - JudgeLineList 非空
  - 每条判定线有 event layers
  - 音符类型 1-4
  - Hold 音符 endTime > startTime
  - positionX 在 [-700, 700]

### AIService (`ai/AIService.kt`)
- OpenAI 兼容 Chat Completions API
- 超时控制：`withTimeoutOrNull`
- 失败回退：返回 null，由 Orchestrator 回退离线生成
- 数据安全：仅发送 BPM、onset 数量、时长、难度，不发送音频

### SettingsManager (`data/SettingsManager.kt`)
- 普通设置：`SharedPreferences`
- 敏感设置（API Key）：`EncryptedSharedPreferences` (AES256-GCM)
- Fallback：加密不可用时降级为普通 Prefs

### GenerationOrchestrator (`GenerationOrchestrator.kt`)
- 编排完整流水线：decode → analyze → (AI) → generate → validate
- 所有重计算在 `Dispatchers.IO`
- 支持取消（`@Volatile cancelled` 标志）
- 进度回调分阶段报告

## RPE 谱面格式

Phira 使用 Re:PhiEdit (RPE) JSON 格式。参考来源：[Phira 源码 rpe.rs](https://github.com/TeamFlos/phira/blob/main/prpr/src/parse/rpe.rs)

### 顶层结构
```json
{
  "META": { "offset": 0, "RPEVersion": 160 },
  "BPMList": [{ "bpm": 120.0, "startTime": [0, 1, 0.0] }],
  "judgeLineList": [ ... ]
}
```

### Triple 时间表示
RPE 使用 `[num, den, beats]` 三元组表示节拍位置：
- `beats = num / den`
- 对应秒数：`seconds = beats * 60 / BPM`

### 音符类型
| 值 | 类型 | 说明 |
|----|------|------|
| 1 | Click | 单击（Tap） |
| 2 | Hold | 长按，需要 endTime > startTime |
| 3 | Flick | 滑动 |
| 4 | Drag | 拖拽 |

### 坐标系统
- RPE_WIDTH = 1350, RPE_HEIGHT = 900
- positionX 范围：-675 ~ 675（0 = 中心）
- above: 1 = 上方, 0 = 下方

### 事件层
每条判定线包含 EventLayer，定义：
- alphaEvents：透明度
- moveXEvents / moveYEvents：位移
- rotateEvents：旋转
- speedEvents：判定线速度

生成谱面使用常量事件（从开始到结束保持固定值）。

## 并发与线程安全

- 所有音频分析和谱面生成在 `Dispatchers.IO` 执行
- UI 状态通过 Compose `remember` + `mutableStateOf` 管理
- 取消机制：volatile 布尔标志 + 协程 `ensureActive()`
- 文件操作：URI → cacheDir 临时文件 → 处理 → 删除

## 隐私设计

- 音频文件：全程本地，不上传
- API Key：EncryptedSharedPreferences 加密存储
- 日志：`PFLog` 自动脱敏（API Key、文件路径）
- AI 数据：仅发送匿名摘要（BPM、onset 数量、时长、难度）