package com.fanqie.auto.utils;

import android.util.Log;
import de.robv.android.xposed.XposedBridge;

/**
 * 日志管理器
 */
public class LogManager {
    private static final String TAG = "[FanqieAuto] ";
    private static final String LOG_TAG = "FanqieAuto";
    
    private static boolean loggingEnabled = true;
    
    public static void setLoggingEnabled(boolean enabled) {
        loggingEnabled = enabled;
    }
    
    public static void d(String message) {
        if (!loggingEnabled) return;
        Log.d(LOG_TAG, message);
        XposedBridge.log(TAG + message);
    }
    
    public static void i(String message) {
        if (!loggingEnabled) return;
        Log.i(LOG_TAG, message);
        XposedBridge.log(TAG + message);
    }
    
    public static void w(String message) {
        if (!loggingEnabled) return;
        Log.w(LOG_TAG, message);
        XposedBridge.log(TAG + message);
    }
    
    public static void e(String message) {
        if (!loggingEnabled) return;
        Log.e(LOG_TAG, message);
        XposedBridge.log(TAG + message);
    }
    
    public static void e(String message, Throwable throwable) {
        if (!loggingEnabled) return;
        Log.e(LOG_TAG, message, throwable);
        XposedBridge.log(TAG + message + ": " + throwable.getMessage());
    }
}
