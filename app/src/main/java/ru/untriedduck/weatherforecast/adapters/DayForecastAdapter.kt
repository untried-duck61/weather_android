package ru.untriedduck.weatherforecast.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.untriedduck.weatherforecast.databinding.ItemHourlyDayGroupBinding


class DayForecastAdapter(private val units: String) : RecyclerView.Adapter<DayForecastAdapter.DayViewHolder>() {
    private var daysList: List<Map<String, Any>> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        return DayViewHolder(ItemHourlyDayGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        with (holder.binding) {
            val dayData = daysList[position]

            tvCardDayTitle.text = dayData["title"] as? String
            tvTempDay.text = dayData["range"] as? String

            val hourAdapter = HourForecastAdapter(units)
            val hours = dayData["hours"] as? List<Map<String, Any>> ?: emptyList()

            rvHoursBubble.adapter = hourAdapter

            hourAdapter.submitList(hours, units)
        }
    }

    override fun getItemCount(): Int = daysList.size

    fun submitList(newDays: List<Map<String, Any>>){
        this.daysList = newDays
        notifyDataSetChanged()
    }
    class DayViewHolder(val binding: ItemHourlyDayGroupBinding) : RecyclerView.ViewHolder(binding.root)
}