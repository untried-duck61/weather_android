package ru.untriedduck.weatherforecast

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ru.untriedduck.weatherforecast.databinding.ActivityFirstRunBinding
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import ru.untriedduck.weatherforecast.weather.HelperApiMethods

@Suppress("DEPRECATION")
class FirstRunActivity : AppCompatActivity() {
    private val citiesNames = ArrayList<String>()
    private val citiesLat = ArrayList<Double>()
    private val citiesLon = ArrayList<Double>()

    private lateinit var searchAdapter: ArrayAdapter<String>
    private lateinit var binding: ActivityFirstRunBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirstRunBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val shared: SharedPreferences = getSharedPreferences(
            "PREFERENCES",
            MODE_PRIVATE
        )
        val firstTime: Boolean = shared.getBoolean("firstRun", false)
        if (!firstTime && !shared.getString("apiKey", "").isNullOrEmpty()) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            shared.edit {
                putBoolean("firstRun", true)
            }
        }
        binding.btnFinish.setOnClickListener {
            val editor: SharedPreferences.Editor = shared.edit()
            if (binding.tfApiKey.text.isNullOrEmpty()) {
                Toast.makeText(this, R.string.empty_api_key_error_text, Toast.LENGTH_SHORT).show()
            } else {
                editor.putString("apiKey", binding.tfApiKey.text.toString())
                editor.putBoolean("firstRun", false)
                editor.apply()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        val weatherUpdateModes = arrayOf(
            getString(R.string.upd_mode_only_gps),
            getString(R.string.upd_mode_only_city)
        )

        binding.updModeSpinner.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                weatherUpdateModes
            )
        )

        binding.updModeSpinner.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> {
                    binding.citySearchInputLayout.visibility = View.GONE
                    binding.welcomeTextStepThree.visibility = View.GONE
                    shared.edit { putBoolean("USE_GPS", true) }
                }

                1 -> {
                    binding.citySearchInputLayout.visibility = View.VISIBLE
                    binding.welcomeTextStepThree.visibility = View.VISIBLE
                    shared.edit { putBoolean("USE_GPS", false) }
                }
            }
        }

        searchAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, citiesNames)
        binding.citySearchAutoComplete.setAdapter(searchAdapter)

        binding.citySearchAutoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.length >= 3) { // Запрос от 3 букв
                    fetchCities(query, shared)
                }
            }
        })

        binding.citySearchAutoComplete.setOnItemClickListener { _, _, position, _ ->
            shared.edit().apply {
                putString("SELECTED_CITY_NAME", citiesNames[position])
                putString("SELECTED_CITY_LAT", citiesLat[position].toString())
                putString("SELECTED_CITY_LON", citiesLon[position].toString())
                apply()
            }
        }
    }

    private fun fetchCities(query: String, shared: SharedPreferences) {
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org")
            .build()

        val apiService = retrofit.create(HelperApiMethods::class.java)
        lifecycleScope.launch {
            try {
                val responseBody =
                    apiService.getCitiesByQuery(query, 5, binding.tfApiKey.text.toString())
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
