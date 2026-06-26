package com.fanqie.auto.model;

import java.util.List;
import java.util.ArrayList;

/**
 * 配置模型
 */
public class Config {
    private boolean autoSignEnabled = true;
    private boolean autoTaskEnabled = true;
    private boolean loggingEnabled = true;
    private String targetPackage = "com.dragon.read";
    private List<String> urlPatterns = new ArrayList<>();
    
    public Config() {
        // 默认URL模式
        urlPatterns.add("sign");
        urlPatterns.add("task");
        urlPatterns.add("reward");
        urlPatterns.add("gold");
        urlPatterns.add("luckycat");
    }
    
    // Getters and Setters
    public boolean isAutoSignEnabled() {
        return autoSignEnabled;
    }
    
    public void setAutoSignEnabled(boolean autoSignEnabled) {
        this.autoSignEnabled = autoSignEnabled;
    }
    
    public boolean isAutoTaskEnabled() {
        return autoTaskEnabled;
    }
    
    public void setAutoTaskEnabled(boolean autoTaskEnabled) {
        this.autoTaskEnabled = autoTaskEnabled;
    }
    
    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }
    
    public void setLoggingEnabled(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }
    
    public String getTargetPackage() {
        return targetPackage;
    }
    
    public void setTargetPackage(String targetPackage) {
        this.targetPackage = targetPackage;
    }
    
    public List<String> getUrlPatterns() {
        return urlPatterns;
    }
    
    public void setUrlPatterns(List<String> urlPatterns) {
        this.urlPatterns = urlPatterns;
    }
    
    public void addUrlPattern(String pattern) {
        if (!urlPatterns.contains(pattern)) {
            urlPatterns.add(pattern);
        }
    }
    
    public boolean matchesUrl(String url) {
        if (url == null) return false;
        for (String pattern : urlPatterns) {
            if (url.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}
