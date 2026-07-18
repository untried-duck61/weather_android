package ru.untriedduck.weatherforecast

//import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import androidx.core.content.res.ResourcesCompat
//import android.util.Log
//import android.widget.Toast
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.snackbar.Snackbar
//import org.json.JSONArray
import org.json.JSONObject
import ru.untriedduck.weatherforecast.databinding.ActivityMainBinding
import kotlin.math.roundToInt
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt


class MainActivity : AppCompatActivity() {
    private lateinit var locationClient: FusedLocationProviderClient
    private val locationPermissionRequest = 1001
    private lateinit var binding: ActivityMainBinding

    // Выносим очередь Volley на уровень класса, чтобы не создавать её при каждом запросе
    private lateinit var requestQueue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        // Включаем отображение "от края до края" под статус-баром
        enableEdgeToEdge()
        setContentView(binding.root)

        // Инициализируем Volley один раз
        requestQueue = Volley.newRequestQueue(this)

        val shared: SharedPreferences = getSharedPreferences("PREFERENCES", MODE_PRIVATE)
        val editor: SharedPreferences.Editor = shared.edit()

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        // Инициализация кнопок верхнего меню App Bar (Material You)
        setupAppBarMenu(shared)

        // Проверка локации и первичный запрос
        checkLocationAndLoadWeather(shared, editor)
    }

    private fun setupAppBarMenu(shared: SharedPreferences) {
        // Навешиваем слушатель кликов на меню нашего нового MaterialToolbar
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.refresh_btn -> {
                    // Логика обновления (ваша старая логика из binding.refreshBtn)
                    val lon = shared.getString("lon", "").toString()
                    val lat = shared.getString("lat", "").toString()
                    val apiKey = shared.getString("apiKey", "").toString()

                    getWeather(lon, lat, apiKey)
                    binding.tvUpdateStatus.text = getString(R.string.tv_update_status_updated_for_saved_location_status)
                    true // Возвращаем true, чтобы подтвердить обработку клика
                }
                R.id.settings_btn -> {
                    // Сюда добавьте переход на экран настроек, когда он появится:
                    // startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun checkLocationAndLoadWeather(shared: SharedPreferences, editor: SharedPreferences.Editor) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionRequest)
            return
        }

        locationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude

                editor.putString("lat", "$lat")
                editor.putString("lon", "$lon")
                editor.apply()

                val lons = shared.getString("lon", "").toString()
                val lats = shared.getString("lat", "").toString()
                val apiKey = shared.getString("apiKey", "").toString()

                getWeather(lons, lats, apiKey)
                binding.tvUpdateStatus.text = getString(R.string.tv_update_status_updated_for_current_location_status)
            } else {
                val lons = shared.getString("lon", "").toString()
                val lats = shared.getString("lat", "").toString()
                val apiKey = shared.getString("apiKey", "").toString()

                getWeather(lons, lats, apiKey)
                binding.tvUpdateStatus.text = getString(R.string.tv_update_status_updated_for_saved_location_status)
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables", "DiscouragedApi")
    private fun getWeather(lon: String, lat: String, apiKey: String) {
        binding.progressBar.visibility = View.VISIBLE

        val url = getString(R.string.__api_url, lat, lon, apiKey, getString(R.string.lang))

        val stringRequest = StringRequest(Request.Method.GET, url, { response ->
            binding.progressBar.visibility = View.GONE
            val root = JSONObject(response)
            val weather = root.getJSONArray("weather").getJSONObject(0)
            val desc = weather.getString("description")
            val icon = weather.getString("icon")

            val main = root.getJSONObject("main")
            val temp = main.getString("temp").toFloat().roundToInt().toString()
            val feelsLike = main.getString("feels_like").toFloat().roundToInt().toString()
            val tempMin = main.getString("temp_min").toFloat().roundToInt().toString()
            val tempMax = main.getString("temp_max").toFloat().roundToInt().toString()
            val humidity = main.getString("humidity").toInt().toString()

            val sys = root.getJSONObject("sys")
            val country = sys.getString("country")
            val name = root.getString("name")

            // Изменение текстов
            binding.tvTemp.text = getString(R.string.temp, temp)

            // СТРОКА ИСПРАВЛЕНА: Теперь город отправляется в CollapsingToolbarLayout
            binding.collapsingToolbarLayout.title = getString(R.string.tv_country_text, name, country)

            binding.tvDesc.text = getString(R.string.tv_desc_text, desc)
            binding.tvFeelsLike.text = getString(R.string.feels_like_text, feelsLike)
            binding.tvTempMin.text = getString(R.string.temp, tempMin)
            binding.tvTempMax.text = getString(R.string.temp, tempMax)
            binding.tvHumid.text = getString(R.string.humidity_text, humidity)

            // Установка иконки погоды (рекомендуется использовать .setImageDrawable вместо .background)
            val iconResId = resources.getIdentifier(
                getString(R.string.__weather_icon_template, icon),
                getString(R.string.__res_type),
                packageName
            )
            if (iconResId != 0) {
                binding.imgCondition.setImageDrawable(ResourcesCompat.getDrawable(resources, iconResId, null))
            }

        }, { error ->
            binding.progressBar.visibility = View.GONE
            // Сюда можно добавить красивый Material Snackbar в случае ошибки сети
            Log.e("WeatherError", "Volley error: ${error.message}")

            // Создаем и показываем Material 3 Snackbar
            Snackbar.make(
                binding.main, // Передаем корневой CoordinatorLayout
                "Не удалось обновить погоду. Проверьте интернет.", // Текст ошибки
                Snackbar.LENGTH_LONG // Время отображения
            ).apply {
                // Добавляем кнопку "Повторить" прямо внутрь уведомления
                setAction("Повторить") {
                    // При нажатии запускаем повторный запрос погоды
                    getWeather(lon, lat, apiKey)
                }
                // Задаем цвет кнопке действия из палитры темы приложения
                setActionTextColor(this@MainActivity.getColorFromAttr(com.google.android.material.R.attr.colorTertiary))

                show() // Показываем Snackbar
            }
        })

        // Добавляем запрос в общую единую очередь класса
        requestQueue.add(stringRequest)
    }

    @ColorInt
    fun Context.getColorFromAttr(@AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}

