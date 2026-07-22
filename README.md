# 透明悬浮浏览器
# Transparent Floating Browser

## 简介 / Introduction
一款轻量、高效、透明背景的悬浮 WebView 浏览器。
A lightweight, efficient floating WebView browser with transparent background support.

可显示指定网页，支持透明背景，适用于显示本地心跳页面等场景。
Displays specified web pages with transparent background, ideal for showing local heartbeat pages and similar use cases.

## 功能 / Features
- 透明背景 WebView 悬浮窗显示
- Transparent background WebView overlay display
- **多窗口支持：可同时显示多个悬浮窗口，最大支持3个窗口**
- **Multi-window support: Can display multiple floating windows simultaneously, up to 3 windows**
- 支持 EDIT / DISPLAY 两种模式切换
- Supports EDIT / DISPLAY mode switching
- 编辑模式下可拖动、缩放、点击网页
- In EDIT mode, the overlay can be dragged, resized, and clicked
- 显示模式下触摸穿透，不阻挡下层 App 操作
- In DISPLAY mode, touch events pass through without blocking underlying App operations
- **Android 12+ 自动透明度修正：根据窗口重叠数量动态调整透明度，保证触摸穿透正常工作**
- **Android 12+ automatic opacity correction: dynamically adjusts opacity based on window overlap to ensure touch pass-through works correctly**
- 独立的控制按钮悬浮窗，始终可点击
- Independent control button overlay, always clickable
- 支持黑色背景 DOM 元素透明化
- Supports black background DOM element transparency
- 支持双指缩放网页
- Supports pinch-to-zoom
- 支持 HTTP 和 HTTPS URL
- Supports both HTTP and HTTPS URLs
- 中英双语界面，可随时切换
- Chinese and English bilingual interface, switchable at any time
- **窗口配置导入/导出（JSON格式）**
- **Window configuration import/export (JSON format)**
- **窗口可见性控制**
- **Window visibility control**

## 模式说明 / Modes

### EDIT 模式 / EDIT Mode
- 窗口可拖动、缩放、操作网页内容
- Window can be moved, resized, and interacted with
- 窗口不透明度固定为 100%
- Window opacity fixed at 100%
- 显示蓝色拖动条和黄色缩放手柄
- Shows blue drag handle and yellow resize handle

### DISPLAY 模式 / DISPLAY Mode
- 隐藏拖动条和缩放手柄
- Hides drag and resize handles
- 支持触摸穿透到底层应用
- Supports touch pass-through to underlying apps
- 根据用户设置和系统限制自动调整透明度
- Automatically adjusts opacity based on user settings and system limits

## 使用方法 / Usage
1. 安装应用后，打开主界面
1. After installing, open the main app interface
2. 点击"检查权限"按钮，授予悬浮窗权限
2. Click "Check Permission" button to grant overlay permission
3. 在输入框中输入网址，或使用默认网址
3. Enter a URL in the input field, or use the default URL
4. 点击"启动悬浮窗"按钮启动悬浮浏览器
4. Click "Start Service" button to start the floating browser
5. 点击悬浮的控制按钮切换 EDIT / DISPLAY 模式
5. Click the floating control button to switch between EDIT and DISPLAY modes
6. 长按控制按钮可关闭悬浮窗
6. Long-press the control button to close the overlay

## 安全说明 / Security Notes
- 本应用可加载任意 URL
- This app can load arbitrary URLs
- WebView 已启用 JavaScript
- WebView has JavaScript enabled
- 本应用未使用 addJavascriptInterface，无原生桥接风险
- This app does not use addJavascriptInterface, no native bridge security risk
- HTTP cleartext 支持仅用于局域网本地工具场景
- HTTP cleartext support is for local LAN tool scenarios only
- 请勿在悬浮窗口中输入敏感信息（如密码、Token）
- Do not enter sensitive information (passwords, tokens) in the floating window
- 悬浮窗权限需要用户手动授权
- Overlay permission requires manual user authorization

## 构建 / Build

### 构建环境 / Build Environment
- Gradle Wrapper: 8.4
- Android Gradle Plugin (AGP): 8.2.1
- Kotlin: 1.9.22
- Java / JDK: 17
- compileSdk: 34
- targetSdk: 34
- minSdk: 27

### 构建命令 / Build Command
```bash
./gradlew :app:assembleDebug
```

构建完成后，APK 文件位于：
After building, the APK is located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 已验证构建环境 / Verified Build Environment
- macOS (Apple Silicon aarch64)
- JDK 17 (Homebrew OpenJDK 17.0.19)
- Gradle 8.4 (via wrapper)
- 构建状态：BUILD SUCCESSFUL / Build status: BUILD SUCCESSFUL

## Android 12+ 触摸穿透与透明度限制
## Android 12+ Touch Pass-through and Opacity Limitation

