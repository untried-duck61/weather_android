package ru.untriedduck.weatherforecast.weather

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface HelperApiMethods {
    @GET("geo/1.0/direct")
    suspend fun getCitiesByQuery(
        @Query("q") cityName: String,
        @Query("limit") limit: Int = 5,
        @Query("appid") apiKey: String
    ): ResponseBody
}