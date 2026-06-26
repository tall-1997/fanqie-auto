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
 * 模块主界面
 */
public class MainActivity extends Activity {
    
    private TextView tvStatus;
    private TextView tvStats;
    private TextView tvLog;
    private ScrollView scrollView;
    private Handler handler;
    private List<String> logList = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper());
        
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
        title.setPadding(0, 0, 0, 8);
        mainLayout.addView(title);
        
        // 版本
        TextView version = new TextView(this);
        version.setText("v9.0.0 | HTTP拦截模式");
        version.setTextSize(14);
        version.setTextColor(Color.parseColor("#4CAF50"));
        version.setGravity(Gravity.CENTER);
        version.setPadding(0, 0, 0, 24);
        mainLayout.addView(version);
        
        // 状态卡片
        LinearLayout statusCard = createCard();
        tvStatus = new TextView(this);
        tvStatus.setText("状态: 等待启动...");
        tvStatus.setTextSize(16);
        tvStatus.setTextColor(Color.parseColor("#FFFFFF"));
        statusCard.addView(tvStatus);
        mainLayout.addView(statusCard);
        
        // 统计卡片
        LinearLayout statsCard = createCard();
        tvStats = new TextView(this);
        tvStats.setText("签到: 0 | 任务: 0 | 奖励: 0");
        tvStats.setTextSize(14);
        tvStats.setTextColor(Color.parseColor("#FFD700"));
        statsCard.addView(tvStats);
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
            ViewGroup.LayoutParams.MATCH_PARENT, 800));
        
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
        
        addLog("模块界面已启动");
        addLog("请在番茄小说中操作以触发日志");
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
    
    public void addLog(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String logEntry = "[" + timestamp + "] " + message;
        
        logList.add(logEntry);
        if (logList.size() > 50) {
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
    
    public void updateStatus(String status) {
        handler.post(() -> tvStatus.setText("状态: " + status));
    }
    
    public void updateStats(int sign, int task, int reward) {
        handler.post(() -> tvStats.setText(
            String.format(Locale.getDefault(), "签到: %d | 任务: %d | 奖励: %d", sign, task, reward)));
    }
}
