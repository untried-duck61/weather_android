// TODO: This class is not ready yet
//package ru
class WeatherPagerAdapter /* :FragmentPageAdapter */{
 val getItemCount: Int = 2
 fun createFragment(position: Int){
  if (position == 0) return 0 /* CurrentForecastFragment */
  if (position == 1) return 1 /* HourlyForecastFragment */
 }
}