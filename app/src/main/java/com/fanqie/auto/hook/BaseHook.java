package com.fanqie.auto.hook;

import de.robv.android.xposed.XC_LoadPackage;
import com.fanqie.auto.utils.LogManager;

/**
 * Hook基类
 */
public abstract class BaseHook {
    protected final String name;
    protected boolean enabled;
    protected boolean initialized;
    protected int priority; // 优先级，数值越小优先级越高
    protected XC_LoadPackage.LoadPackageParam lpparam;
    
    public BaseHook(String name) {
        this(name, 100); // 默认优先级100
    }
    
    public BaseHook(String name, int priority) {
        this.name = name;
        this.priority = priority;
        this.enabled = true;
        this.initialized = false;
    }
    
    public String getName() {
        return name;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            enable();
        } else {
            disable();
        }
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * 初始化Hook
     */
    public final void initHook(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!enabled) {
            LogManager.d("Hook已禁用，跳过初始化: " + name);
            return;
        }
        
        this.lpparam = lpparam;
        try {
            init(lpparam);
            initialized = true;
            LogManager.d("Hook初始化成功: " + name);
        } catch (Throwable e) {
            LogManager.e("Hook初始化失败: " + name, e);
            initialized = false;
        }
    }
    
    /**
     * 子类实现具体的初始化逻辑
     */
    protected abstract void init(XC_LoadPackage.LoadPackageParam lpparam);
    
    /**
     * 启用Hook
     */
    public void enable() {
        if (!initialized) {
            LogManager.w("Hook未初始化，无法启用: " + name);
            return;
        }
        this.enabled = true;
        LogManager.d("Hook已启用: " + name);
    }
    
    /**
     * 禁用Hook
     */
    public void disable() {
        this.enabled = false;
        LogManager.d("Hook已禁用: " + name);
    }
    
    /**
     * 销毁Hook
     */
    public void destroy() {
        disable();
        initialized = false;
        lpparam = null;
        LogManager.d("Hook已销毁: " + name);
    }
    
    /**
     * 获取Hook状态信息
     */
    public String getStatus() {
        return String.format("Hook: %s, 启用: %s, 初始化: %s", 
            name, enabled, initialized);
    }
}
