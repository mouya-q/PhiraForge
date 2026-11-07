package dev.mouya.phiraforge.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * AI service for optional chart enhancement via LLM API.
 *
 * Supports OpenAI-compatible API endpoints (OpenAI, DeepSeek, Moonshot, etc).
 * Falls back to offline generation on any failure.
 *
 * The AI enhances charts by:
 * 1. Receiving the onset analysis summary (BPM, onset count, energy profile)
 * 2. Suggesting structural improvements (phrase boundaries, density curves)
 * 3. The suggestions are applied to the offline-generated chart as adjustments
 */
class AIService(
    private val config: AIConfig
) {
    companion object {
        private const val TAG = "AIService"
    }

    data class AIConfig(
        val enabled: Boolean = false,
        val apiUrl: String = "https://api.openai.com/v1/chat/completions",
        val apiKey: String = "",
        val model: String = "gpt-4o-mini",
        val timeoutSeconds: Long = 30
    )

    data class EnhancementSuggestion(
        val densityCurve: List<Double>? = null,
        val phraseBoundaries: List<Double>? = null,
        val suggestedBpm: Double? = null,
        val noteTypeDistribution: Map<String, Double>? = null,
        val raw: String
    )

    /**
     * Test AI connection with a simple API call.
     * Returns success message or error.
     */
    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank()) {
            return@withContext Result.failure(Exception("API Key is empty"))
        }

        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(config.apiUrl)
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(buildSimpleRequest())
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                Log.i(TAG, "AI connection test successful")
                Result.success("连接成功 (${response.code})")
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.w(TAG, "AI connection test failed: ${response.code} $errorBody")
                Result.failure(Exception("HTTP ${response.code}: ${errorBody.take(200)}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "AI connection test error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Request AI enhancement suggestions for chart generation.
     * Returns null if AI is unavailable or fails.
     */
    suspend fun requestEnhancement(
        bpm: Double,
        onsetCount: Int,
        durationSeconds: Double,
        difficulty: String
    ): EnhancementSuggestion? = withContext(Dispatchers.IO) {
        if (!config.enabled || config.apiKey.isBlank()) {
            return@withContext null
        }

        val timeoutResult = withTimeoutOrNull(config.timeoutSeconds * 1000) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build()

                val prompt = buildPrompt(bpm, onsetCount, durationSeconds, difficulty)

                val requestBody = JSONObject().apply {
                    put("model", config.model)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", "You are a rhythm game chart design expert. Respond with JSON only.")
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                    put("temperature", 0.7)
                    put("max_tokens", 2000)
                }.toString()

                val request = Request.Builder()
                    .url(config.apiUrl)
                    .addHeader("Authorization", "Bearer ${config.apiKey}")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    parseEnhancementResponse(body)
                } else {
                    Log.w(TAG, "AI enhancement failed: ${response.code}")
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "AI enhancement error: ${e.message}")
                null
            }
        }

        timeoutResult
    }

    private fun buildPrompt(bpm: Double, onsetCount: Int, duration: Double, difficulty: String): String {
        return """
You are designing a Phira/Phigros rhythm chart.

Song info:
- BPM: $bpm
- Duration: ${"%.1f".format(duration)} seconds
- Onset count: $onsetCount
- Target difficulty: $difficulty

Provide chart design suggestions as JSON:
{
  "suggestedBpm": <number>,
  "densityCurve": [<8 normalized values 0-1 representing note density over song progression>],
  "phraseBoundaries": [<times in seconds where musical phrases change>],
  "noteTypeDistribution": {
    "tap": <0-1>,
    "hold": <0-1>,
    "drag": <0-1>,
    "flick": <0-1>
  }
}

Respond with JSON only, no markdown.
        """.trimIndent()
    }

    private fun parseEnhancementResponse(body: String): EnhancementSuggestion? {
        return try {
            val json = JSONObject(body)
            val content = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            // Parse the AI's JSON response
            val suggestionJson = JSONObject(content.trim())

            val densityCurve = suggestionJson.optJSONArray("densityCurve")?.let { arr ->
                (0 until arr.length()).map { arr.getDouble(it) }
            }

            val phraseBoundaries = suggestionJson.optJSONArray("phraseBoundaries")?.let { arr ->
                (0 until arr.length()).map { arr.getDouble(it) }
            }

            val suggestedBpm = suggestionJson.optDouble("suggestedBpm", -1.0)
                .takeIf { it > 0 }

            val noteTypeDist = suggestionJson.optJSONObject("noteTypeDistribution")?.let { obj ->
                mapOf(
                    "tap" to obj.optDouble("tap", 0.8),
                    "hold" to obj.optDouble("hold", 0.1),
                    "drag" to obj.optDouble("drag", 0.05),
                    "flick" to obj.optDouble("flick", 0.05)
                )
            }

            EnhancementSuggestion(
                densityCurve = densityCurve,
                phraseBoundaries = phraseBoundaries,
                suggestedBpm = suggestedBpm,
                noteTypeDistribution = noteTypeDist,
                raw = content
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse AI response: ${e.message}")
            null
        }
    }

    private fun buildSimpleRequest(): okhttp3.RequestBody {
        val requestBody = JSONObject().apply {
            put("model", config.model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "Reply with: OK")
                })
            })
            put("max_tokens", 10)
        }.toString()
        return requestBody.toRequestBody("application/json".toMediaType())
    }
}