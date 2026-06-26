package com.fanqie.auto.hook;

import java.util.ArrayList;
import java.util.List;

import com.fanqie.auto.utils.LogManager;

/**
 * Hook管理器
 */
public class HookManager {
    private final List<BaseHook> hooks = new ArrayList<>();
    
    public void registerHook(BaseHook hook) {
        hooks.add(hook);
        LogManager.d("注册Hook: " + hook.getName());
    }
    
    public void initAllHooks(de.robv.android.xposed.XC_LoadPackage.LoadPackageParam lpparam) {
        for (BaseHook hook : hooks) {
            if (hook.isEnabled()) {
                try {
                    hook.init(lpparam);
                    LogManager.d("初始化Hook: " + hook.getName());
                } catch (Throwable e) {
                    LogManager.e("初始化Hook失败: " + hook.getName(), e);
                }
            }
        }
    }
    
    public void enableAllHooks() {
        for (BaseHook hook : hooks) {
            hook.setEnabled(true);
        }
    }
    
    public void disableAllHooks() {
        for (BaseHook hook : hooks) {
            hook.setEnabled(false);
        }
    }
    
    public List<BaseHook> getHooks() {
        return new ArrayList<>(hooks);
    }
}
