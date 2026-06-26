package com.fanqie.auto.hook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 番茄小说自动签到模块 v3.0.0
 * 功能：自动签到、自动领取奖励、自动完成任务
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";
    private static final String MODULE_VERSION = "v3.0.0";

    // 定时任务
    private ScheduledExecutorService scheduler;
    
    // 状态记录
    private static boolean todaySigned = false;
    private static int completedTasks = 0;
    private static int claimedRewards = 0;
    
    // 日志回调
    private static LogCallback logCallback;
    
    public interface LogCallback {
        void onLog(String message);
    }
    
    public static void setLogCallback(LogCallback callback) {
        logCallback = callback;
    }
    
    private static void log(String message) {
        XposedBridge.log(TAG + message);
        if (logCallback != null) {
            logCallback.onLog(message);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        log("==============================");
        log("番茄小说自动签到模块 " + MODULE_VERSION);
        log("目标应用: " + TARGET_PACKAGE);
        log("==============================");

        try {
            ClassLoader classLoader = lpparam.classLoader;
            
            // Hook签到相关
            hookSignIn(classLoader);
            
            // Hook任务完成
            hookTaskDone(classLoader);
            
            // Hook奖励领取
            hookRewardClaim(classLoader);
            
            // Hook HTTP请求监控
            hookHttpRequest(classLoader);
            
            // 启动定时任务
            startScheduler(classLoader);
            
            log("所有Hook初始化完成");
            log("自动功能已启动");
        } catch (Throwable e) {
            log("Hook初始化失败: " + e.getMessage());
        }
    }

    /**
     * Hook签到相关类
     */
    private void hookSignIn(ClassLoader classLoader) {
        try {
            // Hook GoldCoinRepo 的签到方法
            Class<?> goldCoinRepoClass = classLoader.loadClass(
                "com.dragon.read.ug.kmp.readingstatistics.parts.goldcoin.GoldCoinRepo");
            
            // Hook executeSignDoneRequest
            for (Method method : goldCoinRepoClass.getDeclaredMethods()) {
                if (method.getName().contains("SignDone") || method.getName().contains("SignIn")) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            log("签到方法被调用: " + method.getName());
                            todaySigned = true;
                        }
                    });
                    log("Hook签到方法: " + method.getName());
                }
            }
            
            // Hook LongSignInKlayViewModel
            try {
                Class<?> longSignInClass = classLoader.loadClass(
                    "com.dragon.read.ug.kmp.longsignin.viewmodel.LongSignInKlayViewModel");
                
                for (Method method : longSignInClass.getDeclaredMethods()) {
                    if (method.getName().equals("doSignIn")) {
                        XposedBridge.hookMethod(method, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                log("长期签到被调用");
                                todaySigned = true;
                            }
                        });
                        log("Hook长期签到方法");
                    }
                }
            } catch (Throwable e) {
                // 忽略
            }
            
            log("签到Hook完成");
        } catch (Throwable e) {
            log("签到Hook失败: " + e.getMessage());
        }
    }

    /**
     * Hook任务完成相关类
     */
    private void hookTaskDone(ClassLoader classLoader) {
        try {
            // Hook TaskDoneDataRepository
            Class<?> taskDoneRepoClass = classLoader.loadClass(
                "com.dragon.read.ug.kmp.common.repository.TaskDoneDataRepository");
            
            for (Method method : taskDoneRepoClass.getDeclaredMethods()) {
                if (method.getName().contains("TaskDone") || method.getName().contains("executeTask")) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            log("任务完成方法被调用: " + method.getName());
                            completedTasks++;
                        }
                    });
                    log("Hook任务方法: " + method.getName());
                }
            }
            
            // Hook SecondFloorDivideGoldCoinViewModel
            try {
                Class<?> divideGoldCoinClass = classLoader.loadClass(
                    "com.dragon.read.ug.kmp.dividegoldcoin.viewmodel.SecondFloorDivideGoldCoinViewModel");
                
                for (Method method : divideGoldCoinClass.getDeclaredMethods()) {
                    if (method.getName().contains("doDoneTask") || method.getName().contains("Task")) {
                        XposedBridge.hookMethod(method, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                log("金币任务被调用: " + method.getName());
                                completedTasks++;
                            }
                        });
                        log("Hook金币任务方法: " + method.getName());
                    }
                }
            } catch (Throwable e) {
                // 忽略
            }
            
            log("任务Hook完成");
        } catch (Throwable e) {
            log("任务Hook失败: " + e.getMessage());
        }
    }

    /**
     * Hook奖励领取
     */
    private void hookRewardClaim(ClassLoader classLoader) {
        try {
            // Hook GoldCoinViewModel
            Class<?> goldCoinViewModelClass = classLoader.loadClass(
                "com.dragon.read.ug.kmp.readingstatistics.parts.goldcoin.GoldCoinViewModel");
            
            for (Method method : goldCoinViewModelClass.getDeclaredMethods()) {
                if (method.getName().contains("Reward") || method.getName().contains("Claim")) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            log("奖励领取被调用: " + method.getName());
                            claimedRewards++;
                        }
                    });
                    log("Hook奖励方法: " + method.getName());
                }
            }
            
            log("奖励Hook完成");
        } catch (Throwable e) {
            log("奖励Hook失败: " + e.getMessage());
        }
    }

    /**
     * Hook HTTP请求监控
     */
    private void hookHttpRequest(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "okhttp3.Request$Builder",
                classLoader,
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
                            // 忽略
                        }
                    }
                }
            );
            log("HTTP请求Hook成功");
        } catch (Throwable e) {
            log("HTTP请求Hook失败: " + e.getMessage());
        }
    }

    /**
     * 启动定时任务
     */
    private void startScheduler(ClassLoader classLoader) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // 每60秒尝试自动签到
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (!todaySigned) {
                    log("尝试自动签到...");
                    tryAutoSignIn(classLoader);
                }
            } catch (Throwable e) {
                log("自动签到失败: " + e.getMessage());
            }
        }, 10, 60, TimeUnit.SECONDS);
        
        // 每120秒尝试自动完成任务
        scheduler.scheduleAtFixedRate(() -> {
            try {
                log("尝试自动完成任务...");
                tryAutoCompleteTask(classLoader);
            } catch (Throwable e) {
                log("自动任务失败: " + e.getMessage());
            }
        }, 30, 120, TimeUnit.SECONDS);
        
        // 每180秒尝试自动领取奖励
        scheduler.scheduleAtFixedRate(() -> {
            try {
                log("尝试自动领取奖励...");
                tryAutoClaimReward(classLoader);
            } catch (Throwable e) {
                log("自动领取失败: " + e.getMessage());
            }
        }, 60, 180, TimeUnit.SECONDS);
    }

    /**
     * 尝试自动签到
     */
    private void tryAutoSignIn(ClassLoader classLoader) {
        try {
            // 尝试调用GoldCoinRepo的签到方法
            Class<?> goldCoinRepoClass = classLoader.loadClass(
                "com.dragon.read.ug.kmp.readingstatistics.parts.goldcoin.GoldCoinRepo");
            
            // 获取实例
            Object instance = null;
            for (Field field : goldCoinRepoClass.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    instance = field.get(null);
                    if (instance != null) break;
                }
            }
            
            if (instance != null) {
                for (Method method : goldCoinRepoClass.getDeclaredMethods()) {
                    if (method.getName().contains("executeSignDoneRequest") || 
                        method.getName().contains("requestGoldCoinSignInDone")) {
                        method.setAccessible(true);
                        method.invoke(instance);
                        log("调用签到方法成功: " + method.getName());
                        todaySigned = true;
                        return;
                    }
                }
            }
            
            log("未找到签到方法实例");
        } catch (Throwable e) {
            log("自动签到异常: " + e.getMessage());
        }
    }

    /**
     * 尝试自动完成任务
     */
    private void tryAutoCompleteTask(ClassLoader classLoader) {
        try {
            Class<?> taskDoneRepoClass = classLoader.loadClass(
                "com.dragon.read.ug.kmp.common.repository.TaskDoneDataRepository");
            
            // 获取实例
            Object instance = null;
            for (Field field : taskDoneRepoClass.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    instance = field.get(null);
                    if (instance != null) break;
                }
            }
            
            if (instance != null) {
                for (Method method : taskDoneRepoClass.getDeclaredMethods()) {
                    if (method.getName().contains("executeTaskDoneRequest") || 
                        method.getName().contains("requestTaskDone")) {
                        method.setAccessible(true);
                        method.invoke(instance);
                        log("调用任务方法成功: " + method.getName());
                        return;
                    }
                }
            }
            
            log("未找到任务方法实例");
        } catch (Throwable e) {
            log("自动任务异常: " + e.getMessage());
        }
    }

    /**
     * 尝试自动领取奖励
     */
    private void tryAutoClaimReward(ClassLoader classLoader) {
        try {
            Class<?> goldCoinViewModelClass = classLoader.loadClass(
                "com.dragon.read.ug.kmp.readingstatistics.parts.goldcoin.GoldCoinViewModel");
            
            // 获取实例
            Object instance = null;
            for (Field field : goldCoinViewModelClass.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    instance = field.get(null);
                    if (instance != null) break;
                }
            }
            
            if (instance != null) {
                for (Method method : goldCoinViewModelClass.getDeclaredMethods()) {
                    if (method.getName().contains("claimReward") || 
                        method.getName().contains("getReward")) {
                        method.setAccessible(true);
                        method.invoke(instance);
                        log("调用领取方法成功: " + method.getName());
                        claimedRewards++;
                        return;
                    }
                }
            }
            
            log("未找到领取方法实例");
        } catch (Throwable e) {
            log("自动领取异常: " + e.getMessage());
        }
    }

    /**
     * 判断是否为重要URL
     */
    private boolean isImportantUrl(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("luckycat") ||
               lowerUrl.contains("sign_in") ||
               lowerUrl.contains("task/done") ||
               lowerUrl.contains("reward") ||
               lowerUrl.contains("claim");
    }

    /**
     * 记录API请求
     */
    private void logApiRequest(String url) {
        if (url.contains("sign_in")) {
            log("签到请求: " + url);
        } else if (url.contains("task/done")) {
            log("任务请求: " + url);
        } else if (url.contains("reward") || url.contains("claim")) {
            log("奖励请求: " + url);
        }
    }

    /**
     * 获取状态信息
     */
    public static String getStatus() {
        return String.format("签到: %s | 任务: %d | 奖励: %d", 
            todaySigned ? "已完成" : "未完成", completedTasks, claimedRewards);
    }
}
