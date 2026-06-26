package com.fanqie.auto.hook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 番茄小说自动签到模块 v4.0.0
 * 核心功能：自动签到、自动任务、自动领取奖励
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";
    private static final String MODULE_VERSION = "v4.0.0";

    private ScheduledExecutorService scheduler;
    private ClassLoader appClassLoader;
    
    // 统计
    private int signCount = 0;
    private int taskCount = 0;
    private int rewardCount = 0;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "==============================");
        XposedBridge.log(TAG + "番茄小说自动签到模块 " + MODULE_VERSION);
        XposedBridge.log(TAG + "目标应用: " + TARGET_PACKAGE);
        XposedBridge.log(TAG + "==============================");

        appClassLoader = lpparam.classLoader;

        try {
            // Hook关键类
            hookGoldCoinRepo(lpparam.classLoader);
            hookTaskDoneRepository(lpparam.classLoader);
            hookTreasureTaskRepository(lpparam.classLoader);
            
            // 启动定时任务
            startScheduler();
            
            XposedBridge.log(TAG + "所有Hook初始化完成");
            XposedBridge.log(TAG + "自动功能已启动");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "初始化失败: " + e.getMessage());
        }
    }

    /**
     * Hook GoldCoinRepo - 签到相关
     */
    private void hookGoldCoinRepo(ClassLoader classLoader) {
        try {
            Class<?> repoClass = classLoader.loadClass(
                "com.dragon.read.ug.kmp.readingstatistics.parts.goldcoin.GoldCoinRepo");
            
            // Hook executeSignDoneRequest
            for (Method method : repoClass.getDeclaredMethods()) {
                if (method.getName().equals("executeSignDoneRequest") || 
                    method.getName().equals("requestGoldCoinSignInDone")) {
                    
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + "调用签到方法: " + method.getName());
                        }
                        
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.getThrowable() == null) {
                                XposedBridge.log(TAG + "签到成功!");
                                signCount++;
                            } else {
                                XposedBridge.log(TAG + "签到失败: " + param.getThrowable().getMessage());
                            }
                        }
                    });
                    
                    XposedBridge.log(TAG + "Hook签到方法: " + method.getName());
                }
            }
        } catch (Throwable e) {
            XposedBridge.log(TAG + "Hook GoldCoinRepo失败: " + e.getMessage());
        }
    }

    /**
     * Hook TaskDoneDataRepository - 任务相关
     */
    private void hookTaskDoneRepository(ClassLoader classLoader) {
        try {
            Class<?> repoClass = classLoader.loadClass(
                "com.dragon.read.ug.kmp.common.repository.TaskDoneDataRepository");
            
            for (Method method : repoClass.getDeclaredMethods()) {
                if (method.getName().equals("executeTaskDoneRequest") || 
                    method.getName().equals("requestTaskDone") ||
                    method.getName().equals("requestTaskDoneData")) {
                    
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + "调用任务方法: " + method.getName());
                        }
                        
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.getThrowable() == null) {
                                XposedBridge.log(TAG + "任务完成!");
                                taskCount++;
                            } else {
                                XposedBridge.log(TAG + "任务失败: " + param.getThrowable().getMessage());
                            }
                        }
                    });
                    
                    XposedBridge.log(TAG + "Hook任务方法: " + method.getName());
                }
            }
        } catch (Throwable e) {
            XposedBridge.log(TAG + "Hook TaskDoneDataRepository失败: " + e.getMessage());
        }
    }

    /**
     * Hook TreasureTaskDoneDataRepository - 宝箱任务相关
     */
    private void hookTreasureTaskRepository(ClassLoader classLoader) {
        try {
            Class<?> repoClass = classLoader.loadClass(
                "com.dragon.read.ug.kmp.treasurebox.repository.TreasureTaskDoneDataRepository");
            
            for (Method method : repoClass.getDeclaredMethods()) {
                if (method.getName().equals("requestTreasureTaskDone") || 
                    method.getName().equals("requestTreasureTaskDoneWithMetrics")) {
                    
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + "调用宝箱方法: " + method.getName());
                        }
                        
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.getThrowable() == null) {
                                XposedBridge.log(TAG + "宝箱任务完成!");
                                rewardCount++;
                            } else {
                                XposedBridge.log(TAG + "宝箱任务失败: " + param.getThrowable().getMessage());
                            }
                        }
                    });
                    
                    XposedBridge.log(TAG + "Hook宝箱方法: " + method.getName());
                }
            }
        } catch (Throwable e) {
            XposedBridge.log(TAG + "Hook TreasureTaskDoneDataRepository失败: " + e.getMessage());
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
                tryAutoSignIn();
            } catch (Throwable e) {
                XposedBridge.log(TAG + "自动签到异常: " + e.getMessage());
            }
        }, 5, 30, TimeUnit.SECONDS);
        
        // 每60秒尝试完成任务
        scheduler.scheduleAtFixedRate(() -> {
            try {
                tryAutoTask();
            } catch (Throwable e) {
                XposedBridge.log(TAG + "自动任务异常: " + e.getMessage());
            }
        }, 15, 60, TimeUnit.SECONDS);
        
        // 每90秒尝试领取奖励
        scheduler.scheduleAtFixedRate(() -> {
            try {
                tryAutoReward();
            } catch (Throwable e) {
                XposedBridge.log(TAG + "自动奖励异常: " + e.getMessage());
            }
        }, 30, 90, TimeUnit.SECONDS);
        
        XposedBridge.log(TAG + "定时任务已启动");
    }

    /**
     * 尝试自动签到
     */
    private void tryAutoSignIn() {
        if (appClassLoader == null) return;
        
        try {
            Class<?> repoClass = appClassLoader.loadClass(
                "com.dragon.read.ug.kmp.readingstatistics.parts.goldcoin.GoldCoinRepo");
            
            // 尝试获取单例实例
            Object instance = getSingletonInstance(repoClass);
            if (instance == null) {
                XposedBridge.log(TAG + "未找到GoldCoinRepo实例");
                return;
            }
            
            // 调用executeSignDoneRequest
            for (Method method : repoClass.getDeclaredMethods()) {
                if (method.getName().equals("executeSignDoneRequest")) {
                    method.setAccessible(true);
                    XposedBridge.log(TAG + "执行签到...");
                    method.invoke(instance);
                    return;
                }
            }
            
            XposedBridge.log(TAG + "未找到executeSignDoneRequest方法");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "签到异常: " + e.getMessage());
        }
    }

    /**
     * 尝试自动完成任务
     */
    private void tryAutoTask() {
        if (appClassLoader == null) return;
        
        try {
            Class<?> repoClass = appClassLoader.loadClass(
                "com.dragon.read.ug.kmp.common.repository.TaskDoneDataRepository");
            
            Object instance = getSingletonInstance(repoClass);
            if (instance == null) {
                XposedBridge.log(TAG + "未找到TaskDoneDataRepository实例");
                return;
            }
            
            // 调用executeTaskDoneRequest
            for (Method method : repoClass.getDeclaredMethods()) {
                if (method.getName().equals("executeTaskDoneRequest")) {
                    method.setAccessible(true);
                    XposedBridge.log(TAG + "执行任务...");
                    method.invoke(instance);
                    return;
                }
            }
            
            XposedBridge.log(TAG + "未找到executeTaskDoneRequest方法");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "任务异常: " + e.getMessage());
        }
    }

    /**
     * 尝试自动领取奖励
     */
    private void tryAutoReward() {
        if (appClassLoader == null) return;
        
        try {
            Class<?> repoClass = appClassLoader.loadClass(
                "com.dragon.read.ug.kmp.treasurebox.repository.TreasureTaskDoneDataRepository");
            
            Object instance = getSingletonInstance(repoClass);
            if (instance == null) {
                XposedBridge.log(TAG + "未找到TreasureTaskDoneDataRepository实例");
                return;
            }
            
            // 调用requestTreasureTaskDone
            for (Method method : repoClass.getDeclaredMethods()) {
                if (method.getName().equals("requestTreasureTaskDone")) {
                    method.setAccessible(true);
                    XposedBridge.log(TAG + "执行宝箱任务...");
                    method.invoke(instance);
                    return;
                }
            }
            
            XposedBridge.log(TAG + "未找到requestTreasureTaskDone方法");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "奖励异常: " + e.getMessage());
        }
    }

    /**
     * 获取单例实例
     */
    private Object getSingletonInstance(Class<?> clazz) {
        try {
            // 尝试通过Companion获取
            Field companionField = null;
            try {
                companionField = clazz.getDeclaredField("Companion");
                companionField.setAccessible(true);
                Object companion = companionField.get(null);
                if (companion != null) {
                    // 尝试getInstance方法
                    for (Method method : companion.getClass().getDeclaredMethods()) {
                        if (method.getName().equals("getInstance") || 
                            method.getName().equals("get") ||
                            method.getName().equals("instance")) {
                            method.setAccessible(true);
                            return method.invoke(companion);
                        }
                    }
                    return companion;
                }
            } catch (NoSuchFieldException e) {
                // 没有Companion字段
            }
            
            // 尝试INSTANCE字段
            try {
                Field instanceField = clazz.getDeclaredField("INSTANCE");
                instanceField.setAccessible(true);
                return instanceField.get(null);
            } catch (NoSuchFieldException e) {
                // 没有INSTANCE字段
            }
            
            // 尝试单例模式
            for (Field field : clazz.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && 
                    java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                    field.setAccessible(true);
                    Object value = field.get(null);
                    if (value != null && clazz.isInstance(value)) {
                        return value;
                    }
                }
            }
            
            // 尝试newInstance
            return clazz.newInstance();
        } catch (Throwable e) {
            XposedBridge.log(TAG + "获取实例失败: " + e.getMessage());
            return null;
        }
    }
}
