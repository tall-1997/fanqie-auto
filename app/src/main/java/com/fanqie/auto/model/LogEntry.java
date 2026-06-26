package com.fanqie.auto.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 日志条目
 */
public class LogEntry {
    private long timestamp;
    private String level;
    private String tag;
    private String message;
    private String stackTrace;
    
    public LogEntry() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public LogEntry(String level, String tag, String message) {
        this.timestamp = System.currentTimeMillis();
        this.level = level;
        this.tag = tag;
        this.message = message;
    }
    
    public LogEntry(String level, String tag, String message, String stackTrace) {
        this(level, tag, message);
        this.stackTrace = stackTrace;
    }
    
    // Getters and Setters
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getLevel() {
        return level;
    }
    
    public void setLevel(String level) {
        this.level = level;
    }
    
    public String getTag() {
        return tag;
    }
    
    public void setTag(String tag) {
        this.tag = tag;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getStackTrace() {
        return stackTrace;
    }
    
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
    
    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getFormattedTime());
        sb.append(" ").append(level);
        sb.append("/").append(tag);
        sb.append(": ").append(message);
        if (stackTrace != null && !stackTrace.isEmpty()) {
            sb.append("\n").append(stackTrace);
        }
        return sb.toString();
    }
    
    public boolean matchesFilter(String levelFilter, String tagFilter, String keywordFilter) {
        if (levelFilter != null && !levelFilter.isEmpty() && !level.equals(levelFilter)) {
            return false;
        }
        if (tagFilter != null && !tagFilter.isEmpty() && !tag.contains(tagFilter)) {
            return false;
        }
        if (keywordFilter != null && !keywordFilter.isEmpty() && !message.contains(keywordFilter)) {
            return false;
        }
        return true;
    }
}
