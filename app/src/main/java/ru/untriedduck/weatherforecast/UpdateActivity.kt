package ru.untriedduck.weatherforecast

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import ru.untriedduck.weatherforecast.databinding.ActivityUpdateBinding
import io.noties.markwon.Markwon

class UpdateActivity : AppCompatActivity() {

    companion object {
        const val CHANNEL_ID = "updates_channel"
        const val NOTIFICATION_ID = 1001
    }
    private lateinit var binding : ActivityUpdateBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(R.layout.activity_update)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun fetchUpdateInfo() {
        val latestUrl =
            "https://api.github.com/repos/untried-duck61/weather_android/releases/latest"
        val queue = Volley.newRequestQueue(this)

        binding.changelogProgress.visibility = View.VISIBLE

        binding.tvWhatsNewBlockTitle.visibility = View.GONE
        binding.tvChangelog.visibility = View.GONE

        val stringRequest = StringRequest(Request.Method.GET, latestUrl, { response ->
            try {
                val root = JSONObject(response)
                val latestVersion = root.getString("tag_name")

                // Получаем текущую версию приложения безопасным способом для современных SDK
                val currentVersion =
                    packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
                        ?: "0.0.0"

                binding.tvInstalledAppVersion.text = currentVersion
                binding.tvAvailableAppVersion.text = latestVersion

                // 2. Вытаскиваем Markdown-текст изменений с сервера GitHub
                val changelogMarkdown = root.optString("body", "Список изменений пуст.")

                // 3. Выключаем крутилку загрузки и показываем блок чейнджлога
                binding.changelogProgress.visibility = View.GONE
                binding.tvWhatsNewBlockTitle.visibility = View.VISIBLE
                binding.tvChangelog.visibility = View.VISIBLE

                // Инициализируем Markwon и рендерим Markdown в твой TextView
                val markwon = Markwon.create(this)
                markwon.setMarkdown(binding.tvChangelog, changelogMarkdown)

                // 4. Логика переключения кнопки действия
                if (isUpdateNeeded(currentVersion, latestVersion)) {
                    val asset = root.getJSONArray("assets").getJSONObject(0)
                    val downloadUrl = asset.getString("browser_download_url")
                    val sizeBytes = asset.getLong("size")

                    // Обновление нужно: настраиваем кнопку на режим "Загрузить"
                    binding.btnAction.text = "Загрузить обновление"
                    binding.btnAction.isEnabled = true
                    binding.btnAction.setOnClickListener {
                        // Сюда мы повесим запуск скачивания (это будет наш следующий шаг)
                    }
                } else {
                    // Обновление не нужно: делаем кнопку блеклой в стиле M3
                    binding.btnAction.text = "Установлена свежая версия"
                    binding.btnAction.isEnabled = false // Кнопка становится неактивной
                }

            } catch (e: Exception) {
                Log.e("UpdateActivity", "Ошибка парсинга: ${e.message}")
                binding.changelogProgress.visibility = View.GONE
            }
        }, { error ->
            Log.e("UpdateActivity", "Ошибка сети Volley: ${error.message}")
            binding.changelogProgress.visibility = View.GONE
            showToast("Не удалось проверить обновления")
        })

        queue.add(stringRequest)
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
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
}
