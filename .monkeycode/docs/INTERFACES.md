# 接口文档

## 概述

本文档定义了番茄小说自动签到模块的核心接口和数据结构。

## Hook接口

### BaseHook

Hook基类，所有Hook都必须继承此类。

```java
public abstract class BaseHook {
    // 构造方法
    public BaseHook(String name)
    public BaseHook(String name, int priority)
    
    // 生命周期方法
    public final void initHook(XC_LoadPackage.LoadPackageParam lpparam)
    protected abstract void init(XC_LoadPackage.LoadPackageParam lpparam)
    public void enable()
    public void disable()
    public void destroy()
    
    // 状态方法
    public String getName()
    public int getPriority()
    public void setPriority(int priority)
    public boolean isEnabled()
    public void setEnabled(boolean enabled)
    public boolean isInitialized()
    public String getStatus()
}
```

### HookManager

Hook管理器，负责Hook的注册、初始化和管理。

```java
public class HookManager {
    // 注册和注销
    public void registerHook(BaseHook hook)
    public void unregisterHook(BaseHook hook)
    
    // 初始化方法
    public void initAllHooks(XC_LoadPackage.LoadPackageParam lpparam)
    public void initHook(BaseHook hook, XC_LoadPackage.LoadPackageParam lpparam)
    
    // 控制方法
    public void enableAllHooks()
    public void disableAllHooks()
    public void enableHook(String hookName)
    public void disableHook(String hookName)
    public void destroyAllHooks()
    
    // 查询方法
    public List<BaseHook> getHooks()
    public BaseHook getHookByName(String name)
    public int getHookCount()
    public int getEnabledHookCount()
    public int getInitializedHookCount()
    public String getStatusReport()
    
    // 配置方法
    public void setHookTimeout(long timeout)
    public long getHookTimeout()
    
    // 监听器
    public void addHookExecutionListener(HookExecutionListener listener)
    public void removeHookExecutionListener(HookExecutionListener listener)
}
```

### HookExecutionListener

Hook执行监听器，用于监听Hook的执行状态。

```java
public interface HookExecutionListener {
    void onHookExecuted(String hookName, boolean success, long executionTime);
    void onHookError(String hookName, Throwable error);
}
```

## 配置接口

### ConfigManager

配置管理器，负责配置的读取、保存和管理。

```java
public class ConfigManager {
    // 构造方法
    public ConfigManager(Context context)
    
    // 配置读取
    public boolean isAutoSignEnabled()
    public boolean isAutoTaskEnabled()
    public boolean isLoggingEnabled()
    public String getTargetPackage()
    
    // 配置设置
    public void setAutoSignEnabled(boolean enabled)
    public void setAutoTaskEnabled(boolean enabled)
    public void setLoggingEnabled(boolean enabled)
    public void setTargetPackage(String packageName)
    
    // 配置验证
    public boolean isValid()
    
    // 配置重置
    public void resetToDefaults()
    
    // 配置导入导出
    public String exportConfigToJson()
    public boolean importConfigFromJson(String jsonStr)
    public boolean exportConfigToFile(File file)
    public boolean importConfigFromFile(File file)
    
    // 监听器
    public void setConfigChangeListener(ConfigChangeListener listener)
}
```

### ConfigChangeListener

配置变更监听器。

```java
public interface ConfigChangeListener {
    void onConfigChanged(String key, Object value);
}
```

### Config

配置模型，定义配置的数据结构。

```java
public class Config {
    // 构造方法
    public Config()
    
    // 属性访问
    public boolean isAutoSignEnabled()
    public void setAutoSignEnabled(boolean autoSignEnabled)
    public boolean isAutoTaskEnabled()
    public void setAutoTaskEnabled(boolean autoTaskEnabled)
    public boolean isLoggingEnabled()
    public void setLoggingEnabled(boolean loggingEnabled)
    public String getTargetPackage()
    public void setTargetPackage(String targetPackage)
    public List<String> getUrlPatterns()
    public void setUrlPatterns(List<String> urlPatterns)
    
    // URL匹配
    public void addUrlPattern(String pattern)
    public boolean matchesUrl(String url)
}
```

