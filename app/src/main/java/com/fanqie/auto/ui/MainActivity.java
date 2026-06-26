package com.fanqie.auto.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

/**
 * 模块主界面 - 显示运行日志和任务详情
 */
public class MainActivity extends Activity {
    
    private TextView tvStatus;
    private TextView tvLog;
    private ScrollView scrollView;
    private Handler handler;
    private List<String> logList = new ArrayList<>();
    private static final int MAX_LOG_LINES = 100;
    
    // 统计数据
    private int signCount = 0;
    private int taskCount = 0;
    private int rewardCount = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper());
        
        // 创建主布局
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#1a1a2e"));
        mainLayout.setPadding(32, 32, 32, 32);
        
        // 标题
        TextView title = new TextView(this);
        title.setText("番茄小说自动签到模块");
        title.setTextSize(24);
        title.setTextColor(Color.parseColor("#e94560"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 16);
        mainLayout.addView(title);
        
        // 版本信息
        TextView version = new TextView(this);
        version.setText("v3.0.0 | 自动签到/领取/任务");
        version.setTextSize(14);
        version.setTextColor(Color.parseColor("#4CAF50"));
        version.setGravity(Gravity.CENTER);
        version.setPadding(0, 0, 0, 24);
        mainLayout.addView(version);
        
        // 状态卡片
        LinearLayout statusCard = createCard();
        
        TextView statusTitle = new TextView(this);
        statusTitle.setText("运行状态");
        statusTitle.setTextSize(16);
        statusTitle.setTextColor(Color.parseColor("#FFD700"));
        statusTitle.setPadding(0, 0, 0, 12);
        statusCard.addView(statusTitle);
        
        tvStatus = new TextView(this);
        tvStatus.setText("正在初始化...");
        tvStatus.setTextSize(14);
        tvStatus.setTextColor(Color.parseColor("#FFFFFF"));
        tvStatus.setLineSpacing(8, 1);
        statusCard.addView(tvStatus);
        
        mainLayout.addView(statusCard);
        
        // 统计卡片
        LinearLayout statsCard = createCard();
        
        TextView statsTitle = new TextView(this);
        statsTitle.setText("任务统计");
        statsTitle.setTextSize(16);
        statsTitle.setTextColor(Color.parseColor("#FFD700"));
        statsTitle.setPadding(0, 0, 0, 12);
        statsCard.addView(statsTitle);
        
        LinearLayout statsLayout = new LinearLayout(this);
        statsLayout.setOrientation(LinearLayout.HORIZONTAL);
        statsLayout.setGravity(Gravity.CENTER);
        
        // 签到统计
        LinearLayout signStat = createStatItem("签到", "0");
        statsLayout.addView(signStat);
        
        // 任务统计
        LinearLayout taskStat = createStatItem("任务", "0");
        statsLayout.addView(taskStat);
        
        // 奖励统计
        LinearLayout rewardStat = createStatItem("奖励", "0");
        statsLayout.addView(rewardStat);
        
        statsCard.addView(statsLayout);
        mainLayout.addView(statsCard);
        
        // 日志卡片
        LinearLayout logCard = createCard();
        
        TextView logTitle = new TextView(this);
        logTitle.setText("运行日志");
        logTitle.setTextSize(16);
        logTitle.setTextColor(Color.parseColor("#FFD700"));
        logTitle.setPadding(0, 0, 0, 12);
        logCard.addView(logTitle);
        
        scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 600));
        
        tvLog = new TextView(this);
        tvLog.setText("等待日志...");
        tvLog.setTextSize(12);
        tvLog.setTextColor(Color.parseColor("#AAAAAA"));
        tvLog.setLineSpacing(4, 1);
        tvLog.setPadding(8, 8, 8, 8);
        
        scrollView.addView(tvLog);
        logCard.addView(scrollView);
        
        mainLayout.addView(logCard);
        
        setContentView(mainLayout);
        
        // 模拟日志更新
        startLogSimulation();
    }
    
    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#16213e"));
        card.setPadding(24, 24, 24, 24);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        return card;
    }
    
    private LinearLayout createStatItem(String label, String value) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        item.setLayoutParams(params);
        
        TextView valueText = new TextView(this);
        valueText.setText(value);
        valueText.setTextSize(24);
        valueText.setTextColor(Color.parseColor("#4CAF50"));
        valueText.setGravity(Gravity.CENTER);
        item.addView(valueText);
        
        TextView labelText = new TextView(this);
        labelText.setText(label);
        labelText.setTextSize(12);
        labelText.setTextColor(Color.parseColor("#AAAAAA"));
        labelText.setGravity(Gravity.CENTER);
        item.addView(labelText);
        
        return item;
    }
    
    private void startLogSimulation() {
        // 模拟日志更新
        handler.postDelayed(() -> {
            addLog("模块初始化完成");
            addLog("开始监控签到接口...");
            updateStatus("运行中 | 监控中");
        }, 1000);
        
        handler.postDelayed(() -> {
            addLog("检测到签到请求");
            addLog("签到成功! +100金币");
            signCount++;
            updateStats();
        }, 3000);
        
        handler.postDelayed(() -> {
            addLog("检测到可完成任务");
            addLog("自动完成任务: 阅读1分钟");
            taskCount++;
            updateStats();
        }, 5000);
        
        handler.postDelayed(() -> {
            addLog("检测到可领取奖励");
            addLog("自动领取奖励: +50金币");
            rewardCount++;
            updateStats();
        }, 7000);
    }
    
    private void addLog(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String logEntry = "[" + timestamp + "] " + message;
        
        logList.add(logEntry);
        if (logList.size() > MAX_LOG_LINES) {
            logList.remove(0);
        }
        
        handler.post(() -> {
            StringBuilder sb = new StringBuilder();
            for (String log : logList) {
                sb.append(log).append("\n");
            }
            tvLog.setText(sb.toString());
            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
        });
    }
    
    private void updateStatus(String status) {
        handler.post(() -> tvStatus.setText(status));
    }
    
    private void updateStats() {
        handler.post(() -> {
            // 更新统计数字
            LinearLayout statsLayout = (LinearLayout) ((LinearLayout) tvStatus.getParent().getParent()).getChildAt(1);
            if (statsLayout instanceof LinearLayout) {
                LinearLayout statsCard = (LinearLayout) statsLayout;
                if (statsCard.getChildCount() > 1) {
                    LinearLayout statsRow = (LinearLayout) statsCard.getChildAt(1);
                    if (statsRow.getChildCount() >= 3) {
                        ((TextView) ((LinearLayout) statsRow.getChildAt(0)).getChildAt(0)).setText(String.valueOf(signCount));
                        ((TextView) ((LinearLayout) statsRow.getChildAt(1)).getChildAt(0)).setText(String.valueOf(taskCount));
                        ((TextView) ((LinearLayout) statsRow.getChildAt(2)).getChildAt(0)).setText(String.valueOf(rewardCount));
                    }
                }
            }
        });
    }
}
