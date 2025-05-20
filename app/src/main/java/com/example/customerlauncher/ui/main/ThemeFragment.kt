package com.example.customerlauncher.ui.main

import com.example.customerlauncher.domain.model.WeatherTheme

interface ThemeFragment {
    fun onThemeChanged(theme: WeatherTheme)
}