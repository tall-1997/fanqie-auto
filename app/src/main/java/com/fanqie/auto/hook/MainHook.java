package com.fanqie.auto.hook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 番茄小说自动签到模块 v10.0.0
 * 直接HTTP请求模式 - 主动发起签到/任务请求
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";
    private static final String MODULE_VERSION = "v10.0.0";

    private ClassLoader appClassLoader;
    private ScheduledExecutorService scheduler;
    
    // 保存捕获的请求信息
    private String capturedUrl = null;
    private String capturedMethod = null;
    private String capturedHeaders = null;
    private String capturedBody = null;
    
    // 状态
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private int signCount = 0;
    private int taskCount = 0;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "==============================");
        XposedBridge.log(TAG + "番茄小说自动签到模块 " + MODULE_VERSION);
        XposedBridge.log(TAG + "直接HTTP请求模式");
        XposedBridge.log(TAG + "==============================");

        appClassLoader = lpparam.classLoader;

        try {
            // Hook HTTP请求，捕获请求信息
            hookHttpRequest(lpparam.classLoader);
            
            // 启动定时任务
            startScheduler();
            
            XposedBridge.log(TAG + "Hook初始化完成");
            XposedBridge.log(TAG + "等待捕获签到请求...");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "初始化失败: " + e.getMessage());
        }
    }

    /**
     * Hook HTTP请求，捕获请求信息
     */
    private void hookHttpRequest(ClassLoader classLoader) {
        try {
            // Hook OkHttp Request.Builder.build()
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
                            
                            if (url != null && isSignUrl(url)) {
                                XposedBridge.log(TAG + "\n[=== 捕获签到请求 ===]");
                                XposedBridge.log(TAG + "URL: " + url);
                                XposedBridge.log(TAG + "方法: " + method);
                                
                                // 保存请求信息
                                capturedUrl = url;
                                capturedMethod = method;
                                
                                // 尝试获取请求头
                                try {
                                    Object headers = XposedHelpers.getObjectField(request, "headers");
                                    if (headers != null) {
                                        capturedHeaders = headers.toString();
                                        XposedBridge.log(TAG + "请求头: " + capturedHeaders);
                                    }
                                } catch (Throwable e) {
                                    // 忽略
                                }
                                
                                // 尝试获取请求体
                                try {
                                    Object body = XposedHelpers.getObjectField(request, "body");
                                    if (body != null) {
                                        capturedBody = body.toString();
                                        XposedBridge.log(TAG + "请求体: " + capturedBody);
                                    }
                                } catch (Throwable e) {
                                    // 忽略
                                }
                                
                                XposedBridge.log(TAG + "===================\n");
                            }
                        } catch (Throwable e) {
                            // 忽略
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
     * 判断是否为签到URL
     */
    private boolean isSignUrl(String url) {
        String lower = url.toLowerCase();
        return lower.contains("sign_in") || lower.contains("signin") ||
               lower.contains("task/done") || lower.contains("task_done") ||
               lower.contains("reward") || lower.contains("claim") ||
               lower.contains("luckycat");
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
