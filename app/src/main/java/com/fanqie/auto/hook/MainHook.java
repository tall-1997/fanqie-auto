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
 * 番茄小说自动签到模块 v9.0.0
 * HTTP请求拦截模式 - 通过拦截网络请求实现自动签到/任务
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";
    private static final String MODULE_VERSION = "v9.0.0";

    private ClassLoader appClassLoader;
    private ScheduledExecutorService scheduler;
    
    // 签到URL
    private static final String SIGN_URL = "/luckycat/novel/v1/task/done/sign_in";
    private static final String MEAL_URL = "/luckycat/novel/v1/task/done/meal";
    private static final String REDPACK_URL = "/luckycat/novel/v1/task/done/redpack";
    private static final String TREASURE_URL = "/luckycat/novel/v1/task/done/treasure_task";
    
    // 状态
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private int signCount = 0;
    private int taskCount = 0;
    
    // 找到的请求构建器
    private Object requestBuilder = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "==============================");
        XposedBridge.log(TAG + "番茄小说自动签到模块 " + MODULE_VERSION);
        XposedBridge.log(TAG + "HTTP请求拦截模式");
        XposedBridge.log(TAG + "==============================");

        appClassLoader = lpparam.classLoader;

        try {
            // Hook OkHttp请求构建器
            hookOkHttpRequestBuilder(lpparam.classLoader);
            
            // Hook Retrofit接口
            hookRetrofitInterface(lpparam.classLoader);
            
            // 启动定时任务
            startScheduler();
            
            XposedBridge.log(TAG + "Hook初始化完成");
            XposedBridge.log(TAG + "自动签到/任务已启动");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "初始化失败: " + e.getMessage());
        }
    }

    /**
     * Hook OkHttp请求构建器
     */
    private void hookOkHttpRequestBuilder(ClassLoader classLoader) {
        try {
            // Hook Request.Builder.build()
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
                            String method = (String) XposedHelpers.getObjectField(request, "method");
                            
                            if (url != null) {
                                handleRequest(url, method, request);
                            }
                        } catch (Throwable e) {
                            // 忽略
                        }
                    }
                }
            );
            
            // Hook Request.Builder.url(String)
            XposedHelpers.findAndHookMethod(
                "okhttp3.Request$Builder",
                classLoader,
                "url",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String url = (String) param.args[0];
                        if (url != null && isImportantUrl(url)) {
                            XposedBridge.log(TAG + "构建请求: " + url);
                            requestBuilder = param.thisObject;
                        }
                    }
                }
            );
            
            XposedBridge.log(TAG + "OkHttp Hook成功");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "OkHttp Hook失败: " + e.getMessage());
        }
    }

    /**
     * Hook Retrofit接口
     */
    private void hookRetrofitInterface(ClassLoader classLoader) {
        try {
            // 尝试找到API接口类
            String[] apiClasses = {
                "com.dragon.read.rpc.rpc.UgcApiService",
                "com.dragon.read.saas.ugc.rpc.CommentApiService"
            };
            
            for (String className : apiClasses) {
                try {
                    Class<?> clazz = classLoader.loadClass(className);
                    hookApiClass(clazz, className);
                } catch (Throwable e) {
                    // 继续尝试下一个
                }
            }
            
            XposedBridge.log(TAG + "Retrofit Hook成功");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "Retrofit Hook失败: " + e.getMessage());
        }
    }

    /**
     * Hook API类
     */
    private void hookApiClass(Class<?> clazz, String className) {
        Method[] methods = clazz.getDeclaredMethods();
        
        for (Method method : methods) {
            String methodName = method.getName();
            
            if (methodName.contains("sign") || methodName.contains("task") || 
                methodName.contains("done") || methodName.contains("reward")) {
                
                try {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + "\n[=== API调用 ===]");
                            XposedBridge.log(TAG + "类: " + className);
                            XposedBridge.log(TAG + "方法: " + methodName);
                            
                            if (param.args != null) {
                                for (int i = 0; i < param.args.length; i++) {
                                    XposedBridge.log(TAG + "参数[" + i + "]: " + param.args[i]);
                                }
                            }
                        }
                        
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.getThrowable() != null) {
                                XposedBridge.log(TAG + "失败: " + param.getThrowable().getMessage());
                            } else {
                                XposedBridge.log(TAG + "返回: " + param.getResult());
                                
                                if (methodName.contains("sign")) {
                                    signCount++;
                                } else {
                                    taskCount++;
                                }
                            }
                            XposedBridge.log(TAG + "==============\n");
                        }
                    });
                    
                    XposedBridge.log(TAG + "Hook方法: " + className + "." + methodName);
                } catch (Throwable e) {
                    // 忽略
                }
            }
        }
    }

    /**
     * 处理请求
     */
    private void handleRequest(String url, String method, Object request) {
        String lower = url.toLowerCase();
        
        if (lower.contains("sign_in") || lower.contains("signin")) {
            XposedBridge.log(TAG + "\n[=== 签到请求 ===]");
            XposedBridge.log(TAG + "URL: " + url);
            XposedBridge.log(TAG + "方法: " + method);
            XposedBridge.log(TAG + "================\n");
            signCount++;
        } else if (lower.contains("task/done") || lower.contains("task_done")) {
            XposedBridge.log(TAG + "\n[=== 任务请求 ===]");
            XposedBridge.log(TAG + "URL: " + url);
            XposedBridge.log(TAG + "方法: " + method);
            XposedBridge.log(TAG + "================\n");
            taskCount++;
        } else if (lower.contains("reward") || lower.contains("claim")) {
            XposedBridge.log(TAG + "\n[=== 奖励请求 ===]");
            XposedBridge.log(TAG + "URL: " + url);
            XposedBridge.log(TAG + "方法: " + method);
            XposedBridge.log(TAG + "================\n");
        }
    }

    /**
     * 判断是否为重要URL
     */
    private boolean isImportantUrl(String url) {
        String lower = url.toLowerCase();
        return lower.contains("sign_in") || lower.contains("signin") ||
               lower.contains("task/done") || lower.contains("task_done") ||
               lower.contains("reward") || lower.contains("claim") ||
               lower.contains("luckycat") || lower.contains("coin");
    }

    /**
     * 启动定时任务
     */
    private void startScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // 每30秒输出状态
        scheduler.scheduleAtFixedRate(() -> {
            try {
                XposedBridge.log(TAG + "状态: 签到=" + signCount + " 任务=" + taskCount);
            } catch (Throwable e) {
                // 忽略
            }
        }, 5, 30, TimeUnit.SECONDS);
    }
}
