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
 * 番茄小说自动签到模块 v15.0.0
 * 自动捕获Token并签到
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";
    private static final String MODULE_VERSION = "v15.0.0";

    // 签到API端点
    private static final String SIGN_API = "https://api.fqnovel.com/luckycat/novel/v1/task/done/sign_in";
    
    private ClassLoader appClassLoader;
    private ScheduledExecutorService scheduler;
    
    // 状态
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private int signCount = 0;
    private int successCount = 0;
    
    // 捕获的认证信息
    private String capturedToken = null;
    private String capturedCookie = null;
    private String capturedDeviceId = null;
    private String capturedInstallId = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "==============================");
        XposedBridge.log(TAG + "番茄小说自动签到模块 " + MODULE_VERSION);
        XposedBridge.log(TAG + "自动捕获Token模式");
        XposedBridge.log(TAG + "==============================");

        appClassLoader = lpparam.classLoader;

        try {
            // Hook HTTP请求，捕获认证信息
            hookHttpRequest(lpparam.classLoader);
            
            // 启动定时任务
            startScheduler();
            
            XposedBridge.log(TAG + "Hook初始化完成");
            XposedBridge.log(TAG + "等待捕获认证信息...");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "初始化失败: " + e.getMessage());
        }
    }

    /**
     * Hook HTTP请求，捕获认证信息
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
                            
                            if (url != null && isApiUrl(url)) {
                                // 获取请求头
                                try {
                                    Object headers = XposedHelpers.getObjectField(request, "headers");
                                    if (headers != null) {
                                        String headersStr = headers.toString();
                                        
                                        // 解析token
                                        if (headersStr.contains("token") || headersStr.contains("Token") || 
                                            headersStr.contains("Authorization") || headersStr.contains("authorization")) {
                                            parseAuthToken(headersStr);
                                        }
                                        
                                        // 解析cookie
                                        if (headersStr.contains("Cookie") || headersStr.contains("cookie")) {
                                            parseCookie(headersStr);
                                        }
                                    }
                                } catch (Throwable e) {
                                    // 忽略
                                }
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
     * 判断是否为API URL
     */
    private boolean isApiUrl(String url) {
        String lower = url.toLowerCase();
        return lower.contains("api.fqnovel.com") || 
               lower.contains("i.snssdk.com") ||
               lower.contains("ib.snssdk.com") ||
               lower.contains("luckycat");
    }

    /**
     * 解析认证token
     */
    private void parseAuthToken(String headers) {
        try {
            String[] lines = headers.split("\n");
            for (String line : lines) {
                String lower = line.toLowerCase();
                if (lower.contains("token") || lower.contains("authorization")) {
                    // 尝试提取token值
                    String[] parts = line.split(":\\s*");
                    if (parts.length > 1) {
                        String token = parts[1].trim();
                        if (token.length() > 10) {
                            capturedToken = token;
                            XposedBridge.log(TAG + "捕获Token: " + token.substring(0, Math.min(30, token.length())) + "...");
                        }
                    }
                }
            }
        } catch (Throwable e) {
            // 忽略
        }
    }

    /**
     * 解析Cookie
     */
    private void parseCookie(String headers) {
        try {
            String[] lines = headers.split("\n");
            for (String line : lines) {
                if (line.toLowerCase().contains("cookie")) {
                    String[] parts = line.split(":\\s*", 2);
                    if (parts.length > 1) {
                        capturedCookie = parts[1].trim();
                        XposedBridge.log(TAG + "捕获Cookie: " + capturedCookie.substring(0, Math.min(50, capturedCookie.length())) + "...");
                    }
                }
            }
        } catch (Throwable e) {
            // 忽略
        }
    }

    /**
     * 启动定时任务
     */
    private void startScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // 每60秒输出状态并尝试签到
        scheduler.scheduleAtFixedRate(() -> {
            try {
                XposedBridge.log(TAG + "状态: 签到=" + signCount + " 成功=" + successCount);
                XposedBridge.log(TAG + "Token: " + (capturedToken != null ? "已捕获" : "未捕获"));
                
                // 如果有token，尝试签到
                if (capturedToken != null && !isProcessing.get()) {
                    isProcessing.set(true);
                    tryAutoSign();
                    isProcessing.set(false);
                }
            } catch (Throwable e) {
                XposedBridge.log(TAG + "定时任务异常: " + e.getMessage());
                isProcessing.set(false);
            }
        }, 10, 60, TimeUnit.SECONDS);
    }

    /**
     * 尝试自动签到
     */
    private void tryAutoSign() {
        if (capturedToken == null) {
            XposedBridge.log(TAG + "未捕获到Token，跳过签到");
            return;
        }
        
        XposedBridge.log(TAG + "尝试自动签到...");
        signCount++;
        
        // 创建新线程执行HTTP请求
        new Thread(() -> {
            try {
                URL url = new URL(SIGN_API);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "okhttp/3.12.1");
                
                // 添加认证信息
                if (capturedToken != null) {
                    conn.setRequestProperty("token", capturedToken);
                    conn.setRequestProperty("Authorization", "Bearer " + capturedToken);
                }
                if (capturedCookie != null) {
                    conn.setRequestProperty("Cookie", capturedCookie);
                }
                
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                
                // 发送请求
                OutputStream os = conn.getOutputStream();
                os.write("{}".getBytes());
                os.flush();
                os.close();
                
                // 获取响应
                int responseCode = conn.getResponseCode();
                BufferedReader in = new BufferedReader(new InputStreamReader(
                    responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                XposedBridge.log(TAG + "签到响应码: " + responseCode);
                XposedBridge.log(TAG + "签到响应: " + response.toString());
                
                if (responseCode == 200) {
                    String resp = response.toString();
                    if (resp.contains("\"err_no\":0") || resp.contains("\"err_no\": 0") || 
                        resp.contains("success") || resp.contains("签到成功")) {
                        successCount++;
                        XposedBridge.log(TAG + "签到成功!");
                    } else {
                        XposedBridge.log(TAG + "签到返回错误: " + resp);
                    }
                } else {
                    XposedBridge.log(TAG + "签到请求失败: HTTP " + responseCode);
                }
                
            } catch (Throwable e) {
                XposedBridge.log(TAG + "签到请求异常: " + e.getMessage());
            }
        }).start();
    }
}
