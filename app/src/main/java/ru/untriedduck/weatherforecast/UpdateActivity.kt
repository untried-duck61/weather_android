package ru.untriedduck.weatherforecast

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class UpdateActivity : AppCompatActivity() {
    //private val latestVer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_update)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun checkUpdatesFromGitHub() {
        val latestUrl =
            "https://api.github.com/repos/untried-duck61/weather_android/releases/latest"
        val queue = Volley.newRequestQueue(this)

        val stringRequest = StringRequest(
            Request.Method.GET,
            latestUrl,
            { response ->
                val root = JSONObject(response)
                val latestVersion = root.getString("tag_name")
                val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName!!

                val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
                val latestParts = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }

                var isNewerAvailable = false
                val maxParts = maxOf(currentParts.size, latestParts.size)
                for (i in 0 until maxParts) {
                    val currentPart = currentParts.getOrElse(i) { 0 }
                    val latestPart = latestParts.getOrElse(i) { 0 }
                    if (latestPart > currentPart) {
                        isNewerAvailable = true
                        break
                    } else if (currentPart > latestPart) {
                        break
                    }
                }

                if (!isNewerAvailable) {
                    Toast.makeText(
                        this@UpdateActivity,
                        R.string.update_latest,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@StringRequest
                }

                // Сюда мы доходим, только если обновление РЕАЛЬНО есть
                // 1. Инициализируем наши новые безопасные инструменты
                //val installer = ApkInstaller(this@SettingsActivity)
                //val downloader = ApkDownloader(this@SettingsActivity)

                // 2. Проверяем разрешение на установку из неизвестных источников
                /*if (!installer.checkInstallPermission()) {
                    Toast.makeText(
                        this@UpdateActivity,
                        getString(R.string.update_install_request_permission), Toast.LENGTH_LONG
                    ).show()
                    installer.openInstallSettings()
                    return@StringRequest // Останавливаемся, пока пользователь не включит тумблер
                }

                // Достаем прямую ссылку на APK из JSON ответа GitHub
                val latestApkUrl =
                    root.getJSONArray("assets").getJSONObject(0).getString("browser_download_url")

                Toast.makeText(
                    this@SettingsActivity,
                    getString(R.string.update_indicator_downloading), Toast.LENGTH_SHORT
                ).show()

                // 3. Запускаем корутину прямо внутри ответа Volley для фонового скачивания
                lifecycleScope.launch {
                    val downloadedFile = downloader.downloadApk(latestApkUrl)

                    if (downloadedFile != null && downloadedFile.exists()) {
                        // Файл в кэше, разрешение есть — запускаем чистую установку!
                        installer.installApk(downloadedFile)
                    } else {
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.update_download_failed), Toast.LENGTH_LONG
                        ).show()
                    }
                }*/
            },
            { _ ->
                Toast.makeText(this@SettingsActivity, R.string.update_error, Toast.LENGTH_LONG)
                    .show()
            }
        )
        queue.add(stringRequest)
    }
}
