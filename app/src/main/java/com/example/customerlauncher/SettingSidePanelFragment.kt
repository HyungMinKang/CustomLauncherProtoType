package com.example.customerlauncher

import WeatherThemeManager.getThemeForWeather
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SwitchCompat
import androidx.core.graphics.drawable.DrawableCompat.applyTheme
import androidx.fragment.app.DialogFragment
import com.example.customerlauncher.domain.model.WeatherThemeWithLed
import com.example.customerlauncher.ui.main.MainActivity


class SettingSidePanelFragment : DialogFragment() {

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
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        // 🔹 시스템 설정 인텐트 연결
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

        // 🔹 현재 테마 정보 UI
        val themeRow = view.findViewById<View>(R.id.btn_theme_info)
        val themeText = view.findViewById<TextView>(R.id.text_current_theme)
        val themeColor = view.findViewById<View>(R.id.current_theme_color)
        val switch = view.findViewById<Switch>(R.id.switch_auto_theme)
        val isAutoThemeEnabled = prefs.getBoolean("use_weather_theme", true)
        switch.isChecked = isAutoThemeEnabled

// 초기 테마 표시
        val themeCode = prefs.getInt("selected_theme_code", 800)
        val themeWithLed = getThemeForWeather(themeCode)
        themeText.text = "현재 테마: $themeCode (${themeWithLed.name})"
        themeColor.background = themeWithLed.theme.backgroundGradient

        // UI 상태 반영
        themeRow.isEnabled = isAutoThemeEnabled
        themeRow.alpha = if (isAutoThemeEnabled) 1.0f else 0.3f

        // 스위치 동작
        switch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("use_weather_theme", isChecked).apply()

            themeRow.isEnabled = isChecked
            themeRow.alpha = if (isChecked) 1.0f else 0.3f

            if (!isChecked) {
                val fixed = prefs.getInt("selected_theme_code", 800)
                val fixedTheme = getThemeForWeather(fixed)
                applyTheme(fixedTheme)
            } else {
                val current = getThemeForWeather(prefs.getInt("selected_theme_code", 800))
                applyTheme(current)
            }
        }

        view.findViewById<View>(R.id.row_auto_theme_toggle).setOnClickListener {
            val newState = !switch.isChecked
            switch.isChecked = newState // 체크 상태 변경
        }



        // 테마 다이얼로그 열기
        themeRow.setOnClickListener {
            if (switch.isChecked) {
                showThemeSelectDialog(view)
            } else {
                Toast.makeText(context, "테마 고정 상태입니다", Toast.LENGTH_SHORT).show()
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.N)
    private fun showThemeSelectDialog(view: View) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val themeCodes = listOf(200, 300, 500, 600, 700, 800, 801)
        val themeNames = themeCodes.map { "$it - ${getThemeForWeather(it).name}" }

        AlertDialog.Builder(requireContext())
            .setTitle("테마 선택")
            .setItems(themeNames.toTypedArray()) { _, which ->
                val selected = themeCodes[which]
                prefs.edit().putInt("selected_theme_code", selected).apply()

                val selectedTheme = getThemeForWeather(selected)
                view.findViewById<TextView>(R.id.text_current_theme).text =
                    "현재 테마: $selected (${selectedTheme.name})"
                view.findViewById<View>(R.id.current_theme_color)
                    .background = selectedTheme.theme.backgroundGradient

                if (prefs.getBoolean("use_weather_theme", true)) {
                    applyTheme(selectedTheme)
                }
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun applyTheme(themeWithLed: WeatherThemeWithLed) {
        val activity = activity as? MainActivity ?: return
        activity.setWeatherTheme(themeWithLed.theme)
        activity.ledService.setLedColor(
            themeWithLed.ledColor.first,
            themeWithLed.ledColor.second,
            themeWithLed.ledColor.third
        )
    }
}