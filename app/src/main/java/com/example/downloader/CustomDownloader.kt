package com.example.downloader

import android.content.Context
import android.util.Log
import com.example.data.model.DownloadItem
import com.example.data.pref.ServerConfig
import com.example.data.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class CustomDownloader(
    private val context: Context,
    private val repository: DownloadRepository
) {
    private val serverConfig = ServerConfig(context)

    suspend fun startDownload(title: String, url: String, videoId: String) {
        withContext(Dispatchers.IO) {
        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
        val fileName = if (sanitizedTitle.isNotBlank()) "$sanitizedTitle.mp4" else "video_${System.currentTimeMillis()}.mp4"

        // Step 1: Resolve output file path based on preferences config
        val targetDir = if (serverConfig.downloadLocation == ServerConfig.VAL_LOCATION_EXTERNAL) {
            context.getExternalFilesDir(null) ?: context.filesDir
        } else {
            context.filesDir
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val targetFile = File(targetDir, fileName)

        // Step 2: Save to Room as DOWNLOADING
        val downloadItem = DownloadItem(
            title = title,
            url = url,
            videoId = videoId,
            filePath = targetFile.absolutePath,
            status = "DOWNLOADING",
            progress = 0,
            sizeLabel = "Calculando..."
        )
        val id = repository.insert(downloadItem).toInt()

        Log.d("CustomDownloader", "Created download record in Room: ID = $id, Title = $title")

        // Step 3: Run safe network chunk-stream loop
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            val downloadUrl = URL(url)
            connection = downloadUrl.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Servidor remoto retornó error HTTP: ${connection.responseCode}")
            }

            val fileLength = connection.contentLength
            val sizeLabel = if (fileLength > 0) {
                String.format(Locale.US, "%.2f MB", fileLength.toDouble() / (1024 * 1024))
            } else {
                "Desconocido"
            }

            // Update with resolved size
            val preparedItem = downloadItem.copy(id = id, sizeLabel = sizeLabel)
            repository.update(preparedItem)

            inputStream = connection.inputStream
            outputStream = FileOutputStream(targetFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            var lastUpdateTimestamp = System.currentTimeMillis()

            while (inputStream.read(data).also { count = it } != -1) {
                total += count
                outputStream.write(data, 0, count)

                // Limit DB writes on stream progress
                if (fileLength > 0) {
                    val progress = ((total * 100) / fileLength).toInt()
                    val now = System.currentTimeMillis()
                    if (now - lastUpdateTimestamp > 800) { // Every 800ms max
                        repository.updateStatusAndProgress(id, "DOWNLOADING", progress)
                        lastUpdateTimestamp = now
                    }
                }
            }

            // Wrap up file successfully
            outputStream.flush()
            repository.update(preparedItem.copy(status = "COMPLETED", progress = 100))
            Log.d("CustomDownloader", "Download completed successfully: ${targetFile.absolutePath}")

        } catch (e: Exception) {
            Log.e("CustomDownloader", "Streaming video download failed: ${e.message}", e)
            repository.update(downloadItem.copy(id = id, status = "FAILED", sizeLabel = "Error"))
            // Clean up partial file
            if (targetFile.exists()) {
                targetFile.delete()
            }
        } finally {
            try {
                outputStream?.close()
                inputStream?.close()
                connection?.disconnect()
            } catch (ignored: Exception) {}
        }
        Unit
    }
}
}
