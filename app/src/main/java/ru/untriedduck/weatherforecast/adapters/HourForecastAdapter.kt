package ru.untriedduck.weatherforecast.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.untriedduck.weatherforecast.databinding.ItemHourlyForecastCardBinding

class HourForecastAdapter : RecyclerView.Adapter<HourForecastAdapter.HourViewHolder>() {
    private var hoursList: List<Any> = emptyList()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HourViewHolder {
        return HourViewHolder(ItemHourlyForecastCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: HourViewHolder, position: Int) {
        with (holder.binding) {
            tvHourTime.text = "12:00"
            tvHourTemp.text = "-45°C"
        }
    }

    override fun getItemCount(): Int = hoursList.size

    fun submitList(newHours: List<Any>){
        this.hoursList = newHours
        notifyDataSetChanged()
    }

    class HourViewHolder(val binding: ItemHourlyForecastCardBinding) : RecyclerView.ViewHolder(binding.root)
}