package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences

class ServerConfig(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("streampay_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_IP = "ip_address"
        const val KEY_PORT = "port"
        const val KEY_IS_CONFIGURED = "is_configured"
        const val KEY_DOWNLOAD_LOCATION = "download_location"
        const val KEY_KEEP_CACHE = "keep_cache"
        const val KEY_CACHE_CLEAN_INTERVAL = "cache_clean_interval"
        const val KEY_LAST_USER_ID = "last_saved_user_id"

        const val VAL_LOCATION_INTERNAL = "INTERNA"
        const val VAL_LOCATION_EXTERNAL = "EXTERNA"
    }

    var ipAddress: String
        get() = prefs.getString(KEY_IP, "192.168.43.101") ?: "192.168.43.101"
        set(value) = prefs.edit().putString(KEY_IP, value).apply()

    var port: String
        get() = prefs.getString(KEY_PORT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PORT, value).apply()

    var isConfigured: Boolean
        get() = prefs.getBoolean(KEY_IS_CONFIGURED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_CONFIGURED, value).apply()

    var downloadLocation: String
        get() = prefs.getString(KEY_DOWNLOAD_LOCATION, VAL_LOCATION_INTERNAL) ?: VAL_LOCATION_INTERNAL
        set(value) = prefs.edit().putString(KEY_DOWNLOAD_LOCATION, value).apply()

    var keepCache: Boolean
        get() = prefs.getBoolean(KEY_KEEP_CACHE, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_CACHE, value).apply()

    var cacheCleanInterval: String
        get() = prefs.getString(KEY_CACHE_CLEAN_INTERVAL, "NUNCA") ?: "NUNCA"
        set(value) = prefs.edit().putString(KEY_CACHE_CLEAN_INTERVAL, value).apply()

    var lastSavedUserId: String
        get() = prefs.getString(KEY_LAST_USER_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_USER_ID, value).apply()

    fun setServerIpForWifi(wifiKey: String, ip: String, port: String) {
        prefs.edit()
            .putString("wifi_ip_$wifiKey", ip)
            .putString("wifi_port_$wifiKey", port)
            .apply()
    }

    fun getServerIpForWifi(wifiKey: String): Pair<String, String>? {
        val ip = prefs.getString("wifi_ip_$wifiKey", null) ?: return null
        val port = prefs.getString("wifi_port_$wifiKey", "") ?: ""
        return Pair(ip, port)
    }

    fun resetConfig() {
        prefs.edit().clear().apply()
    }
}
