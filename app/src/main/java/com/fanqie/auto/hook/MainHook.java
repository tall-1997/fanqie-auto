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
 * 番茄小说自动签到模块 v17.0.0
 * 修复版 - 改进token捕获和自动签到
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";
    private static final String MODULE_VERSION = "v17.0.0";

    // 正确的API服务器
    private static final String API_SERVER = "https://api5-normal-sinfonlinea.fqnovel.com";
    
    // 签到API端点
    private static final String[] SIGN_APIs = {
        "/luckycat/novel/v1/task/done/sign_in",
        "/luckycat/novel/v1/task/done/bookshelf_sign_in",
        "/luckycat/novel/v1/task/done/meal"
    };
    
    private ClassLoader appClassLoader;
    private ScheduledExecutorService scheduler;
    
    // 状态
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private int signCount = 0;
    private int successCount = 0;
    private int requestCount = 0;
    
    // 捕获的认证信息
    private String capturedToken = null;
    private String capturedCookie = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "==============================");
        XposedBridge.log(TAG + "番茄小说自动签到模块 " + MODULE_VERSION);
        XposedBridge.log(TAG + "修复版 - 改进token捕获");
        XposedBridge.log(TAG + "==============================");

        appClassLoader = lpparam.classLoader;

        try {
            // Hook OkHttp请求
            hookOkHttp(lpparam.classLoader);
            
            // 启动定时任务
            startScheduler();
            
            XposedBridge.log(TAG + "Hook初始化完成");
            XposedBridge.log(TAG + "等待捕获认证信息...");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "初始化失败: " + e.getMessage());
        }
    }

    /**
     * Hook OkHttp请求
     */
    private void hookOkHttp(ClassLoader classLoader) {
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
                            
                            requestCount++;
                            
                            if (url != null) {
                                // 获取请求头
                                Object headers = XposedHelpers.getObjectField(request, "headers");
                                String headersStr = headers != null ? headers.toString() : "";
                                
                                // 捕获token
                                if (headersStr.contains("token") || headersStr.contains("Token")) {
                                    parseHeaders(headersStr);
                                }
                                
                                // 记录API请求
                                if (url.contains("api.fqnovel.com") || url.contains("api5-normal")) {
                                    XposedBridge.log(TAG + "API请求: " + url);
                                    
                                    // 如果是签到API，记录成功
                                    if (url.contains("sign_in") || url.contains("signin")) {
                                        XposedBridge.log(TAG + "检测到签到请求");
                                    }
                                }
                            }
                        } catch (Throwable e) {
                            // 忽略
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
     * 解析请求头
     */
    private void parseHeaders(String headers) {
        try {
            String[] lines = headers.split("\n");
            for (String line : lines) {
                String lower = line.toLowerCase();
                if (lower.contains("token") && !lower.contains("content-type")) {
                    String[] parts = line.split(":\\s*", 2);
                    if (parts.length > 1) {
                        String token = parts[1].trim();
                        if (token.length() > 10 && !token.equals("null") && !token.contains("{")) {
                            capturedToken = token;
                            XposedBridge.log(TAG + "捕获Token: " + token.substring(0, Math.min(30, token.length())) + "...");
                        }
                    }
                }
                if (lower.contains("cookie")) {
                    String[] parts = line.split(":\\s*", 2);
                    if (parts.length > 1) {
                        capturedCookie = parts[1].trim();
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
                XposedBridge.log(TAG + "状态: 请求=" + requestCount + " 签到=" + signCount + " 成功=" + successCount);
                XposedBridge.log(TAG + "Token: " + (capturedToken != null ? "已捕获" : "未捕获"));
                
                // 尝试签到
                if (!isProcessing.get()) {
                    isProcessing.set(true);
                    tryAutoSign();
                    isProcessing.set(false);
                }
            } catch (Throwable e) {
                XposedBridge.log(TAG + "定时任务异常: " + e.getMessage());
                isProcessing.set(false);
            }
        }, 5, 60, TimeUnit.SECONDS);
    }

    /**
     * 尝试自动签到
     */
    private void tryAutoSign() {
        XposedBridge.log(TAG + "尝试自动签到...");
        signCount++;
        
        // 创建新线程执行HTTP请求
        new Thread(() -> {
            for (String apiPath : SIGN_APIs) {
                try {
                    String apiUrl = API_SERVER + apiPath;
                    URL url = new URL(apiUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("User-Agent", "okhttp/3.12.1");
                    
                    // 添加认证信息
                    if (capturedToken != null) {
                        conn.setRequestProperty("token", capturedToken);
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
                    
                    XposedBridge.log(TAG + "签到API: " + apiPath);
                    XposedBridge.log(TAG + "响应码: " + responseCode);
                    XposedBridge.log(TAG + "响应: " + response.toString());
                    
                    if (responseCode == 200) {
                        String resp = response.toString();
                        if (resp.contains("\"err_no\":0") || resp.contains("success") || 
                            resp.contains("签到成功") || resp.contains("已签到")) {
                            successCount++;
                            XposedBridge.log(TAG + "签到成功!");
                            return;
                        }
                    }
                    
                } catch (Throwable e) {
                    XposedBridge.log(TAG + "签到请求异常: " + apiPath + " - " + e.getMessage());
                }
            }
        }).start();
    }
}
