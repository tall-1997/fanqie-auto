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
 * 番茄小说自动签到模块 v11.0.0
 * 真正的自动签到 - 通过WebView执行JavaScript实现
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";
    private static final String MODULE_VERSION = "v11.0.0";

    private ClassLoader appClassLoader;
    private ScheduledExecutorService scheduler;
    
    // 状态
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private int signCount = 0;
    private int taskCount = 0;
    
    // WebView实例
    private Object webView = null;
    private Object webViewClient = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "==============================");
        XposedBridge.log(TAG + "番茄小说自动签到模块 " + MODULE_VERSION);
        XposedBridge.log(TAG + "WebView JavaScript模式");
        XposedBridge.log(TAG + "==============================");

        appClassLoader = lpparam.classLoader;

        try {
            // Hook WebView
            hookWebView(lpparam.classLoader);
            
            // Hook HTTP请求捕获token
            hookHttpRequest(lpparam.classLoader);
            
            // 启动定时任务
            startScheduler();
            
            XposedBridge.log(TAG + "Hook初始化完成");
            XposedBridge.log(TAG + "等待WebView加载...");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "初始化失败: " + e.getMessage());
        }
    }

    /**
     * Hook WebView
     */
    private void hookWebView(ClassLoader classLoader) {
        try {
            // Hook WebView.loadUrl
            XposedHelpers.findAndHookMethod(
                "android.webkit.WebView",
                classLoader,
                "loadUrl",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String url = (String) param.args[0];
                        if (url != null && url.contains("luckycat")) {
                            XposedBridge.log(TAG + "WebView加载: " + url);
                            webView = param.thisObject;
                            
                            // 注入JavaScript
                            injectJavaScript(param.thisObject);
                        }
                    }
                }
            );
            
            // Hook WebView.setWebViewClient
            XposedHelpers.findAndHookMethod(
                "android.webkit.WebView",
                classLoader,
                "setWebViewClient",
                "android.webkit.WebViewClient",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        webViewClient = param.args[0];
                        XposedBridge.log(TAG + "WebViewClient设置");
                    }
                }
            );
            
            XposedBridge.log(TAG + "WebView Hook成功");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "WebView Hook失败: " + e.getMessage());
        }
    }

    /**
     * 注入JavaScript
     */
    private void injectJavaScript(Object webView) {
        try {
            // 自动签到JavaScript代码
            String jsCode = 
                "(function() {" +
                "  console.log('[FanqieAuto] 开始自动签到...');" +
                "  " +
                "  // 尝试点击签到按钮" +
                "  var signBtn = document.querySelector('[class*=sign]') || " +
                "                document.querySelector('[class*=Sign]') || " +
                "                document.querySelector('button[onclick*=sign]');" +
                "  if (signBtn) {" +
                "    console.log('[FanqieAuto] 找到签到按钮，点击');" +
                "    signBtn.click();" +
                "  }" +
                "  " +
                "  // 尝试领取奖励" +
                "  var claimBtn = document.querySelector('[class*=claim]') || " +
                "                 document.querySelector('[class*=Claim]') || " +
                "                 document.querySelector('button[onclick*=claim]');" +
                "  if (claimBtn) {" +
                "    console.log('[FanqieAuto] 找到领取按钮，点击');" +
                "    claimBtn.click();" +
                "  }" +
                "  " +
                "  // 尝试完成任务" +
                "  var taskBtn = document.querySelector('[class*=task]') || " +
                "                document.querySelector('[class*=Task]') || " +
                "                document.querySelector('button[onclick*=task]');" +
                "  if (taskBtn) {" +
                "    console.log('[FanqieAuto] 找到任务按钮，点击');" +
                "    taskBtn.click();" +
                "  }" +
                "  " +
                "  console.log('[FanqieAuto] 自动签到完成');" +
                "})();";
            
            // 通过反射调用evaluateJavascript
            Method evaluateMethod = webView.getClass().getMethod("evaluateJavascript", String.class, Object.class);
            evaluateMethod.invoke(webView, jsCode, null);
            
            XposedBridge.log(TAG + "JavaScript注入成功");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "JavaScript注入失败: " + e.getMessage());
        }
    }

    /**
     * Hook HTTP请求捕获token
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
                            
                            if (url != null && isSignUrl(url)) {
                                XposedBridge.log(TAG + "\n[=== 签到请求 ===]");
                                XposedBridge.log(TAG + "URL: " + url);
                                
                                // 获取请求头
                                try {
                                    Object headers = XposedHelpers.getObjectField(request, "headers");
                                    if (headers != null) {
                                        XposedBridge.log(TAG + "Headers: " + headers.toString());
                                    }
                                } catch (Throwable e) {
                                    // 忽略
                                }
                                
                                XposedBridge.log(TAG + "===================\n");
                                signCount++;
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
                
                // 如果有WebView，尝试自动签到
                if (webView != null) {
                    injectJavaScript(webView);
                }
            } catch (Throwable e) {
                // 忽略
            }
        }, 5, 30, TimeUnit.SECONDS);
    }
}
