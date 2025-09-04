package com.ecolocate

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Wrapper around SharedPreferences to store runtime configuration.
 */
class Config(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ecolocate_config", Context.MODE_PRIVATE)

    var serverBaseUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    var deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, null) ?: newUuid().also { deviceId = it }
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    var pollIntervalMinutes: Int
        get() = prefs.getInt(KEY_POLL_MINUTES, DEFAULT_POLL_MINUTES)
        set(value) = prefs.edit().putInt(KEY_POLL_MINUTES, value).apply()

    var trackingEnabled: Boolean
        get() = prefs.getBoolean(KEY_TRACKING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TRACKING_ENABLED, value).apply()

    var serviceRestartCount: Int
        get() = prefs.getInt(KEY_SERVICE_RESTARTS, 0)
        set(value) = prefs.edit().putInt(KEY_SERVICE_RESTARTS, value).apply()

    private fun newUuid(): String = UUID.randomUUID().toString()

    companion object {
        private const val KEY_SERVER_URL = "serverBaseUrl"
        private const val KEY_DEVICE_ID = "deviceId"
        private const val KEY_API_KEY = "apiKey"
        private const val KEY_POLL_MINUTES = "pollInterval"
        private const val KEY_TRACKING_ENABLED = "trackingEnabled"
        private const val KEY_SERVICE_RESTARTS = "serviceRestartCount"

        private const val DEFAULT_SERVER_URL = "http://192.168.1.100:3000"
        private const val DEFAULT_API_KEY = ""
        private const val DEFAULT_POLL_MINUTES = 10
    }
}


