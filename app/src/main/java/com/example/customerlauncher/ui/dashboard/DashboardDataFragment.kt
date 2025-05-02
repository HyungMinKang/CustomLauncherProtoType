package com.example.customerlauncher.ui.dashboard

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.customerlauncher.R
import com.example.customerlauncher.domain.model.WeatherTheme
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.text.DecimalFormat

class DashboardDataFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dashboard_card, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val networkStatus = view.findViewById<TextView>(R.id.tv_network_status)
        val ipInfo = view.findViewById<TextView>(R.id.tv_ip_address)
        val storageInfo = view.findViewById<TextView>(R.id.tv_storage)

        // 네트워크 상태
        networkStatus.text = getNetworkStatus()

        // 디바이스 정보
        val ip = getLocalIpAddress()
        ipInfo.text = "IP: $ip\nModel"
        // 저장 공간
        storageInfo.text = getStorageInfo()

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getNetworkStatus(): String {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        Log.d("Dash", "${cm.activeNetwork}")
        val network = cm.activeNetwork ?: return "Network State:  네트워크 없음"
        val capabilities = cm.getNetworkCapabilities(network) ?: return "Network State: 네트워크 없음"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Network State:  Wi-Fi 연결됨"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Network State: 이더넷 연결됨"
            else -> "Network State:  네트워크 없음"
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is InetAddress) {
                        val hostAddress = addr.hostAddress ?: continue
                        if (hostAddress.indexOf(':') < 0) return hostAddress
                    }
                }
            }
        } catch (ex: Exception) {}
        return "-"
    }

    private fun getStorageInfo(): String {
        val stat = StatFs(File(requireContext().filesDir.absolutePath).absolutePath)
        val total = stat.blockCountLong * stat.blockSizeLong
        val avail = stat.availableBlocksLong * stat.blockSizeLong
        val format = DecimalFormat("#.##")
        return "저장공간: ${format.format(avail / 1e9)}GB / ${format.format(total / 1e9)}GB"
    }

    private fun launchApp(packageName: String) {
        val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
        launchIntent?.let { startActivity(it) }
    }

    private fun setAppIcon(imageView: ImageView, packageName: String) {
        try {
            val pm = requireContext().packageManager
            val icon = pm.getApplicationIcon(packageName)
            imageView.setImageDrawable(icon)

            imageView.setOnClickListener {
                val intent = pm.getLaunchIntentForPackage(packageName)
                intent?.let { startActivity(it) }
            }
        } catch (e: Exception) {
            //imageView.visibility = View.GONE // 앱 설치 안됨
        }
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

    fun applyTheme(theme: WeatherTheme) {
        view?.findViewById<View>(R.id.layout_dashboard)?.let { dashboardLayout ->
            // 배경
            dashboardLayout.background = theme.cardGradient
            //dashboardLayout.setBackgroundColor(theme.cardGradient)
            Log.d("DashBoard", "${theme.cardGradient} ${dashboardLayout.background}")
            // 텍스트 색상 적용
            val textColor = if (theme.isDarkText) Color.BLACK else Color.WHITE
            applyTextColorToAll(dashboardLayout, textColor)
        }
    }
}


