package com.example.customerlauncher.ui.main

import WeatherThemeManager.getThemeForWeather
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.example.customerlauncher.SettingSidePanelFragment
import android.database.Cursor
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.net.Uri

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

    lateinit var ledService: ILedDriverControl
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
                findViewById<View>(R.id.btn_content).requestFocus()
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
        findViewById<View>(R.id.btn_content).setOnClickListener { changeFragment(ContentFragment()) }
        findViewById<View>(R.id.btn_setting).setOnClickListener {  val settingPanel = SettingSidePanelFragment()
            settingPanel.show(supportFragmentManager, "SettingPanel") }
        findViewById<View>(R.id.btn_ott).setOnClickListener { changeFragment(OttFragment()) }
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
                findViewById<ImageView>(R.id.btn_content).apply {
                    requestFocus()
                    performClick()
                }
                return true
            }

            KeyEvent.KEYCODE_SETTINGS -> {
                findViewById<ImageView>(R.id.btn_setting).apply {
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
            KeyEvent.KEYCODE_9 -> {
                logTvChannels()
                logTvInputs()
                logTvPrograms()
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
        lifecycleScope.launch {
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

    private fun logTvPrograms() {
        val projection = arrayOf(
            TvContract.Programs._ID,
            TvContract.Programs.COLUMN_TITLE,
            TvContract.Programs.COLUMN_CHANNEL_ID,
            TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS,
            TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS
        )

        val cursor = contentResolver.query(
            TvContract.Programs.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            Log.d("TV_PROGRAM", "총 ${cursor.count}개의 프로그램:")
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val title = it.getString(1)
                val channelId = it.getLong(2)
                Log.d("TV_PROGRAM", "프로그램 ID: $id, 제목: $title, 채널ID: $channelId")
            }
        } ?: Log.d("TV_PROGRAM", "프로그램 정보를 가져올 수 없습니다 (null cursor)")
    }

    private fun logTvInputs() {
        val tvInputManager = getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
        val inputList = tvInputManager.tvInputList
        Log.d("TV_INPUT", "총 ${inputList.size}개의 TV 입력 존재:")
        inputList.forEach {
            Log.d("TV_INPUT", "ID: ${it.id}, Label: ${it.loadLabel(this)}")
        }
    }
    private fun logTvChannels() {
        val projection = arrayOf(
            TvContract.Channels._ID,
            TvContract.Channels.COLUMN_DISPLAY_NAME,
            TvContract.Channels.COLUMN_INPUT_ID,
            TvContract.Channels.COLUMN_TYPE
        )

        val uri: Uri = TvContract.Channels.CONTENT_URI

        val cursor: Cursor? = contentResolver.query(
            uri,
            projection,
            null,  // selection
            null,  // selectionArgs
            null   // sortOrder
        )

        cursor?.use {
            Log.d("TV_CHANNEL", "총 ${cursor.count}개의 채널을 찾음")
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val name = it.getString(1)
                val inputId = it.getString(2)
                val type = it.getString(3)
                Log.d("TV_CHANNEL", "채널 ID: $id, 이름: $name, 입력ID: $inputId, 타입: $type")
            }
        } ?: Log.d("TV_CHANNEL", "채널 정보를 가져올 수 없습니다 (null cursor)")
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun setWeatherTheme(theme: WeatherTheme) {
        findViewById<View>(R.id.root_layout).background = theme.backgroundGradient
        findViewById<View>(R.id.weather_card).background = theme.cardGradient
        findViewById<View>(R.id.dashboard_container).setBackgroundColor(Color.TRANSPARENT)

        val dashboardFragment = supportFragmentManager.findFragmentById(R.id.dashboard_container)
        if (dashboardFragment is DashboardDataFragment) {
            dashboardFragment.applyTheme(theme)
        }

        val textColor = if (theme.isDarkText) Color.BLACK else Color.WHITE
        applyTextColorToAll(findViewById(R.id.root_layout), textColor)
        applyIconColorToAll(findViewById(R.id.root_layout), textColor)
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

    fun applyIconColorToAll(root: View, color: Int) {
        if (root is ImageView && root.drawable != null) {
            root.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        } else if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                applyIconColorToAll(root.getChildAt(i), color)
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
        val content = findViewById<LinearLayout>(R.id.btn_content)
        val setting = findViewById<LinearLayout>(R.id.btn_setting)
        val ott = findViewById<LinearLayout>(R.id.btn_ott)
        val favorite = findViewById<LinearLayout>(R.id.btn_favorite)
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
