package com.example.customerlauncher

import WeatherThemeManager.getThemeForWeather
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.example.customerlauncher.domain.model.WeatherThemeWithLed
import com.example.customerlauncher.ui.main.MainActivity

class SettingSidePanelFragment : DialogFragment() {
    private lateinit var themeText: TextView
    private lateinit var themeColor: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), android.R.style.Theme_DeviceDefault_NoActionBar).apply {
            window?.apply {
                setLayout((resources.displayMetrics.widthPixels * 0.4).toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
                setGravity(Gravity.END)
                setBackgroundDrawableResource(android.R.color.transparent)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_setting_sidepanel, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 시스템 설정 진입 버튼 연결
        mapOf(
            R.id.btn_network to Settings.ACTION_WIFI_SETTINGS,
            R.id.btn_device to Settings.ACTION_DEVICE_INFO_SETTINGS,
            R.id.btn_apps to Settings.ACTION_APPLICATION_SETTINGS,
            R.id.btn_storage to Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            R.id.btn_date to Settings.ACTION_DATE_SETTINGS,
            R.id.btn_language to Settings.ACTION_INPUT_METHOD_SETTINGS,
        ).forEach { (id, intentAction) ->
            view.findViewById<View>(id).setOnClickListener {
                try {
                    startActivity(Intent(intentAction).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                } catch (e: Exception) {
                    Toast.makeText(context, "설정 열기 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 테마 정보 표시
        val themeRow = view.findViewById<View>(R.id.btn_theme_info)
        themeText = view.findViewById(R.id.text_current_theme)
        themeColor = view.findViewById(R.id.current_theme_color)

        // 현재 테마 표시
        val prefs = requireContext().getSharedPreferences("setting", Context.MODE_PRIVATE)
        val themeCode = prefs.getInt("selected_theme_code", 800)
        val themeWithLed = getThemeForWeather(themeCode)
        themeText.text = "현재 테마: $themeCode (${themeWithLed.name})"
        themeColor.background = themeWithLed.theme.backgroundGradient

        // 테마 선택 다이얼로그 열기
        themeRow.setOnClickListener {
            showThemeSelectDialog(view)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun showThemeSelectDialog(view: View) {
        val prefs = requireContext().getSharedPreferences("setting", Context.MODE_PRIVATE)
        val themeCodes = listOf(200, 300, 500, 600, 700, 800, 801)

        val themeNames = themeCodes.map { "$it - ${getThemeForWeather(it).name}" }

        AlertDialog.Builder(requireContext())
            .setTitle("테마 선택")
            .setItems(themeNames.toTypedArray()) { _, which ->
                val selected = themeCodes[which]
                // UI 표시용으로만 저장 (앱 재시작 시 덮어쓰기됨)
                prefs.edit().putInt("selected_theme_code", selected).apply()
                val selectedTheme = getThemeForWeather(selected)

                // UI 반영
                view.findViewById<TextView>(R.id.text_current_theme).text =
                    "현재 테마: $selected (${selectedTheme.name})"
                view.findViewById<View>(R.id.current_theme_color)
                    .background = selectedTheme.theme.backgroundGradient

                Log.d("Selected Theme", "${selectedTheme.name}")
                // 테마 적용
                applyTheme(selectedTheme, selected)
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun applyTheme(themeWithLed: WeatherThemeWithLed, weatherCode:Int) {
        val activity = activity as? MainActivity ?: return
        activity.setWeatherTheme(themeWithLed.theme, weatherCode)
        activity.updateWeatherCardAnimationOnly(themeWithLed.animationResId)
        activity.ledService.setLedColor(
            themeWithLed.ledColor.first,
            themeWithLed.ledColor.second,
            themeWithLed.ledColor.third
        )
    }

    override fun onResume() {
        super.onResume()
        updateThemeInfo()
    }

    fun updateThemeInfo() {
        val prefs = requireContext().getSharedPreferences("setting", Context.MODE_PRIVATE)
        val themeCode = prefs.getInt("selected_theme_code", 800)
        val themeWithLed = getThemeForWeather(themeCode)

        themeText.text = "현재 테마: $themeCode (${themeWithLed.name})"
        themeColor.background = themeWithLed.theme.backgroundGradient
    }
}
