package com.example.customerlauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.Handler;
import android.util.Log;

public class StandByModeReceiver extends BroadcastReceiver {
    private static final String TAG = "StandByModeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            Log.d(TAG, "Screen Off detected, setting LED StandBy Mode...");
            // 서비스 호출
            Intent serviceIntent = new Intent(context, LedControlService.class);
            serviceIntent.setAction(LedControlService.ACTION_LED_STANDBY);
            context.startService(serviceIntent);


        } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            Log.d(TAG, "Screen On detected, setting LED Active Mode...");
            Intent serviceIntent = new Intent(context, LedControlService.class);
            serviceIntent.setAction(LedControlService.ACTION_LED_PREACTIVE);
            context.startService(serviceIntent);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent delayedIntent = new Intent(context, LedControlService.class);
                delayedIntent.setAction(LedControlService.ACTION_LED_ACTIVE);
                context.startService(delayedIntent);
            }, 1000);
        }
    }

}
