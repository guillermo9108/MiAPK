package com.example.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.DownloadItem
import com.example.data.pref.ServerConfig
import com.example.data.repository.DownloadRepository
import com.example.downloader.CustomDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

enum class Screen {
    WebView,
    Downloads,
    Config,
    VideoPlayer
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val serverConfig = ServerConfig(application)
    private val downloadDao = AppDatabase.getDatabase(application).downloadDao()
    private val repository = DownloadRepository(downloadDao)
    private val downloader = CustomDownloader(application, repository)

    // Navigation and Active screen states
    private val _currentScreen = MutableStateFlow(Screen.Config)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _activeVideoUriString = MutableStateFlow<String?>(null)
    val activeVideoUriString: StateFlow<String?> = _activeVideoUriString.asStateFlow()

    private val _activeVideoTitle = MutableStateFlow<String?>(null)
    val activeVideoTitle: StateFlow<String?> = _activeVideoTitle.asStateFlow()

    fun playVideo(uriString: String, title: String) {
        _activeVideoUriString.value = uriString
        _activeVideoTitle.value = title
        _currentScreen.value = Screen.VideoPlayer
    }

    fun stopVideo() {
        _activeVideoUriString.value = null
        _activeVideoTitle.value = null
        _currentScreen.value = Screen.Downloads
    }

    // Web view refresh trigger
    private val _refreshWebViewTrigger = MutableStateFlow(0)
    val refreshWebViewTrigger = _refreshWebViewTrigger.asStateFlow()

    fun refreshWebView() {
        _refreshWebViewTrigger.value += 1
    }

    // Config states
    private val _ipAddressState = MutableStateFlow(serverConfig.ipAddress)
    val ipAddressState = _ipAddressState.asStateFlow()

    private val _portState = MutableStateFlow(serverConfig.port)
    val portState = _portState.asStateFlow()

    private val _downloadLocationState = MutableStateFlow(serverConfig.downloadLocation)
    val downloadLocationState = _downloadLocationState.asStateFlow()

    private val _keepCacheState = MutableStateFlow(serverConfig.keepCache)
    val keepCacheState = _keepCacheState.asStateFlow()

    private val _cacheCleanIntervalState = MutableStateFlow(serverConfig.cacheCleanInterval)
    val cacheCleanIntervalState = _cacheCleanIntervalState.asStateFlow()

    private val _userIdState = MutableStateFlow(serverConfig.lastSavedUserId)
    val userIdState = _userIdState.asStateFlow()

    // Dynamic WebView URL mapping
    val webViewUrlFlow = ipAddressState.map { ip ->
        if (ip.isBlank()) return@map ""
        val port = portState.value.trim()
        val base = if (port.isBlank()) ip else "$ip:$port"
        
        if (base.startsWith("http://") || base.startsWith("https://")) {
            base
        } else {
            "http://$base"
        }
    }

    // Database flow for all downloads
    val downloadsFlow = repository.allDownloads

    val activeDownloadsFlow = downloadsFlow.map { list ->
        list.filter { it.status == "DOWNLOADING" || it.status == "PENDING" }
    }

    // Scanning and Discovery states
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    init {
        val wifiKey = getWifiKey(application)
        if (wifiKey != null) {
            val saved = serverConfig.getServerIpForWifi(wifiKey)
            if (saved != null) {
                Log.i("AppViewModel", "Auto-loaded saved server IP for current Wi-Fi network ($wifiKey): ${saved.first}:${saved.second}")
                saveConfig(saved.first, saved.second)
                _currentScreen.value = Screen.WebView
            } else {
                // Not configured for this Wi-Fi yet, start autodiscover!
                autoDiscoverStreamPayServer()
            }
        } else {
            // Default configuration check if not on Wi-Fi
            if (serverConfig.isConfigured && serverConfig.ipAddress.isNotBlank()) {
                _currentScreen.value = Screen.WebView
            }
        }
    }

