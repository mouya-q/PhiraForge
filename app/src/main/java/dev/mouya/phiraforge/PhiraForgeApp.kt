package dev.mouya.phiraforge

import android.app.Application
import android.util.Log
import dev.mouya.phiraforge.data.SettingsManager

/**
 * Application entry point. Initializes global state.
 */
class PhiraForgeApp : Application() {
    companion object {
        private const val TAG = "PhiraForgeApp"
        lateinit var instance: PhiraForgeApp
            private set
    }

    lateinit var settingsManager: SettingsManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsManager = SettingsManager(this)
        Log.i(TAG, "PhiraForge initialized")
    }
}