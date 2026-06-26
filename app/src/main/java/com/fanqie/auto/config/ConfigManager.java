package com.fanqie.auto.config;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 配置管理器
 */
public class ConfigManager {
    private static final String PREF_NAME = "fanqie_auto_config";
    private static final String KEY_AUTO_SIGN = "auto_sign_enabled";
    private static final String KEY_AUTO_TASK = "auto_task_enabled";
    private static final String KEY_LOGGING = "logging_enabled";
    
    private final SharedPreferences prefs;
    
    public ConfigManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public boolean isAutoSignEnabled() {
        return prefs.getBoolean(KEY_AUTO_SIGN, true);
    }
    
    public void setAutoSignEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_SIGN, enabled).apply();
    }
    
    public boolean isAutoTaskEnabled() {
        return prefs.getBoolean(KEY_AUTO_TASK, true);
    }
    
    public void setAutoTaskEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_TASK, enabled).apply();
    }
    
    public boolean isLoggingEnabled() {
        return prefs.getBoolean(KEY_LOGGING, true);
    }
    
    public void setLoggingEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LOGGING, enabled).apply();
    }
}
