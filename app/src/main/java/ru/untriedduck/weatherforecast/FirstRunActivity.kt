package ru.untriedduck.weatherforecast

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ru.untriedduck.weatherforecast.databinding.ActivityFirstRunBinding
//import ru.untriedduck.weatherforecast.databinding.ActivityMainBinding

@Suppress("DEPRECATION")
public class FirstRunActivity : AppCompatActivity() {
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
        var shared : SharedPreferences = getSharedPreferences("PREFERENCES",
            Context.MODE_PRIVATE)
        var firstTime : Boolean = shared.getBoolean("firstRun", false)
        if(!firstTime && !shared.getString("apiKey","").isNullOrEmpty()){
            val intent : Intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
        } else {
            var editor : SharedPreferences.Editor = shared.edit()
            editor.putBoolean("firstRun",true)
            editor.apply()
        }
        binding.btnFinish.setOnClickListener {
            var editor : SharedPreferences.Editor = shared.edit()
            if(binding.tfApiKey.text.isNullOrEmpty()){
                Toast.makeText(this,R.string.empty_api_key_error_text,Toast.LENGTH_SHORT).show()
            } else {
                editor.putString("apiKey",binding.tfApiKey.text.toString())
                editor.putBoolean("firstRun",false)
                editor.apply()
                val intent : Intent = Intent(this,MainActivity::class.java)
                startActivity(intent)
            }
        }
        /*binding.welcomeTextStepOne.append(Html.fromHtml(R.string.welcome_text_step_one.toString(),Html.FROM_HTML_MODE_COMPACT))
        binding.welcomeTextStepOne.movementMethod = LinkMovementMethod.getInstance();*/
        //binding.welcomeTextStepOne.text=Html.fromHtml(R.string.welcome_text_step_one.toString(),Html.FROM_HTML_MODE_COMPACT)

    }
}