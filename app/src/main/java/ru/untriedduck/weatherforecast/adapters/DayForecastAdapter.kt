package ru.untriedduck.weatherforecast.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.untriedduck.weatherforecast.databinding.ItemHourlyDayGroupBinding


class DayForecastAdapter : RecyclerView.Adapter<DayForecastAdapter.DayViewHolder>() {
    private var daysList: List<Any> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        return DayViewHolder(ItemHourlyDayGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        with (holder.binding) {
            tvCardDayTitle.text = "June, 1"
            tvTempDay.text = "-196°C/-45°C" // It's not cold at all :)

            val hourAdapter = HourForecastAdapter()

            rvHoursBubble.adapter = hourAdapter

            hourAdapter.submitList(emptyList())
        }
    }

    override fun getItemCount(): Int = daysList.size

    fun submitList(newDays: List<Any>){
        this.daysList = newDays
        notifyDataSetChanged()
    }
    class DayViewHolder(val binding: ItemHourlyDayGroupBinding) : RecyclerView.ViewHolder(binding.root)
}