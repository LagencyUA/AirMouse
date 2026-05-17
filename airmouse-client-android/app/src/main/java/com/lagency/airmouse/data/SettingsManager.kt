package com.lagency.airmouse.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var serverIp: String?
        get() = prefs.getString(KEY_SERVER_IP, null)
        set(value) = prefs.edit().putString(KEY_SERVER_IP, value).apply()

    var serverPort: Int
        get() = prefs.getInt(KEY_SERVER_PORT, 8888)
        set(value) = prefs.edit().putInt(KEY_SERVER_PORT, value).apply()

    var trustedToken: String?
        get() = prefs.getString(KEY_TRUSTED_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TRUSTED_TOKEN, value).apply()

    var lastDevice: String?
        get() = prefs.getString(KEY_LAST_DEVICE, null)
        set(value) = prefs.edit().putString(KEY_LAST_DEVICE, value).apply()

    var selectedLayout: String?
        get() = prefs.getString(KEY_SELECTED_LAYOUT, "default")
        set(value) = prefs.edit().putString(KEY_SELECTED_LAYOUT, value).apply()

    companion object {
        private const val PREFS_NAME = "airmouse_settings"
        private const val KEY_SERVER_IP = "server_ip"
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_TRUSTED_TOKEN = "trusted_token"
        private const val KEY_LAST_DEVICE = "last_device"
        private const val KEY_SELECTED_LAYOUT = "selected_layout"
    }
}
