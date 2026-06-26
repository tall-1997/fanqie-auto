package com.fanqie.auto.hook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 番茄小说自动签到模块 v6.0.0
 * 深度Hook - 监控所有签到、任务、奖励相关方法
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";
    private static final String MODULE_VERSION = "v6.0.0";

    private ClassLoader appClassLoader;
    private ScheduledExecutorService scheduler;
    
    // 记录找到的方法
    private List<String> foundMethods = new ArrayList<>();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "==============================");
        XposedBridge.log(TAG + "番茄小说自动签到模块 " + MODULE_VERSION);
        XposedBridge.log(TAG + "深度Hook模式");
        XposedBridge.log(TAG + "==============================");

        appClassLoader = lpparam.classLoader;

        try {
            // 深度扫描并Hook所有相关类
            deepScanAndHook(lpparam.classLoader);
            
            // 启动定时任务
            startScheduler();
            
            XposedBridge.log(TAG + "Hook初始化完成");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "初始化失败: " + e.getMessage());
        }
    }

    /**
     * 深度扫描并Hook所有相关类
     */
    private void deepScanAndHook(ClassLoader classLoader) {
        // 签到相关类名模式
        String[] signClassPatterns = {
            "com.dragon.read.ug.kmp.readingstatistics.parts.goldcoin.GoldCoinRepo",
            "com.dragon.read.ug.kmp.longsignin.viewmodel.LongSignInKlayViewModel",
            "com.dragon.read.ug.kmp.newusersignin.viewmodel.NewUserSevenDaySignInViewModel",
            "com.dragon.read.ug.kmp.alarmclock.repository.AlarmClockRepository",
            "com.dragon.read.ug.kmp.common.repository.TaskDoneDataRepository",
            "com.dragon.read.ug.kmp.treasurebox.repository.TreasureTaskDoneDataRepository",
            "com.dragon.read.ug.kmp.dividegoldcoin.viewmodel.SecondFloorDivideGoldCoinViewModel"
        };
        
        // 尝试Hook每个类
        for (String className : signClassPatterns) {
            try {
                Class<?> clazz = classLoader.loadClass(className);
                hookClassMethods(clazz, className);
                XposedBridge.log(TAG + "已Hook类: " + className);
            } catch (Throwable e) {
                XposedBridge.log(TAG + "未找到类: " + className);
            }
        }
    }

    /**
     * Hook类的所有相关方法
     */
    private void hookClassMethods(Class<?> clazz, String className) {
        Method[] methods = clazz.getDeclaredMethods();
        
        for (Method method : methods) {
            String methodName = method.getName();
            
            // 检查是否是相关方法
            if (isRelevantMethod(methodName)) {
                try {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + "\n[=== 方法调用 ===]");
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
                            }
                            XposedBridge.log(TAG + "===================\n");
                        }
                    });
                    
                    foundMethods.add(className + "." + methodName);
                    XposedBridge.log(TAG + "Hook方法: " + methodName);
                } catch (Throwable e) {
                    // 忽略
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
        
        // 每60秒输出状态
        scheduler.scheduleAtFixedRate(() -> {
            try {
                XposedBridge.log(TAG + "状态: 已找到 " + foundMethods.size() + " 个相关方法");
                
                // 尝试调用找到的方法
                tryInvokeMethods();
            } catch (Throwable e) {
                XposedBridge.log(TAG + "定时任务异常: " + e.getMessage());
            }
        }, 10, 60, TimeUnit.SECONDS);
    }

    /**
     * 尝试调用找到的方法
     */
    private void tryInvokeMethods() {
        if (appClassLoader == null) return;
        
        // 尝试调用签到方法
        try {
            Class<?> goldCoinRepoClass = appClassLoader.loadClass(
                "com.dragon.read.ug.kmp.readingstatistics.parts.goldcoin.GoldCoinRepo");
            
            // 尝试获取实例
            Object instance = getInstance(goldCoinRepoClass);
            if (instance != null) {
                // 尝试调用方法
                for (Method method : goldCoinRepoClass.getDeclaredMethods()) {
                    if (method.getName().contains("SignDone") || method.getName().contains("SignIn")) {
                        try {
                            method.setAccessible(true);
                            XposedBridge.log(TAG + "尝试调用: " + method.getName());
                            method.invoke(instance);
                            XposedBridge.log(TAG + "调用成功: " + method.getName());
                        } catch (Throwable e) {
                            XposedBridge.log(TAG + "调用失败: " + method.getName() + " - " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Throwable e) {
            // 忽略
        }
    }

    /**
     * 获取类的实例
     */
    private Object getInstance(Class<?> clazz) {
        try {
            // 尝试INSTANCE字段
            try {
                var field = clazz.getDeclaredField("INSTANCE");
                field.setAccessible(true);
                return field.get(null);
            } catch (NoSuchFieldException e) {
                // 没有INSTANCE字段
            }
            
            // 尝试Companion
            try {
                var companionField = clazz.getDeclaredField("Companion");
                companionField.setAccessible(true);
                return companionField.get(null);
            } catch (NoSuchFieldException e) {
                // 没有Companion字段
            }
            
            // 尝试newInstance
            return clazz.newInstance();
        } catch (Throwable e) {
            return null;
        }
    }
}
