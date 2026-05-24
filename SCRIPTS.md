# 项目自动化脚本

这个目录包含了一系列自动化脚本，用于简化开发流程。所有脚本都不依赖 Android Studio GUI，可以在命令行直接运行。

## 脚本列表

### 1. `build_debug.sh`
构建 Debug 版本的 APK。

**使用方式：**
```bash
./scripts/build_debug.sh
```

**功能：**
- 自动设置正确的 JDK 路径（使用 Android Studio 自带的 JDK）
- 执行 clean 和 assembleDebug 任务
- 显示 APK 位置

---

### 2. `install_debug.sh`
安装 Debug APK 到连接的设备。

**使用方式：**
```bash
./scripts/install_debug.sh
```

**功能：**
- 检查连接的设备
- 验证 APK 是否存在
- 使用 adb install -r 安装（覆盖安装）

---

### 3. `run_app.sh`
启动应用。

**使用方式：**
```bash
./scripts/run_app.sh
```

**功能：**
- 先尝试用 monkey 启动
- 失败则用 explicit activity 方式启动
- 自动检测连接的设备

---

### 4. `log_crash.sh`
查看崩溃日志。

**使用方式：**
```bash
./scripts/log_crash.sh
```

**功能：**
- 过滤显示应用相关的崩溃日志
- 包含的关键字：FATAL EXCEPTION、AndroidRuntime、FloatingWebViewService 等
- 按 Ctrl+C 停止

---

### 5. `build_install_run.sh`
完整流程：构建 → 安装 → 运行。

**使用方式：**
```bash
./scripts/build_install_run.sh
```

**功能：**
- 顺序执行以上三个步骤
- 任何步骤失败都会停止并报错

---

## 常见 Gradle/ADB 命令

以下是一些常用的命令，可以直接使用：

```bash
# 查看所有可用 Gradle 任务
./gradlew tasks

# 构建 Release 版本
./gradlew assembleRelease

# 运行单元测试
./gradlew test

# 运行设备上的测试
./gradlew connectedAndroidTest

# 查看连接的设备
adb devices -l

# 清空应用数据
adb shell pm clear com.samzebrado.transparentfloatingbrowser

# 查看完整 Logcat
adb logcat
```

---

## 学习路径（来自 ChatGPT 建议）

### 第一阶段：命令行自动化（推荐）
✅ **当前状态：已实现**
- 使用 Gradle wrapper 构建
- 使用 ADB 安装/运行/调试
- 完全不依赖 Android Studio GUI

### 第二阶段：项目专用脚本
✅ **当前状态：已实现**
- 构建脚本
- 安装脚本
- 运行脚本
- 日志脚本

### 第三阶段：Android Studio 插件（可选）
- 当需要在 IDE 内增加按钮/面板时再考虑
- 基于 IntelliJ Platform Plugin SDK

---

## 参考资料

- [Android 命令行构建](https://developer.android.com/studio/build/building-cmdline)
- [Android Gradle Plugin 文档](https://developer.android.com/studio/releases/gradle-plugin)
- [ADB 命令参考](https://developer.android.com/studio/command-line/adb)
