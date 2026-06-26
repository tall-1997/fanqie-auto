package com.fanqie.auto.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fanqie.auto.R;
import com.google.android.material.button.MaterialButton;

/**
 * 仪表盘Fragment
 */
public class DashboardFragment extends Fragment {
    
    private TextView tvModuleStatus;
    private TextView tvModuleVersion;
    private TextView tvSignSuccess;
    private TextView tvSignReward;
    private TextView tvSignRate;
    private TextView tvTaskCompleted;
    private TextView tvTaskReward;
    private MaterialButton btnResetStats;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupListeners();
        updateStatistics();
    }
    
    private void initViews(View view) {
        tvModuleStatus = view.findViewById(R.id.tv_module_status);
        tvModuleVersion = view.findViewById(R.id.tv_module_version);
        tvSignSuccess = view.findViewById(R.id.tv_sign_success);
        tvSignReward = view.findViewById(R.id.tv_sign_reward);
        tvSignRate = view.findViewById(R.id.tv_sign_rate);
        tvTaskCompleted = view.findViewById(R.id.tv_task_completed);
        tvTaskReward = view.findViewById(R.id.tv_task_reward);
        btnResetStats = view.findViewById(R.id.btn_reset_stats);
    }
    
    private void setupListeners() {
        btnResetStats.setOnClickListener(v -> {
            // 重置统计数据
            resetStatistics();
        });
    }
    
    private void updateStatistics() {
        // 这里应该从实际的数据源获取统计数据
        // 暂时使用模拟数据
        tvModuleStatus.setText("状态: 运行中");
        tvModuleVersion.setText("版本: v0.0.1");
        
        // 模拟签到统计
        tvSignSuccess.setText("0");
        tvSignReward.setText("0");
        tvSignRate.setText("0%");
        
        // 模拟任务统计
        tvTaskCompleted.setText("0");
        tvTaskReward.setText("0");
    }
    
    private void resetStatistics() {
        // 重置统计数据
        tvSignSuccess.setText("0");
        tvSignReward.setText("0");
        tvSignRate.setText("0%");
        tvTaskCompleted.setText("0");
        tvTaskReward.setText("0");
        
        // 这里应该调用实际的数据重置逻辑
    }
}
