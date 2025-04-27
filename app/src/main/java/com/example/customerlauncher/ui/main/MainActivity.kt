package com.example.customerlauncher.ui.main

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.InputFilter
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.lottie.LottieAnimationView
import com.example.customerlauncher.R
import com.example.customerlauncher.domain.model.WeatherTheme
import com.example.customerlauncher.ui.common.WeatherThemeManager
import com.example.customerlauncher.ui.custom.CustomTitleView
import com.example.customerlauncher.ui.dashboard.DashboardDataFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import vendor.kaon.hardware.LedDriverControl.ILedDriverControl
import java.text.SimpleDateFormat
import java.util.*
import android.text.InputType
import android.text.Spanned
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.customerlauncher.ContentFragment
import com.example.customerlauncher.OttFragment
import com.example.customerlauncher.SettingFragment
import kotlinx.coroutines.delay

class MainActivity : FragmentActivity() {

    private lateinit var ledService: ILedDriverControl
    private val viewModel: HomeViewModel by inject()
    private var r = 0
    private var g = 0
    private var b = 0

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
        setupLedButtons()
        setupTabClicks()
        observeWeather()
        startClockUpdate()
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
            KeyEvent.KEYCODE_1 -> { r=255; g=0; b=0; applyLedColor(); return true }
            KeyEvent.KEYCODE_2 -> { r=0; g=255; b=0; applyLedColor(); return true }
            KeyEvent.KEYCODE_3 -> { r=0; g=0; b=255; applyLedColor(); return true }
            KeyEvent.KEYCODE_4 -> { r=255; g=255; b=255; applyLedColor(); return true }
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
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun applyLedColor() {
        //ledService?.setColor(r, g, b)
        findViewById<View>(R.id.root_layout).setBackgroundColor(Color.rgb(r, g, b))
    }

