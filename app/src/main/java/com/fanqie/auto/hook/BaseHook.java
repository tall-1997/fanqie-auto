package com.fanqie.auto.hook;

import de.robv.android.xposed.XC_LoadPackage;

/**
 * Hook基类
 */
public abstract class BaseHook {
    protected final String name;
    protected boolean enabled;
    
    public BaseHook(String name) {
        this.name = name;
        this.enabled = true;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * 初始化Hook
     */
    public abstract void init(XC_LoadPackage.LoadPackageParam lpparam);
    
    /**
     * 启用Hook
     */
    public abstract void enable();
    
    /**
     * 禁用Hook
     */
    public abstract void disable();
}
