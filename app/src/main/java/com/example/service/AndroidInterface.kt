package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.ui.viewmodel.AppViewModel

fun sendNotification(context: Context, title: String, message: String) {
    try {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channelId = "streampay_push_notifications"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificaciones de StreamPay",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "StreamPay push and web notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    } catch (e: Exception) {
        Log.e("NotificationHelper", "Failed to compile/send local notification: ${e.message}")
    }
}

class AndroidInterface(
    private val context: Context,
    private val viewModel: AppViewModel
) {
    private val handler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun triggerDownload(title: String, url: String, videoId: String) {
        Log.d("AndroidInterface", "JS Triggered Download. Title: $title, Url: $url, VideoId: $videoId")
        
        viewModel.triggerDownload(title, url, videoId)

        handler.post {
            Toast.makeText(context, "Iniciando descarga: $title", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun updateUserId(userId: String) {
        if (userId.isBlank() || userId == viewModel.userIdState.value) {
            return
        }
        Log.d("AndroidInterface", "JS updated UserId: $userId")
        viewModel.updateUserId(userId)
        
        // Restart websocket service with the new userId context
        handler.post {
            BackgroundWebSocketService.startService(context)
        }
    }

    @JavascriptInterface
    fun showNotification(title: String, message: String) {
        Log.i("AndroidInterface", "JS Triggered Notification. Title: $title, Message: $message")
        handler.post {
            sendNotification(context, title, message)
        }
    }

    @JavascriptInterface
    fun postMessage(jsonString: String) {
        Log.i("AndroidInterface", "JS postMessage: $jsonString")
        handler.post {
            try {
                val json = org.json.JSONObject(jsonString)
                val title = json.optString("title", "StreamPay")
                val body = json.optString("body", "")
                if (body.isNotEmpty()) {
                    sendNotification(context, title, body)
                }
            } catch (e: Exception) {
                sendNotification(context, "Notificación StreamPay", jsonString)
            }
        }
    }
}
