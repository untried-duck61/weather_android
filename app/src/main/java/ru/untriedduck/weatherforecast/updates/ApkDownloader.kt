package ru.untriedduck.weatherforecast.updates

import android.content.Context
import kotlinx.coroutines.Dispatchers
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

    suspend fun downloadApk(url: String): File? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.downloadUpdateApk(url)
            val body = response.body()

            if (response.isSuccessful && body != null){
                val apkFile = File(context.cacheDir, "update.apk")
                body.byteStream().use { inputStream ->
                    FileOutputStream(apkFile).use { outputStream ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                        outputStream.flush()
                    }
                }
                return@withContext apkFile
            }
        } catch (e: Exception){
            e.printStackTrace()
        }
        return@withContext null
    }
}