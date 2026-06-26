package com.fanqie.auto.hook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_LoadPackage;

import com.fanqie.auto.utils.LogManager;
import com.fanqie.auto.model.Config;

/**
 * HTTP Hook - 拦截HTTP请求和响应
 */
public class HttpHook extends BaseHook {
    private static final String HOOK_NAME = "HttpHook";
    private Config config;
    private long requestCount = 0;
    private long importantRequestCount = 0;
    
    public HttpHook(Config config) {
        super(HOOK_NAME);
        this.config = config;
    }
    
    @Override
    protected void init(XC_LoadPackage.LoadPackageParam lpparam) {
        hookOkHttpRequest(lpparam);
        hookOkHttpResponse(lpparam);
    }
    
    private void hookOkHttpRequest(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "okhttp3.Request$Builder",
                lpparam.classLoader,
                "build",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Object request = param.getResult();
                            String url = (String) XposedHelpers.getObjectField(request, "url");
                            
                            requestCount++;
                            
                            if (url != null && config.matchesUrl(url)) {
                                importantRequestCount++;
                                LogManager.i("HTTP请求: " + url);
                                
                                // 可以在这里添加请求修改逻辑
                                modifyRequest(request, url);
                            }
                        } catch (Throwable e) {
                            LogManager.e("处理HTTP请求失败", e);
                        }
                    }
                }
            );
            LogManager.d("HTTP请求Hook初始化成功");
        } catch (Throwable e) {
            LogManager.e("HTTP请求Hook初始化失败", e);
        }
    }
    
    private void hookOkHttpResponse(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook OkHttp响应处理
            XposedHelpers.findAndHookMethod(
                "okhttp3.RealCall",
                lpparam.classLoader,
                "getResponseWithInterceptorChain",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Object response = param.getResult();
                            if (response != null) {
                                Object request = XposedHelpers.getObjectField(response, "request");
                                if (request != null) {
                                    String url = (String) XposedHelpers.getObjectField(request, "url");
                                    if (url != null && config.matchesUrl(url)) {
                                        int code = XposedHelpers.getIntField(response, "code");
                                        LogManager.i("HTTP响应: " + url + " 状态码: " + code);
                                        
                                        // 可以在这里添加响应修改逻辑
                                        modifyResponse(response, url, code);
                                    }
                                }
                            }
                        } catch (Throwable e) {
                            LogManager.e("处理HTTP响应失败", e);
                        }
                    }
                }
            );
            LogManager.d("HTTP响应Hook初始化成功");
        } catch (Throwable e) {
            // 这个方法可能不存在，忽略错误
            LogManager.d("HTTP响应Hook跳过（方法不存在）");
        }
    }
    
    private void modifyRequest(Object request, String url) {
        // 可以在这里修改请求头、参数等
        // 例如添加自定义Header
        try {
            Object headers = XposedHelpers.getObjectField(request, "headers");
            if (headers != null) {
                // 添加自定义Header示例
                // XposedHelpers.callMethod(headers, "add", "X-Custom-Header", "value");
            }
        } catch (Throwable e) {
            // 忽略修改失败
        }
    }
    
    private void modifyResponse(Object response, String url, int code) {
        // 可以在这里修改响应内容
        // 例如记录响应体大小
        try {
            Object body = XposedHelpers.getObjectField(response, "body");
            if (body != null) {
                long contentLength = XposedHelpers.getLongField(body, "contentLength");
                LogManager.d("响应体大小: " + contentLength + " bytes");
            }
        } catch (Throwable e) {
            // 忽略修改失败
        }
    }
    
    public long getRequestCount() {
        return requestCount;
    }
    
    public long getImportantRequestCount() {
        return importantRequestCount;
    }
    
    public void resetCounters() {
        requestCount = 0;
        importantRequestCount = 0;
    }
    
    @Override
    public String getStatus() {
        return String.format("%s, 请求总数: %d, 重要请求: %d", 
            super.getStatus(), requestCount, importantRequestCount);
    }
}
