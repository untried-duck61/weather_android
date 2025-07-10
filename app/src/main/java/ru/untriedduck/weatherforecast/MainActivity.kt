package ru.untriedduck.weatherforecast

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ru.untriedduck.weatherforecast.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
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
        binding.refreshBtn.setOnClickListener {

        }
    }

    private fun getWeather(lon: Int, lat: Int){
        val url =
            "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&APPID=7344293e37ef9018d129240c316158a9&units=metric&lang=ru"
    }
}