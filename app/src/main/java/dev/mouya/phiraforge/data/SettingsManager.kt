package dev.mouya.phiraforge.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.mouya.phiraforge.ai.AIService
import dev.mouya.phiraforge.chart.ChartGenerator

/**
 * Secure settings manager using EncryptedSharedPreferences for sensitive data (API keys)
 * and regular SharedPreferences for non-sensitive settings.
 */
class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("phiraforge_settings", Context.MODE_PRIVATE)

    private val securePrefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "ai_config",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to regular prefs if crypto fails (e.g. on older devices)
        context.getSharedPreferences("ai_config_fallback", Context.MODE_PRIVATE)
    }

    var themeMode: String
        get() = prefs.getString("theme_mode", "system") ?: "system"
        set(value) = prefs.edit().putString("theme_mode", value).apply()

    var defaultDifficulty: String
        get() = prefs.getString("default_difficulty", "NORMAL") ?: "NORMAL"
        set(value) = prefs.edit().putString("default_difficulty", value).apply()

    var chartOffsetMs: Int
        get() = prefs.getInt("chart_offset_ms", 0)
        set(value) = prefs.edit().putInt("chart_offset_ms", value).apply()

    // AI settings (secure)
    var aiEnabled: Boolean
        get() = securePrefs.getBoolean("ai_enabled", false)
        set(value) = securePrefs.edit().putBoolean("ai_enabled", value).apply()

    var aiApiUrl: String
        get() = securePrefs.getString("ai_api_url", "https://api.openai.com/v1/chat/completions") ?: ""
        set(value) = securePrefs.edit().putString("ai_api_url", value).apply()

    var aiApiKey: String
        get() = securePrefs.getString("ai_api_key", "") ?: ""
        set(value) = securePrefs.edit().putString("ai_api_key", value).apply()

    var aiModel: String
        get() = securePrefs.getString("ai_model", "gpt-4o-mini") ?: "gpt-4o-mini"
        set(value) = securePrefs.edit().putString("ai_model", value).apply()

    var aiTimeoutSeconds: Long
        get() = securePrefs.getLong("ai_timeout_seconds", 30L)
        set(value) = securePrefs.edit().putLong("ai_timeout_seconds", value).apply()

    fun getAIConfig(): AIService.AIConfig {
        return AIService.AIConfig(
            enabled = aiEnabled,
            apiUrl = aiApiUrl,
            apiKey = aiApiKey,
            model = aiModel,
            timeoutSeconds = aiTimeoutSeconds
        )
    }

    fun getDifficulty(): ChartGenerator.Difficulty {
        return when (defaultDifficulty) {
            "EASY" -> ChartGenerator.Difficulty.EASY
            "HARD" -> ChartGenerator.Difficulty.HARD
            "EXTRA" -> ChartGenerator.Difficulty.EXTRA
            else -> ChartGenerator.Difficulty.NORMAL
        }
    }
}