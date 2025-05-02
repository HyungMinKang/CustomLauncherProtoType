package com.example.customerlauncher.ui.main

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.drawToBitmap
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.lottie.LottieAnimationView
import com.example.customerlauncher.R
import com.example.customerlauncher.ui.custom.CustomTitleView
import com.example.customerlauncher.domain.model.WeatherTheme
import com.example.customerlauncher.ui.common.WeatherThemeManager
import com.example.customerlauncher.ui.dashboard.DashboardDataFragment
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.*


class MainFragment : BrowseSupportFragment() {

    private val mHandler = Handler(Looper.myLooper()!!)
    private val viewModel: HomeViewModel by inject()
    private lateinit var mBackgroundManager: BackgroundManager
    private var mDefaultBackground: Drawable? = null
    private lateinit var mMetrics: DisplayMetrics
    private var mBackgroundTimer: Timer? = null
    private lateinit var clockView: TextView
    private val contentTv by lazy {
        requireActivity().findViewById<TextView>(R.id.content_tv)
    }

    private val settingTv by lazy {
        requireActivity().findViewById<TextView>(R.id.setting_tv)
    }
    private lateinit var handler: Handler


    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        clockView = requireActivity().findViewById<TextView>(R.id.clock)
        handler = Handler(Looper.getMainLooper())
        prepareBackgroundManager()
        setupUIElements()
        loadLocationInfo()
        setFocusClickListenr()
        initFocus()


    }

    private fun initFocus(){
        val content = requireActivity().findViewById<TextView>(R.id.content_tv)
        contentTv.isFocusable = true
        contentTv.isFocusableInTouchMode = true
        contentTv.requestFocus()
    }
    /*
    RCU 버튼 입력시 추가 처리 상항
    content 탭이 focus된 상태가 홈화면이라 인식
    setting 버튼을 누를때 setting 탭에 focus가 가고 클릭된 효과
    단 home key, setting key 모든 시스템적으로 고정된 key라 prebuilt 됬을때 정상적으로 작동되는지 확인 가능
     */
    fun handleKeyDown(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_HOME -> {
                Log.d("KEY_EVENT", "HOME")
                focusToContent()
                true
            }
            KeyEvent.KEYCODE_SETTINGS -> {
                Log.d("KEY_EVENT", "SETTING")
                focusOnSetting()
                true
            }
            else -> false
        }
    }

    /*
    View에 Focus가 갈때 애니메이션 처리
     */
    fun View.applyFocusAnimation() {
        this.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
            }
        }
    }
    private fun focusToContent() {
        val titleView = requireActivity().findViewById<CustomTitleView>(R.id.title_view)
        val content = titleView.findViewById<TextView>(R.id.content_tv)
        Log.d("KEY_EVENT", "HOME")
        content.requestFocus()
        content.performClick()
    }

    private fun focusOnSetting() {
        val titleView = requireActivity().findViewById<CustomTitleView>(R.id.title_view)
        val setting = titleView.findViewById<TextView>(R.id.setting_tv)
        Log.d("KEY_EVENT", "SETTING")
        setting.requestFocus()
        setting.performClick()
    }

    private fun setFocusClickListenr() {
        val titleView = requireActivity().findViewById<CustomTitleView>(R.id.title_view)
        val content = titleView.findViewById<TextView>(R.id.content_tv)
        val setting = titleView.findViewById<TextView>(R.id.setting_tv)
        val ott = titleView.findViewById<TextView>(R.id.ott_tv)
        content.applyFocusAnimation()
        setting.applyFocusAnimation()
        ott.applyFocusAnimation()
    }

    /*
    1. ip 기반 위치 정보 요청
    2. 위치 정보 api response가 오면 viewModel에서 weather api 호출
    3. weather api response 처리 - showWeatherInformation()
     */
    private fun loadLocationInfo() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.loadLocationInformation()
                    showWeatherInformation()
                }
            }
        }
    }

    private suspend fun showWeatherInformation() {
        viewModel.weatherInformationStateFlow.collect {
            it?.let {
                val weatherCardView = requireActivity().findViewById<View>(R.id.weather_card)
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
            }
        }
    }


    /*
   Browse Fragment 초기화 코드
    */
    private fun prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(activity)
        mBackgroundManager.attach(activity!!.window)
        mDefaultBackground = ContextCompat.getDrawable(activity!!, R.drawable.background_gradient)
        mMetrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getMetrics(mMetrics)
    }

    /*
    Browse Fragment 초기화 코드
     */
    private fun setupUIElements() {
        // over title
        headersState = BrowseSupportFragment.HEADERS_DISABLED
        isHeadersTransitionOnBackEnabled = false
        // set fastLane (or headers) background color
        brandColor = ContextCompat.getColor(activity!!, android.R.color.transparent)
        startClockUpdate()

    }



    private fun setWeatherTheme(theme: WeatherTheme){
        // ✅ 배경 적용
        requireActivity().findViewById<View>(R.id.root_layout)?.background = theme.backgroundGradient

        // ✅ 날씨 카드 적용
        val weatherCard = requireActivity().findViewById<View>(R.id.weather_card)
        weatherCard.background = theme.cardGradient

        // ✅ 미니 대시보드도 동일하게
        val dashboardFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.dashboard_container)
        if (dashboardFragment is DashboardDataFragment) {
            dashboardFragment.applyTheme(theme)
        }

        // 텍스트 색상 조정 (선택)
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
    /*
    실시간 시간 업그레이드 기능
     */
    private fun startClockUpdate() {
        val clockTextView = requireActivity().findViewById<TextView>(R.id.clock)
        val handler = Handler(Looper.getMainLooper())
        val updateTimeRunnable = object : Runnable {
            override fun run() {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                timeFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                val currentTime = timeFormat.format(Date())
                clockTextView.text = currentTime
                handler.postDelayed(this, 1000) // 1초마다 갱신
            }
        }
        handler.post(updateTimeRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        mBackgroundTimer?.cancel()
    }

}