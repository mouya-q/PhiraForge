# AI 数据使用声明

**最后更新：2025-01-01**

## 概述

PhiraForge 提供可选的 AI 增强功能，通过用户自行配置的 OpenAI 兼容 API 优化谱面生成质量。本声明详细说明 AI 功能涉及的数据处理。

## 数据流向

```
用户音频 → [本地解码分析] → 匿名摘要 → [用户配置的AI API] → 谱面设计建议 → [本地应用] → 最终谱面
```

**音频原始数据永不离开设备。**

## 发送至 AI API 的数据

| 字段 | 类型 | 示例 | 说明 |
|------|------|------|------|
| bpm | number | 120.0 | 自动检测或用户指定的 BPM |
| onsetCount | integer | 85 | 检测到的节奏点数量 |
| durationSeconds | number | 180.5 | 音频总时长（秒） |
| difficulty | string | "Normal" | 目标难度等级 |

AI API 收到的完整请求示例（system prompt 之外的用户消息内容）：

```
Song info:
- BPM: 120
- Duration: 180.5 seconds
- Onset count: 85
- Target difficulty: Normal
```

## 不发送的数据

- ❌ 音频文件（MP3/WAV/等）
- ❌ 音频 PCM 原始数据
- ❌ 文件名
- ❌ 文件路径
- ❌ 设备型号、序列号
- ❌ Android 版本
- ❌ 用户 IP 地址（由 HTTP 客户端处理，本应用不收集）
- ❌ 用户身份信息

## AI 返回数据的处理

AI 返回的谱面设计建议包括：
- 密度曲线：8 个归一化值（0-1），表示歌曲各段落的建议音符密度
- 段落边界：时间点列表（秒），标识音乐段落变化
- 建议 BPM：AI 建议的 BPM（可选）
- 音符类型分布：tap/hold/drag/flick 的比例

这些数据仅用于**本地优化谱面生成算法**，不会被存储或转发。

## AI 服务商选择

用户自行选择 AI 服务商，本应用不预置任何特定服务商。支持的 API 格式：
- OpenAI Chat Completions API (`/v1/chat/completions`)
- 任何兼容此格式的服务商

## 数据保留

- AI API 的请求和响应**不在本应用中存储**
- AI 配置（API URL、Model）存储在加密 SharedPreferences 中
- API Key 使用 AES256-GCM 加密存储

## 关闭 AI 功能

- 在设置页面取消勾选「启用 AI 增强」即可完全关闭
- 关闭后不会向任何服务器发送任何数据
- 离线生成功能不受影响

## 失败回退

当 AI API 不可用时（网络错误、超时、认证失败等），应用自动回退到纯离线生成，不向用户报错或中断流程。