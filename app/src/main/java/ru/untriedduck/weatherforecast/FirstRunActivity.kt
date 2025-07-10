package ru.untriedduck.weatherforecast

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ru.untriedduck.weatherforecast.databinding.ActivityFirstRunBinding
import ru.untriedduck.weatherforecast.databinding.ActivityMainBinding

public class FirstRunActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFirstRunBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirstRunBinding(layoutInflater)
        enableEdgeToEdge()
        setContentView(R.layout.activity_first_run)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        var shared : SharedPreferences = getSharedPreferences("PREFERENCES",
            Context.MODE_PRIVATE)
        var firstTime : Boolean = shared.getBoolean("firstRun", false)
        if(firstTime){
            val intent : Intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
        } else {
            var editor : SharedPreferences.Editor = shared.edit()
            editor.putBoolean("firstRun",true)
            editor.apply()
        }
        binding.btnFinish.setOnClickListener {

        }

    }
}