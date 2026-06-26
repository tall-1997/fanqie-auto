package com.fanqie.auto.hook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_LoadPackage;

import com.fanqie.auto.utils.LogManager;
import com.fanqie.auto.model.Config;
import com.fanqie.auto.model.SignRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 签到Hook - 自动签到和签到结果处理
 */
public class SignHook extends BaseHook {
    private static final String HOOK_NAME = "SignHook";
    private Config config;
    private final List<SignRecord> signRecords = new ArrayList<>();
    private long totalReward = 0;
    private int successCount = 0;
    private int failCount = 0;
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 3;
    private ScheduledExecutorService retryExecutor;
    
    public SignHook(Config config) {
        super(HOOK_NAME);
        this.config = config;
    }
    
    @Override
    protected void init(XC_LoadPackage.LoadPackageParam lpparam) {
        hookSignResponse(lpparam);
        hookAutoSign(lpparam);
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
                        try {
                            String key = (String) param.args[0];
                            int value = (int) param.getResult();
                            
                            if ("status".equals(key)) {
                                handleSignStatus(value);
                            } else if ("obtain".equals(key)) {
                                handleSignReward(value);
                            }
                        } catch (Throwable e) {
                            LogManager.e("处理签到响应失败", e);
                        }
                    }
                }
            );
            LogManager.d("签到响应Hook初始化成功");
        } catch (Throwable e) {
            LogManager.e("签到响应Hook初始化失败", e);
        }
    }
    
    private void hookAutoSign(XC_LoadPackage.LoadPackageParam lpparam) {
        // 这里可以添加自动签到的逻辑
        // 例如：检测到签到页面时自动点击签到按钮
        // 由于需要具体的UI交互，这里先记录日志
        LogManager.d("自动签到Hook初始化（待实现）");
        
        // 初始化重试执行器
        retryExecutor = Executors.newSingleThreadScheduledExecutor();
    }
    
    private void retrySign() {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            LogManager.i("签到重试，第" + retryCount + "次");
            
            // 延迟重试
            retryExecutor.schedule(() -> {
                // 这里应该调用实际的签到方法
                // 例如：sendSignRequest();
            }, 5, TimeUnit.SECONDS);
        } else {
            LogManager.i("签到重试次数已达上限");
            retryCount = 0;
        }
    }
    
    private void handleSignStatus(int status) {
        if (status == 1) {
            LogManager.i("签到成功!");
            successCount++;
            retryCount = 0; // 重置重试计数
            addSignRecord(true, 0, "签到成功");
        } else if (status == 0) {
            LogManager.i("签到失败或已签到");
            failCount++;
            addSignRecord(false, 0, "签到失败或已签到");
            
            // 如果失败，尝试重试
            if (config.isAutoSignEnabled()) {
                retrySign();
            }
        }
    }
    
    private void handleSignReward(int reward) {
        if (reward > 0) {
            LogManager.i("获得金币: +" + reward);
            totalReward += reward;
            
            // 更新最后一条签到记录
            if (!signRecords.isEmpty()) {
                SignRecord lastRecord = signRecords.get(signRecords.size() - 1);
                if (lastRecord.getReward() == 0) {
                    lastRecord.setReward(reward);
                    lastRecord.setDetails("签到成功，获得金币: +" + reward);
                }
            }
        }
    }
    
    private void addSignRecord(boolean success, int reward, String details) {
        SignRecord record = new SignRecord(success, reward, details);
        signRecords.add(record);
        
        // 保持最近100条记录
        if (signRecords.size() > 100) {
            signRecords.remove(0);
        }
    }
    
    private void sendSignRequest() {
        // 这里应该实现实际的签到请求构造
        // 例如：构造HTTP请求并发送
        LogManager.d("发送签到请求");
        
        // 示例：构造签到请求
        // 1. 获取签到URL
        // 2. 构造请求参数
        // 3. 发送HTTP请求
        // 4. 处理响应
    }
    
    public List<SignRecord> getSignRecords() {
        return new ArrayList<>(signRecords);
    }
    
    public long getTotalReward() {
        return totalReward;
    }
    
    public int getSuccessCount() {
        return successCount;
    }
    
    public int getFailCount() {
        return failCount;
    }
    
    public double getSuccessRate() {
        int total = successCount + failCount;
        if (total == 0) return 0.0;
        return (double) successCount / total;
    }
    
    public void resetStatistics() {
        signRecords.clear();
        totalReward = 0;
        successCount = 0;
        failCount = 0;
    }
    
    @Override
    public void destroy() {
        super.destroy();
        if (retryExecutor != null) {
            retryExecutor.shutdown();
        }
    }
    
    @Override
    public String getStatus() {
        return String.format("%s, 成功: %d, 失败: %d, 总金币: %d, 成功率: %.1f%%", 
            super.getStatus(), successCount, failCount, totalReward, getSuccessRate() * 100);
    }
}
