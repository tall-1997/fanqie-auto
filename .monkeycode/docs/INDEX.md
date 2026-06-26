# 番茄小说自动签到模块文档

## 项目概述

这是一个Android Xposed模块，用于番茄小说应用的自动签到和任务完成。该模块通过Xposed框架 hook 目标应用的关键方法，实现自动签到、任务监控和奖励收集功能。

## 文档结构

- [INDEX.md](INDEX.md) - 文档索引（本文件）
- [ARCHITECTURE.md](ARCHITECTURE.md) - 系统架构文档
- [INTERFACES.md](INTERFACES.md) - 接口文档
- [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) - 开发者指南

## 快速开始

### 环境要求

- Android Studio 2022.3.1 或更高版本
- Android SDK 34
- Java 17
- Xposed Framework 82

### 构建项目

1. 克隆项目
2. 使用Android Studio打开项目
3. 构建项目：`./gradlew assembleDebug`
4. 安装APK到设备
5. 在LSPosed中启用模块
6. 作用域选择"番茄免费小说"
7. 重启番茄小说应用

### 模块功能

- **自动签到**：监控签到接口，自动记录签到结果
- **自动任务**：监控任务接口，自动完成简单任务
- **HTTP监控**：记录所有HTTP请求和响应
- **日志记录**：详细记录所有操作日志
- **配置管理**：支持自定义配置

## 项目结构

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/fanqie/auto/
│   │   │   ├── config/          # 配置管理
│   │   │   ├── hook/            # Hook实现
│   │   │   ├── model/           # 数据模型
│   │   │   ├── ui/              # 用户界面
│   │   │   └── utils/           # 工具类
│   │   ├── res/                 # 资源文件
│   │   └── AndroidManifest.xml  # 应用清单
│   ├── test/                    # 单元测试
│   └── androidTest/             # 集成测试
├── build.gradle                 # 构建配置
└── proguard-rules.pro           # 混淆规则
```

## 核心组件

### Hook管理器
- `BaseHook` - Hook基类
- `HookManager` - Hook管理器
- `HttpHook` - HTTP请求Hook
- `SignHook` - 签到Hook
- `TaskHook` - 任务Hook

### 配置管理
- `ConfigManager` - 配置管理器
- `Config` - 配置模型

### 用户界面
- `MainActivity` - 主Activity
- `DashboardFragment` - 仪表盘
- `LogFragment` - 日志查看
- `SettingsFragment` - 设置界面

### 工具类
- `LogManager` - 日志管理器

## 开发指南

### 添加新Hook

1. 继承`BaseHook`类
2. 实现`init`方法
3. 在`HookManager`中注册Hook
4. 在`MainHook`中初始化

### 修改配置

1. 在`ConfigManager`中添加新的配置项
2. 在`Config`模型中添加对应字段
3. 在`SettingsFragment`中添加UI控件
4. 实现配置的读取和保存

### 添加日志

1. 使用`LogManager`记录日志
2. 选择合适的日志级别（DEBUG、INFO、WARN、ERROR）
3. 日志会自动写入文件和Xposed日志

## 版本历史

### v0.0.1 (当前版本)
- 初始版本
- 基础Hook框架
- 自动签到监控
- HTTP请求监控
- 基础UI界面

## 许可证

本项目仅供学习和研究使用。
