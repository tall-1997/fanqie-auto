package com.fanqie.auto.hook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fanqie.auto.utils.LogManager;

/**
 * Hook管理器
 */
public class HookManager {
    private final List<BaseHook> hooks = new CopyOnWriteArrayList<>();
    private final List<HookExecutionListener> listeners = new CopyOnWriteArrayList<>();
    private long hookTimeout = 5000; // 5秒超时
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    
    public interface HookExecutionListener {
        void onHookExecuted(String hookName, boolean success, long executionTime);
        void onHookError(String hookName, Throwable error);
    }
    
    public void registerHook(BaseHook hook) {
        hooks.add(hook);
        LogManager.d("注册Hook: " + hook.getName());
    }
    
    public void unregisterHook(BaseHook hook) {
        hooks.remove(hook);
        LogManager.d("注销Hook: " + hook.getName());
    }
    
    public void addHookExecutionListener(HookExecutionListener listener) {
        listeners.add(listener);
    }
    
    public void removeHookExecutionListener(HookExecutionListener listener) {
        listeners.remove(listener);
    }
    
    public void setHookTimeout(long timeout) {
        this.hookTimeout = timeout;
    }
    
    public long getHookTimeout() {
        return hookTimeout;
    }
    
    public void initAllHooks(de.robv.android.xposed.XC_LoadPackage.LoadPackageParam lpparam) {
        LogManager.d("开始初始化所有Hook，总数: " + hooks.size());
        
        // 按优先级排序
        List<BaseHook> sortedHooks = new ArrayList<>(hooks);
        Collections.sort(sortedHooks, Comparator.comparingInt(BaseHook::getPriority));
        
        for (BaseHook hook : sortedHooks) {
            initHook(hook, lpparam);
        }
        
        LogManager.d("所有Hook初始化完成");
    }
    
    public void initHook(BaseHook hook, de.robv.android.xposed.XC_LoadPackage.LoadPackageParam lpparam) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 使用超时控制
            Future<?> future = executor.submit(() -> {
                try {
                    hook.initHook(lpparam);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
            
            try {
                future.get(hookTimeout, TimeUnit.MILLISECONDS);
                long executionTime = System.currentTimeMillis() - startTime;
                notifyHookExecuted(hook.getName(), true, executionTime);
            } catch (TimeoutException e) {
                future.cancel(true);
                long executionTime = System.currentTimeMillis() - startTime;
                LogManager.e("Hook初始化超时: " + hook.getName());
                notifyHookError(hook.getName(), e);
                notifyHookExecuted(hook.getName(), false, executionTime);
            }
            
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - startTime;
            notifyHookError(hook.getName(), e);
            notifyHookExecuted(hook.getName(), false, executionTime);
        }
    }
    
    public void enableAllHooks() {
        for (BaseHook hook : hooks) {
            hook.setEnabled(true);
        }
        LogManager.d("所有Hook已启用");
    }
    
    public void disableAllHooks() {
        for (BaseHook hook : hooks) {
            hook.setEnabled(false);
        }
        LogManager.d("所有Hook已禁用");
    }
    
    public void enableHook(String hookName) {
        for (BaseHook hook : hooks) {
            if (hook.getName().equals(hookName)) {
                hook.setEnabled(true);
                break;
            }
        }
    }
    
    public void disableHook(String hookName) {
        for (BaseHook hook : hooks) {
            if (hook.getName().equals(hookName)) {
                hook.setEnabled(false);
                break;
            }
        }
    }
    
    public void destroyAllHooks() {
        for (BaseHook hook : hooks) {
            hook.destroy();
        }
        hooks.clear();
        executor.shutdown();
        LogManager.d("所有Hook已销毁");
    }
    
    public List<BaseHook> getHooks() {
        return new ArrayList<>(hooks);
    }
    
    public BaseHook getHookByName(String name) {
        for (BaseHook hook : hooks) {
            if (hook.getName().equals(name)) {
                return hook;
            }
        }
        return null;
    }
    
    public int getHookCount() {
        return hooks.size();
    }
    
    public int getEnabledHookCount() {
        int count = 0;
        for (BaseHook hook : hooks) {
            if (hook.isEnabled()) {
                count++;
            }
        }
        return count;
    }
    
    public int getInitializedHookCount() {
        int count = 0;
        for (BaseHook hook : hooks) {
            if (hook.isInitialized()) {
                count++;
            }
        }
        return count;
    }
    
    public String getStatusReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Hook状态报告:\n");
        sb.append("总数: ").append(hooks.size()).append("\n");
        sb.append("已启用: ").append(getEnabledHookCount()).append("\n");
        sb.append("已初始化: ").append(getInitializedHookCount()).append("\n\n");
        
        for (BaseHook hook : hooks) {
            sb.append(hook.getStatus()).append("\n");
        }
        
        return sb.toString();
    }
    
    private void notifyHookExecuted(String hookName, boolean success, long executionTime) {
        for (HookExecutionListener listener : listeners) {
            try {
                listener.onHookExecuted(hookName, success, executionTime);
            } catch (Throwable e) {
                LogManager.e("通知Hook执行监听器失败", e);
            }
        }
    }
    
    private void notifyHookError(String hookName, Throwable error) {
        for (HookExecutionListener listener : listeners) {
            try {
                listener.onHookError(hookName, error);
            } catch (Throwable e) {
                LogManager.e("通知Hook错误监听器失败", e);
            }
        }
    }
}
