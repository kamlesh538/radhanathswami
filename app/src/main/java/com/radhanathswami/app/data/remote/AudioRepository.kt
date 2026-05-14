package com.radhanathswami.app.data.remote

import android.content.Context
import com.radhanathswami.app.data.local.DownloadDao
import com.radhanathswami.app.data.local.DownloadEntity
import com.radhanathswami.app.data.local.DownloadStatus
import com.radhanathswami.app.data.model.AudioItem
import com.radhanathswami.app.data.model.BrowseItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRepository @Inject constructor(
    private val scraper: IskconScraper,
    private val downloadDao: DownloadDao,
    private val httpClient: OkHttpClient,
    @ApplicationContext private val context: Context
) {
    suspend fun getTopLevelCategories(): List<BrowseItem.Folder> =
        scraper.getTopLevelCategories()

    suspend fun browseDirectory(path: String): List<BrowseItem> =
        scraper.browseDirectory(path)

    fun getDownloadedAudios(): Flow<List<AudioItem>> =
        downloadDao.getCompletedDownloads().map { entities ->
            entities.map { it.toAudioItem() }
        }

    suspend fun isDownloaded(id: String): Boolean = downloadDao.isDownloaded(id)

    suspend fun downloadAudio(audio: AudioItem): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val dir = File(context.getExternalFilesDir(null), "downloads").also { it.mkdirs() }
            val file = File(dir, "${audio.id}.mp3")

            if (!file.exists()) {
                downloadDao.insert(
                    DownloadEntity(
                        id = audio.id,
                        title = audio.title,
                        url = audio.url,
                        localPath = file.absolutePath,
                        category = audio.category,
                        date = audio.date,
                        fileSizeMb = audio.fileSizeMb,
                        status = DownloadStatus.IN_PROGRESS
                    )
                )

                val request = Request.Builder().url(audio.url).build()
                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    downloadDao.updateStatus(audio.id, DownloadStatus.FAILED)
                    return@withContext Result.failure(Exception("Download failed: ${response.code}"))
                }

                response.body?.byteStream()?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                downloadDao.updateStatus(audio.id, DownloadStatus.COMPLETED)
            }

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            downloadDao.updateStatus(audio.id, DownloadStatus.FAILED)
            Result.failure(e)
        }
    }

    suspend fun deleteDownload(audio: AudioItem) {
        val entity = downloadDao.getDownloadById(audio.id) ?: return
        File(entity.localPath).delete()
        downloadDao.deleteById(audio.id)
    }

    suspend fun getLocalPathIfDownloaded(id: String): String? {
        val entity = downloadDao.getDownloadById(id) ?: return null
        if (entity.status != DownloadStatus.COMPLETED) return null
        val file = File(entity.localPath)
        return if (file.exists()) file.absolutePath else null
    }

    private fun DownloadEntity.toAudioItem() = AudioItem(
        id = id,
        title = title,
        url = url,
        category = category,
        date = date,
        fileSizeMb = fileSizeMb,
        isDownloaded = true,
        localPath = localPath
    )
}
