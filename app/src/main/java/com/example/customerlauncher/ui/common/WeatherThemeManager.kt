import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import com.example.customerlauncher.R
import com.example.customerlauncher.domain.model.WeatherTheme
import com.example.customerlauncher.domain.model.WeatherThemeWithLed

object WeatherThemeManager {
    fun getThemeForWeather(code: Int): WeatherThemeWithLed {
        val ledColor: Triple<Int, Int, Int>
        val bgStart: String
        val bgEnd: String
        val cardStart: String
        val cardEnd: String
        val darkText: Boolean
        val name: String
        val animationResId: Int
        when (code) {
            in 200..299 -> {
                // 뇌우 - 보라 계열
                name = "뇌우"
                ledColor = Triple(156, 39, 176)
                bgStart = "#512DA8"; bgEnd = "#000000"
                cardStart = "#D1C4E9"; cardEnd = "#B39DDB"
                darkText = false
                animationResId = R.raw.thunderstorm
            }
            in 300..399 -> {
                // 잔비 - 하늘 계열
                name = "잔비"
                ledColor = Triple(3, 169, 244)
                bgStart = "#90CAF9"; bgEnd = "#ECEFF1"
                cardStart = "#FFFFFF"; cardEnd = "#E3F2FD"
                darkText = true
                animationResId = R.raw.drizzle
            }
            in 500..599 -> {
                // 비 - 남보라 계열로 구분 강화
                name = "비"
                ledColor = Triple(103, 58, 183) // #673AB7 (딥 퍼플)
                bgStart = "#303F9F"; bgEnd = "#1A237E"  // 딥 인디고 → 남색
                cardStart = "#C5CAE9"; cardEnd = "#9FA8DA"
                darkText = false
                animationResId = R.raw.rain
            }
            in 600..699 -> {
                // 눈 - 핑크 계열
                name = "눈"
                ledColor = Triple(233, 30, 99)
                bgStart = "#F8BBD0"; bgEnd = "#EC407A" // 흰색 제거
                cardStart = "#F48FB1"; cardEnd = "#F06292"
                darkText = true
                animationResId = R.raw.snow
            }
            in 700..799 -> {
                // 안개/먼지 - 빨강과 노랑 혼합 (기존 초록에서 변경)
                name = "먼지"
                ledColor = Triple(76, 175, 80) // 그린 (#4CAF50)
                bgStart = "#AED581"; bgEnd = "#558B2F" // 연두~짙은 초록
                cardStart = "#DCEDC8"; cardEnd = "#C5E1A5"
                darkText = true
                animationResId = R.raw.foggy
            }
            800 -> {
                // 맑음 - 주황 + 연주황
                name = "맑음"
                ledColor = Triple(255, 152, 0)
                bgStart = "#FF9800"; bgEnd = "#FFF59D"
                cardStart = "#FFB74D"; cardEnd = "#FFFDE7"
                darkText = true
                animationResId = R.raw.sunny
            }
            in 801..804 -> {
                // 구름 많음 → 붉은 계열 테마로 전환 (LED + 배경 동기화)
                name = "구름 많음"
                ledColor = Triple(253, 10, 10) //
                bgStart = "#EF5350"            // 연한 레드
                bgEnd = "#C62828"              // 진한 레드
                cardStart = "#FFCDD2"          // 연분홍
                cardEnd = "#E57373"            // 분홍빛 레드
                darkText = true
                animationResId = R.raw.cloudy

            }
            else -> {
                name = "Default"
                ledColor = Triple(63, 81, 181)
                bgStart = "#263238"; bgEnd = "#000000"
                cardStart = "#7986CB"; cardEnd = "#5C6BC0"
                darkText = false
                animationResId = R.raw.sunny
            }
        }

        val weatherTheme = WeatherTheme(
            backgroundGradient = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor(bgStart), Color.parseColor(bgEnd))
            ),
            cardGradient = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor(cardStart), Color.parseColor(cardEnd))
            ).apply {
                cornerRadius = 32f
            },
            isDarkText = darkText
        )

        return WeatherThemeWithLed(
            name = name,
            theme = weatherTheme,
            animationResId = animationResId,
            ledColor = ledColor

        )
    }
}
