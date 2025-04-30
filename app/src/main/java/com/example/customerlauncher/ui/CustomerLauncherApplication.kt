package com.example.customerlauncher.ui

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import com.example.customerlauncher.StandByModeReceiver
import com.example.customerlauncher.di.NetWorkModule
import com.example.customerlauncher.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin


class CustomerLauncherApplication: Application() {

    private var receiver: StandByModeReceiver? = null
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CustomerLauncherApplication)
            modules(appModule, NetWorkModule)
        }

        receiver = StandByModeReceiver()
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(receiver, filter)

    }
}