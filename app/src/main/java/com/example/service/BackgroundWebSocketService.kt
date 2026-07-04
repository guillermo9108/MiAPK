package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.pref.ServerConfig
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class BackgroundWebSocketService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var isCheckLoopRunning = false

    companion object {
        private const val CHANNEL_ID = "streampay_service"
        private const val NOTIFICATION_ID = 4859

        fun startService(context: Context) {
            try {
                val intent = Intent(context, BackgroundWebSocketService::class.java)
                // Start as standard service to avoid strict startForegroundService background transition crashes
                context.startService(intent)
            } catch (e: Exception) {
                Log.e("BackgroundService", "Failed starting Stream service: ${e.message}")
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BackgroundWebSocketService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("BackgroundService", "Service onCreate triggered")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BackgroundService", "Service onStartCommand triggered")

        startBackgroundWebSocketConnection()

        if (!isCheckLoopRunning) {
            isCheckLoopRunning = true
            startNotificationCheckLoop()
        }

        return START_STICKY
    }

    private fun startBackgroundWebSocketConnection() {
        val config = ServerConfig(applicationContext)
        val ip = config.ipAddress
        val port = config.port
        val userId = config.lastSavedUserId
        Log.i("BackgroundService", "Starting mock socket context pointing to: ws://$ip:$port/ws/user/$userId")
    }

    private fun startNotificationCheckLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val config = ServerConfig(applicationContext)
                    val ip = config.ipAddress
                    val port = config.port
                    val userId = config.lastSavedUserId

                    if (ip.isNotBlank() && userId.isNotBlank()) {
                        checkUserNotifications(ip, port, userId)
                    }
                } catch (e: Exception) {
                    Log.e("BackgroundService", "Error in notification check loop: ${e.message}")
                }
                delay(60000) // check every 60 seconds
            }
        }
    }

    private fun checkUserNotifications(ip: String, port: String, userId: String) {
        val base = if (port.isBlank()) ip else "$ip:$port"
        val urlString = if (base.startsWith("http://") || base.startsWith("https://")) {
            "$base/api/notifications?userId=$userId"
        } else {
            "http://$base/api/notifications?userId=$userId"
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"
            connection.useCaches = false

            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                parseAndTriggerNotifications(jsonString)
            }
        } catch (e: Exception) {
            Log.e("BackgroundService", "Failed to pull notifications: ${e.message}")
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseAndTriggerNotifications(jsonString: String) {
        try {
            val prefs = getSharedPreferences("streampay_notifications", Context.MODE_PRIVATE)
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "")
                val title = obj.optString("title", "StreamPay")
                val message = obj.optString("message", "")

                if (id.isNotEmpty() && message.isNotEmpty()) {
                    val wasShown = prefs.getBoolean("notif_shown_$id", false)
                    if (!wasShown) {
                        // Show native notification
                        sendNotification(applicationContext, title, message)
                        // Mark as shown
                        prefs.edit().putBoolean("notif_shown_$id", true).apply()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BackgroundService", "Failed parsing notification JSON: ${e.message}")
        }
    }

    override fun onDestroy() {
        Log.d("BackgroundService", "Foreground Service destroyed")
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Canal de sincronización StreamPay",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
