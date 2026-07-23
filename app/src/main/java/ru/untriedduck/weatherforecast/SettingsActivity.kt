package ru.untriedduck.weatherforecast

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import ru.untriedduck.weatherforecast.databinding.ActivitySettingsBinding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import ru.untriedduck.weatherforecast.updates.ApkDownloader
import ru.untriedduck.weatherforecast.updates.ApkInstaller

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

        binding.btnChangeApiKey.setOnClickListener {
            showChangeApiKeyDialog(shared, editor)
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
                val latestVersion = root.getString("tag_name")
                val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName!!

                // Твоя отличная логика покомпонентного сравнения версий
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
                    Toast.makeText(this@SettingsActivity, R.string.update_latest, Toast.LENGTH_SHORT).show()
                    return@StringRequest
                }

                // Сюда мы доходим, только если обновление РЕАЛЬНО есть
                // 1. Инициализируем наши новые безопасные инструменты
                val installer = ApkInstaller(this@SettingsActivity)
                val downloader = ApkDownloader(this@SettingsActivity)

                // 2. Проверяем разрешение на установку из неизвестных источников
                if (!installer.checkInstallPermission()) {
                    Toast.makeText(this@SettingsActivity,
                        getString(R.string.update_install_request_permission), Toast.LENGTH_LONG).show()
                    installer.openInstallSettings()
                    return@StringRequest // Останавливаемся, пока пользователь не включит тумблер
                }

                // Достаем прямую ссылку на APK из JSON ответа GitHub
                val latestApkUrl = root.getJSONArray("assets").getJSONObject(0).getString("browser_download_url")

                Toast.makeText(this@SettingsActivity,
                    getString(R.string.update_indicator_downloading), Toast.LENGTH_SHORT).show()

                // 3. Запускаем корутину прямо внутри ответа Volley для фонового скачивания
                lifecycleScope.launch {
                    val downloadedFile = downloader.downloadApk(latestApkUrl)

                    if (downloadedFile != null && downloadedFile.exists()) {
                        // Файл в кэше, разрешение есть — запускаем чистую установку!
                        installer.installApk(downloadedFile)
                    } else {
                        Toast.makeText(this@SettingsActivity,
                            getString(R.string.update_download_failed), Toast.LENGTH_LONG).show()
                    }
                }
            },
            { _ ->
                Toast.makeText(this@SettingsActivity, R.string.update_error, Toast.LENGTH_LONG).show()
            }
        )
        queue.add(stringRequest)
    }

    fun showChangeApiKeyDialog(shared: SharedPreferences, editor: SharedPreferences.Editor){
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_api_key, null)
        val tfApiKeyEdit = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.tf_edit_api_key)
        tfApiKeyEdit.setText(shared.getString("apiKey", "").toString())
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_edit_api_key_title))
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save"){ dialog, _ ->
                with(editor) {
                    putString("apiKey", tfApiKeyEdit.text.toString().trim())
                    apply()
                }
                dialog.dismiss()
            }
            .show()
    }

}