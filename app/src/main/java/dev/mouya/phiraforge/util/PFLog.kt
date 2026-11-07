package dev.mouya.phiraforge.util

import android.util.Log

/**
 * Centralized logging utility.
 * Logs diagnostic info without leaking privacy-sensitive data (file paths, API keys).
 */
object PFLog {
    private const val TAG = "PhiraForge"
    private const val MAX_MSG_LEN = 500

    fun d(msg: String) {
        Log.d(TAG, sanitize(msg))
    }

    fun i(msg: String) {
        Log.i(TAG, sanitize(msg))
    }

    fun w(msg: String, t: Throwable? = null) {
        Log.w(TAG, sanitize(msg), t)
    }

    fun e(msg: String, t: Throwable? = null) {
        Log.e(TAG, sanitize(msg), t)
    }

    /**
     * Sanitize log messages to remove potential privacy-sensitive data.
     */
    private fun sanitize(msg: String): String {
        var result = msg
        // Redact API keys
        result = result.replace(Regex("(?i)(api[_-]?key|authorization|bearer)\\s*[:=]\\s*\\S+"), "$1: [REDACTED]")
        // Redact file paths (keep just filename)
        result = result.replace(Regex("/storage/emulated/0/[^\\s]+"), "[path]")
        result = result.replace(Regex("/sdcard/[^\\s]+"), "[path]")
        // Truncate
        if (result.length > MAX_MSG_LEN) {
            result = result.take(MAX_MSG_LEN) + "..."
        }
        return result
    }
}