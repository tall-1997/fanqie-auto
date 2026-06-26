package com.fanqie.auto.hook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 番茄小说自动签到模块 v5.0.0
 * 监控并记录签到、任务、奖励
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";
    private static final String MODULE_VERSION = "v5.0.0";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "==============================");
        XposedBridge.log(TAG + "番茄小说自动签到模块 " + MODULE_VERSION);
        XposedBridge.log(TAG + "目标应用: " + TARGET_PACKAGE);
        XposedBridge.log(TAG + "==============================");

        try {
            // Hook HTTP请求
            hookOkHttpRequest(lpparam);
            
            // Hook JSON响应
            hookJsonResponse(lpparam);
            
            XposedBridge.log(TAG + "Hook初始化完成");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "初始化失败: " + e.getMessage());
        }
    }

    /**
     * Hook OkHttp请求
     */
    private void hookOkHttpRequest(XC_LoadPackage.LoadPackageParam lpparam) {
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
                                handleUrl(url);
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
                            handleIntResponse(key, value);
                        } catch (Throwable ignored) {}
                    }
                }
            );

            // Hook optLong
            XposedHelpers.findAndHookMethod(
                "org.json.JSONObject",
                lpparam.classLoader,
                "optLong",
                String.class,
                long.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            String key = (String) param.args[0];
                            long value = (long) param.getResult();
                            handleLongResponse(key, value);
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
                            handleStringResponse(key, value);
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
     * 处理URL
     */
    private void handleUrl(String url) {
        String lower = url.toLowerCase();
        
        if (lower.contains("sign_in") || lower.contains("signin")) {
            XposedBridge.log(TAG + "===== 签到请求 =====");
            XposedBridge.log(TAG + "URL: " + url);
            XposedBridge.log(TAG + "====================");
        } else if (lower.contains("task/done")) {
            XposedBridge.log(TAG + "===== 任务请求 =====");
            XposedBridge.log(TAG + "URL: " + url);
            XposedBridge.log(TAG + "====================");
        } else if (lower.contains("reward") || lower.contains("claim")) {
            XposedBridge.log(TAG + "===== 奖励请求 =====");
            XposedBridge.log(TAG + "URL: " + url);
            XposedBridge.log(TAG + "====================");
        } else if (lower.contains("luckycat")) {
            XposedBridge.log(TAG + "福利请求: " + url);
        }
    }

    /**
     * 处理整数响应
     */
    private void handleIntResponse(String key, int value) {
        if (key == null) return;
        
        // 签到状态
        if ("status".equals(key) && value == 1) {
            XposedBridge.log(TAG + "签到成功!");
        }
        
        // 金币
        if (key.contains("coin") && value > 0 && value < 1000000) {
            XposedBridge.log(TAG + "金币: " + key + " = " + value);
        }
        
        // 奖励
        if (key.contains("reward") && value > 0 && value < 1000000) {
            XposedBridge.log(TAG + "奖励: " + key + " = " + value);
        }
        
        // 任务
        if (key.contains("task") && value > 0 && value < 1000) {
            XposedBridge.log(TAG + "任务: " + key + " = " + value);
        }
    }

    /**
     * 处理长整数响应
     */
    private void handleLongResponse(String key, long value) {
        if (key == null) return;
        
        if (key.contains("coin") && value > 0 && value < 1000000) {
            XposedBridge.log(TAG + "金币: " + key + " = " + value);
        }
        if (key.contains("reward") && value > 0 && value < 1000000) {
            XposedBridge.log(TAG + "奖励: " + key + " = " + value);
        }
    }

    /**
     * 处理字符串响应
     */
    private void handleStringResponse(String key, String value) {
        if (key == null || value == null || value.isEmpty()) return;
        
        // 签到状态
        if ("todaySigned".equals(key)) {
            XposedBridge.log(TAG + "今日签到: " + value);
        }
        if (key.contains("sign") && key.contains("status")) {
            XposedBridge.log(TAG + "签到状态: " + value);
        }
        
        // 奖励名称
        if (key.contains("reward") && key.contains("name")) {
            XposedBridge.log(TAG + "奖励: " + value);
        }
        
        // 任务名称
        if (key.contains("task") && key.contains("name")) {
            XposedBridge.log(TAG + "任务: " + value);
        }
    }
}
