package ru.untriedduck.weatherforecast.weather

import android.content.res.Resources
import ru.untriedduck.weatherforecast.R

class WeatherConditionDrawable {
    companion object {
        fun getIconById(weatherId: Int, iconString: String): Int {
            val isNight = iconString.endsWith("n")

            return when (weatherId) {
                // Group 2xx: Thunder
                200 -> R.drawable.ic_weather_200
                201 -> R.drawable.ic_weather_201
                202 -> R.drawable.ic_weather_202
                210 -> R.drawable.ic_weather_210
                211 -> R.drawable.ic_weather_211
                212 -> R.drawable.ic_weather_212
                221 -> R.drawable.ic_weather_221
                230 -> R.drawable.ic_weather_230
                231 -> R.drawable.ic_weather_231
                232 -> R.drawable.ic_weather_232

                // Group 3xx: Drizzle
                300 -> R.drawable.ic_weather_300
                301 -> R.drawable.ic_weather_301
                302 -> R.drawable.ic_weather_302
                310 -> R.drawable.ic_weather_310
                311 -> R.drawable.ic_weather_311
                312 -> R.drawable.ic_weather_312
                313 -> R.drawable.ic_weather_313
                314 -> R.drawable.ic_weather_314
                321 -> R.drawable.ic_weather_321

                // Group 5xx: Rain
                500 -> if (isNight) R.drawable.ic_weather_500n else R.drawable.ic_weather_500d
                501 -> R.drawable.ic_weather_501
                502 -> R.drawable.ic_weather_502
                503 -> R.drawable.ic_weather_503
                504 -> R.drawable.ic_weather_504
                511 -> R.drawable.ic_weather_511
                520 -> if (isNight) R.drawable.ic_weather_520n else R.drawable.ic_weather_520d
                521 -> R.drawable.ic_weather_521
                522 -> R.drawable.ic_weather_522
                531 -> R.drawable.ic_weather_531

                // Group 6xx: Snow
                600 -> if (isNight) R.drawable.ic_weather_600n else R.drawable.ic_weather_600d
                601 -> R.drawable.ic_weather_601
                602 -> R.drawable.ic_weather_602
                611 -> R.drawable.ic_weather_611
                612 -> R.drawable.ic_weather_612
                613 -> R.drawable.ic_weather_613
                615 -> R.drawable.ic_weather_615
                616 -> R.drawable.ic_weather_616
                620 -> if (isNight) R.drawable.ic_weather_620n else R.drawable.ic_weather_620d
                621 -> R.drawable.ic_weather_621
                622 -> R.drawable.ic_weather_622

                // Group 7xx: Atmosphere
                701 -> R.drawable.ic_weather_701
                711 -> R.drawable.ic_weather_711
                721 -> R.drawable.ic_weather_721
                731 -> R.drawable.ic_weather_731
                741 -> R.drawable.ic_weather_741
                751 -> R.drawable.ic_weather_751
                761 -> R.drawable.ic_weather_761
                762 -> R.drawable.ic_weather_762
                771 -> R.drawable.ic_weather_771
                781 -> R.drawable.ic_weather_781

                // Group 8xx: Clouds
                800 -> if (isNight) R.drawable.ic_weather_800n else R.drawable.ic_weather_800d
                801 -> if (isNight) R.drawable.ic_weather_801n else R.drawable.ic_weather_801d
                802 -> if (isNight) R.drawable.ic_weather_802n else R.drawable.ic_weather_802d
                803 -> if (isNight) R.drawable.ic_weather_803n else R.drawable.ic_weather_803d
                804 -> R.drawable.ic_weather_804

                // Запасной вариант
                else -> R.drawable.ic_weather_unknown
            }
        }
    }
}