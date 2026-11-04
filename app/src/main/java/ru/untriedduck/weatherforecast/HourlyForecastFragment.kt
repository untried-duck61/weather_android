package ru.untriedduck.weatherforecast

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.untriedduck.weatherforecast.adapters.DayForecastAdapter
import ru.untriedduck.weatherforecast.databinding.FragmentHourlyForecastBinding
import kotlin.math.roundToInt

class HourlyForecastFragment : Fragment() {
    private var _binding: FragmentHourlyForecastBinding? = null
    val binding get() = _binding!!

    private lateinit var dayAdapter: DayForecastAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHourlyForecastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dayAdapter = DayForecastAdapter("metric")

        binding.root.adapter = dayAdapter

        val mainActivity = requireActivity() as? MainActivity
        val sharedData = mainActivity?.savedGroupedForecast
        val currentUnits = mainActivity?.units

        if (sharedData != null && currentUnits != null){
            updateHourlyData(sharedData, currentUnits)
        }
    }

    fun updateHourlyData(
        groupedForecast: LinkedHashMap<String, Pair<String, ArrayList<Map<String, Any>>>>,
        units: String
    ) {

        dayAdapter = DayForecastAdapter(units)
        binding.root.adapter = dayAdapter

        val formattedDays = ArrayList<Map<String, Any>>()

        for ((_, value) in groupedForecast) {
            val dateTitle = value.first
            val hoursList = value.second

            var minTemp = Double.MAX_VALUE
            var maxTemp = -Double.MAX_VALUE

            for (hour in hoursList) {
                val tempNum = hour["temp"] as? Number
                val temp = tempNum?.toDouble() ?: 0.0

                if (temp < minTemp) minTemp = temp
                if (temp > maxTemp) maxTemp = temp
            }

            val rangeString = getString(
                R.string.daily_temp_range_format,
                minTemp.roundToInt().toString(),
                if (units == "imperial") "F" else "C",
                maxTemp.roundToInt().toString()
            )

            val dayMap = mapOf(
                "title" to dateTitle,
                "range" to rangeString,
                "hours" to hoursList
            )

            formattedDays.add(dayMap)
        }

        dayAdapter.submitList(formattedDays)
    }
}