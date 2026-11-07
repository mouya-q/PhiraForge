# 变更日志

## v1.0.0 (2025-01-01)

### 新增
- 完整的音频导入与解码（MediaExtractor + MediaCodec）
- 纯 Kotlin FFT 频谱通量 onset 检测引擎
- 自相关 BPM 估计（60-200 BPM 范围）
- 四档难度谱面生成（Easy/Normal/Hard/Extra）
- RPE 格式谱面导出（Phira 兼容）
- 谱面兼容性验证
- 时间轴可视化预览
- 音符类型分布统计
- 可选 AI 增强生成（OpenAI 兼容 API）
- AI 失败自动回退离线生成
- Miuix KMP UI（HyperOS 风格）
- 深浅色主题适配
- 加密设置存储（EncryptedSharedPreferences）
- 隐私安全日志（自动脱敏）
- 取消与恢复机制
- 进度反馈
- 单元测试（BeatTimeConverter、ChartGenerator、ChartExporter、OnsetDetector）
- GitHub Actions CI 构建
- 完整文档（README、构建指南、用户手册、架构说明、隐私政策、AI 数据声明、免责声明、开源许可、FAQ、变更日志）