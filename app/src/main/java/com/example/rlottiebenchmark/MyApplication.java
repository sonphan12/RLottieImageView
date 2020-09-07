package com.example.rlottiebenchmark;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

public class MyApplication extends Application {

    public static volatile Handler applicationHandler;
    public static volatile Context applicationContext;


    public MyApplication() {
        super();
    }

    @Override
    public void onCreate() {
        try {
            applicationContext = getApplicationContext();
        } catch (Throwable ignore) {

        }

        super.onCreate();

        if (applicationContext == null) {
            applicationContext = getApplicationContext();
        }

        NativeLoader.initNativeLibs();

        applicationHandler = new Handler(applicationContext.getMainLooper());

    }
}
