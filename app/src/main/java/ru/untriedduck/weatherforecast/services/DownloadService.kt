package ru.untriedduck.weatherforecast.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.untriedduck.weatherforecast.R
import ru.untriedduck.weatherforecast.updates.ApkDownloader
import ru.untriedduck.weatherforecast.updates.ApkInstaller

class DownloadService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("DOWNLOAD_URL") ?: ""

        // Сразу переводим в Foreground режим для соблюдения политик Android 14+ (minSdk 34)
        startForeground(
            UpdateCheckService.NOTIFICATION_ID,
            createProgressNotification(0, "0.00", "0.00")
        )

        scope.launch {
            val downloader = ApkDownloader(this@DownloadService)
            val installer = ApkInstaller(this@DownloadService)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Запускаем скачивание с обновлением прогресса в реальном времени
            val file = downloader.downloadApk(url) { bytesRead, totalBytes ->
                val progress = if (totalBytes > 0) ((bytesRead * 100) / totalBytes).toInt() else 0
                val readMb = "%.2f".format(bytesRead / (1024.0 * 1024.0))
                val totalMb = "%.2f".format(totalBytes / (1024.0 * 1024.0))

                nm.notify(UpdateCheckService.NOTIFICATION_ID, createProgressNotification(progress, readMb, totalMb))
            }

            if (file != null && file.exists()) {
                // Готовим Intent для ручного запуска установки из уведомления (резервный вариант)
                val apkUri = FileProvider.getUriForFile(
                    this@DownloadService, "${packageName}.fileprovider", file
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val pInstall = PendingIntent.getActivity(
                    this@DownloadService, 2, installIntent, PendingIntent.FLAG_IMMUTABLE
                )

                val doneNotification = NotificationCompat.Builder(this@DownloadService, UpdateCheckService.CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentTitle(getString(R.string.update_ready_notify_title))
                    .setContentText(getString(R.string.update_ready_notify_text))
                    .setContentIntent(pInstall)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .build()

                nm.notify(UpdateCheckService.NOTIFICATION_ID, doneNotification)

                // ТА САМАЯ ЛОГИКА: Если разрешение на установку есть — запускаем её мгновенно!
                if (installer.checkInstallPermission()) {
                    installer.installApk(file)
                } else {
                    // Если разрешения нет, перенаправляем пользователя в настройки телефона
                    installer.openInstallSettings()
                }
            } else {
                val errorNotification = NotificationCompat.Builder(this@DownloadService, UpdateCheckService.CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentTitle(getString(R.string.update_download_failed))
                    .setOngoing(false)
                    .build()
                nm.notify(UpdateCheckService.NOTIFICATION_ID, errorNotification)
            }
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun createProgressNotification(progress: Int, readMb: String, totalMb: String) =
        NotificationCompat.Builder(this, UpdateCheckService.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.update_downloading_notify_title))
            .setContentText(
                getString(
                    R.string.update_downloading_progress_notify_text,
                    readMb,
                    totalMb
                ))
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setSilent(true)
            .build()

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}