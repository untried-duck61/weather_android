package ru.untriedduck.weatherforecast

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.untriedduck.weatherforecast.adapters.DayForecastAdapter
import ru.untriedduck.weatherforecast.databinding.FragmentHourlyForecastBinding

class HourlyForecastFragment : Fragment() {
    private var _binding : FragmentHourlyForecastBinding? = null
    val binding get() = _binding!!

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

        val dayAdapter = DayForecastAdapter()

        binding.root.adapter = dayAdapter
        dayAdapter.submitList(listOf(1, 2, 3, 4, 5))
    }
}