package com.example.customerlauncher;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


// AIDL 인터페이스 import
import android.os.ServiceManager;
import android.os.RemoteException;
import vendor.kaon.hardware.leddrivercontrol.ILedDriverControl;
import androidx.annotation.Nullable;

public class LedControlService extends Service {

    private static final String TAG = "LedControlService";

    private ILedDriverControl ledService = null;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"Service created");


    }


    public void connectService() {
        try {
            IBinder binder = ServiceManager.waitForDeclaredService("vendor.kaon.hardware.kaondevicecontrol.IKaonDeviceControl/default");
            ledService = ILedDriverControl.Stub.asInterface(binder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setLedColor(int color) {
        if (ledService != null) {
            try {
                ledService.ledOn(color);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
