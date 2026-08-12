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
import android.os.Build
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
import androidx.work.Constraints
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import ru.untriedduck.weatherforecast.services.UpdateWorker
import ru.untriedduck.weatherforecast.weather.WindDirection
import java.util.concurrent.TimeUnit
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
        setupAutomaticUpdateChecks()

        // Проверка локации и первичный запрос
        lifecycleScope.launch { checkLocationAndLoadWeather(shared, editor) }
    }

    private fun createNotificationChannel() {
        // Проверка версии Android (Каналы появились начиная с Android 8.0 / API 26)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // 1. Указываем точный ID канала (должен совпадать с UpdateCheckService.CHANNEL_ID)
            val channelId = "updates_channel"

            // 2. Имя канала, которое пользователь увидит в настройках телефона
            val name = getString(R.string.updates_notify_channel_title)

            // 3. Описание канала, объясняющее пользователю, зачем он нужен
            val descriptionText = getString(R.string.updates_notify_channel_desc)

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
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupAutomaticUpdateChecks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Нужен интернет
            .build()

        val updateRequest = PeriodicWorkRequestBuilder<UpdateWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "daily_weather_update_check",
            ExistingPeriodicWorkPolicy.KEEP, // Если задача уже есть — не пересоздавать
            updateRequest
        )
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

            // Установка иконки погоды (рекомендуется использовать .setImageDrawable вместо .background)
            val iconResId = resources.getIdentifier(
                getString(R.string.__weather_icon_template, icon),
                getString(R.string.__res_type),
                packageName
            )
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

            // Создаем и показываем Material 3 Snackbar
            Snackbar.make(
                binding.main, // Передаем корневой CoordinatorLayout
                getString(R.string.weather_update_failed), // Текст ошибки
                Snackbar.LENGTH_LONG // Время отображения
            ).apply {
                // Добавляем кнопку "Повторить" прямо внутрь уведомления
                setAction(getString(R.string.weather_update_retry_action)) {
                    // При нажатии запускаем повторный запрос погоды
                    getWeather(lon, lat, apiKey, units)
                }
                // Задаем цвет кнопке действия из палитры темы приложения
                setActionTextColor(
                    if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) this@MainActivity.getColorFromAttr(
                        com.google.android.material.R.attr.colorOnTertiary
                    ) else this@MainActivity.getColorFromAttr(com.google.android.material.R.attr.colorTertiary)
                )

                show() // Показываем Snackbar
            }
        })

        // Добавляем запрос в общую единую очередь класса
        requestQueue.add(stringRequest)
    }

    /**
     * Вычисляет округленную точку росы до целого числа на основе температуры и влажности.
     * Automatically converts Fahrenheit to Celsius for calculation and formats the result back.
     *
     * @param temp Текущая температура воздуха (может быть в Цельсиях или Фаренгейтах).
     * @param humidity Текущая относительная влажность воздуха в процентах (в диапазоне от 0.0 до 100.0).
     * @param units Строка системы измерения из OpenWeatherMap ("metric" — Цельсий, "imperial" — Фаренгейт).
     * @return Округленное значение точки росы (Int) в соответствующей системе измерения.
     */
    fun getDewPoint(temp: Double, humidity: Double, units: String): Int {
        // 1. Определяем, используется ли американская система (Фаренгейты)
        val isImperial = units.equals("imperial", ignoreCase = true)

        // 2. Формула Магнуса-Тетенса работает строго с градусами Цельсия.
        // Если на входе Фаренгейты, временно переводим их в Цельсии:
        val tempInCelsius = if (isImperial) (temp - 32.0) * 5.0 / 9.0 else temp

        // 3. Задаем постоянные коэффициенты для формулы Магнуса-Тетенса
        val a = 17.27
        val b = 237.7

        // 4. Вычисляем промежуточное значение alpha
        val alpha = ((a * tempInCelsius) / (b + tempInCelsius)) + log(humidity / 100.0, Math.E)

        // 5. Находим точку росы в градусах Цельсия
        val dewPointInCelsius = (b * alpha) / (a - alpha)

        // 6. Переводим результат обратно в Фаренгейты, если на входе была система imperial
        val finalDewPoint = if (isImperial) (dewPointInCelsius * 9.0 / 5.0) + 32.0 else dewPointInCelsius

        // 7. Округляем до ближайшего целого числа и возвращаем тип Int
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 102)
            }
        }
    }

}

