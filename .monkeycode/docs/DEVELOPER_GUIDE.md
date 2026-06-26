# 开发者指南

## 概述

本指南为番茄小说自动签到模块的开发者提供开发规范和最佳实践。

## 开发环境

### 必需工具

- Android Studio 2022.3.1 或更高版本
- Java Development Kit (JDK) 17
- Android SDK 34
- Git

### 可选工具

- LSPosed Framework（用于测试Xposed模块）
- ADB（Android Debug Bridge）
- Postman（用于测试HTTP请求）

### 环境配置

1. **安装Android Studio**
   - 下载并安装Android Studio
   - 配置Android SDK
   - 安装必要的SDK工具

2. **配置项目**
   - 克隆项目仓库
   - 使用Android Studio打开项目
   - 同步Gradle依赖

3. **配置设备**
   - 启用开发者选项
   - 启用USB调试
   - 安装LSPosed Framework

## 项目结构

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/fanqie/auto/
│   │   │   ├── config/          # 配置管理模块
│   │   │   ├── hook/            # Hook实现模块
│   │   │   ├── model/           # 数据模型模块
│   │   │   ├── ui/              # 用户界面模块
│   │   │   └── utils/           # 工具类模块
│   │   ├── res/                 # 资源文件
│   │   └── AndroidManifest.xml  # 应用清单
│   ├── test/                    # 单元测试
│   └── androidTest/             # 集成测试
├── build.gradle                 # 构建配置
└── proguard-rules.pro           # 混淆规则
```

## 编码规范

### 命名规范

#### 1. 类命名
- 使用PascalCase（大驼峰命名法）
- 类名应该是一个名词或名词短语
- 示例：`ConfigManager`、`HookManager`、`MainActivity`

#### 2. 方法命名
- 使用camelCase（小驼峰命名法）
- 方法名应该是一个动词或动词短语
- 示例：`getConfig()`、`initHook()`、`loadFragment()`

#### 3. 变量命名
- 使用camelCase（小驼峰命名法）
- 变量名应该简洁且有意义
- 示例：`config`、`hookManager`、`isEnabled`

#### 4. 常量命名
- 使用UPPER_SNAKE_CASE（大写下划线命名法）
- 常量名应该简洁且有意义
- 示例：`MAX_RETRY_COUNT`、`DEFAULT_PRIORITY`、`LOG_DIR`

#### 5. 包命名
- 使用小写字母
- 包名应该简洁且有意义
- 示例：`com.fanqie.auto.config`、`com.fanqie.auto.hook`

### 代码风格

#### 1. 缩进和空格
- 使用4个空格进行缩进
- 不要使用Tab字符
- 在操作符前后添加空格
- 在逗号后添加空格

#### 2. 大括号
- 左大括号放在行尾
- 右大括号独占一行
- 单行语句也要使用大括号

```java
// 正确
if (condition) {
    doSomething();
}

// 错误
if (condition) doSomething();
```

#### 3. 注释
- 使用JavaDoc注释文档公共API
- 使用行注释解释复杂逻辑
- 避免无用的注释

```java
/**
 * 配置管理器
 * 负责配置的读取、保存和管理
 */
public class ConfigManager {
    /**
     * 获取自动签到配置
     * @return 自动签到是否启用
     */
    public boolean isAutoSignEnabled() {
        // 从SharedPreferences读取配置
        return prefs.getBoolean(KEY_AUTO_SIGN, true);
    }
}
```

#### 4. 导入语句
- 按包分组导入
- 每组之间用空行分隔
- 避免使用通配符导入

```java
// Android导入
import android.content.Context;
import android.content.SharedPreferences;

// Java导入
import java.util.ArrayList;
import java.util.List;

// 第三方库导入
import org.json.JSONObject;

// 项目内部导入
import com.fanqie.auto.model.Config;
```

### 异常处理

#### 1. 异常捕获
- 捕获具体的异常类型
- 避免捕获通用的Exception
- 记录异常日志

```java
try {
    // 可能抛出异常的代码
    JSONObject json = new JSONObject(jsonStr);
} catch (JSONException e) {
    // 记录异常日志
    LogManager.e("JSON解析失败", e);
    // 处理异常
    return false;
}
```

#### 2. 异常抛出
- 在方法签名中声明检查异常
- 提供有意义的异常信息
- 使用自定义异常类

```java
/**
 * 读取配置文件
 * @param file 配置文件
 * @return 配置对象
 * @throws ConfigException 配置读取失败
 */
public Config readConfig(File file) throws ConfigException {
    if (!file.exists()) {
        throw new ConfigException("配置文件不存在: " + file.getAbsolutePath());
    }
    // 读取配置逻辑
}
```

### 日志记录

#### 1. 日志级别
- `DEBUG`：调试信息，开发阶段使用
- `INFO`：重要信息，生产环境使用
- `WARN`：警告信息，潜在问题
- `ERROR`：错误信息，需要关注

#### 2. 日志内容
- 记录关键操作
- 记录错误信息
- 避免记录敏感信息

```java
// 记录关键操作
LogManager.i("用户登录成功: " + username);

// 记录错误信息
LogManager.e("网络请求失败", exception);

