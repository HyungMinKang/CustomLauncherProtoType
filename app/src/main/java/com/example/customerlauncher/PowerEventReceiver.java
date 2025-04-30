package com.example.customerlauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PowerEventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            Log.d("PowerEventReceiver", "Standby detected, setting LED...");

            // 서비스 호출
        //    Intent serviceIntent = new Intent(context, LedControlService.class);
      //      context.startService(serviceIntent);  // Service에서 HAL 호출
        }
    }
}
