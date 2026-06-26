package com.fanqie.auto.hook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 番茄小说自动签到模块 v8.0.0
 * 安全Hook模式 - 不实例化类，只监控方法调用
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";
    private static final String MODULE_VERSION = "v8.0.0";

    private ClassLoader appClassLoader;
    private ScheduledExecutorService scheduler;
    
    // 状态
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    
    // 统计
    private int signCount = 0;
    private int taskCount = 0;
    private int rewardCount = 0;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "==============================");
        XposedBridge.log(TAG + "番茄小说自动签到模块 " + MODULE_VERSION);
        XposedBridge.log(TAG + "安全Hook模式");
        XposedBridge.log(TAG + "==============================");

        appClassLoader = lpparam.classLoader;

        try {
            // Hook关键方法（不实例化类）
            hookKeyMethods(lpparam.classLoader);
            
            // 启动定时任务
            startScheduler();
            
            XposedBridge.log(TAG + "Hook初始化完成");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "初始化失败: " + e.getMessage());
        }
    }

    /**
     * Hook关键方法（不实例化类）
     */
    private void hookKeyMethods(ClassLoader classLoader) {
        // 签到相关类
        String[] signClasses = {
            "com.dragon.read.ug.kmp.readingstatistics.parts.goldcoin.GoldCoinRepo",
            "com.dragon.read.ug.kmp.longsignin.viewmodel.LongSignInKlayViewModel",
            "com.dragon.read.ug.kmp.newusersignin.viewmodel.NewUserSevenDaySignInViewModel",
            "com.dragon.read.ug.kmp.alarmclock.repository.AlarmClockRepository"
        };
        
        // 任务相关类
        String[] taskClasses = {
            "com.dragon.read.ug.kmp.common.repository.TaskDoneDataRepository",
            "com.dragon.read.ug.kmp.treasurebox.repository.TreasureTaskDoneDataRepository"
        };
        
        // Hook签到类
        for (String className : signClasses) {
            try {
                Class<?> clazz = classLoader.loadClass(className);
                hookClassSafely(clazz, className, "签到");
            } catch (Throwable e) {
                XposedBridge.log(TAG + "未找到签到类: " + className);
            }
        }
        
        // Hook任务类
        for (String className : taskClasses) {
            try {
                Class<?> clazz = classLoader.loadClass(className);
                hookClassSafely(clazz, className, "任务");
            } catch (Throwable e) {
                XposedBridge.log(TAG + "未找到任务类: " + className);
            }
        }
    }

    /**
     * 安全地Hook类（不实例化）
     */
    private void hookClassSafely(Class<?> clazz, String className, String type) {
        XposedBridge.log(TAG + "已Hook" + type + "类: " + className);
        
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            
            // 检查是否是相关方法
            if (isRelevantMethod(methodName)) {
                try {
                    // 只Hook方法，不实例化类
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + "\n[=== " + type + "方法调用 ===]");
                            XposedBridge.log(TAG + "类: " + className);
                            XposedBridge.log(TAG + "方法: " + methodName);
                            
                            // 记录参数
                            if (param.args != null && param.args.length > 0) {
                                for (int i = 0; i < param.args.length; i++) {
                                    XposedBridge.log(TAG + "参数[" + i + "]: " + param.args[i]);
                                }
                            }
                        }
                        
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.getThrowable() != null) {
                                XposedBridge.log(TAG + "异常: " + param.getThrowable().getMessage());
                            } else {
                                XposedBridge.log(TAG + "返回: " + param.getResult());
                                
                                // 统计
                                if (type.equals("签到")) {
                                    signCount++;
                                } else if (type.equals("任务")) {
                                    taskCount++;
                                }
                            }
                            XposedBridge.log(TAG + "===================\n");
                        }
                    });
                    
                    XposedBridge.log(TAG + "Hook方法: " + methodName);
                } catch (Throwable e) {
                    // 忽略Hook失败
                }
            }
        }
    }

    /**
     * 检查是否是相关方法
     */
    private boolean isRelevantMethod(String methodName) {
        String lower = methodName.toLowerCase();
        
        // 签到相关
        if (lower.contains("sign") || lower.contains("signin")) {
            return true;
        }
        
        // 任务相关
        if (lower.contains("task") || lower.contains("done") || lower.contains("complete")) {
            return true;
        }
        
        // 奖励相关
        if (lower.contains("reward") || lower.contains("claim") || lower.contains("coin")) {
            return true;
        }
        
        // 执行相关
        if (lower.contains("execute") || lower.contains("request") || lower.contains("fetch")) {
            return true;
        }
        
        return false;
    }

    /**
     * 启动定时任务
     */
    private void startScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // 每30秒输出状态
        scheduler.scheduleAtFixedRate(() -> {
            try {
                XposedBridge.log(TAG + "状态: 签到=" + signCount + " 任务=" + taskCount + " 奖励=" + rewardCount);
            } catch (Throwable e) {
                // 忽略
            }
        }, 5, 30, TimeUnit.SECONDS);
    }
}
