package com.example.customerlauncher.domain.model

import android.graphics.drawable.GradientDrawable

data class WeatherTheme(
    val backgroundGradient: GradientDrawable,
    val cardGradient: GradientDrawable,
    val isDarkText: Boolean
)

data class WeatherThemeWithLed(
    val name: String,
    val theme: WeatherTheme,
    val ledColor: Triple<Int, Int, Int>  // LED에 보낼 RGB 값
)