package com.example.customerlauncher;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import vendor.kaon.hardware.kaondevicecontrol.IKaonDeviceControl;
import vendor.kaon.hardware.kaondevicecontrol.LedColor;

public class LedControlService extends Service {

    public static final String ACTION_LED_STANDBY = "com.example.customerlauncher.ACTION_LED_STANDBY";
    public static final String ACTION_LED_ACTIVE = "com.example.customerlauncher.ACTION_LED_ACTIVE";
    private static final String TAG = "PowerLedControlService";
    private static final String SERVICE_NAME = "vendor.kaon.hardware.kaondevicecontrol.IKaonDeviceControl/default";
    private static IKaonDeviceControl ledService = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"Service created");
        connectToHal();
    }

    private void connectToHal(){
        try {
            Class<?> smClass = Class.forName("android.os.ServiceManager");
            java.lang.reflect.Method getService = smClass.getMethod("getService", String.class);
            IBinder binder = (IBinder) getService.invoke(null, SERVICE_NAME);

            if (binder != null) {
                ledService = IKaonDeviceControl.Stub.asInterface(binder);
                Log.i(TAG, "LED HAL 서비스 바인딩 성공");
            } else {
                Log.e(TAG, "LED HAL 서비스 바인딩 실패: binder is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "LED HAL 서비스 초기화 실패", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ledService != null) {
            String action = intent.getAction();
            try {
                if (ACTION_LED_STANDBY.equals(action)) {
                    ledService.ledOn(1);
               //     ledService.setBrightness(1,10);
                    ledService.ledOff(0);
                    Log.i(TAG, "Standby 모드 LED 설정");
                } else if (ACTION_LED_ACTIVE.equals(action)) {
                    ledService.ledOn(0);
             //       ledService.setBrightness(0,10);
                    ledService.ledOff(1);
                    Log.i(TAG, "Active 모드 LED 설정");
                }
            } catch (RemoteException e) {
                Log.e(TAG, "LED 제어 실패", e);
            }
        }
        return START_NOT_STICKY;
    }

    public static void setLedColor(int ledColor) throws RemoteException {
        if (ledService != null) {
            if(ledColor==0){ // red
                ledService.ledOn(0);
                ledService.ledOff(1);
            }else{ // white
                ledService.ledOn(1);
                ledService.ledOff(0);
            }
        } else {
            Log.e(TAG, "ledService is null");
        }
    }

    public static void setStandByModeColor() throws RemoteException {
        if (ledService != null) {
           ledService.ledOn(1);
           ledService.ledOff(0);
        } else {
            Log.e(TAG, "ledService is null");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

//    public static void setPowerState(boolean on) {
//        if (ledService != null) {
//            try {
//                ledService.setPowerState(on ? DeviceOnOff.ON : DeviceOnOff.OFF);
//                Log.i(TAG, "LED 전원 상태 변경: " + on);
//            } catch (RemoteException e) {
//                Log.e(TAG, "setPowerState 실패", e);
//            }
//        } else {
//            Log.e(TAG, "ledService is null");
//        }
//    }
}