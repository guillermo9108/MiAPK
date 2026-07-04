package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.DownloadItem
import com.example.ui.theme.CosmicSlateSecondary
import com.example.ui.viewmodel.AppViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val downloads = viewModel.downloadsFlow.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("downloads_screen_layout")
    ) {
        TopAppBar(
            title = {
                Text(
                    "Videos Descargados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.testTag("downloads_top_bar")
        )

        if (downloads.value.isEmpty()) {
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
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "No downloads",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No hay descargas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Los videos descargados desde el reproductor del StreamPay aparecerán listados aquí.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .testTag("downloads_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(downloads.value, key = { it.id }) { item ->
                    DownloadCard(
                        item = item,
                        onPlay = {
                            val file = File(item.filePath)
                            if (file.exists()) {
                                viewModel.playVideo(Uri.fromFile(file).toString(), item.title)
                            } else {
                                Toast.makeText(context, "Archivo de video no encontrado en el almacenamiento.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDelete = { viewModel.deleteDownload(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadCard(
    item: DownloadItem,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("download_card_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tamaño: ${item.sizeLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.status == "COMPLETED") {
                        IconButton(
                            onClick = onPlay,
                            modifier = Modifier.testTag("play_button_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Reproducir offline",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (item.status == "FAILED") {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error de descarga",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_button_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (item.status == "DOWNLOADING") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = item.progress / 100f,
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .testTag("card_progress_${item.id}"),
                        color = CosmicSlateSecondary,
                        trackColor = CosmicSlateSecondary.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${item.progress}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun playVideoOffline(context: Context, item: DownloadItem) {
    try {
        val file = File(item.filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Archivo de video no encontrado en el almacenamiento.", Toast.LENGTH_SHORT).show()
            return
        }

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "com.aistudio.streampay.vnyzqk.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("DownloadsScreen", "Error playing offline video content: ${e.message}", e)
        Toast.makeText(context, "Error al reproducir el video offline.", Toast.LENGTH_SHORT).show()
    }
}
