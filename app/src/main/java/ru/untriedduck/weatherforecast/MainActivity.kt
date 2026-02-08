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
import android.content.pm.PackageManager
//import android.util.Log
//import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
//import org.json.JSONArray
import org.json.JSONObject
import ru.untriedduck.weatherforecast.databinding.ActivityMainBinding
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {
    private lateinit var locationClient: FusedLocationProviderClient
    private val locationpermissionrequest = 1001
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val shared : SharedPreferences = getSharedPreferences("PREFERENCES",
            MODE_PRIVATE
        )
        val editor : SharedPreferences.Editor = shared.edit()
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If permission is not granted, request it from the user
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationpermissionrequest
            )
            return
        }


        locationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // If location is available, extract latitude and longitude
                val lat = location.latitude
                val lon = location.longitude

                // Save current location for future use
                editor.putString("lat","$lat")
                editor.putString("lon","$lon")
                editor.apply()

                // Get saved location
                val lons = shared.getString("lon","").toString()
                val lats = shared.getString("lat","").toString()
                val apiKey = shared.getString("apiKey","").toString()

                // Call update weather request
                getWeather(lons,lats,apiKey)

                //Set weather update status
                binding.tvUpdateStatus.text =
                    getString(R.string.tv_update_status_updated_for_current_location_status)
            } else {
                // Get last saved location
                val lons = shared.getString("lon","").toString()
                val lats = shared.getString("lat","").toString()
                val apiKey = shared.getString("apiKey","").toString()

                // Call update weather request for last saved location
                getWeather(lons,lats,apiKey)

                // Set weather update status
                binding.tvUpdateStatus.text =
                    getString(R.string.tv_update_status_updated_for_saved_location_status)
            }
        }
        binding.refreshBtn.setOnClickListener {
            // Get saved location
            val lon = shared.getString("lon","").toString()
            val lat = shared.getString("lat","").toString()
            val apiKey = shared.getString("apiKey","").toString()

            // Call update weather request for last saved location
            getWeather(lon,lat,apiKey)

            // Set weather update status
            binding.tvUpdateStatus.text =
                getString(R.string.tv_update_status_updated_for_saved_location_status)
        }


    }

    @SuppressLint("UseCompatLoadingForDrawables", "DiscouragedApi")
    private fun getWeather(lon: String, lat: String, apiKey: String){
        val url =
            getString(R.string.__api_url, lat, lon, apiKey, getString(R.string.lang))
        val queue = Volley.newRequestQueue(this)
        val stringRequest = StringRequest(Request.Method.GET,
            url,
            { response ->
                val root = JSONObject(response)
                    val weather = root.getJSONArray("weather").getJSONObject(0)
                        val desc = weather.getString("description")
                        val icon = weather.getString("icon")
                    val main = root.getJSONObject("main")
                        val temp = main.getString("temp").toFloat().roundToInt().toString()
                        val feelsLike = main.getString("feels_like").toFloat().roundToInt().toString()
                        val tempMin = main.getString("temp_min").toFloat().roundToInt().toString()
                        val tempMax = main.getString("temp_max").toFloat().roundToInt().toString()
                        val humidity = main.getString("humidity").toString()
                    val sys = root.getJSONObject("sys")
                        val country = sys.getString("country")
                    val name = root.getString("name")
                binding.tvTemp.text= getString(R.string.temp, temp)
                binding.tvCountry.text = getString(R.string.tv_country_text, name, country)
                binding.tvDesc.text = getString(R.string.tv_desc_text, desc)
                binding.tvFeelsLike.text = getString(R.string.feels_like_text, feelsLike)
                binding.tvTempMin.text = getString(R.string.temp, tempMin)
                binding.tvTempMax.text = getString(R.string.temp,tempMax)
                binding.imgCondition.background = resources.getDrawable(
                    this.resources.getIdentifier(
                        getString(R.string.__weather_icon_template, icon),
                        getString(R.string.__res_type),
                        this.packageName
                    ), null
                )
                binding.tvHumid.text = getString(R.string.humidity_text, humidity)

                //Log.d("MyLog","$weather")
            },
            {
                /*binding.tvErrors.text = getString(
                    R.string.textview_errors_text,
                    binding.tvErrors.text,
                    getString(R.string.volley_error, it)
                )*/
            }
        )
        queue.add(stringRequest)
    }
}