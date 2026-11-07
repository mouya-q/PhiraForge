# 构建指南

## 环境要求

- JDK 17 或以上
- Android SDK 35（compileSdk）
- Android Gradle Plugin 8.7.3
- Kotlin 2.1.0
- Gradle 8.11.1（wrapper 已包含）

## 本地构建

### 1. 准备环境

```bash
# 确保 JAVA_HOME 指向 JDK 17
java -version

# 如果使用 Android Studio：
# File → Settings → Build → Gradle → Gradle JDK = 17
```

### 2. 克隆并构建

```bash
git clone https://github.com/mouya-q/PhiraForge.git
cd PhiraForge

# Debug 构建
./gradlew assembleDebug

# Release 构建（需要签名配置）
./gradlew assembleRelease

# 运行测试
./gradlew testDebugUnitTest
```

### 3. 输出

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- 测试报告: `app/build/test-results/testDebugUnitTest/`

## CI 构建

项目使用 GitHub Actions 自动构建：

1. Push 到 `main` 或 `master` 分支
2. PR 到 `main` 或 `master` 分支
3. 自动触发 `.github/workflows/build.yml`
4. 构建产物在 Actions → Artifacts 下载

## 签名

Release 构建默认使用 debug 签名（方便测试）。正式发布时：

1. 生成 keystore:
```bash
keytool -genkey -v -keystore phiraforge.jks -keyalg RSA -keysize 2048 -validity 10000 -alias phiraforge
```

2. 在 `app/build.gradle.kts` 中修改 `signingConfigs`：
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("path/to/phiraforge.jks")
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = "phiraforge"
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
```

3. 在 CI 中通过 GitHub Secrets 传递密码

## 依赖说明

所有依赖通过 Maven Central 获取，无需特殊仓库配置。

### 核心依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Miuix KMP | 0.9.4 | UI 组件 |
| Compose BOM | 2024.12.01 | Compose 版本管理 |
| Kotlin Serialization | 1.7.3 | JSON 序列化 |
| OkHttp | 4.12.0 | HTTP 客户端 |
| AndroidX Security | 1.1.0-alpha06 | 加密存储 |
| Navigation Compose | 2.8.5 | 页面导航 |

## 常见构建问题

### Q: Miuix 依赖无法解析
确保 `settings.gradle.kts` 中包含 `mavenCentral()` 仓库。Miuix 已发布到 Maven Central。

### Q: MediaCodec 相关编译错误
确保 `compileSdk = 35` 且 `minSdk = 24`。

### Q: EncryptedSharedPreferences 初始化失败
在某些定制 ROM 上可能不支持 AndroidX Security Crypto。应用已内置 fallback 到普通 SharedPreferences。