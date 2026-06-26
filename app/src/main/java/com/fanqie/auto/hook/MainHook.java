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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 番茄小说自动签到模块 v12.0.0
 * 直接反射调用 - 尝试直接调用应用内部签到方法
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";
    private static final String MODULE_VERSION = "v12.0.0";

    private ClassLoader appClassLoader;
    private ScheduledExecutorService scheduler;
    
    // 状态
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private int signCount = 0;
    private int taskCount = 0;
    
    // 找到的类和方法
    private Class<?> goldCoinRepoClass = null;
    private Class<?> taskDoneRepoClass = null;
    private Method signMethod = null;
    private Method taskMethod = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "==============================");
        XposedBridge.log(TAG + "番茄小说自动签到模块 " + MODULE_VERSION);
        XposedBridge.log(TAG + "直接反射调用模式");
        XposedBridge.log(TAG + "==============================");

        appClassLoader = lpparam.classLoader;

        try {
            // 查找签到相关类
            findSignClasses(lpparam.classLoader);
            
            // Hook关键方法
            hookKeyMethods(lpparam.classLoader);
            
            // 启动定时任务
            startScheduler();
            
            XposedBridge.log(TAG + "Hook初始化完成");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "初始化失败: " + e.getMessage());
        }
    }

    /**
     * 查找签到相关类
     */
    private void findSignClasses(ClassLoader classLoader) {
        // 查找GoldCoinRepo类
        try {
            goldCoinRepoClass = classLoader.loadClass(
                "com.dragon.read.ug.kmp.readingstatistics.parts.goldcoin.GoldCoinRepo");
            XposedBridge.log(TAG + "找到GoldCoinRepo类");
            
            // 查找签到方法
            for (Method method : goldCoinRepoClass.getDeclaredMethods()) {
                if (method.getName().contains("executeSignDone") || 
                    method.getName().contains("requestGoldCoinSignInDone")) {
                    signMethod = method;
                    XposedBridge.log(TAG + "找到签到方法: " + method.getName());
                    break;
                }
            }
        } catch (Throwable e) {
            XposedBridge.log(TAG + "未找到GoldCoinRepo类: " + e.getMessage());
        }
        
        // 查找TaskDoneDataRepository类
        try {
            taskDoneRepoClass = classLoader.loadClass(
                "com.dragon.read.ug.kmp.common.repository.TaskDoneDataRepository");
            XposedBridge.log(TAG + "找到TaskDoneDataRepository类");
            
            // 查找任务方法
            for (Method method : taskDoneRepoClass.getDeclaredMethods()) {
                if (method.getName().contains("executeTaskDone") || 
                    method.getName().contains("requestTaskDone")) {
                    taskMethod = method;
                    XposedBridge.log(TAG + "找到任务方法: " + method.getName());
                    break;
                }
            }
        } catch (Throwable e) {
            XposedBridge.log(TAG + "未找到TaskDoneDataRepository类: " + e.getMessage());
        }
    }

    /**
     * Hook关键方法
     */
    private void hookKeyMethods(ClassLoader classLoader) {
        // Hook签到方法
        if (signMethod != null) {
            try {
                XposedBridge.hookMethod(signMethod, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log(TAG + "\n[=== 签到方法调用 ===]");
                        XposedBridge.log(TAG + "方法: " + signMethod.getName());
                    }
                    
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.getThrowable() != null) {
                            XposedBridge.log(TAG + "签到失败: " + param.getThrowable().getMessage());
                        } else {
                            XposedBridge.log(TAG + "签到成功!");
                            signCount++;
                        }
                        XposedBridge.log(TAG + "===================\n");
                    }
                });
                XposedBridge.log(TAG + "Hook签到方法成功");
            } catch (Throwable e) {
                XposedBridge.log(TAG + "Hook签到方法失败: " + e.getMessage());
            }
        }
        
        // Hook任务方法
        if (taskMethod != null) {
            try {
                XposedBridge.hookMethod(taskMethod, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log(TAG + "\n[=== 任务方法调用 ===]");
                        XposedBridge.log(TAG + "方法: " + taskMethod.getName());
                    }
                    
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.getThrowable() != null) {
                            XposedBridge.log(TAG + "任务失败: " + param.getThrowable().getMessage());
                        } else {
                            XposedBridge.log(TAG + "任务完成!");
                            taskCount++;
                        }
                        XposedBridge.log(TAG + "===================\n");
                    }
                });
                XposedBridge.log(TAG + "Hook任务方法成功");
            } catch (Throwable e) {
                XposedBridge.log(TAG + "Hook任务方法失败: " + e.getMessage());
            }
        }
    }

    /**
     * 启动定时任务
     */
    private void startScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // 每30秒尝试签到
        scheduler.scheduleAtFixedRate(() -> {
            try {
                XposedBridge.log(TAG + "状态: 签到=" + signCount + " 任务=" + taskCount);
                
                // 尝试调用签到方法
                if (signMethod != null && !isProcessing.get()) {
                    isProcessing.set(true);
                    tryAutoSign();
                    isProcessing.set(false);
                }
            } catch (Throwable e) {
                XposedBridge.log(TAG + "定时任务异常: " + e.getMessage());
                isProcessing.set(false);
            }
        }, 5, 30, TimeUnit.SECONDS);
    }

    /**
     * 尝试自动签到
     */
    private void tryAutoSign() {
        if (goldCoinRepoClass == null || signMethod == null) {
            XposedBridge.log(TAG + "签到类或方法未找到");
            return;
        }
        
        XposedBridge.log(TAG + "尝试自动签到...");
        
        try {
            // 尝试获取单例实例
            Object instance = getInstance(goldCoinRepoClass);
            if (instance != null) {
                signMethod.setAccessible(true);
                signMethod.invoke(instance);
                XposedBridge.log(TAG + "调用签到方法成功");
            } else {
                XposedBridge.log(TAG + "无法获取GoldCoinRepo实例");
            }
        } catch (Throwable e) {
            XposedBridge.log(TAG + "签到异常: " + e.getMessage());
        }
    }

    /**
     * 获取类的单例实例
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
            
            return null;
        } catch (Throwable e) {
            return null;
        }
    }
}
