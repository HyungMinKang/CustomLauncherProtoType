package com.example.customerlauncher.ui.dashboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.example.customerlauncher.ui.main.MainActivity
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.text.DecimalFormat

class DashboardDataFragment : Fragment() {
    private lateinit var networkStatus: TextView
    private lateinit var ipInfo: TextView
    private lateinit var networkReceiver: BroadcastReceiver
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dashboard_card, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        networkStatus = view.findViewById<TextView>(R.id.tv_network_status)
        ipInfo = view.findViewById<TextView>(R.id.tv_ip_address)
        val storageInfo = view.findViewById<TextView>(R.id.tv_storage)
        storageInfo.text = getStorageInfo()
        networkReceiver = object : BroadcastReceiver() {
            private var lastConnected = true // 연결 상태 변화 감지를 위해 사용


            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                if (intent?.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                    val isConnected = isNetworkConnected()

                    // UI 업데이트
                    networkStatus.text = getNetworkStatus()
                    ipInfo.text = "IP: ${getLocalIpAddress()}"
                    if (isConnected != lastConnected) {
                        lastConnected = isConnected
                        (activity as? MainActivity)?.forceWeatherRefresh()
                    }
                }
            }
        }
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireContext().registerReceiver(networkReceiver, filter)

        // 최초 한 번 초기값 설정
        networkStatus.text = getNetworkStatus()
        ipInfo.text = "IP: ${getLocalIpAddress()}"


    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireContext().unregisterReceiver(networkReceiver)
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
    @RequiresApi(Build.VERSION_CODES.M)
    private fun isNetworkConnected(): Boolean {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
            // 텍스트 색상 적용
            val textColor = if (theme.isDarkText) Color.BLACK else Color.WHITE
            applyTextColorToAll(dashboardLayout, textColor)
        }
    }
}


