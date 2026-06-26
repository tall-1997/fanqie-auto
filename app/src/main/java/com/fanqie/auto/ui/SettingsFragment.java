package com.fanqie.auto.ui;

import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.fanqie.auto.R;

/**
 * 设置Fragment
 */
public class SettingsFragment extends PreferenceFragmentCompat {
    
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        
        setupPreferences();
    }
    
    private void setupPreferences() {
        // 自动签到开关
        SwitchPreferenceCompat autoSignPref = findPreference("auto_sign_enabled");
        if (autoSignPref != null) {
            autoSignPref.setOnPreferenceChangeListener((preference, newValue) -> {
                // 处理自动签到开关变化
                boolean enabled = (Boolean) newValue;
                // ConfigManager.setAutoSignEnabled(enabled);
                return true;
            });
        }
        
        // 自动任务开关
        SwitchPreferenceCompat autoTaskPref = findPreference("auto_task_enabled");
        if (autoTaskPref != null) {
            autoTaskPref.setOnPreferenceChangeListener((preference, newValue) -> {
                // 处理自动任务开关变化
                boolean enabled = (Boolean) newValue;
                // ConfigManager.setAutoTaskEnabled(enabled);
                return true;
            });
        }
        
        // 日志记录开关
        SwitchPreferenceCompat loggingPref = findPreference("logging_enabled");
        if (loggingPref != null) {
            loggingPref.setOnPreferenceChangeListener((preference, newValue) -> {
                // 处理日志记录开关变化
                boolean enabled = (Boolean) newValue;
                // LogManager.setLoggingEnabled(enabled);
                return true;
            });
        }
        
        // 目标包名
        EditTextPreference targetPackagePref = findPreference("target_package");
        if (targetPackagePref != null) {
            targetPackagePref.setOnPreferenceChangeListener((preference, newValue) -> {
                // 处理目标包名变化
                String packageName = (String) newValue;
                // ConfigManager.setTargetPackage(packageName);
                return true;
            });
        }
        
        // Hook超时时间
        EditTextPreference hookTimeoutPref = findPreference("hook_timeout");
        if (hookTimeoutPref != null) {
            hookTimeoutPref.setOnPreferenceChangeListener((preference, newValue) -> {
                // 处理Hook超时时间变化
                String timeoutStr = (String) newValue;
                try {
                    long timeout = Long.parseLong(timeoutStr);
                    // HookManager.setHookTimeout(timeout);
                } catch (NumberFormatException e) {
                    // 忽略无效输入
                }
                return true;
            });
        }
        
        // 导出配置
        Preference exportPref = findPreference("export_config");
        if (exportPref != null) {
            exportPref.setOnPreferenceClickListener(preference -> {
                // 导出配置
                exportConfig();
                return true;
            });
        }
        
        // 导入配置
        Preference importPref = findPreference("import_config");
        if (importPref != null) {
            importPref.setOnPreferenceClickListener(preference -> {
                // 导入配置
                importConfig();
                return true;
            });
        }
        
        // 重置配置
        Preference resetPref = findPreference("reset_config");
        if (resetPref != null) {
            resetPref.setOnPreferenceClickListener(preference -> {
                // 重置配置
                resetConfig();
                return true;
            });
        }
    }
    
    private void exportConfig() {
        // 这里应该实现配置导出逻辑
        // ConfigManager.exportConfig();
    }
    
    private void importConfig() {
        // 这里应该实现配置导入逻辑
        // ConfigManager.importConfig();
    }
    
    private void resetConfig() {
        // 这里应该实现配置重置逻辑
        // ConfigManager.resetToDefaults();
    }
}
