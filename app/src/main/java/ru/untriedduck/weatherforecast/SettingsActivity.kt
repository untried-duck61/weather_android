package ru.untriedduck.weatherforecast

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import ru.untriedduck.weatherforecast.databinding.ActivitySettingsBinding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.json.JSONArray
import ru.untriedduck.weatherforecast.services.UpdateCheckService
import ru.untriedduck.weatherforecast.updates.ApkDownloader
import ru.untriedduck.weatherforecast.updates.ApkInstaller
import ru.untriedduck.weatherforecast.weather.HelperApiMethods
import kotlin.jvm.java

class SettingsActivity : AppCompatActivity() {
    private val citiesNames = ArrayList<String>()
    private val citiesLat = ArrayList<Double>()
    private val citiesLon = ArrayList<Double>()

    private lateinit var searchAdapter: ArrayAdapter<String>
    private lateinit var binding: ActivitySettingsBinding
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
            val intent = Intent(this, UpdateCheckService::class.java).apply {
                putExtra("IS_MANUAL_CHECK", true)
            }
            startService(intent)
        }

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName ?: "unknown"

            binding.tvCurrentVersion.text =
                getString(R.string.tv_current_version_format, versionName)
        } catch (e: Exception) {
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
            with(editor) {
                putBoolean("use_fahrenheit", isChecked)
                apply()
            }
        }

        binding.btnChangeApiKey.setOnClickListener {
            showChangeApiKeyDialog(shared, editor)
        }

        binding.switchWeatherUpdMode.isChecked = shared.getBoolean("USE_GPS", false)

        binding.switchWeatherUpdMode.setOnCheckedChangeListener { _, isChecked ->
            with(editor) {
                putBoolean("USE_GPS", isChecked)
                apply()
            }
            binding.cityNameTextfieldContainer.post {
                binding.cityNameTextfieldContainer.visibility = if (!isChecked) View.VISIBLE else View.GONE
                binding.cityNameTextfieldContainer.requestLayout()
            }
        }

        binding.cityNameTextfieldContainer.post {
            binding.cityNameTextfieldContainer.visibility = if (!shared.getBoolean("USE_GPS", false)) View.VISIBLE else View.GONE
        }

        searchAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, citiesNames)
        binding.cityNameTextfield.setAdapter(searchAdapter)

        binding.cityNameTextfield.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.length >= 3) { // Запрос от 3 букв
                    fetchCities(query, shared)
                }
            }
        })

    }

    fun showChangeApiKeyDialog(shared: SharedPreferences, editor: SharedPreferences.Editor) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_api_key, null)
        val tfApiKeyEdit =
            view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.tf_edit_api_key)
        tfApiKeyEdit.setText(shared.getString("apiKey", "").toString())
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_edit_api_key_title))
            .setView(view)
            .setNegativeButton(getString(R.string.dialog_api_key_cancel_btn), null)
            .setPositiveButton(getString(R.string.dialog_api_key_save_btn)) { dialog, _ ->
                with(editor) {
                    putString("apiKey", tfApiKeyEdit.text.toString().trim())
                    apply()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun fetchCities(query: String, shared: SharedPreferences) {
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org")
            .build()

        val apiService = retrofit.create(HelperApiMethods::class.java)
        lifecycleScope.launch {
            try {
                val responseBody =
                    apiService.getCitiesByQuery(query, 5, shared.getString("apiKey", "")!!)
                val jsonString = responseBody.string()

                val jsonArray = JSONArray(jsonString)

                citiesNames.clear()
                citiesLat.clear()
                citiesLon.clear()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)

                    // Собираем красивую строку: "Имя, Страна (Область)"
                    val name = if (obj.getJSONObject("local_names")
                            .getString(getString(R.string.lang)) == null
                    ) obj.getString("name") else obj.getJSONObject("local_names")
                        .getString(getString(R.string.lang))
                    val country = obj.getString("country")
                    val state = obj.optString("state", "")
                    val fullName =
                        if (state.isNotEmpty()) "$name, $country ($state)" else "$name, $country"

                    citiesNames.add(fullName)
                    citiesLat.add(obj.getDouble("lat"))
                    citiesLon.add(obj.getDouble("lon"))
                }

                searchAdapter.notifyDataSetChanged()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}