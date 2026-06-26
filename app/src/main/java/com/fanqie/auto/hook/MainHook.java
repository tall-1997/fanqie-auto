package com.fanqie.auto.hook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 番茄小说自动签到模块 v3.1.0
 * 功能：监控签到、任务、奖励接口
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";
    private static final String MODULE_VERSION = "v3.1.0";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "==============================");
        XposedBridge.log(TAG + "番茄小说自动签到模块 " + MODULE_VERSION);
        XposedBridge.log(TAG + "目标应用: " + TARGET_PACKAGE);
        XposedBridge.log(TAG + "==============================");

        try {
            // Hook HTTP请求
            hookHttpRequest(lpparam);
            
            // Hook JSON响应
            hookJsonResponse(lpparam);
            
            XposedBridge.log(TAG + "所有Hook初始化完成");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "Hook初始化失败: " + e.getMessage());
        }
    }

    /**
     * Hook HTTP请求
     */
    private void hookHttpRequest(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "okhttp3.Request$Builder",
                lpparam.classLoader,
                "build",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Object request = param.getResult();
                            String url = (String) XposedHelpers.getObjectField(request, "url");
                            if (url != null) {
                                logUrl(url);
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            );
            XposedBridge.log(TAG + "HTTP请求Hook成功");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "HTTP请求Hook失败: " + e.getMessage());
        }
    }

    /**
     * Hook JSON响应
     */
    private void hookJsonResponse(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook optInt
            XposedHelpers.findAndHookMethod(
                "org.json.JSONObject",
                lpparam.classLoader,
                "optInt",
                String.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            String key = (String) param.args[0];
                            int value = (int) param.getResult();
                            handleIntField(key, value);
                        } catch (Throwable ignored) {}
                    }
                }
            );

            // Hook optString
            XposedHelpers.findAndHookMethod(
                "org.json.JSONObject",
                lpparam.classLoader,
                "optString",
                String.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            String key = (String) param.args[0];
                            String value = (String) param.getResult();
                            handleStringField(key, value);
                        } catch (Throwable ignored) {}
                    }
                }
            );

            XposedBridge.log(TAG + "JSON响应Hook成功");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "JSON响应Hook失败: " + e.getMessage());
        }
    }

    /**
     * 记录URL
     */
    private void logUrl(String url) {
        String lower = url.toLowerCase();
        if (lower.contains("sign_in") || lower.contains("signin")) {
            XposedBridge.log(TAG + "签到请求: " + url);
        } else if (lower.contains("task/done")) {
            XposedBridge.log(TAG + "任务请求: " + url);
        } else if (lower.contains("reward") || lower.contains("claim")) {
            XposedBridge.log(TAG + "奖励请求: " + url);
        } else if (lower.contains("luckycat")) {
            XposedBridge.log(TAG + "福利请求: " + url);
        }
    }

    /**
     * 处理整数字段
     */
    private void handleIntField(String key, int value) {
        if (key == null) return;
        
        if ("status".equals(key) && value == 1) {
            XposedBridge.log(TAG + "签到成功!");
        }
        if (key.contains("coin") && value > 0) {
            XposedBridge.log(TAG + "金币: " + key + " = " + value);
        }
        if (key.contains("reward") && value > 0) {
            XposedBridge.log(TAG + "奖励: " + key + " = " + value);
        }
        if (key.contains("task") && value > 0) {
            XposedBridge.log(TAG + "任务: " + key + " = " + value);
        }
    }

    /**
     * 处理字符串字段
     */
    private void handleStringField(String key, String value) {
        if (key == null || value == null || value.isEmpty()) return;
        
        if ("todaySigned".equals(key)) {
            XposedBridge.log(TAG + "今日签到: " + value);
        }
        if (key.contains("sign") && key.contains("status")) {
            XposedBridge.log(TAG + "签到状态: " + value);
        }
        if (key.contains("reward") && key.contains("name")) {
            XposedBridge.log(TAG + "奖励名称: " + value);
        }
    }
}
