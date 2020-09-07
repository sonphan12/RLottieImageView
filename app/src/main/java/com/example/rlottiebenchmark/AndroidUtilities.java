package com.example.rlottiebenchmark;

public class AndroidUtilities {

    public static float density = 1;

    public static int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

    public static void runOnUIThread(Runnable runnable) {
        runOnUIThread(runnable, 0);
    }

    public static void runOnUIThread(Runnable runnable, long delay) {
        if (delay == 0) {
            MyApplication.applicationHandler.post(runnable);
        } else {
            MyApplication.applicationHandler.postDelayed(runnable, delay);
        }
    }
}