## 数据模型接口

### SignRecord

签到记录模型。

```java
public class SignRecord {
    // 构造方法
    public SignRecord()
    public SignRecord(boolean success, int reward, String details)
    
    // 属性访问
    public long getTimestamp()
    public void setTimestamp(long timestamp)
    public boolean isSuccess()
    public void setSuccess(boolean success)
    public int getReward()
    public void setReward(int reward)
    public String getDetails()
    public void setDetails(String details)
    
    // 格式化方法
    public String getFormattedTime()
    public String toString()
}
```

### LogEntry

日志条目模型。

```java
public class LogEntry {
    // 构造方法
    public LogEntry()
    public LogEntry(String level, String tag, String message)
    public LogEntry(String level, String tag, String message, String stackTrace)
    
    // 属性访问
    public long getTimestamp()
    public void setTimestamp(long timestamp)
    public String getLevel()
    public void setLevel(String level)
    public String getTag()
    public void setTag(String tag)
    public String getMessage()
    public void setMessage(String message)
    public String getStackTrace()
    public void setStackTrace(String stackTrace)
    
    // 格式化方法
    public String getFormattedTime()
    public String toString()
    
    // 过滤方法
    public boolean matchesFilter(String levelFilter, String tagFilter, String keywordFilter)
}
```

## 工具类接口

### LogManager

日志管理器，提供日志记录和管理功能。

```java
public class LogManager {
    // 初始化
    public static void init(Context appContext)
    
    // 配置
    public static void setLoggingEnabled(boolean enabled)
    
    // 日志记录
    public static void d(String message)
    public static void i(String message)
    public static void w(String message)
    public static void e(String message)
    public static void e(String message, Throwable throwable)
    
    // 日志查询
    public static List<LogEntry> getLogs()
    public static List<LogEntry> getFilteredLogs(String level, String tag, String keyword)
    public static String getLogStats()
    
    // 日志管理
    public static void clearLogs()
    public static File getLogFile()
    public static File getLogDir()
    
    // 日志导出
    public static boolean exportLogsToFile(File file)
    public static boolean exportLogsToFile(File file, String level, String keyword)
    public static String exportLogsToString()
    public static String exportLogsToString(String level, String keyword)
}
```

## UI接口

### MainActivity

主Activity，管理Fragment切换。

```java
public class MainActivity extends AppCompatActivity {
    // 生命周期方法
    protected void onCreate(Bundle savedInstanceState)
    
    // Fragment管理
    private void loadFragment(Fragment fragment)
}
```

### DashboardFragment

仪表盘Fragment，显示模块状态和统计信息。

```java
public class DashboardFragment extends Fragment {
    // 生命周期方法
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    public void onViewCreated(View view, Bundle savedInstanceState)
    
    // UI更新方法
    private void updateStatistics()
    private void resetStatistics()
}
```

### LogFragment

日志查看Fragment，支持日志过滤和搜索。

```java
public class LogFragment extends Fragment {
    // 生命周期方法
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    public void onViewCreated(View view, Bundle savedInstanceState)
    
    // 日志操作方法
    private void searchLogs()
    private void clearLogs()
    private void exportLogs()
}
```

### SettingsFragment

设置Fragment，管理模块配置。

```java
public class SettingsFragment extends PreferenceFragmentCompat {
    // 生命周期方法
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    
    // 配置操作方法
    private void exportConfig()
    private void importConfig()
    private void resetConfig()
}
```

## Hook实现接口

### HttpHook

HTTP请求Hook。

```java
public class HttpHook extends BaseHook {
    // 构造方法
    public HttpHook(Config config)
    
    // 生命周期方法
    protected void init(XC_LoadPackage.LoadPackageParam lpparam)
    
    // 统计方法
    public long getRequestCount()
    public long getImportantRequestCount()
    public void resetCounters()
}
```