    fun getLocalIp(context: android.content.Context): String? {
        val wifiManager = context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
        val ipAddress = wifiManager?.connectionInfo?.ipAddress ?: 0
        if (ipAddress != 0) {
            return String.format(
                java.util.Locale.US,
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
        }
        
        // Fallback to checking NetworkInterfaces
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        val ip = addr.hostAddress
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip
                        }
                    }
                }
            }
        } catch (ignored: Exception) {}
        return null
    }

    fun getWifiKey(context: android.content.Context): String? {
        val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return null
        val network = connectivityManager.activeNetwork ?: return null
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null
        if (!capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
            return null // Not on Wi-Fi
        }

        // We are on Wi-Fi, try to get SSID
        val wifiManager = context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
        val connectionInfo = wifiManager?.connectionInfo
        val ssid = connectionInfo?.ssid?.replace("\"", "") ?: ""
        if (ssid.isNotEmpty() && ssid != "<unknown ssid>") {
            return "SSID:$ssid"
        }

        // Fallback to subnet prefix as SSID might be hidden or permission-restricted
        val localIp = getLocalIp(context) ?: return "WIFI_UNKNOWN"
        val parts = localIp.split(".")
        if (parts.size == 4) {
            return "SUBNET:${parts[0]}.${parts[1]}.${parts[2]}"
        }
        return "WIFI_UNKNOWN"
    }

    fun autoDiscoverStreamPayServer() {
        val context = getApplication<Application>()
        val wifiKey = getWifiKey(context)
        if (wifiKey == null) {
            Log.i("Discovery", "Not connected to Wi-Fi. Discovery aborted.")
            return
        }

        _isScanning.value = true
        _scanProgress.value = 0f

        viewModelScope.launch(Dispatchers.IO) {
            val localIp = getLocalIp(context)
            if (localIp == null) {
                _isScanning.value = false
                return@launch
            }
            val parts = localIp.split(".")
            if (parts.size != 4) {
                _isScanning.value = false
                return@launch
            }
            val subnet = "${parts[0]}.${parts[1]}.${parts[2]}"

            val portsToScan = listOf("", "8080", "3000", "5000", "8000")
            var foundIp: String? = null
            var foundPort: String? = null

            // First, test the gateway (.1), common IPs like .100, .101, .102, .50 to see if we get an instant match!
            val fastTrackIps = listOf("${subnet}.1", "${subnet}.100", "${subnet}.101", "${subnet}.102", "${subnet}.50")
            for (ip in fastTrackIps) {
                for (p in portsToScan) {
                    if (testServerIp(ip, p)) {
                        foundIp = ip
                        foundPort = p
                        break
                    }
                }
                if (foundIp != null) break
            }

            if (foundIp == null) {
                // If not found, scan the range 1..254 in batches of 32 parallel coroutines to be fast yet safe
                val batchSize = 32
                val completedCount = java.util.concurrent.atomic.AtomicInteger(0)
                val totalTasks = 254 * portsToScan.size

                for (batchStart in 1..254 step batchSize) {
                    if (foundIp != null) break
                    val jobs = mutableListOf<kotlinx.coroutines.Job>()
                    val batchEnd = (batchStart + batchSize - 1).coerceAtMost(254)
                    
                    for (i in batchStart..batchEnd) {
                        val testIp = "$subnet.$i"
                        if (testIp == localIp) continue
                        
                        val job = launch {
                            for (p in portsToScan) {
                                if (foundIp != null) break
                                if (testServerIp(testIp, p)) {
                                    foundIp = testIp
                                    foundPort = p
                                    break
                                }
                                val prog = completedCount.incrementAndGet().toFloat() / totalTasks
                                _scanProgress.value = prog
                            }
                        }
                        jobs.add(job)
                    }
                    jobs.forEach { it.join() }
                }
            }

            _isScanning.value = false
            _scanProgress.value = 1f

            if (foundIp != null) {
                val resolvedIp = foundIp!!
                val resolvedPort = foundPort ?: ""
                
                // Save it associated with the current Wi-Fi network
                serverConfig.setServerIpForWifi(wifiKey, resolvedIp, resolvedPort)
                
                viewModelScope.launch(Dispatchers.Main) {
                    saveConfig(resolvedIp, resolvedPort)
                    android.widget.Toast.makeText(
                        context,
                        "¡Servidor StreamPay detectado y guardado en esta Wi-Fi: $resolvedIp!",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Log.i("Discovery", "StreamPay server not found on this subnet.")
                viewModelScope.launch(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "No se autodetectó ningún servidor StreamPay. Ingrese el IP manualmente.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun testServerIp(ip: String, port: String): Boolean {
        var connection: java.net.HttpURLConnection? = null
        try {
            val urlString = if (port.isNotBlank()) "http://$ip:$port/" else "http://$ip/"
            val url = java.net.URL(urlString)
            connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 400 // Fast connection timeout
            connection.readTimeout = 600    // Fast read timeout
            connection.requestMethod = "GET"
            connection.useCaches = false
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val content = connection.inputStream.bufferedReader().use { it.readText() }
                if (content.contains("StreamPay", ignoreCase = true) || content.contains("streampay", ignoreCase = true)) {
                    Log.i("Discovery", "Successfully verified StreamPay server at: $urlString")
                    return true
                }
            }
        } catch (ignored: Exception) {
        } finally {
            connection?.disconnect()
        }
        return false
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun saveConfig(ip: String, port: String) {
        serverConfig.ipAddress = ip
        serverConfig.port = port
        serverConfig.isConfigured = ip.isNotBlank()
        
        _ipAddressState.value = ip
        _portState.value = port

        // Start / update background synchronization service safely
        if (serverConfig.isConfigured && serverConfig.lastSavedUserId.isNotBlank()) {
            try {
                com.example.service.BackgroundWebSocketService.startService(getApplication())
            } catch (ignored: Exception) {}
        }

        // Jump directly to stream WebView!
        _currentScreen.value = Screen.WebView
    }

    fun onIpAddressChanged(ip: String) {
        _ipAddressState.value = ip
    }

    fun onPortChanged(port: String) {
        _portState.value = port
    }

    fun onUserIdChanged(userId: String) {
        _userIdState.value = userId
    }

    fun resetConfig() {
        serverConfig.resetConfig()
        _ipAddressState.value = serverConfig.ipAddress
        _portState.value = serverConfig.port
        _downloadLocationState.value = ServerConfig.VAL_LOCATION_INTERNAL
        _keepCacheState.value = true
        _cacheCleanIntervalState.value = "NUNCA"
        _userIdState.value = ""
        
        _currentScreen.value = Screen.Config
    }

    fun saveDownloadLocation(location: String) {
        serverConfig.downloadLocation = location
        _downloadLocationState.value = location
    }

    fun saveKeepCache(keep: Boolean) {
        serverConfig.keepCache = keep
        _keepCacheState.value = keep
    }

    fun saveCacheCleanInterval(interval: String) {
        serverConfig.cacheCleanInterval = interval
        _cacheCleanIntervalState.value = interval
    }

    fun updateUserId(userId: String) {
        serverConfig.lastSavedUserId = userId
        _userIdState.value = userId
    }

    // State for storage permission checks on downloading
    data class PendingDownloadRequest(
        val title: String,
        val url: String,
        val videoId: String
    )

    private val _pendingDownloadRequest = MutableStateFlow<PendingDownloadRequest?>(null)
    val pendingDownloadRequest = _pendingDownloadRequest.asStateFlow()

    fun clearPendingDownloadRequest() {
        _pendingDownloadRequest.value = null
    }

    fun triggerDownload(title: String, url: String, videoId: String = "") {
        // Enforce runtime storage permission check to guarantee correct file saving capabilities
        val hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                getApplication(),
                android.Manifest.permission.READ_MEDIA_VIDEO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                getApplication(),
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (!hasStoragePermission && serverConfig.downloadLocation == ServerConfig.VAL_LOCATION_EXTERNAL) {
            Log.i("AppViewModel", "Storage permission is missing for EXTERNAL download config. Triggering runtime permission check.")
            _pendingDownloadRequest.value = PendingDownloadRequest(title, url, videoId)
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                downloader.startDownload(title, url, videoId)
            }
        }
    }

    fun forceDownloadNow(title: String, url: String, videoId: String = "", useInternalFallback: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (useInternalFallback) {
                Log.i("AppViewModel", "Permission denied/unavailable. Falling back to secure Internal Storage.")
                // Temporarily alter location for this download or run with internal filesDir
                serverConfig.downloadLocation = ServerConfig.VAL_LOCATION_INTERNAL
                _downloadLocationState.value = ServerConfig.VAL_LOCATION_INTERNAL
            }
            downloader.startDownload(title, url, videoId)
        }
    }

    fun deleteDownload(item: DownloadItem) {
        viewModelScope.launch(Dispatchers.IO) {
            // Delete DB entry
            repository.delete(item)

            // Delete physical file from device memory safely
            if (item.filePath.isNotBlank()) {
                try {
                    val file = File(item.filePath)
                    if (file.exists()) {
                        file.delete()
                        Log.d("AppViewModel", "Physical file deleted successfully: ${item.filePath}")
                    }
                } catch (e: Exception) {
                    Log.e("AppViewModel", "Failed to delete physical file: ${e.message}")
                }
            }
        }
    }

    fun clearWebCache() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val context = getApplication<Application>()
                // Force WebView cache clearance
                val testWebView = WebView(context)
                testWebView.clearCache(true)
                
                // Clear user cookies as well
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies(null)
                cookieManager.flush()

                Log.d("AppViewModel", "WebView data cache cleared successfully.")
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error wiping cookies or cache states: ${e.message}")
            }
        }
    }

    // Shared File States & Live Streams
    data class ShareFileInfo(
        val uri: android.net.Uri,
        val name: String,
        val size: Long,
        val mimeType: String
    )

    private val _sharedFile = MutableStateFlow<ShareFileInfo?>(null)
    val sharedFile: StateFlow<ShareFileInfo?> = _sharedFile.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Int?>(null)
    val uploadProgress = _uploadProgress.asStateFlow()

    private val _uploadStatus = MutableStateFlow<String?>(null)
    val uploadStatus = _uploadStatus.asStateFlow()

    fun setSharedFile(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var name = "archivo_compartido"
                var size = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) name = cursor.getString(nameIndex)
                        if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                    }
                }
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                _sharedFile.value = ShareFileInfo(uri, name, size, mimeType)
                _uploadProgress.value = null
                _uploadStatus.value = null
                Log.i("AppViewModel", "Extracted share file info: name='$name', size=$size, mime='$mimeType'")
            } catch (e: Exception) {
                Log.e("AppViewModel", "Failed to extract shared file info: ${e.message}")
            }
        }
    }

    fun clearSharedFile() {
        _sharedFile.value = null
        _uploadProgress.value = null
        _uploadStatus.value = null
    }

    fun uploadSharedFile(info: ShareFileInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            _uploadProgress.value = 0
            _uploadStatus.value = "Conectando al servidor..."
            
            val ip = serverConfig.ipAddress
            val port = serverConfig.port
            
            if (ip.isBlank()) {
                _uploadStatus.value = "Error: El servidor StreamPay no está configurado."
                _uploadProgress.value = null
                return@launch
            }

            val uploadUrlString = if (port.isNotBlank()) {
                "http://$ip:$port/api/upload"
            } else {
                "http://$ip/api/upload"
            }
            
            var connection: java.net.HttpURLConnection? = null
            var outputStream: java.io.OutputStream? = null
            var inputStream: java.io.InputStream? = null
            
            try {
                val url = java.net.URL(uploadUrlString)
                connection = url.openConnection() as java.net.HttpURLConnection
                connection.doOutput = true
                connection.doInput = true
                connection.useCaches = false
                connection.requestMethod = "POST"
                
                val boundary = "===StreamPayBoundary" + System.currentTimeMillis() + "==="
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connection.setRequestProperty("Connection", "Keep-Alive")
                connection.setRequestProperty("Cache-Control", "no-cache")
                
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                
                outputStream = connection.outputStream
                val writer = java.io.PrintWriter(outputStream.writer(Charsets.UTF_8), true)
                
                // Write boundary declaration with filename
                writer.append("--$boundary").append("\r\n")
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"${info.name}\"").append("\r\n")
                writer.append("Content-Type: ${info.mimeType}").append("\r\n")
                writer.append("Content-Transfer-Encoding: binary").append("\r\n")
                writer.append("\r\n")
                writer.flush()
                
                // Open real byte stream from the shared Uri
                inputStream = getApplication<Application>().contentResolver.openInputStream(info.uri)
                if (inputStream == null) throw Exception("No se pudo leer el archivo seleccionado.")
                
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesWritten = 0L
                val totalLength = if (info.size > 0) info.size else 1L
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesWritten += bytesRead
                    val progress = ((totalBytesWritten * 100) / totalLength).toInt().coerceIn(0, 100)
                    _uploadProgress.value = progress
                    _uploadStatus.value = "Subiendo archivo... $progress%"
                }
                outputStream.flush()
                
                // Close boundary block
                writer.append("\r\n")
                writer.append("--$boundary--").append("\r\n")
                writer.flush()
                
                val responseCode = connection.responseCode
                Log.d("AppViewModel", "Upload request completed with response code: $responseCode")
                
                if (responseCode in 200..299) {
                    _uploadProgress.value = 100
                    _uploadStatus.value = "¡Archivo subido con éxito al servidor!"
                } else {
                    _uploadProgress.value = null
                    _uploadStatus.value = "Fallo: El servidor respondió con código $responseCode"
                }
                
            } catch (e: Exception) {
                Log.e("AppViewModel", "Multi-part upload stream encountered an error: ${e.message}", e)
                _uploadProgress.value = null
                _uploadStatus.value = "Error de subida: ${e.message}"
            } finally {
                try {
                    inputStream?.close()
                    outputStream?.close()
                    connection?.disconnect()
                } catch (ignored: Exception) {}
            }
        }
    }
}
