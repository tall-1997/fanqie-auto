package com.fanqie.auto.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 签到记录
 */
public class SignRecord {
    private long timestamp;
    private boolean success;
    private int reward;
    private String details;
    
    public SignRecord() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public SignRecord(boolean success, int reward, String details) {
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.reward = reward;
        this.details = details;
    }
    
    // Getters and Setters
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public int getReward() {
        return reward;
    }
    
    public void setReward(int reward) {
        this.reward = reward;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s (+%d金币)", 
            getFormattedTime(), 
            success ? "成功" : "失败", 
            details, 
            reward);
    }
}
