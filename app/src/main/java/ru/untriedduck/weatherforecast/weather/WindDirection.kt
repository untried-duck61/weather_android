package ru.untriedduck.weatherforecast.weather

import androidx.annotation.StringRes
import ru.untriedduck.weatherforecast.R

enum class WindDirection(@StringRes val resId: Int) {
    N(R.string.wind_name_n),
    NE(R.string.wind_name_ne),
    E(R.string.wind_name_e),
    SE(R.string.wind_name_se),
    S(R.string.wind_name_s),
    SW(R.string.wind_name_sw),
    W(R.string.wind_name_w),
    NW(R.string.wind_name_nw);

    companion object {
        fun fromDegrees(degrees: Number): WindDirection {
            val doubleValue = degrees.toDouble()
            // Нормализуем градусы в диапазон [0, 360)
            val normalized = (doubleValue % 360.0 + 360.0) % 360.0

            // Сдвиг на 22.5 градуса центрирует сектор (каждый сектор теперь по 45 градусов)
            val index = (((normalized + 22.5) / 45.0).toInt()) % 8

            return entries[index]
        }
    }
}