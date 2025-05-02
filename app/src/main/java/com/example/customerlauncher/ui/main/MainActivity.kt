package com.example.customerlauncher.ui.main

import WeatherThemeManager.getThemeForWeather
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.example.customerlauncher.ContentFragment
import com.example.customerlauncher.OttFragment
import com.example.customerlauncher.R
import com.example.customerlauncher.SettingFragment
import com.example.customerlauncher.databinding.ActivityMainBinding
import com.example.customerlauncher.databinding.CustomTitleviewBinding
import com.example.customerlauncher.databinding.DashboardCardBinding
import com.example.customerlauncher.databinding.WeatherCardBinding
import com.example.customerlauncher.domain.model.WeatherInfo
import com.example.customerlauncher.domain.model.WeatherTheme
import com.example.customerlauncher.ui.dashboard.DashboardDataFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import vendor.kaon.hardware.LedDriverControl.ILedDriverControl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.text.format

class MainActivity : FragmentActivity() {

    private lateinit var ledService: ILedDriverControl
    private val viewModel: HomeViewModel by inject()
    private var lastGroup = -1
    private val themeCodes = listOf(200, 300, 500, 600, 700, 800, 801)
    private var currentThemeIndex = 0


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 초기 화면 ContentFragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ContentFragment())
                .commitNowAllowingStateLoss()

            supportFragmentManager.beginTransaction()
                .replace(R.id.dashboard_container, DashboardDataFragment())
                .commitNowAllowingStateLoss()

            Handler(Looper.getMainLooper()).postDelayed({
                findViewById<ImageView>(R.id.iv_content).requestFocus()
            }, 100) // 100ms 정도 딜레이
        }

        connectAidlService()
        initTabFocusAndAnimation()
        startAdcUpdater()
        setupTabClicks()
        observeWeather()
        startClockUpdate()
        startAdcMonitoring()
    }

    private fun setupTabClicks() {
        findViewById<ImageView>(R.id.iv_content).setOnClickListener { changeFragment(ContentFragment()) }
        findViewById<ImageView>(R.id.iv_setting).setOnClickListener { changeFragment(SettingFragment()) }
        findViewById<ImageView>(R.id.iv_ott).setOnClickListener { changeFragment(OttFragment()) }
    }

    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_HOME -> {
                findViewById<ImageView>(R.id.iv_content).apply {
                    requestFocus()
                    performClick()
                }
                return true
            }

            KeyEvent.KEYCODE_SETTINGS -> {
                findViewById<ImageView>(R.id.iv_setting).apply {
                    requestFocus()
                    performClick()
                }
                return true
            }

            KeyEvent.KEYCODE_1 -> {
                showDriverSelectDialog(this)
                return true
            }

            KeyEvent.KEYCODE_2 -> {
                cycleToNextWeatherTheme()
                return true
            }

            KeyEvent.KEYCODE_3 -> {
                ledService.setLedColor(0, 0, 0) // 모든 LED 끄기
                return true
            }

            KeyEvent.KEYCODE_4 -> {
                ledService.setLedColor(255, 255, 255) // 화이트
                return true
            }

        }
        return super.onKeyDown(keyCode, event)
    }

    // AiDL 서비스 연결
    @SuppressLint("PrivateApi")
    private fun connectAidlService() {
        try {
            val smClass = Class.forName("android.os.ServiceManager")
            val getService = smClass.getMethod("getService", String::class.java)
            val rawBinder = getService.invoke(
                null,
                "vendor.kaon.hardware.LedDriverControl.ILedDriverControl/default"
            )

            if (rawBinder != null) {
                val binder = rawBinder as IBinder
                ledService = ILedDriverControl.Stub.asInterface(binder)
                ledService.setDriverType("aw21036")
                Log.d("LED", "AIDL service connected")
            } else {
                Log.e("LED", "AIDL service not available")
            }
        } catch (e: Exception) {
            Log.e("LED", "Failed to connect to AIDL service", e)
        }
    }

    private fun startAdcUpdater() {
        lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val adc = ledService.getAdcValue()
                    findViewById<TextView>(R.id.tv_adc_value).text = adc.toString()
                    if (adc in 0..1024) {
                        Log.d("ADC", "Observed ADC=$adc -> adjustByAdc called")
                    } else {
                        Log.w("ADC", "Invalid ADC value: $adc")
                    }

                } catch (e: Exception) {
                    Log.e("ADC", "Error in ADC observer", e)
                }
                delay(1000)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun observeWeather() {
        lifecycleScope.launch {
            viewModel.loadLocationInformation()
            viewModel.weatherInformationStateFlow.collect {
                it?.let { info ->
                    findViewById<View>(R.id.loading_overlay).visibility = View.GONE
                    val themeWithLed = getThemeForWeather(info.weatherId)
                    setWeatherTheme(themeWithLed.theme)
                    ledService.setLedColor(
                        themeWithLed.ledColor.first,
                        themeWithLed.ledColor.second,
                        themeWithLed.ledColor.third
                    )
                    currentThemeIndex = themeCodes.indexOfFirst { code ->
                        info.weatherId in resolveWeatherCodeRange(code)
                    }.takeIf { it >= 0 } ?: 0  // fallback to 0

                    updateWeatherCard(info)
                }
            }
        }
    }

    fun startAdcMonitoring() {
        lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                val adc = ledService.adcValue
                val group = when (adc) {
                    in 0..200 -> 0
                    in 201..400 -> 1
                    in 401..600 -> 2
                    in 601..800 -> 3
                    else -> 4
                }
                if (group != lastGroup) {
                    lastGroup = group
                    val alpha = listOf(1.0f, 0.8f, 0.6f, 0.5f, 0.4f)[group]
                    withContext(Dispatchers.Main) {
                        Log.d("LedService", "Alpha Changed $alpha")
                        findViewById<View>(R.id.root_layout).alpha = alpha
                    }
                }

                delay(10000L)  // 30초 간격 (서비스보다 훨씬 여유롭게)
            }
        }
    }

    private fun updateWeatherCard(info: WeatherInfo) {

        val weatherCard = findViewById<View>(R.id.weather_card)
        weatherCard.findViewById<TextView>(R.id.temperatureText).text = "${info.temp}°"
        weatherCard.findViewById<TextView>(R.id.cityText).text = info.city
        weatherCard.findViewById<TextView>(R.id.humidityText).text = "습도 ${info.humidity}%"
        weatherCard.findViewById<TextView>(R.id.dateText).text = getLocalTimeFormat()
        weatherCard.findViewById<TextView>(R.id.windText).text =
            "바람: ${String.format("%.1f", info.windSpeed)} m/s"
        weatherCard.findViewById<LottieAnimationView>(R.id.weatherIcon).apply {
            setAnimation(R.raw.sunny)
            playAnimation()
        }

    }

    private fun getLocalTimeFormat(): String {
        val dateFormat = SimpleDateFormat("yyyy.MM.dd (E)", Locale.KOREAN).apply {
            timeZone = TimeZone.getTimeZone("Asia/Seoul")
        }
        return dateFormat.format(Date())
    }

    private fun resolveWeatherCodeRange(code: Int): IntRange = when (code) {
        200 -> 200..299
        300 -> 300..399
        500 -> 500..599
        600 -> 600..699
        700 -> 700..799
        800 -> 800..800
        801 -> 801..804
        else -> 0..1000  // fallback
    }

    private fun startClockUpdate() {
        val clockTextView = findViewById<TextView>(R.id.clock)
        val handler = Handler(mainLooper)
        val updateTimeRunnable = object : Runnable {
            override fun run() {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val currentTime = timeFormat.format(Date())
                clockTextView.text = currentTime
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateTimeRunnable)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setWeatherTheme(theme: WeatherTheme) {
        findViewById<View>(R.id.root_layout).background = theme.backgroundGradient
        findViewById<View>(R.id.weather_card).background = theme.cardGradient
        findViewById<View>(R.id.dashboard_container).setBackgroundColor(Color.TRANSPARENT)

        val dashboardFragment = supportFragmentManager.findFragmentById(R.id.dashboard_container)
        if (dashboardFragment is DashboardDataFragment) {
            dashboardFragment.applyTheme(theme)
        }

        val textColor = if (theme.isDarkText) Color.BLACK else Color.WHITE
        applyTextColorToAll(findViewById(R.id.root_layout), textColor)
        applyIconColor(textColor)
    }


    private fun applyTextColorToAll(view: View, color: Int) {
        when (view) {
            is TextView -> view.setTextColor(color)
            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    applyTextColorToAll(view.getChildAt(i), color)
                }
            }
        }
    }

    private fun applyIconColor(color: Int) {
        findViewById<ImageView>(R.id.iv_content).setColorFilter(color, PorterDuff.Mode.SRC_IN)
        findViewById<ImageView>(R.id.iv_setting).setColorFilter(color, PorterDuff.Mode.SRC_IN)
        findViewById<ImageView>(R.id.iv_ott).setColorFilter(color, PorterDuff.Mode.SRC_IN)
        findViewById<ImageView>(R.id.iv_fav).setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    private fun View.applyFocusAnimation() {
        this.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
            }
        }
    }

    private fun initTabFocusAndAnimation() {
        val content = findViewById<ImageView>(R.id.iv_content)
        val setting = findViewById<ImageView>(R.id.iv_setting)
        val ott = findViewById<ImageView>(R.id.iv_ott)
        val favorite = findViewById<ImageView>(R.id.iv_fav)
        val weatherCard = findViewById<View>(R.id.weather_card)
        val dashboardContainer = findViewById<View>(R.id.dashboard_container)

        val allFocusableViews = listOf<View>(content, setting, ott, favorite, weatherCard, dashboardContainer)

        allFocusableViews.forEach { view ->
            view.applyFocusAnimation()
            view.isFocusable = true
            view.isFocusableInTouchMode = true
        }

        content.requestFocus()
    }

    fun showDriverSelectDialog(context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_driver_select, null)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinner_driver)

        val drivers = listOf("et6296y", "aw20072", "aw21036")
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, drivers)
        spinner.adapter = adapter

        AlertDialog.Builder(context)
            .setTitle("LED 드라이버 선택")
            .setView(dialogView)
            .setPositiveButton("확인") { _, _ ->
                val selected = spinner.selectedItem.toString()
                ledService.setDriverType(selected)
                findViewById<TextView>(R.id.tv_driver_type).text = selected
            }
            .setNegativeButton("취소", null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun cycleToNextWeatherTheme() {
        currentThemeIndex = (currentThemeIndex + 1) % themeCodes.size
        val weatherCode = themeCodes[currentThemeIndex]
        val themeWithLed = getThemeForWeather(weatherCode)
        setWeatherTheme(themeWithLed.theme)
        ledService.setLedColor(
            themeWithLed.ledColor.first,
            themeWithLed.ledColor.second,
            themeWithLed.ledColor.third
        )
    }

}
