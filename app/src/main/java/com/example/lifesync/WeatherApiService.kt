package com.example.lifesync

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("weather")
    fun getWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String, // 記得填入你的 API Key
        @Query("units") units: String
    ): Call<WeatherResponse>
}