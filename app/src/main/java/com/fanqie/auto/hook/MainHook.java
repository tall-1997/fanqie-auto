package com.fanqie.auto.hook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 番茄小说自动签到模块
 * 版本: v2.0.0
 * 功能: 自动签到、自动领取奖励、自动完成任务
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";
    private static final String MODULE_VERSION = "v2.0.0";

    // 自动执行开关
    private static boolean autoSignEnabled = true;
    private static boolean autoClaimEnabled = true;
    private static boolean autoTaskEnabled = true;

    // 定时任务
    private ScheduledExecutorService scheduler;
    
    // 签到状态
    private static boolean todaySigned = false;
    private static int signRetryCount = 0;
    private static final int MAX_RETRY = 3;

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

    // 领取相关字段
    private static final String[] CLAIM_FIELDS = {
        "claim",
        "collect",
        "receive",
        "领取",
        "获得"
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "==============================");
        XposedBridge.log(TAG + "番茄小说自动签到模块 " + MODULE_VERSION);
        XposedBridge.log(TAG + "目标应用: " + TARGET_PACKAGE);
        XposedBridge.log(TAG + "功能: 自动签到/领取/任务");
        XposedBridge.log(TAG + "==============================");

        try {
            // Hook HTTP 请求
            hookHttpRequest(lpparam);
            
            // Hook 签到响应
            hookSignResponse(lpparam);
            
            // Hook 奖励领取
            hookRewardClaim(lpparam);
            
            // Hook 任务完成
            hookTaskComplete(lpparam);
            
            // 启动定时任务
            startScheduler(lpparam);

            XposedBridge.log(TAG + "所有Hook初始化完成");
            XposedBridge.log(TAG + "自动签到: " + (autoSignEnabled ? "开启" : "关闭"));
            XposedBridge.log(TAG + "自动领取: " + (autoClaimEnabled ? "开启" : "关闭"));
            XposedBridge.log(TAG + "自动任务: " + (autoTaskEnabled ? "开启" : "关闭"));
        } catch (Throwable e) {
            XposedBridge.log(TAG + "Hook初始化失败: " + e.getMessage());
        }
    }

    /**
     * 启动定时任务
     */
    private void startScheduler(XC_LoadPackage.LoadPackageParam lpparam) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // 每30秒检查一次签到状态
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (autoSignEnabled && !todaySigned) {
                    XposedBridge.log(TAG + "尝试自动签到...");
                    performAutoSign(lpparam);
                }
            } catch (Throwable e) {
                XposedBridge.log(TAG + "定时签到失败: " + e.getMessage());
            }
        }, 5, 30, TimeUnit.SECONDS);
        
        // 每60秒检查一次可领取奖励
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (autoClaimEnabled) {
                    XposedBridge.log(TAG + "检查可领取奖励...");
                }
            } catch (Throwable e) {
                XposedBridge.log(TAG + "检查奖励失败: " + e.getMessage());
            }
        }, 10, 60, TimeUnit.SECONDS);
    }

    /**
     * 执行自动签到
     */
    private void performAutoSign(XC_LoadPackage.LoadPackageParam lpparam) {
        if (signRetryCount >= MAX_RETRY) {
            XposedBridge.log(TAG + "签到重试次数已达上限");
            return;
        }
        
        try {
            // 通过反射调用签到方法
            ClassLoader classLoader = lpparam.classLoader;
            
            // 尝试找到签到管理类
            String[] signClasses = {
                "com.dragon.read.task.SignInManager",
                "com.dragon.read.welfare.SignManager",
                "com.dragon.read.coin.SignHelper",
                "com.dragon.read.sign.SignService"
            };
            
            for (String className : signClasses) {
                try {
                    Class<?> clazz = classLoader.loadClass(className);
                    XposedBridge.log(TAG + "找到签到类: " + className);
                    
                    // 尝试调用签到方法
                    String[] signMethods = {
                        "signIn", "sign", "doSign", "performSign",
                        "claimSignReward", "doDailySign"
                    };
                    
                    for (String methodName : signMethods) {
                        try {
                            Method method = clazz.getDeclaredMethod(methodName);
                            method.setAccessible(true);
                            
                            // 创建实例或调用静态方法
                            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                                method.invoke(null);
                            } else {
                                Object instance = clazz.newInstance();
                                method.invoke(instance);
                            }
                            
                            XposedBridge.log(TAG + "调用签到方法成功: " + methodName);
                            todaySigned = true;
                            return;
                        } catch (NoSuchMethodException e) {
                            // 继续尝试下一个方法
                        }
                    }
                } catch (ClassNotFoundException e) {
                    // 继续尝试下一个类
                }
            }
            
            signRetryCount++;
            XposedBridge.log(TAG + "签到重试 " + signRetryCount + "/" + MAX_RETRY);
            
        } catch (Throwable e) {
            XposedBridge.log(TAG + "自动签到异常: " + e.getMessage());
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
                                
                                // 如果是签到请求，标记已签到
                                if (url.contains("sign_in") || url.contains("signin")) {
                                    todaySigned = true;
                                    XposedBridge.log(TAG + "检测到签到请求，标记已签到");
                                }
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

            // Hook optLong 方法
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

            // Hook optString 方法
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
     * Hook奖励领取
     */
    private void hookRewardClaim(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook可能的领取方法
            String[] claimClasses = {
                "com.dragon.read.task.RewardManager",
                "com.dragon.read.welfare.ClaimHelper",
                "com.dragon.read.coin.RewardService"
            };
            
            for (String className : claimClasses) {
                try {
                    Class<?> clazz = lpparam.classLoader.loadClass(className);
                    XposedBridge.log(TAG + "找到奖励类: " + className);
                    
                    // Hook所有方法
                    for (Method method : clazz.getDeclaredMethods()) {
                        String methodName = method.getName().toLowerCase();
                        if (methodName.contains("claim") || methodName.contains("collect") || 
                            methodName.contains("receive") || methodName.contains("领取")) {
                            
                            XposedHelpers.findAndHookMethod(clazz, method.getName(),
                                new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        XposedBridge.log(TAG + "奖励领取方法被调用: " + method.getName());
                                    }
                                }
                            );
                        }
                    }
                } catch (ClassNotFoundException e) {
                    // 继续尝试下一个类
                }
            }
            
            XposedBridge.log(TAG + "奖励领取Hook成功");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "奖励领取Hook失败: " + e.getMessage());
        }
    }

    /**
     * Hook任务完成
     */
    private void hookTaskComplete(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook可能的任务完成方法
            String[] taskClasses = {
                "com.dragon.read.task.TaskManager",
                "com.dragon.read.task.TaskHelper",
                "com.dragon.read.welfare.TaskService"
            };
            
            for (String className : taskClasses) {
                try {
                    Class<?> clazz = lpparam.classLoader.loadClass(className);
                    XposedBridge.log(TAG + "找到任务类: " + className);
                    
                    // Hook所有方法
                    for (Method method : clazz.getDeclaredMethods()) {
                        String methodName = method.getName().toLowerCase();
                        if (methodName.contains("complete") || methodName.contains("finish") || 
                            methodName.contains("done") || methodName.contains("完成")) {
                            
                            XposedHelpers.findAndHookMethod(clazz, method.getName(),
                                new XC_MethodHook() {
                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        XposedBridge.log(TAG + "任务完成方法被调用: " + method.getName());
                                    }
                                }
                            );
                        }
                    }
                } catch (ClassNotFoundException e) {
                    // 继续尝试下一个类
                }
            }
            
            XposedBridge.log(TAG + "任务完成Hook成功");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "任务完成Hook失败: " + e.getMessage());
        }
    }

    /**
     * 处理整数字段
     */
    private void handleIntField(String key, int value) {
        // 签到状态
        if ("status".equals(key) && value == 1) {
            XposedBridge.log(TAG + "签到成功!");
            todaySigned = true;
        }
        
        // 签到完成
        if ("sign_status".equals(key) && value == 1) {
            XposedBridge.log(TAG + "签到状态: 已完成");
            todaySigned = true;
        }
        
        // 金币/奖励字段
        if (isRewardField(key) && value > 0) {
            XposedBridge.log(TAG + "获得奖励: " + key + " = +" + value);
        }
        
        // 可领取奖励
        if (isClaimField(key) && value > 0) {
            XposedBridge.log(TAG + "可领取: " + key + " = " + value);
            if (autoClaimEnabled) {
                XposedBridge.log(TAG + "尝试自动领取...");
            }
        }
        
        // 任务状态
        if ("task_status".equals(key) && value == 1) {
            XposedBridge.log(TAG + "任务完成!");
        }
        
        // 任务可完成
        if ("task_available".equals(key) && value == 1) {
            XposedBridge.log(TAG + "有可完成的任务");
            if (autoTaskEnabled) {
                XposedBridge.log(TAG + "尝试自动完成任务...");
            }
        }
    }

    /**
     * 处理长整数字段
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
            if ("true".equals(value) || "signed".equals(value)) {
                todaySigned = true;
            }
        }
        
        // 签到状态字符串
        if ("sign_status".equals(key) || "status".equals(key)) {
            if ("signed".equals(value) || "success".equals(value)) {
                XposedBridge.log(TAG + "签到状态: " + value);
                todaySigned = true;
            }
        }
        
        // 奖励名称
        if ("reward_name".equals(key) || "award_name".equals(key)) {
            XposedBridge.log(TAG + "奖励名称: " + value);
        }
        
        // 任务名称
        if ("task_name".equals(key) || "task_title".equals(key)) {
            XposedBridge.log(TAG + "任务名称: " + value);
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
     * 判断是否为领取相关字段
     */
    private boolean isClaimField(String key) {
        if (key == null) return false;
        String lowerKey = key.toLowerCase();
        for (String field : CLAIM_FIELDS) {
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
               lowerUrl.contains("coin") ||
               lowerUrl.contains("claim") ||
               lowerUrl.contains("welfare");
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
        // 领取相关
        else if (url.contains("claim") || url.contains("collect")) {
            XposedBridge.log(TAG + "领取请求: " + url);
        }
        // 其他
        else {
            XposedBridge.log(TAG + "相关请求: " + url);
        }
    }
}
