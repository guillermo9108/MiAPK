package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.pref.ServerConfig
import com.example.ui.theme.CosmicSlateAccent
import com.example.ui.theme.CosmicSlateSecondary
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val ipAddress by viewModel.ipAddressState.collectAsState()
    val port by viewModel.portState.collectAsState()
    val downloadLocation = viewModel.downloadLocationState.collectAsState()
    val keepCache = viewModel.keepCacheState.collectAsState()
    val cacheCleanInterval = viewModel.cacheCleanIntervalState.collectAsState()
    val userId by viewModel.userIdState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .testTag("config_screen_layout")
    ) {
        TopAppBar(
            title = {
                Text(
                    "Ajustes de StreamPay",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.testTag("config_top_bar")
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val isScanning by viewModel.isScanning.collectAsState()
            val scanProgress by viewModel.scanProgress.collectAsState()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_autodetect"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Búsqueda Automática de Red 🌐",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Escanea de forma inteligente la subred local Wi-Fi en busca de tu servidor StreamPay (comprobando puertos web estándar como 80, 8080, 3000, 5000 y 8000).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isScanning) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = scanProgress,
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                            Text(
                                text = "Buscando en la subred local... ${(scanProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.autoDiscoverStreamPayServer() },
                            modifier = Modifier.fillMaxWidth().testTag("autodetect_wifi_server_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Autodetectar Servidor StreamPay 🔍")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_server_config"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Servidor Host",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = ipAddress,
                        onValueChange = { viewModel.onIpAddressChanged(it) },
                        label = { Text("Dirección IP del Host") },
                        placeholder = { Text("ej. 192.168.1.100 o api.streampay.tv") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ip_input")
                    )

                    OutlinedTextField(
                        value = port,
                        onValueChange = { viewModel.onPortChanged(it) },
                        label = { Text("Puerto (Opcional)") },
                        placeholder = { Text("ej. 8080") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("port_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (ipAddress.isBlank()) {
                                    Toast.makeText(context, "Por favor ingresa una dirección IP válida", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.saveConfig(ipAddress.trim(), port.trim())
                                    Toast.makeText(context, "Servidor configurado correctamente", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_config_button")
                        ) {
                            Text("Guardar y Conectar")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.resetConfig()
                                Toast.makeText(context, "Valores de red restablecidos", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("reset_config_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Restablecer")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_storage_config"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Ruta de Descargas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val isInternal = downloadLocation.value == ServerConfig.VAL_LOCATION_INTERNAL

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp)
                                .testTag("location_internal_card")
                                .border(
                                    BorderStroke(
                                        2.dp,
                                        if (isInternal) CosmicSlateSecondary else Color.Transparent
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.saveDownloadLocation(ServerConfig.VAL_LOCATION_INTERNAL)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isInternal) CosmicSlateAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.background
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("INTERNA", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Text("Memoria local del app", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp)
                                .testTag("location_external_card")
                                .border(
                                    BorderStroke(
                                        2.dp,
                                        if (!isInternal) CosmicSlateSecondary else Color.Transparent
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.saveDownloadLocation(ServerConfig.VAL_LOCATION_EXTERNAL)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (!isInternal) CosmicSlateAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.background
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("EXTERNA", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Text("Memoria de dispositivo", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Text(
                        text = "Gestión de Caché",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Mantener Caché de Reproductor", style = MaterialTheme.typography.bodyMedium)
                            Text("Acelera la carga de videos transmitidos", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = keepCache.value,
                            onCheckedChange = { viewModel.saveKeepCache(it) },
                            modifier = Modifier.testTag("keep_cache_switch")
                        )
                    }

                    var expandedInterval by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Intervalo de Limpieza", style = MaterialTheme.typography.bodyMedium)
                        Box {
                            OutlinedButton(
                                onClick = { expandedInterval = true },
                                modifier = Modifier.testTag("interval_dropdown_button")
                            ) {
                                Text(cacheCleanInterval.value)
                            }
                            DropdownMenu(
                                expanded = expandedInterval,
                                onDismissRequest = { expandedInterval = false }
                            ) {
                                listOf("DIARIO", "SEMANAL", "MENSUAL", "NUNCA").forEach { valName ->
                                    DropdownMenuItem(
                                        text = { Text(valName) },
                                        onClick = {
                                            viewModel.saveCacheCleanInterval(valName)
                                            expandedInterval = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            viewModel.clearWebCache()
                            Toast.makeText(context, "Caché y cookies limpiadas del dispositivo", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_cache_btn")
                    ) {
                        Text("Borrar cookies y caché", color = Color.White)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_account_synchronization"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Sincronización Inteligente",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = userId,
                        onValueChange = { viewModel.onUserIdChanged(it) },
                        label = { Text("ID de Usuario StreamPay (UUID)") },
                        placeholder = { Text("Ingresa tu ID de usuario u organización") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user_id_input")
                    )

                    Button(
                        onClick = {
                            viewModel.updateUserId(userId.trim())
                            Toast.makeText(context, "UUID de usuario guardado y conectado", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("connect_user_button")
                    ) {
                        Text("Sincronizar y Conectar Cuenta")
                    }
                }
            }
        }
    }
}
