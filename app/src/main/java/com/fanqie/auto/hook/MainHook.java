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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 番茄小说自动签到模块 v7.0.0
 * 深度Hook + 自动执行
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";
    private static final String MODULE_VERSION = "v7.0.0";

    private ClassLoader appClassLoader;
    private ScheduledExecutorService scheduler;
    
    // 找到的类和方法
    private Object goldCoinRepoInstance = null;
    private Object taskDoneRepoInstance = null;
    private Object treasureTaskRepoInstance = null;
    
    // 签到方法
    private Method signDoneMethod = null;
    private Method signInMethod = null;
    
    // 任务方法
    private Method taskDoneMethod = null;
    
    // 状态
    private AtomicBoolean isSigning = new AtomicBoolean(false);
    private AtomicBoolean isTasking = new AtomicBoolean(false);
    
    // 统计
    private int signCount = 0;
    private int taskCount = 0;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "==============================");
        XposedBridge.log(TAG + "番茄小说自动签到模块 " + MODULE_VERSION);
        XposedBridge.log(TAG + "深度Hook + 自动执行");
        XposedBridge.log(TAG + "==============================");

        appClassLoader = lpparam.classLoader;

        try {
            // 深度扫描并Hook
            deepScanAndHook(lpparam.classLoader);
            
            // 启动定时任务
            startScheduler();
            
            XposedBridge.log(TAG + "Hook初始化完成");
            XposedBridge.log(TAG + "自动执行已启动");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "初始化失败: " + e.getMessage());
        }
    }

    /**
     * 深度扫描并Hook
     */
    private void deepScanAndHook(ClassLoader classLoader) {
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
                hookSignClass(clazz, className);
            } catch (Throwable e) {
                XposedBridge.log(TAG + "未找到签到类: " + className);
            }
        }
        
        // Hook任务类
        for (String className : taskClasses) {
            try {
                Class<?> clazz = classLoader.loadClass(className);
                hookTaskClass(clazz, className);
            } catch (Throwable e) {
                XposedBridge.log(TAG + "未找到任务类: " + className);
            }
        }
    }

    /**
     * Hook签到类
     */
    private void hookSignClass(Class<?> clazz, String className) {
        XposedBridge.log(TAG + "已Hook签到类: " + className);
        
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            
            // 签到相关方法
            if (methodName.contains("executeSignDone") || methodName.contains("requestGoldCoinSignInDone") ||
                methodName.contains("doSignIn") || methodName.contains("signIn")) {
                
                try {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + "\n[=== 签到方法调用 ===]");
                            XposedBridge.log(TAG + "类: " + className);
                            XposedBridge.log(TAG + "方法: " + methodName);
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
                    
                    // 记录方法用于自动调用
                    if (methodName.contains("executeSignDone") || methodName.contains("requestGoldCoinSignInDone")) {
                        signDoneMethod = method;
                        XposedBridge.log(TAG + "记录签到方法: " + methodName);
                    }
                    
                } catch (Throwable e) {
                    // 忽略
                }
            }
        }
        
        // 尝试获取实例
        try {
            Object instance = getInstance(clazz);
            if (instance != null) {
                if (className.contains("GoldCoinRepo")) {
                    goldCoinRepoInstance = instance;
                    XposedBridge.log(TAG + "获取GoldCoinRepo实例成功");
                }
            }
        } catch (Throwable e) {
            // 忽略
        }
    }

    /**
     * Hook任务类
     */
    private void hookTaskClass(Class<?> clazz, String className) {
        XposedBridge.log(TAG + "已Hook任务类: " + className);
        
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            
            // 任务相关方法
            if (methodName.contains("executeTaskDone") || methodName.contains("requestTaskDone") ||
                methodName.contains("requestTreasureTaskDone")) {
                
                try {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + "\n[=== 任务方法调用 ===]");
                            XposedBridge.log(TAG + "类: " + className);
                            XposedBridge.log(TAG + "方法: " + methodName);
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
                    
                    // 记录方法用于自动调用
                    if (methodName.contains("executeTaskDone") || methodName.contains("requestTaskDone")) {
                        taskDoneMethod = method;
                        XposedBridge.log(TAG + "记录任务方法: " + methodName);
                    }
                    
                } catch (Throwable e) {
                    // 忽略
                }
            }
        }
        
        // 尝试获取实例
        try {
            Object instance = getInstance(clazz);
            if (instance != null) {
                if (className.contains("TaskDoneDataRepository")) {
                    taskDoneRepoInstance = instance;
                    XposedBridge.log(TAG + "获取TaskDoneDataRepository实例成功");
                } else if (className.contains("TreasureTaskDoneDataRepository")) {
                    treasureTaskRepoInstance = instance;
                    XposedBridge.log(TAG + "获取TreasureTaskDoneDataRepository实例成功");
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
                Object companion = companionField.get(null);
                if (companion != null) {
                    // 尝试getInstance方法
                    for (Method method : companion.getClass().getDeclaredMethods()) {
                        if (method.getName().equals("getInstance") || method.getName().equals("get") ||
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
            
            // 尝试newInstance
            return clazz.newInstance();
        } catch (Throwable e) {
            return null;
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
                if (!isSigning.get()) {
                    isSigning.set(true);
                    tryAutoSignIn();
                    isSigning.set(false);
                }
            } catch (Throwable e) {
                XposedBridge.log(TAG + "自动签到异常: " + e.getMessage());
                isSigning.set(false);
            }
        }, 5, 30, TimeUnit.SECONDS);
        
        // 每60秒尝试任务
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (!isTasking.get()) {
                    isTasking.set(true);
                    tryAutoTask();
                    isTasking.set(false);
                }
            } catch (Throwable e) {
                XposedBridge.log(TAG + "自动任务异常: " + e.getMessage());
                isTasking.set(false);
            }
        }, 15, 60, TimeUnit.SECONDS);
        
        // 每10秒输出状态
        scheduler.scheduleAtFixedRate(() -> {
            XposedBridge.log(TAG + "状态: 签到=" + signCount + " 任务=" + taskCount);
        }, 0, 10, TimeUnit.SECONDS);
    }

    /**
     * 尝试自动签到
     */
    private void tryAutoSignIn() {
        XposedBridge.log(TAG + "尝试自动签到...");
        
        // 方法1: 直接调用找到的签到方法
        if (signDoneMethod != null) {
            try {
                signDoneMethod.setAccessible(true);
                
                // 尝试不同的实例
                Object[] instances = {goldCoinRepoInstance, null};
                for (Object instance : instances) {
                    try {
                        if (instance != null || Modifier.isStatic(signDoneMethod.getModifiers())) {
                            signDoneMethod.invoke(instance);
                            XposedBridge.log(TAG + "调用签到方法成功");
                            return;
                        }
                    } catch (Throwable e) {
                        // 继续尝试下一个实例
                    }
                }
            } catch (Throwable e) {
                XposedBridge.log(TAG + "调用签到方法失败: " + e.getMessage());
            }
        }
        
        // 方法2: 通过反射查找并调用
        try {
            Class<?> repoClass = appClassLoader.loadClass(
                "com.dragon.read.ug.kmp.readingstatistics.parts.goldcoin.GoldCoinRepo");
            
            Object instance = getInstance(repoClass);
            if (instance != null) {
                for (Method method : repoClass.getDeclaredMethods()) {
                    if (method.getName().contains("executeSignDone") || 
                        method.getName().contains("requestGoldCoinSignInDone")) {
                        try {
                            method.setAccessible(true);
                            method.invoke(instance);
                            XposedBridge.log(TAG + "反射调用签到方法成功: " + method.getName());
                            return;
                        } catch (Throwable e) {
                            XposedBridge.log(TAG + "反射调用失败: " + method.getName() + " - " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Throwable e) {
            XposedBridge.log(TAG + "反射查找签到类失败: " + e.getMessage());
        }
        
        XposedBridge.log(TAG + "自动签到失败，未找到可用方法");
    }

    /**
     * 尝试自动任务
     */
    private void tryAutoTask() {
        XposedBridge.log(TAG + "尝试自动任务...");
        
        // 方法1: 直接调用找到的任务方法
        if (taskDoneMethod != null) {
            try {
                taskDoneMethod.setAccessible(true);
                
                // 尝试不同的实例
                Object[] instances = {taskDoneRepoInstance, treasureTaskRepoInstance, null};
                for (Object instance : instances) {
                    try {
                        if (instance != null || Modifier.isStatic(taskDoneMethod.getModifiers())) {
                            taskDoneMethod.invoke(instance);
                            XposedBridge.log(TAG + "调用任务方法成功");
                            return;
                        }
                    } catch (Throwable e) {
                        // 继续尝试下一个实例
                    }
                }
            } catch (Throwable e) {
                XposedBridge.log(TAG + "调用任务方法失败: " + e.getMessage());
            }
        }
        
        // 方法2: 通过反射查找并调用
        String[] taskClassNames = {
            "com.dragon.read.ug.kmp.common.repository.TaskDoneDataRepository",
            "com.dragon.read.ug.kmp.treasurebox.repository.TreasureTaskDoneDataRepository"
        };
        
        for (String className : taskClassNames) {
            try {
                Class<?> repoClass = appClassLoader.loadClass(className);
                Object instance = getInstance(repoClass);
                
                if (instance != null) {
                    for (Method method : repoClass.getDeclaredMethods()) {
                        if (method.getName().contains("executeTaskDone") || 
                            method.getName().contains("requestTaskDone") ||
                            method.getName().contains("requestTreasureTaskDone")) {
                            try {
                                method.setAccessible(true);
                                method.invoke(instance);
                                XposedBridge.log(TAG + "反射调用任务方法成功: " + method.getName());
                                return;
                            } catch (Throwable e) {
                                XposedBridge.log(TAG + "反射调用失败: " + method.getName() + " - " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                // 继续尝试下一个类
            }
        }
        
        XposedBridge.log(TAG + "自动任务失败，未找到可用方法");
    }
}
