package com.fanqie.auto.hook;

import java.lang.reflect.Method;

import androidx.annotation.NonNull;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
/**
 * 番茄小说自动签到模块 - 主入口
 * 版本: v1.0.2
 * 支持: libxposed API 102
 */
public class MainHook extends XposedModule implements XposedModuleInterface {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";
    private static final String MODULE_VERSION = "v1.0.2";

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
    public void onPackageReady(@NonNull PackageReadyParam param) {
        if (!param.getPackageName().equals(TARGET_PACKAGE)) return;

        log(TAG + "==============================");
        log(TAG + "番茄小说自动签到模块 " + MODULE_VERSION);
        log(TAG + "目标应用: " + TARGET_PACKAGE);
        log(TAG + "API版本: " + getApiVersion());
        log(TAG + "==============================");

        try {
            ClassLoader classLoader = param.getClassLoader();
            
            // Hook HTTP 请求
            hookHttpRequest(classLoader);
            
            // Hook 签到响应
            hookSignResponse(classLoader);

            log(TAG + "所有Hook初始化完成");
        } catch (Throwable e) {
            log(TAG + "Hook初始化失败: " + e.getMessage());
        }
    }

    /**
     * Hook HTTP请求，监控签到和任务相关接口
     */
    private void hookHttpRequest(ClassLoader classLoader) {
        try {
            Class<?> builderClass = classLoader.loadClass("okhttp3.Request$Builder");
            Method buildMethod = builderClass.getMethod("build");
            
            hook(buildMethod).intercept(chain -> {
                try {
                    Object request = chain.proceed();
                    // 通过反射获取URL
                    java.lang.reflect.Field urlField = request.getClass().getDeclaredField("url");
                    urlField.setAccessible(true);
                    String url = (String) urlField.get(request);
                    
                    if (url != null && isImportantUrl(url)) {
                        logApiRequest(url);
                    }
                    return request;
                } catch (Throwable e) {
                    return chain.proceed();
                }
            });
            
            log(TAG + "HTTP请求Hook成功");
        } catch (Throwable e) {
            log(TAG + "HTTP请求Hook失败: " + e.getMessage());
        }
    }

    /**
     * Hook签到响应，提取签到状态和金币奖励
     */
    private void hookSignResponse(ClassLoader classLoader) {
        try {
            Class<?> jsonObjectClass = classLoader.loadClass("org.json.JSONObject");
            
            // Hook optInt 方法
            Method optIntMethod = jsonObjectClass.getMethod("optInt", String.class, int.class);
            hook(optIntMethod).intercept(chain -> {
                try {
                    int result = (int) chain.proceed();
                    String key = (String) chain.getArgs().get(0);
                    handleIntField(key, result);
                    return result;
                } catch (Throwable e) {
                    return chain.proceed();
                }
            });

            // Hook optLong 方法（处理大额金币）
            Method optLongMethod = jsonObjectClass.getMethod("optLong", String.class, long.class);
            hook(optLongMethod).intercept(chain -> {
                try {
                    long result = (long) chain.proceed();
                    String key = (String) chain.getArgs().get(0);
                    handleLongField(key, result);
                    return result;
                } catch (Throwable e) {
                    return chain.proceed();
                }
            });

            // Hook optString 方法（处理签到状态字符串）
            Method optStringMethod = jsonObjectClass.getMethod("optString", String.class, String.class);
            hook(optStringMethod).intercept(chain -> {
                try {
                    String result = (String) chain.proceed();
                    String key = (String) chain.getArgs().get(0);
                    handleStringField(key, result);
                    return result;
                } catch (Throwable e) {
                    return chain.proceed();
                }
            });

            log(TAG + "签到响应Hook成功");
        } catch (Throwable e) {
            log(TAG + "签到响应Hook失败: " + e.getMessage());
        }
    }

    /**
     * 处理整数字段
     */
    private void handleIntField(String key, int value) {
        // 签到状态
        if ("status".equals(key) && value == 1) {
            log(TAG + "签到成功!");
        }
        
        // 金币/奖励字段
        if (isRewardField(key) && value > 0) {
            log(TAG + "获得奖励: " + key + " = +" + value);
        }
        
        // 任务状态
        if ("task_status".equals(key) && value == 1) {
            log(TAG + "任务完成!");
        }
    }

    /**
     * 处理长整数字段（大额金币）
     */
    private void handleLongField(String key, long value) {
        if (isRewardField(key) && value > 0) {
            log(TAG + "获得奖励: " + key + " = +" + value);
        }
    }

    /**
     * 处理字符串字段
     */
    private void handleStringField(String key, String value) {
        if (value == null || value.isEmpty()) return;
        
        // 签到日期
        if ("todaySigned".equals(key) || "sign_date".equals(key)) {
            log(TAG + "签到日期: " + value);
        }
        
        // 签到状态字符串
        if ("sign_status".equals(key) || "status".equals(key)) {
            if ("signed".equals(value) || "success".equals(value)) {
                log(TAG + "签到状态: " + value);
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
            log(TAG + "签到请求: " + url);
        }
        // 任务相关
        else if (url.contains("task")) {
            log(TAG + "任务请求: " + url);
        }
        // 奖励相关
        else if (url.contains("reward") || url.contains("gold") || url.contains("coin")) {
            log(TAG + "奖励请求: " + url);
        }
        // 其他
        else {
            log(TAG + "相关请求: " + url);
        }
    }

    /**
     * 记录日志
     */
    private void log(String message) {
        android.util.Log.d("FanqieAuto", message);
    }
}
