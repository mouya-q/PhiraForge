# 第三方开源许可证清单

PhiraForge 使用以下开源组件。以下为各组件的许可证信息。

## 核心依赖

### Miuix KMP
- **仓库**: https://github.com/compose-miuix-ui/miuix
- **许可证**: Apache-2.0
- **用途**: UI 组件库（MiuixTheme, NavigationBar, Card, Button 等）
- **版本**: 0.9.4

### Kotlin Serialization
- **仓库**: https://github.com/Kotlin/kotlinx.serialization
- **许可证**: Apache-2.0
- **用途**: RPE 谱面 JSON 序列化/反序列化
- **版本**: 1.7.3

### OkHttp
- **仓库**: https://github.com/square/okhttp
- **许可证**: Apache-2.0
- **用途**: AI API HTTP 客户端
- **版本**: 4.12.0

### AndroidX Core KTX
- **仓库**: https://github.com/androidx/androidx
- **许可证**: Apache-2.0
- **用途**: Kotlin 扩展
- **版本**: 1.15.0

### AndroidX Lifecycle
- **仓库**: https://github.com/androidx/androidx
- **许可证**: Apache-2.0
- **用途**: ViewModel + Compose 集成
- **版本**: 2.8.7

### AndroidX Activity Compose
- **仓库**: https://github.com/androidx/androidx
- **许可证**: Apache-2.0
- **用途**: Activity + Compose 集成
- **版本**: 1.9.3

### AndroidX Compose
- **仓库**: https://github.com/androidx/androidx
- **许可证**: Apache-2.0
- **用途**: Jetpack Compose UI 框架
- **版本**: BOM 2024.12.01

### AndroidX Navigation Compose
- **仓库**: https://github.com/androidx/androidx
- **许可证**: Apache-2.0
- **用途**: 页面导航
- **版本**: 2.8.5

### AndroidX DocumentFile
- **仓库**: https://github.com/androidx/androidx
- **许可证**: Apache-2.0
- **用途**: SAF 文件访问
- **版本**: 1.0.1

### AndroidX DataStore Preferences
- **仓库**: https://github.com/androidx/androidx
- **许可证**: Apache-2.0
- **用途**: 偏好设置存储
- **版本**: 1.1.1

### AndroidX Security Crypto
- **仓库**: https://github.com/androidx/androidx
- **许可证**: Apache-2.0
- **用途**: API Key 加密存储
- **版本**: 1.1.0-alpha06

### Kotlinx Coroutines
- **仓库**: https://github.com/Kotlin/kotlinx.coroutines
- **许可证**: Apache-2.0
- **用途**: 异步任务调度
- **版本**: 1.9.0

## 参考项目

以下项目的源码被参考用于理解 Phira 谱面格式，但其代码未直接包含在 PhiraForge 中：

### Phira
- **仓库**: https://github.com/TeamFlos/phira
- **许可证**: GPL-3.0
- **参考内容**: RPE 谱面格式定义（prpr/src/parse/rpe.rs）

### phichain
- **仓库**: https://github.com/Ivan-1F/phichain
- **许可证**: LGPL-3.0
- **参考内容**: Phigros 制谱工具链格式定义

## PhiraForge 自身许可证

PhiraForge 采用 **GPL-3.0** 许可证。

详见 [LICENSE](../LICENSE) 文件。