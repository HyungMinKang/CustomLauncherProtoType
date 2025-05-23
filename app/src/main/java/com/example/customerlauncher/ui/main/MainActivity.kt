package com.example.customerlauncher.ui.main

import WeatherThemeManager.getThemeForWeather
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.net.Uri
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.customerlauncher.ContentFragment
import com.example.customerlauncher.R
import com.example.customerlauncher.SettingSidePanelFragment
import com.example.customerlauncher.domain.model.WeatherInfo
import com.example.customerlauncher.domain.model.WeatherTheme
import com.example.customerlauncher.ui.dashboard.DashboardDataFragment
import com.example.customerlauncher.ui.favorite.FavoriteFragment
import com.example.customerlauncher.ui.ott.OttFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.ext.android.inject
import vendor.kaon.hardware.LedDriverControl.ILedDriverControl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : FragmentActivity() {

    lateinit var ledService: ILedDriverControl
    private val viewModel: HomeViewModel by inject()
    private var lastGroup = -1
    private val themeCodes = listOf(200, 300, 500, 600, 700, 800, 801)
    private var currentThemeIndex = 0// 기본값: 맑음
    private var currentTheme: WeatherTheme? = null

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
        startPeriodicWeatherUpdate()
        setupTabClicks()
        observeWeather()
        startClockUpdate()
        //startAdcMonitoring()

    }

    private fun setupTabClicks() {
        findViewById<View>(R.id.btn_content).setOnClickListener { changeFragment(ContentFragment()) }
        findViewById<View>(R.id.btn_setting).setOnClickListener {  val settingPanel = SettingSidePanelFragment()
            settingPanel.show(supportFragmentManager, "SettingPanel") }
        findViewById<View>(R.id.btn_ott).setOnClickListener { changeFragment(OttFragment()) }
        findViewById<View>(R.id.btn_favorite).setOnClickListener { changeFragment(FavoriteFragment()) }
    }

    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            // 로그 출력 등으로 실제 동작 확인
            Log.d("KeyLongPress", "OK 버튼 롱클릭 감지됨")
            return true  // 시스템에 전달되지 않도록 소비

        }
        return super.onKeyLongPress(keyCode, event)

    }



    @RequiresApi(Build.VERSION_CODES.N)
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
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
            KeyEvent.KEYCODE_5 ->{
                val intent = Intent()
                intent.component = ComponentName("com.example.posservice", "com.example.posservice.PoseService")
                ContextCompat.startForegroundService(this, intent)
                Toast.makeText(this, "PoseService 시작됨", Toast.LENGTH_SHORT).show()
                startExternalPoseService()
                Log.i("service", "service call")
            }

            KeyEvent.KEYCODE_6 ->{
                stopExternalPoseService()
                Toast.makeText(this, "PoseService 종료됨", Toast.LENGTH_SHORT).show()
                Log.i("service", "service stop call")
            }
            KeyEvent.KEYCODE_7 ->{
                startAdcMonitoring()
            }
            KeyEvent.KEYCODE_8->{
                lifecycleScope.launch {
                    ledService.setDriverType("aw21036")
                    findViewById<TextView>(R.id.tv_driver_type).text = "드라이버: AW21036"
                    val themeWithLed = getThemeForWeather(themeCodes[currentThemeIndex])
                    delay(300)
                    ledService.setLedColor(
                        themeWithLed.ledColor.first,
                        themeWithLed.ledColor.second,
                        themeWithLed.ledColor.third
                    )
                }

            }
            KeyEvent.KEYCODE_9 -> {
                lifecycleScope.launch {
                    ledService.setDriverType("aw20072")
                    findViewById<TextView>(R.id.tv_driver_type).text = "드라이버: AW20072"
                    val themeWithLed = getThemeForWeather(themeCodes[currentThemeIndex])
                    delay(300)
                    ledService.setLedColor(
                        themeWithLed.ledColor.first,
                        themeWithLed.ledColor.second,
                        themeWithLed.ledColor.third
                    )
                }

            }
            KeyEvent.KEYCODE_0 -> {
                lifecycleScope.launch {
                    ledService.setDriverType("et6296y")
                    findViewById<TextView>(R.id.tv_driver_type).text = "드라이버: ET6296Y"
                    val themeWithLed = getThemeForWeather(themeCodes[currentThemeIndex])
                    delay(300)
                    ledService.setLedColor(
                        themeWithLed.ledColor.first,
                        themeWithLed.ledColor.second,
                        themeWithLed.ledColor.third
                    )
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

                ledService.readAdcSub()
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
        lifecycleScope.launch {
            while (true) {
                try {
                    var adc = withTimeoutOrNull(300) {
                        ledService.getAdcValue()
                    } ?: -1
                    if(adc>=1024){
                        adc = 1024
                    }
                    else if(adc >=-1){
                        adc =ledService.getAdcValue()
                        Log.e("ADC", "Error in ADC observer")
                    }
                    findViewById<TextView>(R.id.tv_adc_value).text = adc.toString()
//                    findViewById<TextView>(R.id.tv_driver_type).text = "드라이버: "+ ledService.getDriverType()
                    if (adc in 0..1024) {
                        Log.d("ADC", "Observed ADC=$adc -> adjustByAdc called")
                    } else {
                        Log.w("ADC", "Invalid ADC value: $adc")
                    }

                } catch (e: Exception) {
                    Log.e("ADC", "Error in ADC observer", e)
                }
                delay(500)
            }
        }
    }

    private fun startPeriodicWeatherUpdate() {
        lifecycleScope.launch {
            while (true) {
                try {
                    Log.d("WeatherUpdate", "날씨 정보 갱신 호출")
                    viewModel.loadLocationInformation()
                } catch (e: Exception) {
                    Log.e("WeatherUpdate", "날씨 정보 갱신 실패", e)
                }
                delay(30 *60* 1000L) // 30분 주기
            }
        }
    }
    fun forceWeatherRefresh() {
        viewModel.loadLocationInformation()  // 위치 → 날씨 재요청
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun observeWeather() {
        lifecycleScope.launch {
            viewModel.weatherInformationStateFlow.collect {
                it?.let { info ->
                    val prefs = getSharedPreferences("setting", Context.MODE_PRIVATE)
                    findViewById<View>(R.id.loading_overlay).visibility = View.GONE

                    val themeCode = info.weatherId
                    val themeWithLed = getThemeForWeather(themeCode)

                    prefs.edit().putInt("selected_theme_code", themeCode).apply()

                    setWeatherTheme(themeWithLed.theme, themeCode)
                    ledService.setLedColor(
                        themeWithLed.ledColor.first,
                        themeWithLed.ledColor.second,
                        themeWithLed.ledColor.third
                    )

                    currentThemeIndex = themeCodes.indexOfFirst { code ->
                        themeCode in resolveWeatherCodeRange(code)
                    }.takeIf { it >= 0 } ?: 0

                    updateWeatherCard(info, themeWithLed.animationResId)
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

                delay(500L)  // 30초 간격 (서비스보다 훨씬 여유롭게)
            }
        }
    }

    private fun updateWeatherCard(info: WeatherInfo, animationResId: Int) {

        if(info.weatherId!=-1) {
            val weatherCard = findViewById<View>(R.id.weather_card)
            weatherCard.findViewById<TextView>(R.id.temperatureText).text = "${info.temp}°"
            weatherCard.findViewById<TextView>(R.id.cityText).text = info.city
            weatherCard.findViewById<TextView>(R.id.humidityText).text = "습도 ${info.humidity}%"
            weatherCard.findViewById<TextView>(R.id.dateText).text = getLocalTimeFormat()
            weatherCard.findViewById<TextView>(R.id.windText).text = "바람: ${String.format("%.1f", info.windSpeed)} m/s"
            weatherCard.findViewById<LottieAnimationView>(R.id.weatherIcon).apply {
                setAnimation(animationResId)
                playAnimation()
            }
        }
        else{
            val weatherCard = findViewById<View>(R.id.weather_card)
            weatherCard.findViewById<TextView>(R.id.temperatureText).text = "-"
            weatherCard.findViewById<TextView>(R.id.cityText).text = "-"
            weatherCard.findViewById<TextView>(R.id.humidityText).text = "-"
            weatherCard.findViewById<TextView>(R.id.dateText).text = "-"
            weatherCard.findViewById<TextView>(R.id.windText).text =
                "바람: - "
            weatherCard.findViewById<LottieAnimationView>(R.id.weatherIcon).apply {
                setAnimation(animationResId)
                playAnimation()
            }
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
    fun startClockUpdate() {
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
    fun setWeatherTheme(theme: WeatherTheme, weatherCode: Int) {
        findViewById<View>(R.id.root_layout).background = theme.backgroundGradient
        findViewById<View>(R.id.weather_card).background = theme.cardGradient
        findViewById<View>(R.id.dashboard_container).setBackgroundColor(Color.TRANSPARENT)
        currentTheme = theme
        weatherCode.let {
            currentThemeIndex = themeCodes.indexOfFirst { code ->
                it in resolveWeatherCodeRange(code)
            }.takeIf { it >= 0 } ?: 0
        }
        val dashboardFragment = supportFragmentManager.findFragmentById(R.id.dashboard_container)
        if (dashboardFragment is DashboardDataFragment) {
            dashboardFragment.applyTheme(theme)
        }

        val textColor = if (theme.isDarkText) Color.BLACK else Color.WHITE
        applyTextColorToAll(findViewById(R.id.root_layout), textColor)
        applyIconColorToAll(findViewById(R.id.root_layout), textColor)
        supportFragmentManager.fragments.forEach {
            if (it is ThemeFragment) it.onThemeChanged(theme)
        }
        val current = WeatherThemeManager.getThemeForWeather(weatherCode)
        Toast.makeText(this, "테마 변경: $weatherCode - ${current.name}", Toast.LENGTH_SHORT).show()
    }

    fun getCurrentTheme(): WeatherTheme? {
        return currentTheme
    }

    fun applyTextColorToAll(view: View, color: Int) {
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
        if (root is RecyclerView) return // 제외
        if (root is ImageView && root.id != R.id.thumbnailImageView && root.drawable != null) {
            root.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        } else if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                applyIconColorToAll(root.getChildAt(i), color)
            }
        }
    }

    private fun initTabFocusAndAnimation() {
        val content = findViewById<LinearLayout>(R.id.btn_content)
        val weatherCard = findViewById<View>(R.id.weather_card)
        val dashboardContainer = findViewById<View>(R.id.dashboard_container)
        val allFocusableViews = listOf<View>(weatherCard, dashboardContainer)
        allFocusableViews.forEach { view ->
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
                val themeWithLed = getThemeForWeather(themeCodes[currentThemeIndex])
                lifecycleScope.launch {
                    ledService.setDriverType(selected)
                    delay(300)  // 💡 init 후 안정화 시간 확보 (300ms~500ms 권장)

                    ledService.setLedColor(
                        themeWithLed.ledColor.first,
                        themeWithLed.ledColor.second,
                        themeWithLed.ledColor.third
                    )

                    findViewById<TextView>(R.id.tv_driver_type).text = selected
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    @SuppressLint("CommitPrefEdits")
    @RequiresApi(Build.VERSION_CODES.N)
    private fun cycleToNextWeatherTheme() {
        val prefs = getSharedPreferences("setting", Context.MODE_PRIVATE)

        currentThemeIndex = (currentThemeIndex + 1) % themeCodes.size
        val weatherCode = themeCodes[currentThemeIndex]
        val themeWithLed = getThemeForWeather(weatherCode)

        // 테마 적용
        setWeatherTheme(themeWithLed.theme, weatherCode)
        updateWeatherCardAnimationOnly(themeWithLed.animationResId)
        ledService.setLedColor(
            themeWithLed.ledColor.first,
            themeWithLed.ledColor.second,
            themeWithLed.ledColor.third
        )

        // 테마 코드 저장 (UI 확인용)
        prefs.edit().putInt("selected_theme_code", weatherCode).apply()


    }

    fun updateWeatherCardAnimationOnly(animationResId: Int) {
        val weatherCard = findViewById<View>(R.id.weather_card)
        weatherCard.findViewById<LottieAnimationView>(R.id.weatherIcon).apply {
            setAnimation(animationResId)
            playAnimation()
        }
    }
    private fun startExternalPoseService() {
        val intent = Intent().apply {
            component = ComponentName(
                "com.example.cameraapplication",                  // 서비스가 들어 있는 앱의 패키지명
                "com.example.posservice.PoseService"       // 서비스 클래스의 전체 경로
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }


    private fun stopExternalPoseService() {
        val intent = Intent().apply {
            component = ComponentName(
                "com.example.cameraapplication",
                "com.example.posservice.PoseService"
            )
        }
        stopService(intent)
    }
    override fun onDestroy() {
        super.onDestroy()
        stopExternalPoseService();
        Log.d("MainActivity", "onDestroy: PoseService 중단됨")
    }
    private val poseGestureReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.N)
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.ACTION_GESTURE") {
                val gestureType = intent.getIntExtra("gesture_type", -1)
                when (gestureType) {
                    1 -> {
                        Log.d("MainActivity", "왼손 감지 → 테마 변경")
                        cycleToNextWeatherTheme()
                    }
                    2 -> {
                        Log.d("MainActivity", "오른손 감지 → 볼륨 낮춤 ") }
                    3 -> {
                        Log.d("MainActivity", "양손 감지 → 볼륨 높임 ")

                    }
                    else -> {
                        Log.w("MainActivity", "알 수 없는 제스처 수신: $gestureType")
                    }
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("com.example.ACTION_GESTURE")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                poseGestureReceiver,
                filter,
                Context.RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(poseGestureReceiver, filter)
        }
    }


    override fun onPause() {
        super.onPause()
        unregisterReceiver(poseGestureReceiver)
    }
}
