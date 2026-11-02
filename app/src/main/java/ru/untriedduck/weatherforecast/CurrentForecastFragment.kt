package ru.untriedduck.weatherforecast

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import ru.untriedduck.weatherforecast.databinding.FragmentCurrentForecastBinding
import ru.untriedduck.weatherforecast.weather.WeatherConditionDrawable
import ru.untriedduck.weatherforecast.weather.WindDirection

class CurrentForecastFragment : Fragment() {
    private var _binding: FragmentCurrentForecastBinding? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCurrentForecastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // HUGE FUNCTION XD
    // I don't know how to make it shorter, but I think that variant fits better
    fun displayWeather(
        temp: String,
        units: String,
        desc: String,
        feelsLike: String,
        tempMin: String,
        tempMax: String,
        humidity: String,
        dewPoint: Int,
        totalPressure: Float,
        seaPressure: Float,
        grndPressure: Float,
        windDegree: Float,
        windSpeed: String,
        windGust: String,
        sunrise: Long,
        sunset: Long,
        weatherId: Int,
        icon: String
    ) {
        binding.tvTemp.text =
            getString(R.string.temp, temp, if (units == "imperial") "F" else "C")

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
        binding.tvDewPoint.text = getString(
            R.string.dew_point_format,
            dewPoint.toString(),
            if (units == "imperial") "F" else "C"
        )

        binding.barometerTotal.currentPressure = totalPressure
        binding.barometerSea.currentPressure = seaPressure
        binding.barometerGround.currentPressure = grndPressure

        binding.ivWindDirectionArrow.rotation = (windDegree + 180) % 360
        binding.tvWindDirectionName.text =
            getString(WindDirection.fromDegrees(windDegree).resId)
        binding.tvWindSpeed.text = getString(
            R.string.wind_card_wind_speed_format,
            windSpeed,
            if (units == "imperial") getString(R.string.wind_speed_units_miles_h) else getString(R.string.wind_speed_units_meters_sec)
        )
        binding.tvWindGust.text = getString(
            R.string.wind_card_wind_gust_format,
            windGust,
            if (units == "imperial") getString(R.string.wind_speed_units_miles_h) else getString(R.string.wind_speed_units_meters_sec)
        )

        binding.sunDayChart.setData(sunrise, sunset, System.currentTimeMillis() / 1000)

        val iconResId = WeatherConditionDrawable.getIconById(weatherId, icon)
        if (iconResId != 0) {
            binding.imgCondition.setImageDrawable(
                ResourcesCompat.getDrawable(
                    requireContext().resources,
                    iconResId,
                    null
                )
            )
        }

    }

    fun displayUvi(uviValue: Int) {
        binding.tvUviNumber.text = uviValue.toString()
        binding.tvUviDesc.text = getUviDesc(uviValue)
        binding.tvUviAdvice.text = getUviAdvice(uviValue)
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

    fun displayAqi(
        aqiVal: Int,
        aqiCO: String,
        aqiNO: String,
        aqiNO2: String,
        aqiO3: String,
        aqiSO2: String,
        aqiPMf2t5: String,
        aqiPM10: String,
        aqiNH3: String
    ) {
        binding.tvAqiNumber.text = aqiVal.toString()
        binding.tvAqiDesc.text = getAqiDesc(aqiVal)
        binding.pbAqiVal.progress = aqiVal

        val (progressRes, trackRes) = getAqiColorRes(aqiVal)
        val progressColor = ContextCompat.getColor(requireContext(), progressRes)
        val trackColor = ContextCompat.getColor(requireContext(), trackRes)
        binding.pbAqiVal.setIndicatorColor(progressColor)
        binding.pbAqiVal.trackColor = trackColor

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

    fun changeProgressBarVisibility(visibility: Int) {
        with(binding) {
            progressBar.visibility = visibility
        }
    }

    fun changeUpdateStatus(status: Int) {
        binding.tvUpdateStatus.text =
            getString(status)
    }
}