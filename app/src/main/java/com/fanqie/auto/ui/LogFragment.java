package com.fanqie.auto.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fanqie.auto.R;
import com.google.android.material.button.MaterialButton;

/**
 * 日志Fragment
 */
public class LogFragment extends Fragment {
    
    private Spinner spinnerLevel;
    private EditText etSearch;
    private MaterialButton btnSearch;
    private MaterialButton btnClearLogs;
    private MaterialButton btnExportLogs;
    private RecyclerView rvLogs;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupSpinner();
        setupListeners();
        setupRecyclerView();
    }
    
    private void initViews(View view) {
        spinnerLevel = view.findViewById(R.id.spinner_level);
        etSearch = view.findViewById(R.id.et_search);
        btnSearch = view.findViewById(R.id.btn_search);
        btnClearLogs = view.findViewById(R.id.btn_clear_logs);
        btnExportLogs = view.findViewById(R.id.btn_export_logs);
        rvLogs = view.findViewById(R.id.rv_logs);
    }
    
    private void setupSpinner() {
        String[] levels = {"全部", "DEBUG", "INFO", "WARN", "ERROR"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            levels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLevel.setAdapter(adapter);
    }
    
    private void setupListeners() {
        btnSearch.setOnClickListener(v -> {
            // 搜索日志
            searchLogs();
        });
        
        btnClearLogs.setOnClickListener(v -> {
            // 清除日志
            clearLogs();
        });
        
        btnExportLogs.setOnClickListener(v -> {
            // 导出日志
            exportLogs();
        });
    }
    
    private void setupRecyclerView() {
        // 设置RecyclerView
        rvLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        // 这里应该设置实际的适配器
        // rvLogs.setAdapter(new LogAdapter());
    }
    
    private void searchLogs() {
        String level = spinnerLevel.getSelectedItem().toString();
        String keyword = etSearch.getText().toString().trim();
        
        // 这里应该调用实际的日志搜索逻辑
        // LogManager.getFilteredLogs(level, null, keyword);
    }
    
    private void clearLogs() {
        // 这里应该调用实际的日志清除逻辑
        // LogManager.clearLogs();
    }
    
    private void exportLogs() {
        // 这里应该调用实际的日志导出逻辑
        // LogManager.exportLogs();
    }
}
