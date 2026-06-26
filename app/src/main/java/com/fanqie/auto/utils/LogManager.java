package com.fanqie.auto.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.fanqie.auto.model.LogEntry;

/**
 * 日志管理器
 */
public class LogManager {
    private static final String TAG = "[FanqieAuto] ";
    private static final String LOG_TAG = "FanqieAuto";
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE_PREFIX = "fanqie_auto_";
    private static final String LOG_FILE_SUFFIX = ".log";
    private static final long MAX_LOG_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_LOG_FILES = 5;
    
    private static boolean loggingEnabled = true;
    private static Context context;
    private static File logDir;
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final List<LogEntry> logBuffer = new ArrayList<>();
    private static final int MAX_BUFFER_SIZE = 1000;
    
    public static void init(Context appContext) {
        context = appContext;
        logDir = new File(context.getFilesDir(), LOG_DIR);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        cleanOldLogFiles();
    }
    
    public static void setLoggingEnabled(boolean enabled) {
        loggingEnabled = enabled;
    }
    
    public static void d(String message) {
        log("DEBUG", message, null);
    }
    
    public static void i(String message) {
        log("INFO", message, null);
    }
    
    public static void w(String message) {
        log("WARN", message, null);
    }
    
    public static void e(String message) {
        log("ERROR", message, null);
    }
    
    public static void e(String message, Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        log("ERROR", message, sw.toString());
    }
    
    private static void log(String level, String message, String stackTrace) {
        if (!loggingEnabled) return;
        
        LogEntry entry = new LogEntry(level, LOG_TAG, message, stackTrace);
        
        // 输出到Android日志
        switch (level) {
            case "DEBUG":
                Log.d(LOG_TAG, message);
                break;
            case "INFO":
                Log.i(LOG_TAG, message);
                break;
            case "WARN":
                Log.w(LOG_TAG, message);
                break;
            case "ERROR":
                Log.e(LOG_TAG, message);
                break;
        }
        
        // 输出到Xposed日志（使用Android Log代替）
        Log.i(LOG_TAG, TAG + message);
        
        // 写入文件
        writeToFile(entry);
        
        // 添加到缓冲区
        addToBuffer(entry);
    }
    
    private static void writeToFile(LogEntry entry) {
        if (logDir == null) return;
        
        lock.writeLock().lock();
        try {
            String fileName = LOG_FILE_PREFIX + getCurrentDate() + LOG_FILE_SUFFIX;
            File logFile = new File(logDir, fileName);
            
            // 检查文件大小，进行轮转
            if (logFile.exists() && logFile.length() > MAX_LOG_FILE_SIZE) {
                rotateLogFile(logFile);
            }
            
            FileWriter writer = new FileWriter(logFile, true);
            writer.append(entry.toString()).append("\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "写入日志文件失败", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private static void rotateLogFile(File logFile) {
        // 删除最旧的日志文件
        File[] logFiles = logDir.listFiles((dir, name) -> 
            name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_SUFFIX));
        
        if (logFiles != null && logFiles.length >= MAX_LOG_FILES) {
            // 按修改时间排序
            java.util.Arrays.sort(logFiles, (f1, f2) -> 
                Long.compare(f1.lastModified(), f2.lastModified()));
            
            // 删除最旧的文件
            for (int i = 0; i < logFiles.length - MAX_LOG_FILES + 1; i++) {
                logFiles[i].delete();
            }
        }
        
        // 重命名当前文件
        String newName = logFile.getName().replace(LOG_FILE_SUFFIX, 
            "_" + System.currentTimeMillis() + LOG_FILE_SUFFIX);
        File newFile = new File(logDir, newName);
        logFile.renameTo(newFile);
    }
    
    private static void cleanOldLogFiles() {
        if (logDir == null) return;
        
        File[] logFiles = logDir.listFiles((dir, name) -> 
            name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_SUFFIX));
        
        if (logFiles != null && logFiles.length > MAX_LOG_FILES) {
            // 按修改时间排序
            java.util.Arrays.sort(logFiles, (f1, f2) -> 
                Long.compare(f1.lastModified(), f2.lastModified()));
            
            // 删除多余的文件
            for (int i = 0; i < logFiles.length - MAX_LOG_FILES; i++) {
                logFiles[i].delete();
            }
        }
    }
    
    private static void addToBuffer(LogEntry entry) {
        lock.writeLock().lock();
        try {
            logBuffer.add(entry);
            if (logBuffer.size() > MAX_BUFFER_SIZE) {
                logBuffer.remove(0);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public static List<LogEntry> getLogs() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(logBuffer);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public static List<LogEntry> getFilteredLogs(String level, String tag, String keyword) {
        lock.readLock().lock();
        try {
            List<LogEntry> filtered = new ArrayList<>();
            for (LogEntry entry : logBuffer) {
                if (entry.matchesFilter(level, tag, keyword)) {
                    filtered.add(entry);
                }
            }
            return filtered;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public static void clearLogs() {
        lock.writeLock().lock();
        try {
            logBuffer.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public static File getLogFile() {
        if (logDir == null) return null;
        String fileName = LOG_FILE_PREFIX + getCurrentDate() + LOG_FILE_SUFFIX;
        return new File(logDir, fileName);
    }
    
    public static File getLogDir() {
        return logDir;
    }
    
    private static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    public static String getLogStats() {
        lock.readLock().lock();
        try {
            int debugCount = 0, infoCount = 0, warnCount = 0, errorCount = 0;
            for (LogEntry entry : logBuffer) {
                switch (entry.getLevel()) {
                    case "DEBUG": debugCount++; break;
                    case "INFO": infoCount++; break;
                    case "WARN": warnCount++; break;
                    case "ERROR": errorCount++; break;
                }
            }
            return String.format(Locale.getDefault(), 
                "总计: %d条 (DEBUG: %d, INFO: %d, WARN: %d, ERROR: %d)",
                logBuffer.size(), debugCount, infoCount, warnCount, errorCount);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public static boolean exportLogsToFile(File file) {
        lock.readLock().lock();
        try {
            try (FileWriter writer = new FileWriter(file)) {
                for (LogEntry entry : logBuffer) {
                    writer.write(entry.toString());
                    writer.write("\n");
                }
                return true;
            } catch (IOException e) {
                LogManager.e("导出日志失败", e);
                return false;
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public static boolean exportLogsToFile(File file, String level, String keyword) {
        lock.readLock().lock();
        try {
            try (FileWriter writer = new FileWriter(file)) {
                for (LogEntry entry : logBuffer) {
                    if (entry.matchesFilter(level, null, keyword)) {
                        writer.write(entry.toString());
                        writer.write("\n");
                    }
                }
                return true;
            } catch (IOException e) {
                LogManager.e("导出日志失败", e);
                return false;
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public static String exportLogsToString() {
        lock.readLock().lock();
        try {
            StringBuilder sb = new StringBuilder();
            for (LogEntry entry : logBuffer) {
                sb.append(entry.toString()).append("\n");
            }
            return sb.toString();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public static String exportLogsToString(String level, String keyword) {
        lock.readLock().lock();
        try {
            StringBuilder sb = new StringBuilder();
            for (LogEntry entry : logBuffer) {
                if (entry.matchesFilter(level, null, keyword)) {
                    sb.append(entry.toString()).append("\n");
                }
            }
            return sb.toString();
        } finally {
            lock.readLock().unlock();
        }
    }
}