    @SuppressLint("PrivateApi")
    private fun connectAidlService() {
        try {
            val smClass = Class.forName("android.os.ServiceManager")
            val getService = smClass.getMethod("getService", String::class.java)
            val rawBinder = getService.invoke(null, "vendor.kaon.hardware.LedDriverControl.ILedDriverControl/default")

            if (rawBinder != null) {
                val binder = rawBinder as IBinder
                ledService = ILedDriverControl.Stub.asInterface(binder)
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
        lifecycleScope.launch {
            while (true) {
                try {
                    val adc = ledService?.getAdcValue() ?: -1
                    textView.text = "ADC: $adc"
                } catch (_: Exception) {
                    textView.text = "ADC: ERR"
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
                    val theme = WeatherThemeManager.getThemeForWeather(800)
                    setWeatherTheme(theme)
                    weatherCardView.findViewById<TextView>(R.id.temperatureText).text = "${it.temp}°"
                    weatherCardView.findViewById<TextView>(R.id.cityText).text = it.city
                    weatherCardView.findViewById<TextView>(R.id.humidityText).text = "습도 ${it.humidity}%"
                    val cardContent = weatherCardView.findViewById<View>(R.id.cardContent)
                    val dateText = weatherCardView.findViewById<TextView>(R.id.dateText)
                    val windText = weatherCardView.findViewById<TextView>(R.id.windText)
                    val lottieView = weatherCardView.findViewById<LottieAnimationView>(R.id.weatherIcon)
                    lottieView.setAnimation(R.raw.sunny)
                    lottieView.playAnimation()
                    val dateFormat = SimpleDateFormat("yyyy.MM.dd (E)", Locale.KOREAN)
                    dateFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                    val currentDate = dateFormat.format(Date())
                    Log.d("Weather", "$currentDate")
                    dateText.text = currentDate

// 바람 정보 출력
                    val windSpeed = it.windSpeed ?: 0.0 // ex: 4.63
                    windText.text = "바람: ${String.format("%.1f", windSpeed)} m/s"
                    // 기타 UI 반영
                }
            }
        }
    }

    private fun setupLedButtons() {
        findViewById<Button>(R.id.btn_red).setOnClickListener { r=255; g=0; b=0; applyLedColor() }
        findViewById<Button>(R.id.btn_green).setOnClickListener { r=0; g=255; b=0; applyLedColor() }
        findViewById<Button>(R.id.btn_blue).setOnClickListener { r=0; g=0; b=255; applyLedColor() }
        findViewById<Button>(R.id.btn_white).setOnClickListener { r=255; g=255; b=255; applyLedColor() }
        findViewById<Button>(R.id.btn_brightness).setOnClickListener {
            showBrightnessDialog()
        }
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

        val btnRed = findViewById<Button>(R.id.btn_red)
        val btnGreen = findViewById<Button>(R.id.btn_green)
        val btnBlue = findViewById<Button>(R.id.btn_blue)
        val btnBrightness = findViewById<Button>(R.id.btn_brightness)
        val btnWhite = findViewById<Button>(R.id.btn_white)

        val tvAdcValue = findViewById<TextView>(R.id.tv_adc_value)

        val weatherCard = findViewById<View>(R.id.weather_card)
        val dashboardContainer = findViewById<View>(R.id.dashboard_container)

        val allFocusableViews = listOf<View>(
            content, setting, ott,
            btnRed, btnGreen, btnBlue, btnBrightness, btnWhite,
            tvAdcValue,
            weatherCard, dashboardContainer
        )

        allFocusableViews.forEach { view ->
            view.applyFocusAnimation()
            view.isFocusable = true
            view.isFocusableInTouchMode = true
        }

        // 최초 포커스
        content.requestFocus()
    }
    private fun showBrightnessDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_brightness_control, null)
        val inputR = dialogView.findViewById<EditText>(R.id.value_r)
        val inputG = dialogView.findViewById<EditText>(R.id.value_g)
        val inputB = dialogView.findViewById<EditText>(R.id.value_b)

        fun setupEdit(edit: EditText, value: Int) {
            edit.setText(value.toString())
            edit.inputType = InputType.TYPE_CLASS_NUMBER
            edit.filters = arrayOf(object : InputFilter {
                override fun filter(source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int): CharSequence? {
                    val result = (dest?.substring(0, dstart) ?: "") + source?.substring(start, end) + (dest?.substring(dend) ?: "")
                    return try {
                        val number = result.toInt()
                        if (number in 0..255) null else ""
                    } catch (e: Exception) {
                        ""
                    }
                }
            })
        }

        setupEdit(inputR, r)
        setupEdit(inputG, g)
        setupEdit(inputB, b)

        dialogView.findViewById<Button>(R.id.btn_r_up).setOnClickListener {
            val value = inputR.text.toString().toIntOrNull() ?: 0
            inputR.setText((value + 5).coerceAtMost(255).toString())
        }
        dialogView.findViewById<Button>(R.id.btn_r_down).setOnClickListener {
            val value = inputR.text.toString().toIntOrNull() ?: 0
            inputR.setText((value - 5).coerceAtLeast(0).toString())
        }

        dialogView.findViewById<Button>(R.id.btn_g_up).setOnClickListener {
            val value = inputG.text.toString().toIntOrNull() ?: 0
            inputG.setText((value + 5).coerceAtMost(255).toString())
        }
        dialogView.findViewById<Button>(R.id.btn_g_down).setOnClickListener {
            val value = inputG.text.toString().toIntOrNull() ?: 0
            inputG.setText((value - 5).coerceAtLeast(0).toString())
        }

        dialogView.findViewById<Button>(R.id.btn_b_up).setOnClickListener {
            val value = inputB.text.toString().toIntOrNull() ?: 0
            inputB.setText((value + 5).coerceAtMost(255).toString())
        }
        dialogView.findViewById<Button>(R.id.btn_b_down).setOnClickListener {
            val value = inputB.text.toString().toIntOrNull() ?: 0
            inputB.setText((value - 5).coerceAtLeast(0).toString())
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .setPositiveButton("적용") { _, _ ->
                r = inputR.text.toString().toIntOrNull()?.coerceIn(0, 255) ?: 0
                g = inputG.text.toString().toIntOrNull()?.coerceIn(0, 255) ?: 0
                b = inputB.text.toString().toIntOrNull()?.coerceIn(0, 255) ?: 0
                applyLedColor()
            }
            .setNegativeButton("취소", null)
            .create()

        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                dialog.dismiss()
                true
            } else false
        }

        dialog.show()
    }
}
