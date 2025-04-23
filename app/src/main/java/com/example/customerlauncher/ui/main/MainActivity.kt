package com.example.customerlauncher.ui.main

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.replace
import com.airbnb.lottie.animation.content.Content
import com.example.customerlauncher.ContentFragment
import com.example.customerlauncher.OttFragment
import com.example.customerlauncher.R
import com.example.customerlauncher.SettingFragment
import com.example.customerlauncher.ui.dashboard.DashboardDataFragment

/**
 * Loads [MainFragment].
 */
class MainActivity : FragmentActivity(), OnTabChangeRequestListener,OnLedColorChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, MainFragment())
                .commitNow()

            supportFragmentManager.beginTransaction()
                .replace(R.id.dashboard_container, DashboardDataFragment())
                .commit()
        }
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val fragment = supportFragmentManager.findFragmentById(R.id.main_browse_fragment)
        if (fragment is MainFragment) {
            if (fragment.handleKeyDown(keyCode)) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }


    private fun changeFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_browse_fragment,fragment)
            .commit()

    }

    override fun onRequestTabChange(tab: String) {

            val fragment = when (tab) {
                "CONTENT" -> ContentFragment()
                "SETTING" -> SettingFragment()
                "OTT" -> OttFragment()
                else -> ContentFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, fragment)
                .commit()

    }

    override fun onRequestChangeBackground(color: Int) {
        findViewById<View>(R.id.root_layout)?.setBackgroundColor(color)
    }
}


