package com.example.rlottiebenchmark;

public class NativeLoader {

    private final static String LIB_NAME = "zlottie";

    private static volatile boolean nativeLoaded = false;

    public static synchronized void initNativeLibs() {
        if (nativeLoaded) {
            return;
        }

        try {
            System.loadLibrary(LIB_NAME);
            nativeLoaded = true;
        } catch (Error e) {
        }
    }

}
