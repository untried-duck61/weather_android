package ru.untriedduck.weatherforecast.updates

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream

class ApkDownloader(private val context: Context) {
    private val apiService: UpdateApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://github.com/")
            .build()
            .create(UpdateApiService::class.java)
    }

    suspend fun downloadApk(url: String, onProgress: (Long, Long) -> Unit): File? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.downloadUpdateApk(url)
            val body = response.body()

            if (response.isSuccessful && body != null) {
                val totalBytes = body.contentLength()
                val apkFile = File(context.filesDir, "update.apk")

                body.byteStream().use { inputStream ->
                    FileOutputStream(apkFile).use { outputStream ->
                        val buffer = ByteArray(8192) // Чуть увеличили буфер для скорости
                        var bytesRead: Int
                        var bytesWritten = 0L

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            ensureActive()

                            outputStream.write(buffer, 0, bytesRead)
                            bytesWritten += bytesRead
                            // Передаем текущий прогресс вверх
                            onProgress(bytesWritten, totalBytes)
                        }
                        outputStream.flush()
                    }
                }
                return@withContext apkFile
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
        }
        return@withContext null
    }
}