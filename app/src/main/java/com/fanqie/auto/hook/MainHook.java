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
 * 番茄小说自动签到模块 v14.0.0
 * 直接API调用 - 自动发起签到请求
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";
    private static final String MODULE_VERSION = "v14.0.0";

    // 签到API端点
    private static final String[] SIGN_APIs = {
        "https://api.fqnovel.com/luckycat/novel/v1/task/done/sign_in",
        "https://i.snssdk.com/luckycat/novel/v1/task/done/sign_in",
        "https://ib.snssdk.com/luckycat/novel/v1/task/done/sign_in"
    };
    
    private ClassLoader appClassLoader;
    private ScheduledExecutorService scheduler;
    
    // 状态
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private int signCount = 0;
    private int taskCount = 0;
    
    // 应用上下文
    private Object appContext = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "==============================");
        XposedBridge.log(TAG + "番茄小说自动签到模块 " + MODULE_VERSION);
        XposedBridge.log(TAG + "直接API调用模式");
        XposedBridge.log(TAG + "==============================");

        appClassLoader = lpparam.classLoader;

        try {
            // 获取应用上下文
            hookApplication(lpparam.classLoader);
            
            // 启动定时任务
            startScheduler();
            
            XposedBridge.log(TAG + "Hook初始化完成");
            XposedBridge.log(TAG + "自动签到已启动");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "初始化失败: " + e.getMessage());
        }
    }

    /**
     * Hook Application获取上下文
     */
    private void hookApplication(ClassLoader classLoader) {
        try {
            // Hook Application.onCreate获取上下文
            XposedHelpers.findAndHookMethod(
                "android.app.Application",
                classLoader,
                "onCreate",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        appContext = param.thisObject;
                        XposedBridge.log(TAG + "获取应用上下文成功");
                    }
                }
            );
            XposedBridge.log(TAG + "Application Hook成功");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "Application Hook失败: " + e.getMessage());
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
        }, 5, 30, TimeUnit.SECONDS);
    }

    /**
     * 尝试自动签到
     */
    private void tryAutoSign() {
        XposedBridge.log(TAG + "尝试自动签到...");
        
        // 创建新线程执行HTTP请求
        new Thread(() -> {
            for (String apiUrl : SIGN_APIs) {
                try {
                    URL url = new URL(apiUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    
                    // 发送请求
                    OutputStream os = conn.getOutputStream();
                    os.write("{}".getBytes());
                    os.flush();
                    os.close();
                    
                    // 获取响应
                    int responseCode = conn.getResponseCode();
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    
                    XposedBridge.log(TAG + "签到API: " + apiUrl);
                    XposedBridge.log(TAG + "响应码: " + responseCode);
                    XposedBridge.log(TAG + "响应内容: " + response.toString());
                    
                    if (responseCode == 200) {
                        signCount++;
                        XposedBridge.log(TAG + "签到请求成功!");
                        return; // 成功就退出
                    }
                    
                } catch (Throwable e) {
                    XposedBridge.log(TAG + "签到请求失败: " + apiUrl + " - " + e.getMessage());
                }
            }
        }).start();
    }
}
