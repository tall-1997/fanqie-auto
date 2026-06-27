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
 * 番茄小说自动签到模块 v19.0.0
 * 集成会员解锁 + 自动签到
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";
    private static final String MODULE_VERSION = "v19.0.0";

    // 正确的API服务器
    private static final String API_SERVER = "https://api5-normal-sinfonlinea.fqnovel.com";
    
    // 签到API端点
    private static final String[] SIGN_APIs = {
        "/luckycat/novel/v1/task/done/sign_in",
        "/luckycat/novel/v1/task/done/bookshelf_sign_in",
        "/luckycat/novel/v1/task/done/meal",
        "/luckycat/novel/v1/task/done/daily_1min_earn_money",
        "/luckycat/novel/v1/task/done/daily_short_video_collect",
        "/luckycat/novel/v1/task/done/mix_task_collect"
    };
    
    private ClassLoader appClassLoader;
    private ScheduledExecutorService scheduler;
    
    // 状态
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private int signCount = 0;
    private int successCount = 0;
    
    // 捕获的认证信息
    private String capturedToken = null;
    private Object appContext = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "==============================");
        XposedBridge.log(TAG + "番茄小说自动签到模块 " + MODULE_VERSION);
        XposedBridge.log(TAG + "会员解锁 + 自动签到");
        XposedBridge.log(TAG + "==============================");

        appClassLoader = lpparam.classLoader;

        try {
            // Hook Application获取上下文
            hookApplication(lpparam.classLoader);
            
            // Hook会员解锁
            hookVipUnlock(lpparam.classLoader);
            
            // Hook LuckyDog SDK获取token
            hookLuckyDogSdk(lpparam.classLoader);
            
            // 启动定时任务
            startScheduler();
            
            XposedBridge.log(TAG + "Hook初始化完成");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "初始化失败: " + e.getMessage());
        }
    }

    /**
     * Hook Application获取上下文
     */
    private void hookApplication(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Application",
                classLoader,
                "onCreate",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        appContext = param.thisObject;
                        XposedBridge.log(TAG + "获取应用上下文成功");
                        
                        // 尝试从SharedPreferences读取token
                        readTokenFromPrefs();
                    }
                }
            );
            XposedBridge.log(TAG + "Application Hook成功");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "Application Hook失败: " + e.getMessage());
        }
    }

    /**
     * Hook会员解锁
     */
    private void hookVipUnlock(ClassLoader classLoader) {
        try {
            // Hook NsVipImpl.willShowLynxBanner 返回 false
            XposedHelpers.findAndHookMethod(
                "com.dragon.read.component.biz.impl.NsVipImpl",
                classLoader,
                "willShowLynxBanner",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(false);
                        XposedBridge.log(TAG + "Hook willShowLynxBanner -> false");
                    }
                }
            );
            XposedBridge.log(TAG + "NsVipImpl Hook成功");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "NsVipImpl Hook失败: " + e.getMessage());
        }

        try {
            // Hook VipInfoModel 构造函数，设置VIP状态
            XposedHelpers.findAndHookConstructor(
                "com.dragon.read.user.model.VipInfoModel",
                classLoader,
                String.class,  // userId
                String.class,  // vipType
                String.class,  // expireTime
                boolean.class, // isVip
                boolean.class, // isAutoRenew
                int.class,     // vipLevel
                boolean.class, // isAnnualVip
                "com.dragon.read.rpc.model.VipCommonSubType", // subType
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            // 设置VIP状态
                            XposedHelpers.setBooleanField(param.thisObject, "isVip", true);
                            XposedHelpers.setBooleanField(param.thisObject, "isAutoRenew", true);
                            XposedHelpers.setIntField(param.thisObject, "vipLevel", 1);
                            XposedHelpers.setBooleanField(param.thisObject, "isAnnualVip", true);
                            
                            // 设置过期时间（远未来）
                            XposedHelpers.setObjectField(param.thisObject, "expireTime", "3093518135349");
                            
                            XposedBridge.log(TAG + "Hook VipInfoModel -> VIP解锁");
                        } catch (Throwable e) {
                            XposedBridge.log(TAG + "设置VIP状态失败: " + e.getMessage());
                        }
                    }
                }
            );
            XposedBridge.log(TAG + "VipInfoModel Hook成功");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "VipInfoModel Hook失败: " + e.getMessage());
        }
    }

    /**
     * Hook LuckyDog SDK获取token
     */
    private void hookLuckyDogSdk(ClassLoader classLoader) {
        try {
            Class<?> tokenInterceptorClass = classLoader.loadClass(
                "com.bytedance.ug.sdk.luckydog.tokenunion.interceptor.TokenUnionInterceptor");
            
            for (Method method : tokenInterceptorClass.getDeclaredMethods()) {
                if (method.getName().contains("getToken") || method.getName().contains("intercept")) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                Object result = param.getResult();
                                if (result != null) {
                                    String token = result.toString();
                                    if (token.length() > 10) {
                                        capturedToken = token;
                                        XposedBridge.log(TAG + "从LuckyDog SDK获取Token: " + 
                                            token.substring(0, Math.min(30, token.length())) + "...");
                                    }
                                }
                            } catch (Throwable e) {
                                // 忽略
                            }
                        }
                    });
                    XposedBridge.log(TAG + "Hook方法: " + method.getName());
                }
            }
            
            XposedBridge.log(TAG + "LuckyDog SDK Hook成功");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "LuckyDog SDK Hook失败: " + e.getMessage());
        }
    }

    /**
     * 从SharedPreferences读取token
     */
    private void readTokenFromPrefs() {
        if (appContext == null) return;
        
        try {
            Object prefs = XposedHelpers.callMethod(appContext, 
                "getSharedPreferences", "luckydog_sdk_config.prefs", 0);
            
            if (prefs != null) {
                String token = (String) XposedHelpers.callMethod(prefs, 
                    "getString", "luckydog_token_key", "");
                
                if (token != null && !token.isEmpty() && token.length() > 10) {
                    capturedToken = token;
                    XposedBridge.log(TAG + "从SharedPreferences获取Token: " + 
                        token.substring(0, Math.min(30, token.length())) + "...");
                } else {
                    XposedBridge.log(TAG + "SharedPreferences中未找到Token");
                }
            }
        } catch (Throwable e) {
            XposedBridge.log(TAG + "读取SharedPreferences失败: " + e.getMessage());
        }
    }

    /**
     * 启动定时任务
     */
    private void startScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                XposedBridge.log(TAG + "状态: 签到=" + signCount + " 成功=" + successCount);
                XposedBridge.log(TAG + "Token: " + (capturedToken != null ? "已捕获" : "未捕获"));
                
                if (capturedToken != null && !isProcessing.get()) {
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
        if (capturedToken == null) {
            XposedBridge.log(TAG + "未捕获到Token，跳过签到");
            return;
        }
        
        XposedBridge.log(TAG + "尝试自动签到...");
        signCount++;
        
        new Thread(() -> {
            for (String apiPath : SIGN_APIs) {
                try {
                    String apiUrl = API_SERVER + apiPath;
                    URL url = new URL(apiUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("User-Agent", "okhttp/3.12.1");
                    conn.setRequestProperty("token", capturedToken);
                    
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    
                    OutputStream os = conn.getOutputStream();
                    os.write("{}".getBytes());
                    os.flush();
                    os.close();
                    
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
