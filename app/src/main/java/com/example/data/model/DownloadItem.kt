package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val videoId: String,
    val filePath: String,
    val status: String, // "PENDING", "DOWNLOADING", "COMPLETED", "FAILED"
    val progress: Int,  // 0 to 100
    val sizeLabel: String = "0 KB",
    val timestamp: Long = System.currentTimeMillis()
)
