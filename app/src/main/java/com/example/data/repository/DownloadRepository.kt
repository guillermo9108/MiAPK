package com.example.data.repository

import com.example.data.db.DownloadDao
import com.example.data.model.DownloadItem
import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val downloadDao: DownloadDao) {

    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads()

    suspend fun insert(item: DownloadItem): Long {
        return downloadDao.insert(item)
    }

    suspend fun update(item: DownloadItem) {
        downloadDao.update(item)
    }

    suspend fun delete(item: DownloadItem) {
        downloadDao.delete(item)
    }

    suspend fun getByVideoId(videoId: String): DownloadItem? {
        return downloadDao.getByVideoId(videoId)
    }

    suspend fun getById(id: Int): DownloadItem? {
        return downloadDao.getById(id)
    }

    suspend fun updateStatusAndProgress(id: Int, status: String, progress: Int) {
        downloadDao.updateStatusAndProgress(id, status, progress)
    }
}
