package ru.untriedduck.weatherforecast

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.util.Log
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import ru.untriedduck.weatherforecast.databinding.ActivityMainBinding
import kotlin.math.roundToInt
import android.util.TypedValue
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import ru.untriedduck.weatherforecast.weather.WeatherConditionDrawable
import ru.untriedduck.weatherforecast.weather.WindDirection
import kotlin.coroutines.resume
import kotlin.math.log


class MainActivity : AppCompatActivity() {
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var binding: ActivityMainBinding

    // Выносим очередь Volley на уровень класса, чтобы не создавать её при каждом запросе
    private lateinit var requestQueue: RequestQueue

    // 1. Регистрируем лаунчер в начале класса Activity
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionContinuation?.resume(it)
        }
    private var permissionContinuation: CancellableContinuation<Boolean>? = null

    // 2. Превращаем асинхронный запрос в "ожидаемый"
    private suspend fun ActivityResultLauncher<String>.launchSuspend(permission: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            permissionContinuation = continuation
            launch(permission)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        // Включаем отображение "от края до края" под статус-баром
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, windowInsets ->
            val statusBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())

            // Задаем верхний отступ для AppBarLayout
            view.updatePadding(top = statusBarInsets.top)

            windowInsets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, windowInsets ->
            val navigationBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())

            // Добавляем нижний отступ вашему контенту, чтобы его не перекрывала полоса жестов
            binding.main.updatePadding(bottom = navigationBarsInsets.bottom)

            windowInsets
        }

        // Инициализируем Volley один раз
        requestQueue = Volley.newRequestQueue(this)

        val shared: SharedPreferences = getSharedPreferences("PREFERENCES", MODE_PRIVATE)
        val editor: SharedPreferences.Editor = shared.edit()

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        // Инициализация кнопок верхнего меню App Bar (Material You)
        setupAppBarMenu(shared, editor)
        createNotificationChannel()
        //setupAutomaticUpdateChecks()

        // Проверка локации и первичный запрос
        lifecycleScope.launch { checkLocationAndLoadWeather(shared, editor) }
    }

    private fun createNotificationChannel() {
        // Проверка версии Android (Каналы появились начиная с Android 8.0 / API 26)

        // 1. Указываем точный ID канала (должен совпадать с UpdateCheckService.CHANNEL_ID)
        val channelId = "updates_channel"

        // 2. Имя канала, которое пользователь увидит в настройках телефона
        val name = getString(R.string.weather_notify_channel_title)

        // 3. Описание канала, объясняющее пользователю, зачем он нужен
        val descriptionText = getString(R.string.weather_notify_channel_desc)

        // 4. Уровень важности (DEFAULT или HIGH, чтобы уведомление всплывало баннером сверху)
        val importance = NotificationManager.IMPORTANCE_DEFAULT

        // Создаем сам объект канала
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
            // Можно добавить дополнительные системные фишки по желанию:
            enableLights(true) // Включать светодиод при уведомлении
            lightColor = android.graphics.Color.BLUE
        }

        // Регистрируем созданный канал в системе через NotificationManager
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun setupAppBarMenu(shared: SharedPreferences, editor: SharedPreferences.Editor) {
        // Навешиваем слушатель кликов на меню нашего нового MaterialToolbar
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.refresh_btn -> {
                    lifecycleScope.launch { checkLocationAndLoadWeather(shared, editor) }
                    true // Возвращаем true, чтобы подтвердить обработку клика
                }

                R.id.settings_btn -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }
    }

    private suspend fun checkLocationAndLoadWeather(
        shared: SharedPreferences,
        editor: SharedPreferences.Editor
    ) {
        requestNotificationPermission()

        // МИГРАЦИЯ ДАННЫХ: Проверяем, заходил ли пользователь на этой версии ранее
        if (!shared.contains("USE_GPS")) {
            // Если ключа "USE_GPS" нет, значит это апдейт со старой версии.
            // Старая версия всегда работала только по GPS, поэтому принудительно пишем true.
            editor.putBoolean("USE_GPS", true).apply()
        }

        if (shared.getBoolean("USE_GPS", false)) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Программа сама приостановит выполнение на этой строчке, пока юзер не нажмет кнопку
                val isGranted =
                    requestPermissionLauncher.launchSuspend(Manifest.permission.ACCESS_FINE_LOCATION)
                if (!isGranted) {
                    // Если отказал, загружаем старое и выходим
                    loadSavedWeather(shared)
                    return
                }
            }

            val location = locationClient.lastLocation.await()
            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude

                editor.putString("lat", "$lat")
                editor.putString("lon", "$lon")
                editor.apply()

                val apiKey = shared.getString("apiKey", "").toString()
                val units =
                    if (!shared.getBoolean("use_fahrenheit", false)) "metric" else "imperial"

                getWeather(lon.toString(), lat.toString(), apiKey, units)
                binding.tvUpdateStatus.text =
                    getString(R.string.tv_update_status_updated_for_current_location_status)
            } else {
                loadSavedWeather(shared)
            }
        } else {
            loadWeatherBySavedCity(shared)
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables", "DiscouragedApi")
    private fun getWeather(lon: String, lat: String, apiKey: String, units: String) {
        binding.progressBar.visibility = View.VISIBLE

        val url = getString(R.string.__api_url, lat, lon, apiKey, getString(R.string.lang), units)
        val uviUrl =
            getString(R.string.__uvi_api_url, lat, lon, apiKey, units, getString(R.string.lang))
        val aqiUrl =
            getString(R.string.__aqi_api_url, lat, lon, apiKey, units, getString(R.string.lang))

        val stringRequest = StringRequest(Request.Method.GET, url, { response ->
            binding.progressBar.visibility = View.GONE
            val root = JSONObject(response)
            val weather = root.getJSONArray("weather").getJSONObject(0)
            val weatherId = weather.getInt("id")
            val desc = weather.getString("description")
            val icon = weather.getString("icon")

            val main = root.getJSONObject("main")
            val temp = main.getString("temp").toFloat().roundToInt().toString()
            val feelsLike = main.getString("feels_like").toFloat().roundToInt().toString()
            val tempMin = main.getString("temp_min").toFloat().roundToInt().toString()
            val tempMax = main.getString("temp_max").toFloat().roundToInt().toString()
            val humidity = main.getString("humidity").toInt().toString()
            val dewPoint = getDewPoint(temp.toDouble(), humidity.toDouble(), units)
            val totalPressure = main.getString("pressure").toFloat()
            val seaPressure = main.getString("sea_level").toFloat()
            val grndPressure = main.getString("grnd_level").toFloat()

            val wind = root.getJSONObject("wind")
            val windSpeed = wind.getString("speed")
            val windDegree = wind.getString("deg").toFloat()
            val windGust = wind.getString("gust")

            val sys = root.getJSONObject("sys")
            val country = sys.getString("country")
            val sunrise = sys.getLong("sunrise")
            val sunset = sys.getLong("sunset")
            val name = root.getString("name")

            // Изменение текстов
            binding.tvTemp.text =
                getString(R.string.temp, temp, if (units == "imperial") "F" else "C")

            // СТРОКА ИСПРАВЛЕНА: Теперь город отправляется в CollapsingToolbarLayout
            binding.collapsingToolbarLayout.title =
                getString(R.string.tv_country_text, name, country)

            binding.tvDesc.text = getString(R.string.tv_desc_text, desc)

            binding.tvFeelsLike.text = getString(
                R.string.feels_like_text,
                feelsLike,
                if (units == "imperial") "F" else "C"
            )
            binding.tvTempMin.text =
                getString(R.string.temp, tempMin, if (units == "imperial") "F" else "C")
            binding.tvTempMax.text =
                getString(R.string.temp, tempMax, if (units == "imperial") "F" else "C")

            binding.tvHumid.text = getString(R.string.humidity_text, humidity)
            binding.tvDewPoint.text = getString(R.string.dew_point_format, dewPoint.toString(), if (units == "imperial") "F" else "C")

            binding.barometerTotal.currentPressure = totalPressure
            binding.barometerSea.currentPressure = seaPressure
            binding.barometerGround.currentPressure = grndPressure

            binding.ivWindDirectionArrow.rotation = (windDegree + 180) % 360
            binding.tvWindDirectionName.text =
                getString(WindDirection.fromDegrees(windDegree).resId)
            binding.tvWindSpeed.text = getString(R.string.wind_card_wind_speed_format, windSpeed, if (units == "imperial") getString(R.string.wind_speed_units_miles_h) else getString(R.string.wind_speed_units_meters_sec))
            binding.tvWindGust.text = getString(R.string.wind_card_wind_gust_format, windGust, if (units == "imperial") getString(R.string.wind_speed_units_miles_h) else getString(R.string.wind_speed_units_meters_sec))

            binding.sunDayChart.setData(sunrise, sunset, System.currentTimeMillis() / 1000)

            val iconResId = WeatherConditionDrawable.getIconById(weatherId, icon)
            if (iconResId != 0) {
                binding.imgCondition.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        iconResId,
                        null
                    )
                )
            }

        }, { error ->
            binding.progressBar.visibility = View.GONE
            // Сюда можно добавить красивый Material Snackbar в случае ошибки сети
            Log.e("WeatherError", "Volley error: ${error.message}")

            Snackbar.make(
                binding.main,
                getString(R.string.weather_update_failed),
                Snackbar.LENGTH_LONG
            ).apply {
                setAction(getString(R.string.weather_update_retry_action)) {
                    getWeather(lon, lat, apiKey, units)
                }
                setActionTextColor(
                    if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) this@MainActivity.getColorFromAttr(
                        com.google.android.material.R.attr.colorOnTertiary
                    ) else this@MainActivity.getColorFromAttr(com.google.android.material.R.attr.colorTertiary)
                )

                show()
            }
        })

        val uviStringRequest = StringRequest(Request.Method.GET, uviUrl, { uviResponse ->
            val uviRoot = JSONObject(uviResponse)
            val uviValue = uviRoot.getString("value").toFloat().roundToInt()
            binding.tvUviNumber.text = uviValue.toString()
            binding.tvUviDesc.text = getUviDesc(uviValue)
            binding.tvUviAdvice.text = getUviAdvice(uviValue)
        }, { error ->
            binding.progressBar.visibility = View.GONE
            Log.e("WeatherError", "Volley error: ${error.message}")

            Snackbar.make(
                binding.main,
                getString(R.string.weather_update_failed),
                Snackbar.LENGTH_LONG
            ).apply {
                setAction(getString(R.string.weather_update_retry_action)) {
                    getWeather(lon, lat, apiKey, units)
                }
                setActionTextColor(
                    if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) this@MainActivity.getColorFromAttr(
                        com.google.android.material.R.attr.colorOnTertiary
                    ) else this@MainActivity.getColorFromAttr(com.google.android.material.R.attr.colorTertiary)
                )

                show()
            }
        })

        val aqiStringRequest = StringRequest(Request.Method.GET, aqiUrl, { uviResponse ->
            val aqiRoot = JSONObject(uviResponse)
            val aqiList = aqiRoot.getJSONArray("list")

            val aqiMain = aqiList.getJSONObject(0).getJSONObject("main")
            val aqiVal = aqiMain.getInt("aqi")
            binding.tvAqiNumber.text = aqiVal.toString()
            binding.tvAqiDesc.text = getAqiDesc(aqiVal)
            binding.pbAqiVal.progress = aqiVal

            val (progressRes, trackRes) = getAqiColorRes(aqiVal)
            val progressColor = ContextCompat.getColor(this, progressRes)
            val trackColor = ContextCompat.getColor(this, trackRes)
            binding.pbAqiVal.setIndicatorColor(progressColor)
            binding.pbAqiVal.trackColor = trackColor

            val aqiComponents = aqiList.getJSONObject(0).getJSONObject("components")
            val aqiCO = aqiComponents.getString("co")
            val aqiNO = aqiComponents.getString("no")
            val aqiNO2 = aqiComponents.getString("no2")
            val aqiO3 = aqiComponents.getString("o3")
            val aqiSO2 = aqiComponents.getString("so2")
            val aqiPMf2t5 = aqiComponents.getString("pm2_5")
            val aqiPM10 = aqiComponents.getString("pm10")
            val aqiNH3 = aqiComponents.getString("nh3")
            binding.tvCOval.text = getString(R.string.aqi_val_format,
                getString(R.string.co_gas), aqiCO)
            binding.tvNOval.text = getString(R.string.aqi_val_format,
                getString(R.string.no_gas), aqiNO)
            binding.tvNO2val.text = getString(R.string.aqi_val_format,
                getString(R.string.no2_gas), aqiNO2)
            binding.tvO3val.text = getString(R.string.aqi_val_format,
                getString(R.string.o3_gas), aqiO3)
            binding.tvSO2val.text = getString(R.string.aqi_val_format,
                getString(R.string.so2_gas), aqiSO2)
            binding.tvPM25val.text = getString(R.string.aqi_val_format,
                getString(R.string.pm25_gas), aqiPMf2t5)
            binding.tvPM10val.text = getString(R.string.aqi_val_format,
                getString(R.string.pm10_gas), aqiPM10)
            binding.tvNH3val.text = getString(R.string.aqi_val_format,
                getString(R.string.nh3_gas), aqiNH3)
        }, { error ->
            binding.progressBar.visibility = View.GONE
            Log.e("WeatherError", "Volley error: ${error.message}")

            Snackbar.make(
                binding.main,
                getString(R.string.weather_update_failed),
                Snackbar.LENGTH_LONG
            ).apply {
                setAction(getString(R.string.weather_update_retry_action)) {
                    getWeather(lon, lat, apiKey, units)
                }
                setActionTextColor(
                    if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) this@MainActivity.getColorFromAttr(
                        com.google.android.material.R.attr.colorOnTertiary
                    ) else this@MainActivity.getColorFromAttr(com.google.android.material.R.attr.colorTertiary)
                )

                show()
            }
        })

        requestQueue.add(stringRequest)
        requestQueue.add(uviStringRequest)
        requestQueue.add(aqiStringRequest)
    }

    fun getUviDesc(uviVal: Int) : String {
        return when {
            uviVal < 0 -> getString(R.string.unknown)
            uviVal in 0..2 -> getString(R.string.uvi_val_low)
            uviVal in 3..5 -> getString(R.string.uvi_val_moderate)
            uviVal in 6..7 -> getString(R.string.uvi_val_high)
            else -> getString(R.string.uvi_val_dangerous)
        }
    }

    fun getUviAdvice(uviVal: Int) : String {
        return when {
            uviVal < 0 -> getString(R.string.unknown)
            uviVal in 0..2 -> getString(R.string.uvi_adv_ok)
            uviVal in 3..5 -> getString(R.string.uvi_adv_sunglasses_n_cap)
            uviVal in 6..7 -> getString(R.string.uvi_adv_spf_cream)
            else -> getString(R.string.uvi_adv_beware_of_sun)
        }
    }

    fun getAqiDesc(aqiVal: Int) : String {
        return when {
            aqiVal < 0 -> getString(R.string.unknown)
            aqiVal in 0..50 -> getString(R.string.aqi_val_good)
            aqiVal in 51..100 -> getString(R.string.aqi_val_moderate)
            aqiVal in 101..150 -> getString(R.string.aqi_val_unhealthy_for_sensitive_groups)
            aqiVal in 151..200 -> getString(R.string.aqi_val_unhealthy)
            aqiVal in 201..300 -> getString(R.string.aqi_val_very_unhealthy)
            else -> getString(R.string.aqi_val_hazardous)
        }
    }

    @ColorRes
    fun getAqiColorRes(aqi: Int): Pair<Int, Int> {
        return when {
            aqi < 0 -> Pair(R.color.aqi_invalid_progress, R.color.aqi_invalid_track)
            aqi in 0..50 -> Pair(R.color.aqi_good_progress, R.color.aqi_good_track)
            aqi in 51..100 -> Pair(R.color.aqi_moderate_progress, R.color.aqi_moderate_track)
            aqi in 101..150 -> Pair(R.color.aqi_unhealthy_sensitive_progress, R.color.aqi_unhealthy_sensitive_track)
            aqi in 151..200 -> Pair(R.color.aqi_unhealthy_progress, R.color.aqi_unhealthy_track)
            aqi in 201..300 -> Pair(R.color.aqi_very_unhealthy_progress, R.color.aqi_very_unhealthy_track)
            else -> Pair(R.color.aqi_hazardous_progress, R.color.aqi_hazardous_track)
        }
    }

    fun getDewPoint(temp: Double, humidity: Double, units: String): Int {
        val isImperial = units.equals("imperial", ignoreCase = true)

        val tempInCelsius = if (isImperial) (temp - 32.0) * 5.0 / 9.0 else temp

        val a = 17.27
        val b = 237.7

        val alpha = ((a * tempInCelsius) / (b + tempInCelsius)) + log(humidity / 100.0, Math.E)

        val dewPointInCelsius = (b * alpha) / (a - alpha)

        val finalDewPoint = if (isImperial) (dewPointInCelsius * 9.0 / 5.0) + 32.0 else dewPointInCelsius

        return finalDewPoint.roundToInt()
    }

    @ColorInt
    fun Context.getColorFromAttr(@AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun loadSavedWeather(shared: SharedPreferences) {
        val lons = shared.getString("lon", "").toString()
        val lats = shared.getString("lat", "").toString()
        val apiKey = shared.getString("apiKey", "").toString()
        val units = if (!shared.getBoolean("use_fahrenheit", false)) "metric" else "imperial"
        getWeather(lons, lats, apiKey, units)
        binding.tvUpdateStatus.text =
            getString(R.string.tv_update_status_updated_for_saved_location_status)
    }

    private fun loadWeatherBySavedCity(shared: SharedPreferences) {
        val lons = shared.getString("SELECTED_CITY_LON", "").toString()
        val lats = shared.getString("SELECTED_CITY_LAT", "").toString()
        val apiKey = shared.getString("apiKey", "").toString()
        val units = if (!shared.getBoolean("use_fahrenheit", false)) "metric" else "imperial"
        getWeather(lons, lats, apiKey, units)
        binding.tvUpdateStatus.text =
            getString(R.string.tv_update_status_updated_for_selected_city_status)
    }

    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 102)
        }
    }

}

