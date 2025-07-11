package ru.untriedduck.weatherforecast

import android.content.Context
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
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import ru.untriedduck.weatherforecast.databinding.ActivityMainBinding
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {
    private lateinit var locationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST = 1001
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
        var shared : SharedPreferences = getSharedPreferences("PREFERENCES",
            Context.MODE_PRIVATE)
        var editor : SharedPreferences.Editor = shared.edit();
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
                LOCATION_PERMISSION_REQUEST
            )
            return
        }


        locationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // If location is available, extract latitude and longitude
                val lat = location.latitude
                val lon = location.longitude

                // Display location in the TextView
                //locationText.text = "Latitude: $lat\nLongitude: $lon"
                editor.putString("lat","$lat")
                editor.putString("lon","$lon")
                editor.apply()
            } else {
                Toast.makeText(this, getString(R.string.location_null_error), Toast.LENGTH_LONG).show()
            }
        }
        binding.refreshBtn.setOnClickListener {
            val lon = shared.getString("lon","").toString()
            val lat = shared.getString("lat","").toString()
            val apiKey = shared.getString("apiKey","").toString()
            //binding.tvTemp.text = "$lon, $lat"
            getWeather(lon,lat,apiKey)
        }

        val lon = shared.getString("lon","").toString()
        val lat = shared.getString("lat","").toString()
        val apiKey = shared.getString("apiKey","").toString()
        //binding.tvTemp.text = "$lon, $lat"
        getWeather(lon,lat,apiKey)
    }

    private fun getWeather(lon: String, lat: String, apiKey: String){
        val url =
            "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&APPID=$apiKey&units=metric&lang=${R.string.lang}"
        val queue = Volley.newRequestQueue(this)
        val stringRequest = StringRequest(Request.Method.GET,
            url,
            { response ->
                val root = JSONObject(response)
                    val weather = JSONArray(root.getJSONArray("weather")).getJSONObject(0)
                        val desc = weather.getString("description")
                    val main = root.getJSONObject("main")
                        val temp = main.getString("temp").toFloat().roundToInt().toString()
                        val feels_like = main.getString("feels_like").toFloat().roundToInt().toString()
                    val sys = root.getJSONObject("sys")
                        val country = sys.getString("country")
                    val name = root.getString("name")
                binding.tvTemp.text= getString(R.string.temp, temp)
                binding.tvCountry.text = getString(R.string.tv_country_text, name, country)
                binding.tvDesc.text = getString(R.string.tv_desc_text, desc)
                //Log.d("MyLog","$temp")
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