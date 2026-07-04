package com.example

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.BackgroundWebSocketService
import com.example.ui.screens.ConfigScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.WebViewScreen
import com.example.ui.theme.StreamPayTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Safe isolation of WebView directories using modern Android SDK suffixing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                WebView.setDataDirectorySuffix("streampay")
                Log.i("MainActivity", "Successfully isolated WebView data directories with suffix 'streampay'.")
            } catch (e: Exception) {
                Log.e("MainActivity", "Did not set WebView directory suffix: ${e.message}")
            }
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Log.i("MainActivity", "StreamPay Client onCreate started.")

        // Intercept deep links on startup
        handleIntent(intent)

        // Re-request background service boot if configured on launch
        try {
            val serverConfig = com.example.data.pref.ServerConfig(applicationContext)
            if (serverConfig.isConfigured && serverConfig.lastSavedUserId.isNotBlank()) {
                BackgroundWebSocketService.startService(applicationContext)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Launch service startup failed: ${e.message}")
        }

        setContent {
            StreamPayTheme {
                MainAppNavigator(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent == null) return

        intent.data?.let { uri ->
            try {
                val host = uri.host ?: ""
                val portVal = if (uri.port != -1) uri.port.toString() else ""
                Log.i("MainActivity", "Captured deep link intent: host='$host', port='$portVal'")
                if (host.isNotBlank()) {
                    // Update server configuration dynamically
                    viewModel.saveConfig(host, portVal)
                    Log.i("MainActivity", "Successfully updated ServerConfig to: $host:$portVal via DeepLink.")
                }
                Unit
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to parse deep link URI: ${e.message}")
            }
        }

        // Handle incoming media share (SEND)
        val action = intent.action
        val type = intent.type
        if (android.content.Intent.ACTION_SEND == action && type != null) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM) as? android.net.Uri
            }
            if (uri != null) {
                Log.i("MainActivity", "Received shared file uri: $uri")
                viewModel.setSharedFile(uri, applicationContext)
            }
        } else if (android.content.Intent.ACTION_SEND_MULTIPLE == action && type != null) {
            val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM)
            }
            if (!uris.isNullOrEmpty()) {
                Log.i("MainActivity", "Received ${uris.size} shared files. Processing the first one.")
                viewModel.setSharedFile(uris[0], applicationContext)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigator(viewModel: AppViewModel) {
    val context = LocalContext.current
    val currentScreen = viewModel.currentScreen.collectAsState()
    val activeDownloads = viewModel.activeDownloadsFlow.collectAsState(initial = emptyList())

    // Define all requested permissions based on Android API level
    val permissionsToRequest = remember {
        mutableListOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(android.Manifest.permission.READ_MEDIA_IMAGES)
                add(android.Manifest.permission.READ_MEDIA_VIDEO)
                add(android.Manifest.permission.READ_MEDIA_AUDIO)
                add(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    // Interactive confirmation dialog and permission launcher state
    var hasAllPermissions by remember {
        mutableStateOf(
            permissionsToRequest.all { permission ->
                androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }
    
    var showPermissionExplanationDialog by remember { mutableStateOf(!hasAllPermissions) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasAllPermissions = results.values.all { it }
        showPermissionExplanationDialog = false
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        val pending = viewModel.pendingDownloadRequest.value
        if (pending != null) {
            if (granted) {
                viewModel.forceDownloadNow(pending.title, pending.url, pending.videoId)
                android.widget.Toast.makeText(context, "Permiso concedido. Iniciando descarga...", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                viewModel.forceDownloadNow(pending.title, pending.url, pending.videoId, useInternalFallback = true)
                android.widget.Toast.makeText(context, "Permiso rechazado. Sincronizando descarga de video vía memoria interna segura.", android.widget.Toast.LENGTH_LONG).show()
            }
            viewModel.clearPendingDownloadRequest()
        }
    }

    // Modern floating slide-out navigation panel state
    var isMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // 1. Core Viewport Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("primary_screen_viewport")
            ) {
                when (currentScreen.value) {
                    Screen.WebView -> WebViewScreen(viewModel = viewModel)
                    Screen.Downloads -> DownloadsScreen(viewModel = viewModel)
                    Screen.Config -> ConfigScreen(viewModel = viewModel)
                    Screen.VideoPlayer -> {
                        val videoUri = viewModel.activeVideoUriString.collectAsState().value ?: ""
                        val title = viewModel.activeVideoTitle.collectAsState().value ?: "StreamPay Player"
                        com.example.ui.screens.VideoPlayerScreen(
                            videoUriString = videoUri,
                            title = title,
                            onBack = { viewModel.stopVideo() }
                        )
                    }
                }
            }

            // 2. Beautiful floating slide-out tab clung vertically to the right edge (Alignment.CenterEnd)
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = if (isMenuExpanded) 0.dp else 4.dp), // Snug fit to the right border
                verticalAlignment = Alignment.CenterVertically
            ) {
                // The little toggle tab sticking out of the edge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { isMenuExpanded = !isMenuExpanded }
                        .padding(horizontal = 8.dp, vertical = 20.dp)
                        .testTag("floating_menu_anchor"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMenuExpanded) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Menu Desplegable",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // The floating menu itself when expanded
                AnimatedVisibility(
                    visible = isMenuExpanded,
                    enter = slideInHorizontally { it } + fadeIn(),
                    exit = slideOutHorizontally { it } + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .width(180.dp)
                            .padding(end = 12.dp, top = 8.dp, bottom = 8.dp)
                            .testTag("floating_menu_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Navegación",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            
                            FloatingMenuItem(
                                label = "Reproductor",
                                icon = Icons.Default.PlayArrow,
                                isSelected = currentScreen.value == Screen.WebView,
                                onClick = {
                                    viewModel.navigateTo(Screen.WebView)
                                    isMenuExpanded = false
                                }
                            )

                            FloatingMenuItem(
                                label = "Descargas",
                                icon = Icons.Default.List,
                                isSelected = currentScreen.value == Screen.Downloads,
                                onClick = {
                                    viewModel.navigateTo(Screen.Downloads)
                                    isMenuExpanded = false
                                }
                            )

                            FloatingMenuItem(
                                label = "Actualizar",
                                icon = Icons.Default.Refresh,
                                isSelected = false,
                                onClick = {
                                    viewModel.refreshWebView()
                                    isMenuExpanded = false
                                }
                            )

                            FloatingMenuItem(
                                label = "Ajustes",
                                icon = Icons.Default.Settings,
                                isSelected = currentScreen.value == Screen.Config,
                                onClick = {
                                    viewModel.navigateTo(Screen.Config)
                                    isMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // 3. Floating Download Indicator appearing automatically when active downloads list is not empty
            val downloadingItem = activeDownloads.value.firstOrNull()
            AnimatedVisibility(
                visible = downloadingItem != null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 24.dp),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                if (downloadingItem != null) {
                    val progress = downloadingItem.progress
                    Card(
                        modifier = Modifier
                            .clickable { viewModel.navigateTo(Screen.Downloads) }
                            .testTag("floating_download_progress_fab"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = progress / 100f,
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 3.dp,
                                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Descargando",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Descargando... $progress%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = downloadingItem.title.take(15) + if (downloadingItem.title.length > 15) "..." else "",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Initial System Permission request dialog prompt
            if (showPermissionExplanationDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionExplanationDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ajustes de Permisos",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Permisos Requeridos")
                        }
                    },
                    text = {
                        Text(
                            text = "Para que la aplicación StreamPay funcione al 100% y de forma fluida, es necesario otorgar los permisos de Cámara (para videollamadas), Micrófono (para capturar llamadas/audio) y Almacenamiento/Contenido multimedia (para descargar y guardar videos localmente).\n\n¿Deseas conceder estos permisos ahora?",
                            fontSize = 15.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                permissionLauncher.launch(permissionsToRequest.toTypedArray())
                            }
                        ) {
                            Text("Confirmar y Otorgar")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showPermissionExplanationDialog = false }
                        ) {
                            Text("Más tarde")
                        }
                    }
                )
            }

            // 5. Shared File detected overlay
            val sharedFile = viewModel.sharedFile.collectAsState()
            val uploadProgress = viewModel.uploadProgress.collectAsState()
            val uploadStatus = viewModel.uploadStatus.collectAsState()
            val sharedFileData = sharedFile.value

            if (sharedFileData != null) {
                AlertDialog(
                    onDismissRequest = { 
                        if (uploadProgress.value == null || uploadProgress.value == 100) {
                            viewModel.clearSharedFile() 
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Archivo Compartido",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Archivo Recibido 📥")
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Se ha detectado un archivo multimedia listo para subir al servidor StreamPay.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Nombre: ${sharedFileData.name}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    val sizeMb = if (sharedFileData.size > 0) {
                                        String.format(java.util.Locale.US, "%.2f MB", sharedFileData.size.toDouble() / (1024 * 1024))
                                    } else {
                                        "Desconocido"
                                    }
                                    Text(
                                        text = "Tamaño: $sizeMb",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Tipo: ${sharedFileData.mimeType}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            val progress = uploadProgress.value
                            val statusStr = uploadStatus.value
                            if (statusStr != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = statusStr,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (progress != null) {
                                    LinearProgressIndicator(
                                        progress = progress / 100f,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        if (uploadProgress.value == null) {
                            Button(
                                onClick = { viewModel.uploadSharedFile(sharedFileData) }
                            ) {
                                Text("Subir al Servidor 🚀")
                            }
                        } else if (uploadProgress.value == 100) {
                            Button(
                                onClick = { viewModel.clearSharedFile() }
                            ) {
                                Text("Aceptar ✅")
                            }
                        }
                    },
                    dismissButton = {
                        if (uploadProgress.value == null || uploadProgress.value == 100) {
                            TextButton(
                                onClick = { viewModel.clearSharedFile() }
                            ) {
                                Text("Descartar")
                            }
                        }
                    }
                )
            }

            // 6. Dialogue to request storage access permissions on manual download intercepts
            val pendingDownload = viewModel.pendingDownloadRequest.collectAsState()
            val pendingRequest = pendingDownload.value
            if (pendingRequest != null) {
                AlertDialog(
                    onDismissRequest = {
                        // Cancel request or default to internal
                        viewModel.forceDownloadNow(pendingRequest.title, pendingRequest.url, pendingRequest.videoId, useInternalFallback = true)
                        viewModel.clearPendingDownloadRequest()
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Permiso de almacenamiento",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Acceso al Almacenamiento")
                        }
                    },
                    text = {
                        Text(
                            text = "StreamPay requiere acceso al almacenamiento del dispositivo para poder guardar correctamente el archivo de video '${pendingRequest.title}' en la ruta externa de descargas.\n\n¿Desea otorgar este permiso para continuar?",
                            fontSize = 15.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    arrayOf(
                                        android.Manifest.permission.READ_MEDIA_VIDEO
                                    )
                                } else {
                                    arrayOf(
                                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    )
                                }
                                storagePermissionLauncher.launch(perms)
                            }
                        ) {
                            Text("Permitir Acceso")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                // Default to internal memory fallback
                                viewModel.forceDownloadNow(pendingRequest.title, pendingRequest.url, pendingRequest.videoId, useInternalFallback = true)
                                android.widget.Toast.makeText(context, "Guardando en memoria interna de la aplicación.", android.widget.Toast.LENGTH_SHORT).show()
                                viewModel.clearPendingDownloadRequest()
                            }
                        ) {
                            Text("Guardar en Memoria Interna")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FloatingMenuItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