### SignHook

签到Hook。

```java
public class SignHook extends BaseHook {
    // 构造方法
    public SignHook(Config config)
    
    // 生命周期方法
    protected void init(XC_LoadPackage.LoadPackageParam lpparam)
    public void destroy()
    
    // 统计方法
    public List<SignRecord> getSignRecords()
    public long getTotalReward()
    public int getSuccessCount()
    public int getFailCount()
    public double getSuccessRate()
    public void resetStatistics()
}
```

### TaskHook

任务Hook。

```java
public class TaskHook extends BaseHook {
    // 构造方法
    public TaskHook(Config config)
    
    // 生命周期方法
    protected void init(XC_LoadPackage.LoadPackageParam lpparam)
    
    // 任务信息
    public Map<String, TaskInfo> getTaskMap()
    public int getCompletedTasks()
    public int getFailedTasks()
    public long getTotalTaskReward()
    public void resetStatistics()
    
    // 内部类
    public static class TaskInfo {
        public String taskId;
        public String taskName;
        public boolean completed;
        public boolean rewardCollected;
        public long completedTime;
        public int reward;
    }
}
```

## 事件接口

### 配置变更事件

当配置发生变更时触发。

```java
public interface ConfigChangeListener {
    void onConfigChanged(String key, Object value);
}
```

### Hook执行事件

当Hook执行完成时触发。

```java
public interface HookExecutionListener {
    void onHookExecuted(String hookName, boolean success, long executionTime);
    void onHookError(String hookName, Throwable error);
}
```

## 常量定义

### 配置常量

```java
// SharedPreferences名称
private static final String PREF_NAME = "fanqie_auto_config";

// 配置键名
private static final String KEY_AUTO_SIGN = "auto_sign_enabled";
private static final String KEY_AUTO_TASK = "auto_task_enabled";
private static final String KEY_LOGGING = "logging_enabled";
private static final String KEY_TARGET_PACKAGE = "target_package";
```

### 日志常量

```java
// 日志标签
private static final String TAG = "[FanqieAuto] ";
private static final String LOG_TAG = "FanqieAuto";

// 日志文件配置
private static final String LOG_DIR = "logs";
private static final String LOG_FILE_PREFIX = "fanqie_auto_";
private static final String LOG_FILE_SUFFIX = ".log";
private static final long MAX_LOG_FILE_SIZE = 10 * 1024 * 1024; // 10MB
private static final int MAX_LOG_FILES = 5;
private static final int MAX_BUFFER_SIZE = 1000;
```

### Hook常量

```java
// Hook名称
private static final String HOOK_NAME = "HttpHook";
private static final String HOOK_NAME = "SignHook";
private static final String HOOK_NAME = "TaskHook";

// 默认优先级
public static final int DEFAULT_PRIORITY = 100;

// 超时配置
private long hookTimeout = 5000; // 5秒

// 重试配置
private static final int MAX_RETRY_COUNT = 3;
```

## 错误码定义

### 配置错误

- `CONFIG_INVALID_PACKAGE` - 无效的包名
- `CONFIG_FILE_NOT_FOUND` - 配置文件不存在
- `CONFIG_PARSE_ERROR` - 配置解析错误

### Hook错误

- `HOOK_INIT_FAILED` - Hook初始化失败
- `HOOK_TIMEOUT` - Hook执行超时
- `HOOK_METHOD_NOT_FOUND` - 目标方法不存在

### 网络错误

- `NETWORK_REQUEST_FAILED` - 网络请求失败
- `NETWORK_RESPONSE_ERROR` - 网络响应错误
- `NETWORK_SSL_ERROR` - SSL证书错误

## 回调接口

### 异步回调

```java
public interface AsyncCallback<T> {
    void onSuccess(T result);
    void onError(Throwable error);
}
```

### 进度回调

```java
public interface ProgressCallback {
    void onProgressUpdate(int progress);
    void onComplete();
    void onError(Throwable error);
}
```
