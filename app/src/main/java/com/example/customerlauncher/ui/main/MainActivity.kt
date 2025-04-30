package com.example.customerlauncher.ui.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.example.customerlauncher.ContentFragment
import com.example.customerlauncher.OttFragment
import com.example.customerlauncher.R
import com.example.customerlauncher.SettingFragment
import com.example.customerlauncher.domain.model.WeatherInfo
import com.example.customerlauncher.domain.model.WeatherTheme
import com.example.customerlauncher.ui.common.WeatherThemeManager.getThemeForWeather
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

class MainActivity : FragmentActivity() {

    private lateinit var ledService: ILedDriverControl
    private val viewModel: HomeViewModel by inject()
    private var lastGroup = -1
    private val themeCodes = listOf(300, 500, 800, 801, 200)
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
                findViewById<TextView>(R.id.content_tv).requestFocus()
            }, 100) // 100ms 정도 딜레이
        }

        connectAidlService()
        initTabFocusAndAnimation()
        startAdcUpdater()
        setUpTestModeBtn()
        setupTabClicks()
        observeWeather()
        startClockUpdate()
        startAdcMonitoring()
    }

    private fun setupTabClicks() {
        findViewById<TextView>(R.id.content_tv).setOnClickListener {
            changeFragment(ContentFragment())
        }
        findViewById<TextView>(R.id.setting_tv).setOnClickListener {
            changeFragment(SettingFragment())
        }
        findViewById<TextView>(R.id.ott_tv).setOnClickListener {
            changeFragment(OttFragment())
        }
    }

    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_HOME -> {
                findViewById<TextView>(R.id.content_tv).apply {
                    requestFocus()
                    performClick()
                }
                return true
            }

            KeyEvent.KEYCODE_SETTINGS -> {
                findViewById<TextView>(R.id.setting_tv).apply {
                    requestFocus()
                    performClick()
                }
                return true
            }
            KeyEvent.KEYCODE_0 -> {
                if (event?.action == KeyEvent.ACTION_DOWN) {
                    val controlRow = findViewById<LinearLayout>(R.id.led_control_row)
                    controlRow.visibility =
                        if (controlRow.isVisible) View.GONE else View.VISIBLE
                    return true
                }
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
                ledService.setDriverType("aw20072")
                Log.d("LED", "AIDL service connected")
            } else {
                Log.e("LED", "AIDL service not available")
            }
        } catch (e: Exception) {
            Log.e("LED", "Failed to connect to AIDL service", e)
        }
    }

    private fun startAdcUpdater() {
        val textView = findViewById<TextView>(R.id.tv_adc_value)
        lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val adc = ledService?.getAdcValue() ?: -1
                    textView.text = "ADC: $adc"

                    if (adc in 0..1024) {
                        Log.d("ADC", "Observed ADC=$adc -> adjustByAdc called")
                    } else {
                        Log.w("ADC", "Invalid ADC value: $adc")
                    }

                } catch (e: Exception) {
                    textView.text = "ADC: ERR"
                    Log.e("ADC", "Error in ADC observer", e)
                }
                delay(1000)
            }
        }
    }

    private fun observeWeather() {
        lifecycleScope.launch {
            viewModel.loadLocationInformation()
            viewModel.weatherInformationStateFlow.collect {
                it?.let { info ->
                    val weatherCardView = findViewById<View>(R.id.weather_card)
                    val theme = getThemeForWeather(info.weatherId)
                    setWeatherTheme(theme)

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
                        findViewById<View>(R.id.root_layout)?.alpha = alpha
                    }
                }

                delay(10000L)  // 30초 간격 (서비스보다 훨씬 여유롭게)
            }
        }
    }

    private fun updateWeatherCard(info: WeatherInfo) {
        val weatherCardView = findViewById<View>(R.id.weather_card)

        weatherCardView.findViewById<TextView>(R.id.temperatureText).text = "${info.temp}°"
        weatherCardView.findViewById<TextView>(R.id.cityText).text = info.city
        weatherCardView.findViewById<TextView>(R.id.humidityText).text = "습도 ${info.humidity}%"

        val dateText = weatherCardView.findViewById<TextView>(R.id.dateText)
        val windText = weatherCardView.findViewById<TextView>(R.id.windText)
        val lottieView = weatherCardView.findViewById<LottieAnimationView>(R.id.weatherIcon)

        lottieView.setAnimation(R.raw.sunny) // TODO: 날씨 코드 기반으로 바꾸기
        lottieView.playAnimation()

        val dateFormat = SimpleDateFormat("yyyy.MM.dd (E)", Locale.KOREAN).apply {
            timeZone = TimeZone.getTimeZone("Asia/Seoul")
        }
        dateText.text = dateFormat.format(Date())

        val windSpeed = info.windSpeed ?: 0.0
        windText.text = "바람: ${String.format("%.1f", windSpeed)} m/s"
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setUpTestModeBtn() {
        // Theme 버튼: 날씨 테마 순환 및 LED 색상 동기화
        findViewById<Button>(R.id.btn_theme).setOnClickListener {
            cycleToNextWeatherTheme()
        }

        // Driver 버튼: 드라이버 선택 다이얼로그 호출
        findViewById<Button>(R.id.btn_driver).setOnClickListener {
            showDriverSelectDialog(this)
        }
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
        val handler = android.os.Handler(mainLooper)
        val updateTimeRunnable = object : Runnable {
            override fun run() {
                val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val currentTime = timeFormat.format(java.util.Date())
                clockTextView.text = currentTime
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateTimeRunnable)
    }

    private fun setWeatherTheme(theme: WeatherTheme) {
        findViewById<View>(R.id.root_layout).background = theme.backgroundGradient

        val weatherCard = findViewById<View>(R.id.weather_card)
        weatherCard.background = theme.cardGradient

        val dashboardFragment = supportFragmentManager.findFragmentById(R.id.dashboard_container)
        if (dashboardFragment is DashboardDataFragment) {
            dashboardFragment.applyTheme(theme)
        }

        val textColor = if (theme.isDarkText) Color.BLACK else Color.WHITE
        applyTextColorToAll(weatherCard, textColor)
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
        val titleView = findViewById<View>(R.id.title_view)
        val content = titleView.findViewById<TextView>(R.id.content_tv)
        val setting = titleView.findViewById<TextView>(R.id.setting_tv)
        val ott = titleView.findViewById<TextView>(R.id.ott_tv)
        val btnDriver = findViewById<Button>(R.id.btn_driver)
        val btnTheme = findViewById<Button>(R.id.btn_theme)
        val tvAdcValue = findViewById<TextView>(R.id.tv_adc_value)
        val weatherCard = findViewById<View>(R.id.weather_card)
        val dashboardContainer = findViewById<View>(R.id.dashboard_container)
        val allFocusableViews = listOf<View>(content, setting, ott, btnDriver,btnTheme, tvAdcValue, weatherCard, dashboardContainer)

        allFocusableViews.forEach { view ->
            view.applyFocusAnimation()
            view.isFocusable = true
            view.isFocusableInTouchMode = true
        }
        // 최초 포커스
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
            }
            .setNegativeButton("취소", null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun cycleToNextWeatherTheme() {
        currentThemeIndex = (currentThemeIndex + 1) % themeCodes.size
        val weatherCode = themeCodes[currentThemeIndex]

        val theme = getThemeForWeather(weatherCode)
        setWeatherTheme(theme)

        // LED도 동기화
        val (r, g, b) = extractDominantColorFromTheme(theme)
        ledService.setLedColor(r,g,b)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun extractDominantColorFromTheme(theme: WeatherTheme): Triple<Int, Int, Int> {
        val colors = (theme.backgroundGradient.colors ?: intArrayOf(Color.WHITE, Color.LTGRAY))
        val avgColor = ColorUtils.blendARGB(colors[0], colors[1], 0.5f)
        val r = Color.red(avgColor)
        val g = Color.green(avgColor)
        val b = Color.blue(avgColor)
        return Triple(r, g, b)
    }
}
