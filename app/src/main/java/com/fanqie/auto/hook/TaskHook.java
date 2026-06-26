package com.fanqie.auto.hook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_LoadPackage;

import com.fanqie.auto.utils.LogManager;
import com.fanqie.auto.model.Config;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务Hook - 自动任务处理和奖励收集
 */
public class TaskHook extends BaseHook {
    private static final String HOOK_NAME = "TaskHook";
    private Config config;
    private final Map<String, TaskInfo> taskMap = new HashMap<>();
    private int completedTasks = 0;
    private int failedTasks = 0;
    private long totalTaskReward = 0;
    
    public static class TaskInfo {
        public String taskId;
        public String taskName;
        public boolean completed;
        public boolean rewardCollected;
        public long completedTime;
        public int reward;
        
        public TaskInfo(String taskId, String taskName) {
            this.taskId = taskId;
            this.taskName = taskName;
            this.completed = false;
            this.rewardCollected = false;
        }
    }
    
    public TaskHook(Config config) {
        super(HOOK_NAME);
        this.config = config;
    }
    
    @Override
    protected void init(XC_LoadPackage.LoadPackageParam lpparam) {
        hookTaskResponse(lpparam);
        hookTaskCompletion(lpparam);
        hookRewardCollection(lpparam);
    }
    
    private void hookTaskResponse(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook任务列表响应
            XposedHelpers.findAndHookMethod(
                "org.json.JSONObject",
                lpparam.classLoader,
                "optString",
                String.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            String key = (String) param.args[0];
                            String value = (String) param.getResult();
                            
                            if ("task_id".equals(key) && value != null && !value.isEmpty()) {
                                LogManager.d("检测到任务ID: " + value);
                                processTaskId(value);
                            }
                        } catch (Throwable e) {
                            LogManager.e("处理任务响应失败", e);
                        }
                    }
                }
            );
            LogManager.d("任务响应Hook初始化成功");
        } catch (Throwable e) {
            LogManager.e("任务响应Hook初始化失败", e);
        }
    }
    
    private void hookTaskCompletion(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook任务完成状态
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
                            
                            if ("task_status".equals(key) && value == 1) {
                                LogManager.i("任务完成");
                                handleTaskCompletion();
                            }
                        } catch (Throwable e) {
                            LogManager.e("处理任务完成状态失败", e);
                        }
                    }
                }
            );
            LogManager.d("任务完成Hook初始化成功");
        } catch (Throwable e) {
            LogManager.e("任务完成Hook初始化失败", e);
        }
    }
    
    private void hookRewardCollection(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook奖励领取
            XposedHelpers.findAndHookMethod(
                "org.json.JSONObject",
                lpparam.classLoader,
                "optLong",
                String.class,
                long.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            String key = (String) param.args[0];
                            long value = (long) param.getResult();
                            
                            if ("task_reward".equals(key) && value > 0) {
                                LogManager.i("任务奖励: +" + value);
                                handleTaskReward(value);
                            }
                        } catch (Throwable e) {
                            LogManager.e("处理任务奖励失败", e);
                        }
                    }
                }
            );
            LogManager.d("任务奖励Hook初始化成功");
        } catch (Throwable e) {
            LogManager.e("任务奖励Hook初始化失败", e);
        }
    }
    
    private void processTaskId(String taskId) {
        if (!taskMap.containsKey(taskId)) {
            TaskInfo taskInfo = new TaskInfo(taskId, "任务-" + taskId);
            taskMap.put(taskId, taskInfo);
            LogManager.d("添加新任务: " + taskId);
        }
    }
    
    private void handleTaskCompletion() {
        completedTasks++;
        LogManager.i("任务完成计数: " + completedTasks);
        
        // 更新任务状态
        for (TaskInfo taskInfo : taskMap.values()) {
            if (!taskInfo.completed) {
                taskInfo.completed = true;
                taskInfo.completedTime = System.currentTimeMillis();
                LogManager.d("标记任务完成: " + taskInfo.taskId);
                break;
            }
        }
    }
    
    private void handleTaskReward(long reward) {
        totalTaskReward += reward;
        LogManager.i("任务总奖励: " + totalTaskReward);
        
        // 更新任务奖励状态
        for (TaskInfo taskInfo : taskMap.values()) {
            if (taskInfo.completed && !taskInfo.rewardCollected) {
                taskInfo.rewardCollected = true;
                taskInfo.reward = (int) reward;
                LogManager.d("标记任务奖励已领取: " + taskInfo.taskId);
                break;
            }
        }
    }
    
    public Map<String, TaskInfo> getTaskMap() {
        return new HashMap<>(taskMap);
    }
    
    public int getCompletedTasks() {
        return completedTasks;
    }
    
    public int getFailedTasks() {
        return failedTasks;
    }
    
    public long getTotalTaskReward() {
        return totalTaskReward;
    }
    
    public void resetStatistics() {
        taskMap.clear();
        completedTasks = 0;
        failedTasks = 0;
        totalTaskReward = 0;
    }
    
    @Override
    public String getStatus() {
        return String.format("%s, 完成任务: %d, 失败任务: %d, 总奖励: %d", 
            super.getStatus(), completedTasks, failedTasks, totalTaskReward);
    }
}
