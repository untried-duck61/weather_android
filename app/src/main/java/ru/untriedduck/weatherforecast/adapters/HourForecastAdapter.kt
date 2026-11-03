package ru.untriedduck.weatherforecast.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import ru.untriedduck.weatherforecast.databinding.ItemHourlyForecastCardBinding
import ru.untriedduck.weatherforecast.weather.WeatherConditionDrawable
import java.util.Locale

class HourForecastAdapter(private val units: String) : RecyclerView.Adapter<HourForecastAdapter.HourViewHolder>() {
    private var hoursList: List<Map<String, Any>> = emptyList()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HourViewHolder {
        return HourViewHolder(ItemHourlyForecastCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: HourViewHolder, position: Int) {
        with (holder.binding) {
            val hourData = hoursList[position]
            tvHourTime.text = hourData["time"] as? String

            val temp = (hourData["temp"] as? Number)?.toDouble() ?: 0.0
            tvHourTemp.text = String.format(Locale.getDefault(), "%+.0f°${if (units == "imperial") "F" else "C"}", temp)

            val weatherId = (hourData["id"] as? Number)?.toInt() ?: 800
            val iconName = hourData["icon"] as? String ?: "01d"

            val iconResId = WeatherConditionDrawable.getIconById(weatherId, iconName)
            if (iconResId != 0) {
                ivHourIcon.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        ivHourIcon.context.resources,
                        iconResId,
                        null
                    )
                )
            }
        }
    }

    override fun getItemCount(): Int = hoursList.size

    fun submitList(newHours: List<Map<String, Any>>, units: String){
        this.hoursList = newHours
        notifyDataSetChanged()
    }

    class HourViewHolder(val binding: ItemHourlyForecastCardBinding) : RecyclerView.ViewHolder(binding.root)
}