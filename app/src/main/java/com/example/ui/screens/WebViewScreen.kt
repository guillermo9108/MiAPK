package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.service.AndroidInterface
import com.example.service.sendNotification
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.delay

fun isNetworkAvailable(context: android.content.Context): Boolean {
    val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val nw = cm.activeNetwork ?: return false
        val actNw = cm.getNetworkCapabilities(nw) ?: return false
        return actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
               actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    } else {
        @Suppress("DEPRECATION")
        val nwInfo = cm.activeNetworkInfo ?: return false
        @Suppress("DEPRECATION")
        return nwInfo.isConnected
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val urlState = viewModel.webViewUrlFlow.collectAsState(initial = "")
    val refreshTrigger by viewModel.refreshWebViewTrigger.collectAsState(initial = 0)
    
    var isLoading by remember { mutableStateOf(false) }
    var progressVal by remember { mutableIntStateOf(0) }
    
    var webErrorOccurred by remember { mutableStateOf(false) }
    var tryingCache by remember { mutableStateOf(false) }
    var retryAttempt by remember { mutableStateOf(0) }
    var retryAttemptState by remember { mutableStateOf(0) }
    var refreshTriggerState by remember { mutableStateOf(0) }

    // Start auto reconnection logic when connection is broken
    LaunchedEffect(webErrorOccurred) {
        while (webErrorOccurred) {
            delay(4000)
            Log.i("WebViewScreen", "Reconnection ticker: retrying server connection...")
            retryAttempt++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("webview_screen_layout")
    ) {
        if (urlState.value.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "StreamPay no configurado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Por favor ingresa la dirección IP de tu servidor StreamPay en la sección de ajustes para empezar la reproducción.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.navigateTo(Screen.Config) },
                        modifier = Modifier.testTag("go_to_config_from_web_button")
                    ) {
                        Text("Ir a Ajustes")
                    }
                }
            }
        } else if (webErrorOccurred) {
            // Screen in pure black with server reconnecting status
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Conectando al servidor...",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Intentando reconexión con el servidor StreamPay en:\n${urlState.value}\nEsta pantalla se actualizará automáticamente hasta establecer conexión.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                webErrorOccurred = false
                                tryingCache = false
                                retryAttempt++
                            }
                        ) {
                            Text("Reintentar Ahora 🔄")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.navigateTo(Screen.Downloads)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Ver Descargas Offline 📥")
                        }
                    }
                }
            }
        } else {
            if (isLoading) {
                LinearProgressIndicator(
                    progress = progressVal / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .testTag("webview_progress_bar"),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            }

            AndroidView(
                factory = { ctx ->
                    try {
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                allowFileAccess = true
                                allowContentAccess = true
                                
                                // Dynamic cache configuring based on connection
                                if (isNetworkAvailable(ctx)) {
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    Log.i("WebViewScreen", "Internet connection active. Set LOAD_DEFAULT.")
                                } else {
                                    cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                                    Log.i("WebViewScreen", "Device offline/No Wifi. Set LOAD_CACHE_ELSE_NETWORK.")
                                }
                            }

                            // Capture and redirect download requests
                            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                                val fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype) ?: "Download_file"
                                Log.i("WebViewScreen", "Intercepted file download: $fileName, URL: $url")
                                viewModel.triggerDownload(fileName, url, "")
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    progressVal = 0
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    // Inject bridge for HTML5 Notification API so web alerts are channeled natively!
                                    view?.evaluateJavascript("""
                                        (function() {
                                            if (!window.Notification) {
                                                window.Notification = function(title, options) {
                                                    this.title = title;
                                                    this.options = options || {};
                                                    var body = this.options.body || '';
                                                    if (window.AndroidInterface && window.AndroidInterface.showNotification) {
                                                        window.AndroidInterface.showNotification(title, body);
                                                    }
                                                    return this;
                                                };
                                                window.Notification.permission = 'granted';
                                                window.Notification.requestPermission = function(callback) {
                                                    if (callback) callback('granted');
                                                    return Promise.resolve('granted');
                                                };
                                            }
                                            
                                            // Smart User ID Autodetection from localStorage, sessionStorage, and cookies
                                            function checkUser() {
                                                try {
                                                    var userId = null;
                                                    
                                                    // 1. Direct keys
                                                    var directKeys = ['userId', 'user_id', 'id', 'uid', 'current_user_id', 'logged_in_user'];
                                                    for (var i = 0; i < directKeys.length; i++) {
                                                        var val = localStorage.getItem(directKeys[i]);
                                                        if (val && val.length > 2 && val.length < 100) {
                                                            if (val.indexOf('{') === -1) {
                                                                userId = val.trim().replace(/['"]/g, '');
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    
                                                    // 2. JSON objects
                                                    if (!userId) {
                                                        var jsonKeys = ['user', 'session', 'auth', 'account', 'profile', 'userInfo'];
                                                        for (var i = 0; i < jsonKeys.length; i++) {
                                                            var val = localStorage.getItem(jsonKeys[i]);
                                                            if (val) {
                                                                try {
                                                                    var obj = JSON.parse(val);
                                                                    if (obj) {
                                                                        var possibleId = obj.id || obj._id || obj.userId || obj.user_id || obj.uid || (obj.user && (obj.user.id || obj.user._id || obj.user.userId));
                                                                        if (possibleId && typeof possibleId === 'string' && possibleId.length > 2) {
                                                                            userId = possibleId;
                                                                            break;
                                                                        } else if (possibleId && typeof possibleId === 'number') {
                                                                            userId = String(possibleId);
                                                                            break;
                                                                        }
                                                                    }
                                                                } catch (e) {}
                                                            }
                                                        }
                                                    }
                                                    
                                                    // 3. JWT parser
                                                    if (!userId) {
                                                        var tokenKeys = ['token', 'jwt', 'accessToken', 'id_token', 'auth_token'];
                                                        for (var i = 0; i < tokenKeys.length; i++) {
                                                            var token = localStorage.getItem(tokenKeys[i]) || sessionStorage.getItem(tokenKeys[i]);
                                                            if (token && token.split('.').length === 3) {
                                                                try {
                                                                    var base64Url = token.split('.')[1];
                                                                    var base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
                                                                    var jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                                                                        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
                                                                    }).join(''));
                                                                    var payload = JSON.parse(jsonPayload);
                                                                    var possibleId = payload.id || payload.userId || payload.user_id || payload.sub || payload.uid;
                                                                    if (possibleId) {
                                                                        userId = String(possibleId);
                                                                        break;
                                                                    }
                                                                } catch (e) {}
                                                            }
                                                        }
                                                    }

                                                    // 4. Cookies
                                                    if (!userId && document.cookie) {
                                                        var cookies = document.cookie.split(';');
                                                        for (var i = 0; i < cookies.length; i++) {
                                                            var parts = cookies[i].split('=');
                                                            var name = parts[0].trim();
                                                            var value = parts[1] ? parts[1].trim() : '';
                                                            if (name === 'userId' || name === 'user_id' || name === 'uid') {
                                                                userId = value;
                                                                break;
                                                            }
                                                        }
                                                    }

                                                    if (userId && window.AndroidInterface && window.AndroidInterface.updateUserId) {
                                                        window.AndroidInterface.updateUserId(userId);
                                                    }
                                                } catch(e) {
                                                    console.error('Error in autoDetectUser: ' + e);
                                                }
                                            }
                                            
                                            checkUser();
                                            // Set up a periodic check in case they log in on an SPA without reloading
                                            if (!window._streampay_user_check_registered) {
                                                window._streampay_user_check_registered = true;
                                                setInterval(checkUser, 4000);
                                            }
                                        })();
                                    """.trimIndent(), null)
                                }

                                @Deprecated("Deprecated in Java")
                                override fun onReceivedError(
                                    view: WebView?,
                                    errorCode: Int,
                                    description: String?,
                                    failingUrl: String?
                                ) {
                                    Log.w("WebViewScreen", "onReceivedError ($errorCode): $description for URL: $failingUrl")
                                    if (failingUrl == urlState.value || failingUrl?.trimEnd('/') == urlState.value.trimEnd('/')) {
                                        if (!tryingCache) {
                                            tryingCache = true
                                            view?.settings?.cacheMode = WebSettings.LOAD_CACHE_ONLY
                                            view?.loadUrl(urlState.value)
                                        } else {
                                            webErrorOccurred = true
                                        }
                                    }
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: android.webkit.WebResourceRequest?,
                                    error: android.webkit.WebResourceError?
                                ) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        val failingUrl = request?.url?.toString() ?: ""
                                        val isMainFrame = request?.isForMainFrame ?: false
                                        Log.w("WebViewScreen", "onReceivedError M (${error?.errorCode}): ${error?.description} for: $failingUrl (mainFrame=$isMainFrame)")
                                        if (isMainFrame && (failingUrl == urlState.value || failingUrl.trimEnd('/') == urlState.value.trimEnd('/'))) {
                                            if (!tryingCache) {
                                                tryingCache = true
                                                view?.settings?.cacheMode = WebSettings.LOAD_CACHE_ONLY
                                                view?.loadUrl(urlState.value)
                                            } else {
                                                webErrorOccurred = true
                                            }
                                        }
                                    }
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progressVal = newProgress
                                }

                                // Handle permission requests for Web camera/microphone usage
                                override fun onPermissionRequest(request: PermissionRequest?) {
                                    Log.i("WebViewScreen", "Granting camera/audio permissions inside WebView.")
                                    request?.grant(request.resources)
                                }

                                // Capture any standard JS Alert notifications
                                override fun onJsAlert(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean {
                                    Log.i("WebViewScreen", "Intercepted JS Alert: $message")
                                    message?.let {
                                        sendNotification(context, "Notificación StreamPay Web", it)
                                    }
                                    result?.confirm()
                                    return true
                                }
                            }

                            val androidBridge = AndroidInterface(context, viewModel)
                            addJavascriptInterface(androidBridge, "AndroidShare")
                            addJavascriptInterface(androidBridge, "AndroidInterface")

                            loadUrl(urlState.value)
                        }
                    } catch (t: Throwable) {
                        Log.e("WebViewScreen", "Fallo al inicializar componente WebView: ${t.message}", t)
                        android.widget.FrameLayout(ctx).apply {
                            val textView = android.widget.TextView(ctx).apply {
                                text = "El componente WebView no está disponible en este dispositivo.\nDetalles: ${t.message}."
                                textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
                                setTextColor(android.graphics.Color.parseColor("#FF5E5E"))
                                setPadding(48, 48, 48, 48)
                                layoutParams = android.widget.FrameLayout.LayoutParams(
                                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                                    android.view.Gravity.CENTER
                                )
                            }
                            addView(textView)
                        }
                    }
                },
                update = { webView ->
                    val castedWebView = webView as? WebView
                    if (castedWebView != null) {
                        // Process external refresh triggers
                        if (refreshTriggerState < refreshTrigger) {
                            refreshTriggerState = refreshTrigger
                            webErrorOccurred = false
                            tryingCache = false
                            castedWebView.settings.cacheMode = WebSettings.LOAD_DEFAULT
                            castedWebView.reload()
                        }
                        
                        // Process auto retry connection triggers
                        if (retryAttemptState < retryAttempt) {
                            retryAttemptState = retryAttempt
                            webErrorOccurred = false
                            tryingCache = false
                            castedWebView.settings.cacheMode = WebSettings.LOAD_DEFAULT
                            castedWebView.loadUrl(urlState.value)
                        }

                        val currentLoadedUrl = castedWebView.url ?: ""
                        if (currentLoadedUrl != urlState.value && urlState.value.isNotBlank() && currentLoadedUrl.isBlank()) {
                            castedWebView.settings.cacheMode = WebSettings.LOAD_DEFAULT
                            castedWebView.loadUrl(urlState.value)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("webview_content_viewport")
            )
        }
    }
}
