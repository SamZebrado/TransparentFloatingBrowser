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
```bash
./gradlew assembleDebug
```
构建完成后，APK 文件位于 app/build/outputs/apk/debug/app-debug.apk
After building, the APK is located at app/build/outputs/apk/debug/app-debug.apk

## 已验证环境 / Tested Environment
- 小米平板 (Android)
- Xiaomi Tablet (Android)
- Android SDK 34
- Android SDK 34

## 已知限制 / Known Limitations
- DISPLAY 模式下蓝色拖动条和黄色缩放手柄会隐藏
- In DISPLAY mode, the blue drag handle and yellow resize handle are hidden
- Android 12+ 对 overlay opacity 有限制
- Android 12+ has limitations on overlay opacity
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
