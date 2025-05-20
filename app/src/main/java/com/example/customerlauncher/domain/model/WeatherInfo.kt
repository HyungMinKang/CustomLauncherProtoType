package com.example.customerlauncher.domain.model

data class WeatherInfo(
    val temp : String,
    val city: String,
    val humidity: String,
    val weather: String,
    val weatherIcon: String,
    val weatherId: Int,
    val windSpeed: Double,
) {
    companion object {
        fun default(): WeatherInfo = WeatherInfo(
            temp = "-",
            city = "UNKNOWN",
            humidity = "-",
            weather = "기본 테마",
            weatherIcon = "ic_default",
            weatherId = -1,
            windSpeed = 0.0
        )
    }
}
