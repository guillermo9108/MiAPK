package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.pref.ServerConfig

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("BootReceiver", "Power On/Boot complete event received: ${intent?.action}")
        val config = ServerConfig(context)
        if (config.isConfigured && config.lastSavedUserId.isNotBlank()) {
            BackgroundWebSocketService.startService(context)
        }
    }
}