// 避免记录敏感信息
LogManager.d("处理请求: " + url); // 不要记录请求参数中的敏感信息
```

## 开发流程

### 1. 功能开发

#### 步骤1：需求分析
- 理解需求文档
- 确定技术方案
- 评估工作量

#### 步骤2：设计实现
- 设计类结构
- 定义接口
- 编写伪代码

#### 步骤3：编码实现
- 遵循编码规范
- 编写清晰代码
- 添加必要注释

#### 步骤4：单元测试
- 编写单元测试
- 测试边界条件
- 测试异常情况

#### 步骤5：集成测试
- 测试组件集成
- 测试完整流程
- 测试性能

#### 步骤6：代码审查
- 自我审查
- 同事审查
- 修复问题

### 2. Bug修复

#### 步骤1：重现问题
- 确认问题存在
- 记录重现步骤
- 收集相关信息

#### 步骤2：分析原因
- 分析日志信息
- 定位问题代码
- 确定根本原因

#### 步骤3：修复问题
- 编写修复代码
- 添加测试用例
- 验证修复效果

#### 步骤4：回归测试
- 测试修复功能
- 测试相关功能
- 确保无副作用

### 3. 代码重构

#### 步骤1：识别问题
- 识别代码异味
- 分析改进点
- 评估重构范围

#### 步骤2：制定计划
- 设计重构方案
- 制定实施计划
- 准备测试用例

#### 步骤3：执行重构
- 逐步重构
- 保持功能不变
- 持续测试验证

#### 步骤4：验证结果
- 运行所有测试
- 检查代码质量
- 确认性能无退化

## 测试规范

### 1. 单元测试

#### 测试命名
- 使用`test`前缀
- 描述测试场景
- 示例：`testIsAutoSignEnabled_DefaultTrue`

#### 测试结构
- 准备测试数据
- 执行测试操作
- 验证测试结果

```java
@Test
public void testIsAutoSignEnabled_DefaultTrue() {
    // 准备
    when(mockPrefs.getBoolean(eq("auto_sign_enabled"), eq(true))).thenReturn(true);
    
    // 执行
    boolean result = configManager.isAutoSignEnabled();
    
    // 验证
    assertTrue(result);
}
```

#### 测试覆盖
- 测试正常流程
- 测试边界条件
- 测试异常情况

### 2. 集成测试

#### 测试范围
- 测试组件交互
- 测试完整流程
- 测试外部依赖

#### 测试环境
- 使用测试数据库
- 模拟外部服务
- 隔离测试环境

### 3. 性能测试

#### 测试指标
- 响应时间
- 内存使用
- CPU占用
- 网络流量

#### 测试工具
- Android Profiler
- LeakCanary
- TraceView

## 版本控制

### 1. Git规范

#### 提交信息
- 使用中文提交信息
- 遵循Conventional Commits规范
- 示例：`feat: 添加自动签到功能`

#### 分支管理
- `main`：主分支，稳定版本
- `develop`：开发分支，最新功能
- `feature/*`：功能分支
- `bugfix/*`：修复分支
- `release/*`：发布分支

#### 工作流程
1. 从`develop`创建功能分支
2. 在功能分支上开发
3. 提交Pull Request
4. 代码审查
5. 合并到`develop`
6. 测试验证
7. 发布到`main`

### 2. 版本号

#### 版本格式
- 主版本号.次版本号.修订号
- 示例：`1.0.0`、`1.1.0`、`1.1.1`

#### 版本规则
- 主版本号：不兼容的API修改
- 次版本号：向下兼容的功能性新增
- 修订号：向下兼容的问题修正

## 文档规范

### 1. 代码文档

#### JavaDoc注释
- 为所有公共API编写JavaDoc
- 包含方法描述、参数、返回值、异常
- 示例：

```java
/**
 * 读取配置文件
 * 
 * @param file 配置文件
 * @return 配置对象，如果读取失败返回null
 * @throws ConfigException 配置读取失败
 */
public Config readConfig(File file) throws ConfigException {
    // 实现逻辑
}
```

#### 行内注释
- 解释复杂逻辑
- 说明设计决策
- 标记待办事项

### 2. 项目文档

#### README.md
- 项目介绍
- 快速开始
- 使用说明
- 贡献指南

#### 架构文档
- 系统架构
- 模块设计
- 数据流设计

#### API文档
- 接口定义
- 参数说明
- 返回值说明

## 发布流程

### 1. 准备发布

#### 检查清单
- [ ] 所有测试通过
- [ ] 代码审查完成
- [ ] 文档更新完成
- [ ] 版本号更新
- [ ] 变更日志更新

#### 版本准备
- 更新版本号
- 更新变更日志
- 准备发布说明

### 2. 构建发布

#### 构建步骤
```bash
# 清理项目
./gradlew clean

# 运行测试
./gradlew test

# 构建Release版本
./gradlew assembleRelease

# 签名APK
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore my-release-key.keystore app-release-unsigned.apk alias_name
```

#### 发布检查
- 检查APK大小
- 检查权限要求
- 检查兼容性

### 3. 发布后

#### 发布后工作
- 创建Git标签
- 发布到GitHub
- 更新文档
- 通知用户

## 问题排查

### 1. 常见问题

#### 构建失败
- 检查依赖版本
- 检查网络连接
- 清理缓存重试

#### 运行时错误
- 检查日志信息
- 检查权限配置
- 检查设备兼容性

#### 性能问题
- 分析性能日志
- 检查内存使用
- 优化关键路径

### 2. 调试技巧

#### 日志调试
- 添加详细日志
- 使用不同日志级别
- 分析日志模式

#### 断点调试
- 设置断点
- 单步执行
- 检查变量值

#### 性能分析
- 使用Android Profiler
- 分析内存快照
- 跟踪方法调用

## 最佳实践

### 1. 代码质量
- 遵循编码规范
- 编写清晰代码
- 定期重构

### 2. 测试质量
- 编写充分测试
- 测试边界条件
- 持续集成测试

### 3. 文档质量
- 保持文档更新
- 编写清晰文档
- 提供示例代码

### 4. 安全实践
- 输入验证
- 权限控制
- 数据加密

### 5. 性能优化
- 避免内存泄漏
- 优化关键路径
- 使用缓存机制
