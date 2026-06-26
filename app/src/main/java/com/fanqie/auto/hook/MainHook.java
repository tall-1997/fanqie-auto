package com.fanqie.auto.hook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 番茄小说自动签到模块
 * 版本: v1.0.3
 * 兼容: 所有版本 LSPosed / Xposed Framework
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";
    private static final String MODULE_VERSION = "v1.0.3";

    // 签到相关API路径
    private static final String[] SIGN_API_PATTERNS = {
        "sign_in",
        "signin",
        "sign_in_popup",
        "alarm_clock_sign_in",
        "low_activity_user_signin",
        "new_user_signin",
        "bookshelf_sign_in"
    };

    // 任务相关API路径
    private static final String[] TASK_API_PATTERNS = {
        "task/",
        "task/done",
        "task/list",
        "task/detail"
    };

    // 奖励相关字段
    private static final String[] REWARD_FIELDS = {
        "obtain",
        "coin",
        "gold",
        "goldcoin",
        "coinAmount",
        "coinCount",
        "reward",
        "award"
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "==============================");
        XposedBridge.log(TAG + "番茄小说自动签到模块 " + MODULE_VERSION);
        XposedBridge.log(TAG + "目标应用: " + TARGET_PACKAGE);
        XposedBridge.log(TAG + "==============================");

        try {
            // Hook HTTP 请求
            hookHttpRequest(lpparam);
            
            // Hook 签到响应
            hookSignResponse(lpparam);

            XposedBridge.log(TAG + "所有Hook初始化完成");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "Hook初始化失败: " + e.getMessage());
        }
    }

    /**
     * Hook HTTP请求，监控签到和任务相关接口
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
                            if (url != null && isImportantUrl(url)) {
                                logApiRequest(url);
                            }
                        } catch (Throwable e) {
                            // 忽略解析错误
                        }
                    }
                }
            );
            XposedBridge.log(TAG + "HTTP请求Hook成功");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "HTTP请求Hook失败: " + e.getMessage());
        }
    }

    /**
     * Hook签到响应，提取签到状态和金币奖励
     */
    private void hookSignResponse(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook optInt 方法
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
                        } catch (Throwable e) {
                            // 忽略解析错误
                        }
                    }
                }
            );

            // Hook optLong 方法（处理大额金币）
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
                            handleLongField(key, value);
                        } catch (Throwable e) {
                            // 忽略解析错误
                        }
                    }
                }
            );

            // Hook optString 方法（处理签到状态字符串）
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
                        } catch (Throwable e) {
                            // 忽略解析错误
                        }
                    }
                }
            );

            XposedBridge.log(TAG + "签到响应Hook成功");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "签到响应Hook失败: " + e.getMessage());
        }
    }

    /**
     * 处理整数字段
     */
    private void handleIntField(String key, int value) {
        // 签到状态
        if ("status".equals(key) && value == 1) {
            XposedBridge.log(TAG + "签到成功!");
        }
        
        // 金币/奖励字段
        if (isRewardField(key) && value > 0) {
            XposedBridge.log(TAG + "获得奖励: " + key + " = +" + value);
        }
        
        // 任务状态
        if ("task_status".equals(key) && value == 1) {
            XposedBridge.log(TAG + "任务完成!");
        }
    }

    /**
     * 处理长整数字段（大额金币）
     */
    private void handleLongField(String key, long value) {
        if (isRewardField(key) && value > 0) {
            XposedBridge.log(TAG + "获得奖励: " + key + " = +" + value);
        }
    }

    /**
     * 处理字符串字段
     */
    private void handleStringField(String key, String value) {
        if (value == null || value.isEmpty()) return;
        
        // 签到日期
        if ("todaySigned".equals(key) || "sign_date".equals(key)) {
            XposedBridge.log(TAG + "签到日期: " + value);
        }
        
        // 签到状态字符串
        if ("sign_status".equals(key) || "status".equals(key)) {
            if ("signed".equals(value) || "success".equals(value)) {
                XposedBridge.log(TAG + "签到状态: " + value);
            }
        }
    }

    /**
     * 判断是否为奖励相关字段
     */
    private boolean isRewardField(String key) {
        if (key == null) return false;
        String lowerKey = key.toLowerCase();
        for (String field : REWARD_FIELDS) {
            if (lowerKey.contains(field.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为重要URL
     */
    private boolean isImportantUrl(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();
        
        // 检查签到相关API
        for (String pattern : SIGN_API_PATTERNS) {
            if (lowerUrl.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        
        // 检查任务相关API
        for (String pattern : TASK_API_PATTERNS) {
            if (lowerUrl.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        
        // 检查通用关键词
        return lowerUrl.contains("luckycat") ||
               lowerUrl.contains("reward") ||
               lowerUrl.contains("gold") ||
               lowerUrl.contains("coin");
    }

    /**
     * 记录API请求日志
     */
    private void logApiRequest(String url) {
        // 签到相关
        if (url.contains("sign_in") || url.contains("signin")) {
            XposedBridge.log(TAG + "签到请求: " + url);
        }
        // 任务相关
        else if (url.contains("task")) {
            XposedBridge.log(TAG + "任务请求: " + url);
        }
        // 奖励相关
        else if (url.contains("reward") || url.contains("gold") || url.contains("coin")) {
            XposedBridge.log(TAG + "奖励请求: " + url);
        }
        // 其他
        else {
            XposedBridge.log(TAG + "相关请求: " + url);
        }
    }
}
