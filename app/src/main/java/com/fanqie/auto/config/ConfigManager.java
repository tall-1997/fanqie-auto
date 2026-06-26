package com.fanqie.auto.config;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 配置管理器
 */
public class ConfigManager {
    private static final String PREF_NAME = "fanqie_auto_config";
    private static final String KEY_AUTO_SIGN = "auto_sign_enabled";
    private static final String KEY_AUTO_TASK = "auto_task_enabled";
    private static final String KEY_LOGGING = "logging_enabled";
    private static final String KEY_TARGET_PACKAGE = "target_package";
    
    private final SharedPreferences prefs;
    private ConfigChangeListener listener;
    
    public interface ConfigChangeListener {
        void onConfigChanged(String key, Object value);
    }
    
    public ConfigManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            if (listener != null) {
                Object value = getValueByKey(key);
                listener.onConfigChanged(key, value);
            }
        });
    }
    
    public void setConfigChangeListener(ConfigChangeListener listener) {
        this.listener = listener;
    }
    
    public boolean isAutoSignEnabled() {
        return prefs.getBoolean(KEY_AUTO_SIGN, true);
    }
    
    public void setAutoSignEnabled(boolean enabled) {
        if (validateBoolean(enabled)) {
            prefs.edit().putBoolean(KEY_AUTO_SIGN, enabled).apply();
        }
    }
    
    public boolean isAutoTaskEnabled() {
        return prefs.getBoolean(KEY_AUTO_TASK, true);
    }
    
    public void setAutoTaskEnabled(boolean enabled) {
        if (validateBoolean(enabled)) {
            prefs.edit().putBoolean(KEY_AUTO_TASK, enabled).apply();
        }
    }
    
    public boolean isLoggingEnabled() {
        return prefs.getBoolean(KEY_LOGGING, true);
    }
    
    public void setLoggingEnabled(boolean enabled) {
        if (validateBoolean(enabled)) {
            prefs.edit().putBoolean(KEY_LOGGING, enabled).apply();
        }
    }
    
    public String getTargetPackage() {
        return prefs.getString(KEY_TARGET_PACKAGE, "com.dragon.read");
    }
    
    public void setTargetPackage(String packageName) {
        if (validatePackageName(packageName)) {
            prefs.edit().putString(KEY_TARGET_PACKAGE, packageName).apply();
        }
    }
    
    private boolean validateBoolean(boolean value) {
        // 布尔值总是有效的
        return true;
    }
    
    private boolean validatePackageName(String packageName) {
        return packageName != null && !packageName.trim().isEmpty();
    }
    
    private Object getValueByKey(String key) {
        switch (key) {
            case KEY_AUTO_SIGN:
                return isAutoSignEnabled();
            case KEY_AUTO_TASK:
                return isAutoTaskEnabled();
            case KEY_LOGGING:
                return isLoggingEnabled();
            case KEY_TARGET_PACKAGE:
                return getTargetPackage();
            default:
                return null;
        }
    }
    
    public void resetToDefaults() {
        prefs.edit()
            .putBoolean(KEY_AUTO_SIGN, true)
            .putBoolean(KEY_AUTO_TASK, true)
            .putBoolean(KEY_LOGGING, true)
            .putString(KEY_TARGET_PACKAGE, "com.dragon.read")
            .apply();
    }
    
    public boolean isValid() {
        String packageName = getTargetPackage();
        return validatePackageName(packageName);
    }
    
    public String exportConfigToJson() {
        try {
            JSONObject json = new JSONObject();
            json.put(KEY_AUTO_SIGN, isAutoSignEnabled());
            json.put(KEY_AUTO_TASK, isAutoTaskEnabled());
            json.put(KEY_LOGGING, isLoggingEnabled());
            json.put(KEY_TARGET_PACKAGE, getTargetPackage());
            return json.toString(2); // 缩进2个空格
        } catch (JSONException e) {
            return null;
        }
    }
    
    public boolean importConfigFromJson(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            
            if (json.has(KEY_AUTO_SIGN)) {
                setAutoSignEnabled(json.getBoolean(KEY_AUTO_SIGN));
            }
            if (json.has(KEY_AUTO_TASK)) {
                setAutoTaskEnabled(json.getBoolean(KEY_AUTO_TASK));
            }
            if (json.has(KEY_LOGGING)) {
                setLoggingEnabled(json.getBoolean(KEY_LOGGING));
            }
            if (json.has(KEY_TARGET_PACKAGE)) {
                setTargetPackage(json.getString(KEY_TARGET_PACKAGE));
            }
            
            return true;
        } catch (JSONException e) {
            return false;
        }
    }
    
    public boolean exportConfigToFile(File file) {
        String json = exportConfigToJson();
        if (json == null) return false;
        
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    public boolean importConfigFromFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return importConfigFromJson(sb.toString());
        } catch (IOException e) {
            return false;
        }
    }
}
