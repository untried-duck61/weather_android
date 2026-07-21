package ru.untriedduck.weatherforecast

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import ru.untriedduck.weatherforecast.databinding.ActivitySettingsBinding
import java.io.File
import androidx.core.content.FileProvider
import android.net.Uri

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }

        binding.btnCheckUpdates.setOnClickListener {
            checkUpdatesFromGitHub()
        }

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName ?: "unknown"

            binding.tvCurrentVersion.text = getString(R.string.tv_current_version_format,versionName)
        } catch (e: Exception){
            e.printStackTrace()

            binding.tvCurrentVersion.text = getString(R.string.tv_current_version_format, "unknown")
        }

        // 1. Получаем доступ к SharedPreferences (они у тебя называются "PREFERENCES")
        val shared: SharedPreferences = getSharedPreferences("PREFERENCES", MODE_PRIVATE)
        val editor: SharedPreferences.Editor = shared.edit()

        // 2. Читаем сохраненный выбор (по умолчанию false - Цельсий)
        val isFahrenheit = shared.getBoolean("use_fahrenheit", false)

        // 3. Ставим тумблер в нужное положение при входе на экран
        binding.switchTempUnit.isChecked = isFahrenheit

        // 4. Пишем слушатель переключения тумблера
        binding.switchTempUnit.setOnCheckedChangeListener { _, isChecked ->
            // Сохраняем выбор пользователя (true, если включен Фаренгейт)
            editor.putBoolean("use_fahrenheit", isChecked)
            editor.apply()
        }

    }

    fun checkUpdatesFromGitHub() {
        val latestUrl = "https://api.github.com/repos/untried-duck61/weather_android/releases/latest"
        val queue = Volley.newRequestQueue(this)

        val stringRequest = StringRequest(
            Request.Method.GET,
            latestUrl,
            { response ->
                val root = JSONObject(response)
                val latestVersion = root.getString("tag_name")!!
                val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName!!
                // Разбиваем строки версий по точкам, переводим в числа (если не число, то 0)
                val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
                val latestParts = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }

                // Ищем первую разницу в цифрах версий
                var isNewerAvailable = false
                val maxParts = maxOf(currentParts.size, latestParts.size)
                for (i in 0 until maxParts) {
                    val currentPart = currentParts.getOrElse(i) { 0 }
                    val latestPart = latestParts.getOrElse(i) { 0 }
                    if (latestPart > currentPart) {
                        isNewerAvailable = true
                        break
                    } else if (currentPart > latestPart) {
                        break // Локальная версия новее, обновляться не нужно
                    }
                }

                // Если обновления нет или локальная версия новее — выходим
                if (!isNewerAvailable) {
                    Toast.makeText(this@SettingsActivity, R.string.update_latest, Toast.LENGTH_SHORT).show()
                    return@StringRequest
                }
                val latestApkUrl = root.getJSONArray("assets").getJSONObject(0).getString("browser_download_url")
                val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                val uri = latestApkUrl.toUri()

                val request = DownloadManager.Request(uri)
                    .setTitle(getString(R.string.dlmgr_upd_title))
                    .setDescription(getString(R.string.dlmgr_upd_desc, latestVersion))
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "weather_update.apk")

                val downloadId = downloadManager.enqueue(request)
                // 1. Создаём приёмник
                val onDownloadComplete = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        // Проверяем, что скачался именно наш файл
                        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                        if (id == downloadId) {

                            // 1. Получаем путь к скачанному файлу в папке Downloads
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val apkFile = File(downloadsDir, "weather_update.apk")

                            if (apkFile.exists()) {
                                // 2. Превращаем файл в безопасную URI-ссылку через наш FileProvider
                                val apkUri: Uri = FileProvider.getUriForFile(
                                    this@SettingsActivity,
                                    "${packageName}.fileprovider", // Должно строго совпадать с authorities в манифесте!
                                    apkFile
                                )

                                // 3. Создаем интент для запуска системного установщика пакетов
                                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(apkUri, "application/vnd.android.package-archive")

                                    // Даем системе временные права на чтение этого APK-файла
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                                    // Открываем установщик в новом процессе, чтобы наше приложение могло спокойно закрыться
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }

                                // 4. Запускаем установку
                                try {
                                    startActivity(installIntent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(this@SettingsActivity,
                                        getString(R.string.pkgmgr_init_error), Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(this@SettingsActivity,
                                    getString(R.string.pkgmgr_enoent), Toast.LENGTH_SHORT).show()
                            }

                            unregisterReceiver(this)
                        }
                    }
                }

                // 2. Регистрируем наш приёмник в системе, чтобы он слушал событие окончания загрузки
                registerReceiver(
                    onDownloadComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    RECEIVER_NOT_EXPORTED // Параметр безопасности для современных версий Android
                )


            },
            { error ->
                // 2. Сюда прилетит ошибка, если нет интернета
                Toast.makeText(this@SettingsActivity, R.string.update_error, Toast.LENGTH_LONG).show()
            }
        )

        queue.add(stringRequest)
    }
}