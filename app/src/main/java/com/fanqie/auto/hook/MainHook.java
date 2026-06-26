package com.fanqie.auto.hook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_LoadPackage;

/**
 * 番茄小说自动签到模块 - 主入口
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "[FanqieAuto] ";
    private static final String TARGET_PACKAGE = "com.dragon.read";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) return;

        XposedBridge.log(TAG + "模块已加载 v0.0.1");
        XposedBridge.log(TAG + "目标应用: " + TARGET_PACKAGE);

        // Hook HTTP 请求
        hookHttpRequest(lpparam);
        
        // Hook 签到响应
        hookSignResponse(lpparam);
    }

    private void hookHttpRequest(XC_LoadPackage.LoadPackageParam lpparam) {
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
                            if (url != null && isImportantUrl(url)) {
                                XposedBridge.log(TAG + "请求: " + url);
                            }
                        } catch (Throwable e) {
                            // 忽略
                        }
                    }
                }
            );
            XposedBridge.log(TAG + "HTTP Hook 成功");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "HTTP Hook 失败: " + e.getMessage());
        }
    }

    private void hookSignResponse(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "org.json.JSONObject",
                lpparam.classLoader,
                "optInt",
                String.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String key = (String) param.args[0];
                        int value = (int) param.getResult();
                        
                        if ("status".equals(key) && value == 1) {
                            XposedBridge.log(TAG + "签到成功!");
                        }
                        if ("obtain".equals(key) && value > 0) {
                            XposedBridge.log(TAG + "获得金币: +" + value);
                        }
                    }
                }
            );
            XposedBridge.log(TAG + "Sign Hook 成功");
        } catch (Throwable e) {
            XposedBridge.log(TAG + "Sign Hook 失败: " + e.getMessage());
        }
    }

    private boolean isImportantUrl(String url) {
        return url.contains("sign") ||
               url.contains("task") ||
               url.contains("reward") ||
               url.contains("gold") ||
               url.contains("luckycat");
    }
}
