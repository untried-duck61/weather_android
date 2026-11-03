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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.log


class MainActivity : AppCompatActivity() {
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var binding: ActivityMainBinding

    private lateinit var requestQueue: RequestQueue

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionContinuation?.resume(it)
        }
    private var permissionContinuation: CancellableContinuation<Boolean>? = null

    private suspend fun ActivityResultLauncher<String>.launchSuspend(permission: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            permissionContinuation = continuation
            launch(permission)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, windowInsets ->
            val statusBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())

            view.updatePadding(top = statusBarInsets.top)

            windowInsets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, windowInsets ->
            binding.main.updatePadding(bottom = 0)

            windowInsets
        }

        requestQueue = Volley.newRequestQueue(this)

        val shared: SharedPreferences = getSharedPreferences("PREFERENCES", MODE_PRIVATE)
        val editor: SharedPreferences.Editor = shared.edit()

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        setupAppBarMenu(shared, editor)
        createNotificationChannel()

        binding.viewPager.adapter = WeatherPagerAdapter(this)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_current -> {
                    binding.viewPager.setCurrentItem(0, true)
                    true
                }

                R.id.action_hourly -> {
                    binding.viewPager.setCurrentItem(1, true)
                    true
                }

                else -> false
            }
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> {
                        binding.bottomNav.selectedItemId = R.id.action_current
                    }

                    1 -> {
                        binding.bottomNav.selectedItemId = R.id.action_hourly
                    }
                }
            }
        })

        lifecycleScope.launch { checkLocationAndLoadWeather(shared, editor) }
    }

    private fun createNotificationChannel() {
        val channelId = "updates_channel"

        val name = getString(R.string.weather_notify_channel_title)

        val descriptionText = getString(R.string.weather_notify_channel_desc)

        val importance = NotificationManager.IMPORTANCE_DEFAULT

        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
            enableLights(true)
            lightColor = android.graphics.Color.BLUE
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun setupAppBarMenu(shared: SharedPreferences, editor: SharedPreferences.Editor) {
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.refresh_btn -> {
                    lifecycleScope.launch { checkLocationAndLoadWeather(shared, editor) }
                    true
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
        val forecastFragment =
            supportFragmentManager.findFragmentByTag("f0") as? CurrentForecastFragment

        requestNotificationPermission()

        if (!shared.contains("USE_GPS")) {
            editor.putBoolean("USE_GPS", true).apply()
        }

        if (shared.getBoolean("USE_GPS", false)) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val isGranted =
                    requestPermissionLauncher.launchSuspend(Manifest.permission.ACCESS_FINE_LOCATION)
                if (!isGranted) {
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
                forecastFragment?.changeUpdateStatus(R.string.tv_update_status_updated_for_current_location_status)
            } else {
                loadSavedWeather(shared)
            }
        } else {
            loadWeatherBySavedCity(shared)
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables", "DiscouragedApi")
    private fun getWeather(lon: String, lat: String, apiKey: String, units: String) {
        val forecastFragment =
            supportFragmentManager.findFragmentByTag("f0") as? CurrentForecastFragment
        forecastFragment?.changeProgressBarVisibility(View.VISIBLE)

        var errorCount : Int = 0

        val url = getString(R.string.__api_url, lat, lon, apiKey, getString(R.string.lang), units)
        val uviUrl =
            getString(R.string.__uvi_api_url, lat, lon, apiKey, units, getString(R.string.lang))
        val aqiUrl =
            getString(R.string.__aqi_api_url, lat, lon, apiKey, units, getString(R.string.lang))

        val stringRequest = StringRequest(Request.Method.GET, url, { response ->
            forecastFragment?.changeProgressBarVisibility(View.GONE)
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

            binding.collapsingToolbarLayout.title =
                getString(R.string.tv_country_text, name, country)

            forecastFragment?.displayWeather(
                temp, units, desc, feelsLike, tempMin, tempMax,
                humidity, dewPoint, totalPressure, seaPressure, grndPressure,
                windDegree, windSpeed, windGust, sunrise, sunset, weatherId, icon
            )

        }, { error ->
            errorCount++
        })

        val uviStringRequest = StringRequest(Request.Method.GET, uviUrl, { uviResponse ->
            val uviRoot = JSONObject(uviResponse)
            val uviValue = uviRoot.getString("value").toFloat().roundToInt()
            forecastFragment?.displayUvi(uviValue)
        }, { error ->
            errorCount++
        })

        val aqiStringRequest = StringRequest(Request.Method.GET, aqiUrl, { aqiResponse ->
            val aqiRoot = JSONObject(aqiResponse)
            val aqiList = aqiRoot.getJSONArray("list")

            val aqiMain = aqiList.getJSONObject(0).getJSONObject("main")
            val aqiVal = aqiMain.getInt("aqi")

            val aqiComponents = aqiList.getJSONObject(0).getJSONObject("components")
            val aqiCO = aqiComponents.getString("co")
            val aqiNO = aqiComponents.getString("no")
            val aqiNO2 = aqiComponents.getString("no2")
            val aqiO3 = aqiComponents.getString("o3")
            val aqiSO2 = aqiComponents.getString("so2")
            val aqiPMf2t5 = aqiComponents.getString("pm2_5")
            val aqiPM10 = aqiComponents.getString("pm10")
            val aqiNH3 = aqiComponents.getString("nh3")

            forecastFragment?.displayAqi(
                aqiVal, aqiCO, aqiNO, aqiNO2, aqiO3, aqiSO2, aqiPMf2t5, aqiPM10, aqiNH3
            )

        }, { error ->
            errorCount++
        })

        val dailyUrl =
            getString(R.string.__daily_api_url, lat, lon, apiKey, units, getString(R.string.lang))

        val dailyStringRequest = StringRequest(Request.Method.GET, dailyUrl, { dailyResponse ->
            val dailyRoot = JSONObject(dailyResponse)
            val dailyArray = dailyRoot.getJSONArray("list")

            val groupedForecast = LinkedHashMap<String, Pair<String, ArrayList<Map<String, Any>>>>()

            for (day in 0 until dailyArray.length()) {
                val item = dailyArray.getJSONObject(day)
                val dayDt = item.getLong("dt")

                val dayMain = item.getJSONObject("main")
                val dayTemp = dayMain.getDouble("temp").roundToInt().toString()

                val dayWeather = item.getJSONArray("weather").getJSONObject(0)
                val dayWeatherIcon = dayWeather.getString("icon")
                val dayWeatherId = dayWeather.getString("id")

                val calendar = Calendar.getInstance().apply {
                    timeInMillis = dayDt * 1000L
                }

                val year = calendar.get(Calendar.YEAR)
                val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
                val dayKey = "$year-$dayOfYear"

                val hourString = String.format(Locale.getDefault(), "%02d:00", calendar.get(Calendar.HOUR_OF_DAY))

                val sdf = java.text.SimpleDateFormat("EEEE, d MMM", Locale.getDefault())
                val dateTitle = sdf.format(calendar.time).replaceFirstChar { it.uppercase() }

                val hourMap = mapOf(
                    "time" to hourString,
                    "temp" to dayTemp,
                    "icon" to dayWeatherIcon,
                    "id" to dayWeatherId
                )

                if (!groupedForecast.containsKey(dayKey)) {
                    groupedForecast[dayKey] = Pair(dateTitle, ArrayList())
                }

                groupedForecast[dayKey]?.second?.add(hourMap)

            }

            val hourlyFragment = supportFragmentManager.findFragmentByTag("f1") as? HourlyForecastFragment
            hourlyFragment?.updateHourlyData(groupedForecast, units)
        }, { error ->
            errorCount++
        })

        requestQueue.add(stringRequest)
        requestQueue.add(uviStringRequest)
        requestQueue.add(aqiStringRequest)
        requestQueue.add(dailyStringRequest)

        if (errorCount > 0) {
            forecastFragment?.changeProgressBarVisibility(View.GONE)

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
        }
    }

    fun getDewPoint(temp: Double, humidity: Double, units: String): Int {
        val isImperial = units.equals("imperial", ignoreCase = true)

        val tempInCelsius = if (isImperial) (temp - 32.0) * 5.0 / 9.0 else temp

        val a = 17.27
        val b = 237.7

        val alpha = ((a * tempInCelsius) / (b + tempInCelsius)) + log(humidity / 100.0, Math.E)

        val dewPointInCelsius = (b * alpha) / (a - alpha)

        val finalDewPoint =
            if (isImperial) (dewPointInCelsius * 9.0 / 5.0) + 32.0 else dewPointInCelsius

        return finalDewPoint.roundToInt()
    }

    @ColorInt
    fun Context.getColorFromAttr(@AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun loadSavedWeather(shared: SharedPreferences) {
        val forecastFragment =
            supportFragmentManager.findFragmentByTag("f0") as? CurrentForecastFragment
        val lons = shared.getString("lon", "").toString()
        val lats = shared.getString("lat", "").toString()
        val apiKey = shared.getString("apiKey", "").toString()
        val units = if (!shared.getBoolean("use_fahrenheit", false)) "metric" else "imperial"
        getWeather(lons, lats, apiKey, units)
        forecastFragment?.changeUpdateStatus(R.string.tv_update_status_updated_for_saved_location_status)
    }

    private fun loadWeatherBySavedCity(shared: SharedPreferences) {
        val forecastFragment =
            supportFragmentManager.findFragmentByTag("f0") as? CurrentForecastFragment
        val lons = shared.getString("SELECTED_CITY_LON", "").toString()
        val lats = shared.getString("SELECTED_CITY_LAT", "").toString()
        val apiKey = shared.getString("apiKey", "").toString()
        val units = if (!shared.getBoolean("use_fahrenheit", false)) "metric" else "imperial"
        getWeather(lons, lats, apiKey, units)
        forecastFragment?.changeUpdateStatus(R.string.tv_update_status_updated_for_selected_city_status)
    }

    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                102
            )
        }
    }

}

