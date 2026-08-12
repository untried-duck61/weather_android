package ru.untriedduck.weatherforecast.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import ru.untriedduck.weatherforecast.R

class UpdateCheckService : Service() {
    companion object {
        const val CHANNEL_ID = "updates_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isManual = intent?.getBooleanExtra("IS_MANUAL_CHECK", false) ?: false
        fetchUpdateInfo(isManual)
        return START_NOT_STICKY
    }

    private fun fetchUpdateInfo(isManual: Boolean) {
        // Твоя реальная рабочая ссылка на GitHub API
        val latestUrl = "https://api.github.com/repos/untried-duck61/weather_android/releases/latest"
        val queue = Volley.newRequestQueue(this)

        if (isManual) {
            showToast(getString(R.string.update_indicator_checking)) // "Проверка обновлений..."
        }

        val stringRequest = StringRequest(Request.Method.GET, latestUrl, { response ->
            try {
                val root = JSONObject(response)
                val latestVersion = root.getString("tag_name")
                val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "0.0.0.0"

                if (isUpdateNeeded(currentVersion, latestVersion)) {
                    val asset = root.getJSONArray("assets").getJSONObject(0)
                    val downloadUrl = asset.getString("browser_download_url")
                    val sizeBytes = asset.getLong("size")

                    showUpdateAvailableNotification(latestVersion, downloadUrl, sizeBytes)
                } else {
                    if (isManual) {
                        showUpToDateNotification()
                    }
                }
            } catch (e: Exception) {
                Log.e("UpdateService", "Parse error: ${e.message}")
            }
            stopSelf()
        }, {
            Log.e("UpdateService", "Volley network error")
            stopSelf()
        })
        queue.add(stringRequest)
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showUpToDateNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_system_update)
            .setContentTitle(getString(R.string.update_latest))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun isUpdateNeeded(current: String, latest: String): Boolean {
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val maxParts = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxParts) {
            val c = currentParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (c > l) return false
        }
        return false
    }

    private fun showUpdateAvailableNotification(version: String, url: String, size: Long) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val mbSize = getString(R.string.file_size_format).format(size / (1024.0 * 1024.0))

        val downloadIntent = Intent(this, UpdateReceiver::class.java).apply {
            action = "ACTION_START_DOWNLOAD"
            putExtra("DOWNLOAD_URL", url)
            putExtra("FILE_SIZE", size)
        }

        val pDownload = PendingIntent.getBroadcast(
            this, 1, downloadIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_system_update)
            .setContentTitle(getString(R.string.update_available_notify_title, version))
            .setContentText(getString(R.string.update_available_notify_text, mbSize))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(R.drawable.ic_download, getString(R.string.update_available_notify_action_update), pDownload)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}