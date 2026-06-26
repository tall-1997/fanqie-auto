package com.fanqie.auto;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.*;

/**
 * 模块主界面
 */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#1a1a2e"));
        layout.setPadding(48, 48, 48, 48);
        
        // 标题
        TextView title = new TextView(this);
        title.setText("番茄小说自动签到");
        title.setTextSize(28);
        title.setTextColor(Color.parseColor("#e94560"));
        title.setPadding(0, 0, 0, 32);
        layout.addView(title);
        
        // 版本信息
        TextView version = new TextView(this);
        version.setText("版本: v0.0.1");
        version.setTextSize(14);
        version.setTextColor(Color.parseColor("#666666"));
        version.setPadding(0, 0, 0, 24);
        layout.addView(version);
        
        // 功能说明
        TextView features = new TextView(this);
        features.setText(
            "功能特性:\n\n" +
            "• 自动监控签到接口\n" +
            "• 自动监控任务接口\n" +
            "• HTTP 请求日志\n" +
            "• 签到结果记录\n\n" +
            "使用说明:\n\n" +
            "1. 在 LSPosed 中启用此模块\n" +
            "2. 作用域选择「番茄免费小说」\n" +
            "3. 重启番茄小说 App\n" +
            "4. 查看 Xposed 日志获取运行状态\n\n" +
            "日志命令:\n" +
            "adb logcat -s Xposed:* FanqieAuto:*"
        );
        features.setTextSize(14);
        features.setTextColor(Color.parseColor("#a8a8a8"));
        layout.addView(features);
        
        setContentView(layout);
    }
}
