package com.example.customerlauncher.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customerlauncher.domain.model.IpInfo
import com.example.customerlauncher.domain.model.WeatherInfo
import com.example.customerlauncher.domain.repostority.IpinfoRepository
import com.example.customerlauncher.domain.repostority.WeatherRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(private val weatherRepository: WeatherRepository, private val ipinfoRepository: IpinfoRepository) : ViewModel(){

    private val _locationInformationStateFlow = MutableStateFlow<IpInfo?>(null)
    val locationInformationStateFlow= _locationInformationStateFlow.asStateFlow()

    private val _weatherInformationStateFlow = MutableStateFlow<WeatherInfo?>(null)
    val weatherInformationStateFlow= _weatherInformationStateFlow.asStateFlow()

    init {
        observeLocationInformation()
    }

    fun loadLocationInformation(){
        viewModelScope.launch {
            try {
                val ipInfo = ipinfoRepository.getIpInfo()
                _locationInformationStateFlow.emit(ipInfo)
            } catch (e: Exception) {
                // fallback: 서울 좌표
                val fallback = IpInfo(
                    city = "Seoul",
                    ip = "192.168.1.170",
                    loc = "37.5665,126.9780"
                )
                _locationInformationStateFlow.emit(fallback)
            }
        }
    }

    fun loadWeatherInformation(latitude: Double, longitude:Double){
        viewModelScope.launch {
            try {
                val weather = weatherRepository.getWeatherInfo(latitude, longitude)
                _weatherInformationStateFlow.emit(weather)
            } catch (e: Exception) {
                val fallback = WeatherInfo.default()
                _weatherInformationStateFlow.emit(fallback)
            }
        }
    }



    private fun observeLocationInformation() {
        locationInformationStateFlow
            .filterNotNull()
            .onEach { ipInfo ->
                val loc = ipInfo.loc.split(",")
                val latitude = loc[0].toDoubleOrNull() ?: 37.5665
                val longitude = loc[1].toDoubleOrNull() ?: 126.9780
                loadWeatherInformation(latitude, longitude)
            }
            .launchIn(viewModelScope)
    }
}