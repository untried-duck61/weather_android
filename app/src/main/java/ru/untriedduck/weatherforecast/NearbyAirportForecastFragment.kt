package ru.untriedduck.weatherforecast

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.untriedduck.weatherforecast.databinding.FragmentNearbyAirportForecastBinding

class NearbyAirportForecastFragment : Fragment() {
    private var _binding : FragmentNearbyAirportForecastBinding? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNearbyAirportForecastBinding.inflate(inflater, container, false)
        return binding.root
    }
}