从 Android 12 开始，系统会阻止某些穿过悬浮窗的触摸事件，以防止恶意 overlay 遮挡或诱导用户点击下层应用。`TYPE_APPLICATION_OVERLAY` 类型的悬浮窗不属于 trusted window，因此即使设置了 `FLAG_NOT_TOUCHABLE`，也仍然受到系统的遮挡透明度限制。

Starting from Android 12, the system may block touch events that pass through overlay windows to prevent unsafe overlay-based interactions. Windows of type `TYPE_APPLICATION_OVERLAY` are not trusted windows, so even when `FLAG_NOT_TOUCHABLE` is used, they are still subject to the system's obscuring-opacity limit.

### 系统限制 / System Limitation

Android 12+ 默认允许的最大 **combined obscuring opacity**（合成不透明度）通常为 `0.8`。如果某个触摸位置上方被一个或多个悬浮窗遮挡，并且这些窗口的合成不透明度超过系统限制，下层应用可能无法收到触摸事件。

On Android 12+, the default maximum combined obscuring opacity is usually `0.8`. If one or more overlay windows cover the touched area and their combined opacity exceeds the system limit, the underlying app may not receive the touch event.

合成不透明度的计算方式：
Combined opacity calculation:

```text
combinedOpacity = 1 - (1 - alpha1) * (1 - alpha2) * ... * (1 - alphaN)
```

其中 `alpha`（不透明度）取值范围为 0.0（完全透明）到 1.0（完全不透明）。
Where `alpha` (opacity) ranges from 0.0 (fully transparent) to 1.0 (fully opaque).

### 本项目的自动修正 / Automatic Correction in This Project

本项目在 DISPLAY 模式下会自动计算安全 alpha 值，避免用户设置过高透明度时导致触摸穿透失效。具体规则如下：

This app automatically calculates a safe alpha value in DISPLAY mode to reduce the chance that an overly opaque overlay blocks touch pass-through. The rules are:

| 重叠窗口数量 / Overlapping windows | 每个窗口安全不透明度上限 / Safe opacity per window |
|-----------------------------------|---------------------------------------------------|
| 1 个 / 1 window                   | ~80%                                              |
| 2 个 / 2 windows                  | ~55%                                              |
| 3 个 / 3 windows                  | ~42%                                              |

注意：窗口越多、重叠越明显，每个窗口允许的安全不透明度就越低。
Note: The more overlapping windows, the lower the safe opacity per window.

## 已验证环境 / Tested Environment
- 小米平板 (Android) / Xiaomi Tablet (Android)
- Android SDK 34
- macOS Apple Silicon (aarch64)
- JDK 17 (Homebrew OpenJDK 17.0.19)
- Gradle 8.4 (via wrapper)
- AGP 8.2.1 / Kotlin 1.9.22

## 已知限制 / Known Limitations
- DISPLAY 模式下蓝色拖动条和黄色缩放手柄会隐藏
- In DISPLAY mode, the blue drag handle and yellow resize handle are hidden
- **Android 12+ 会限制不可信悬浮窗的合成不透明度，若窗口过于不透明，触摸穿透可能被系统阻止**
- **Android 12+ limits the combined obscuring opacity of untrusted overlay windows. If the overlay is too opaque, touch pass-through may be blocked by the system**
- 多个 DISPLAY 模式窗口重叠时，系统会按合成不透明度判断是否允许触摸穿透
- When multiple DISPLAY-mode windows overlap, the system evaluates their combined opacity
- 不支持 WebView 内播放受 DRM 保护的内容
- Does not support DRM-protected content playback in WebView

## 项目结构 / Project Structure
```
app/src/main/java/com/samzebrado/transparentfloatingbrowser/
├── MainActivity.kt              # 主界面 / Main interface
├── FloatingWebViewService.kt     # 悬浮窗服务 / Floating window service
├── FloatingWebViewController.kt  # WebView 控制 / WebView controller
├── OverlayControlBubble.kt       # 控制按钮 / Control button
├── TransparentStyleInjector.kt   # 透明样式注入 / Transparent style injection
├── AppPrefs.kt                  # 偏好设置 / App preferences
├── LocaleHelper.kt               # 语言切换 / Language helper
├── FloatingWindowConfig.kt       # 窗口配置数据类 / Window configuration data class
├── FloatingWindowInstance.kt     # 运行时窗口实例 / Runtime window instance
└── OverlayMode.kt                # 模式枚举（EDIT/DISPLAY）/ Mode enum (EDIT/DISPLAY)
```

## 参考项目 / Referenced Projects
本项目从零开发，未直接复制其他开源项目的代码。
This project was developed from scratch without directly copying code from other open source projects.

主要参考了 Android 官方文档：
Main reference: Android Official Documentation:
- WindowManager 悬浮窗实现 / WindowManager overlay implementation
- WebView 配置和使用 / WebView configuration and usage
- Android 权限系统 / Android permission system

## 许可证 / License
本项目基于 Apache License 2.0 开源。
This project is open source under the Apache License 2.0.

详细内容请参阅 LICENSE 文件。
See LICENSE file for details.
