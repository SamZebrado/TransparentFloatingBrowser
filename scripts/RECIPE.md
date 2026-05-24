# ADB 遥控操作指南

本目录包含一系列实用的 ADB 脚本，用于遥控 Android 平板。

---

## 脚本列表

### 1. `screenshot.sh` - 截图
从平板截图并保存到电脑。

**使用方式：**
```bash
./scripts/screenshot.sh
```

**功能：**
- 自动截图
- 保存到 `screenshots/` 目录，文件名带时间戳
- macOS 自动打开截图

---

### 2. `tap.sh` - 点击
在指定坐标点击。

**使用方式：**
```bash
./scripts/tap.sh <x> <y>
```

**示例：**
```bash
./scripts/tap.sh 500 1000
```

---

### 3. `swipe.sh` - 滑动
从一个坐标滑动到另一个坐标。

**使用方式：**
```bash
./scripts/swipe.sh <x1> <y1> <x2> <y2> [duration_ms]
```

**示例：**
```bash
# 向上滑动（滚动）
./scripts/swipe.sh 500 1500 500 500

# 向右滑动（返回）
./scripts/swipe.sh 100 1000 900 1000

# 自定义速度（500ms）
./scripts/swipe.sh 500 1500 500 500 500
```

---

### 4. `input_text.sh` - 输入文本
输入文字到当前焦点的输入框。

**使用方式：**
```bash
./scripts/input_text.sh "<text>"
```

**示例：**
```bash
./scripts/input_text.sh "Hello World"
```

**注意：** 
- 空格会被自动替换为 `%s`
- 需要先点击输入框使其获得焦点

---

### 5. `press_key.sh` - 按键
模拟物理按键。

**使用方式：**
```bash
./scripts/press_key.sh <keycode>
```

**常见 keycode：**

| Keycode | 功能 |
|---------|------|
| 3 | HOME 键 |
| 4 | BACK 键 |
| 26 | POWER 键（熄屏/亮屏） |
| 24 | VOLUME_UP |
| 25 | VOLUME_DOWN |
| 66 | ENTER |
| 67 | BACKSPACE |
| 187 | 最近应用/多任务 |

**示例：**
```bash
# 按 HOME 键
./scripts/press_key.sh 3

# 按 BACK 键
./scripts/press_key.sh 4
```

---

### 6. `get_screen_size.sh` - 获取屏幕尺寸
获取屏幕分辨率、密度等信息。

**使用方式：**
```bash
./scripts/get_screen_size.sh
```

**用途：**
- 帮助确定点击/滑动的坐标
- 了解设备的实际像素

---

## 实用组合操作

### 操作流程示例

**1. 解锁设备：**
```bash
# 先亮屏
./scripts/press_key.sh 26

# 等待一下
sleep 1

# 向上滑动解锁（根据设备调整坐标）
./scripts/swipe.sh 500 1500 500 500
```

**2. 打开应用并操作：**
```bash
# 回到首页
./scripts/press_key.sh 3

# 等待
sleep 1

# 点击应用图标位置（需要先确定坐标）
./scripts/tap.sh 200 400

# 等待应用打开
sleep 2

# 在应用内操作
./scripts/tap.sh 500 800
```

**3. 测试并验证：**
```bash
# 先截图看看当前状态
./scripts/screenshot.sh

# 执行操作
./scripts/tap.sh 500 1000

# 再截图看结果
./scripts/screenshot.sh
```

---

## 获取坐标的方法

### 方法 1：使用开发者选项的指针位置

1. 打开平板的「设置」→「开发者选项」
2. 开启「指针位置」
3. 手指触摸屏幕，顶部会显示当前坐标
4. 记下坐标后使用

### 方法 2：截图后在电脑上看

```bash
# 先截图
./scripts/screenshot.sh

# 打开截图，在图片查看器中看坐标
```

### 方法 3：使用 `getevent` 实时查看

```bash
adb shell getevent
# 然后触摸屏幕看输出，不过需要转换（比较麻烦）
```

---

## 常用 ADB 命令速查

### 设备相关
```bash
# 查看连接的设备
adb devices -l

# 查看设备详细信息
adb shell getprop

# 重启设备
adb reboot

# 进入 recovery
adb reboot recovery
```

### 应用相关
```bash
# 查看当前前台应用
adb shell dumpsys window | grep -E 'mCurrentFocus'

# 启动应用
adb shell am start -n com.package.name/.ActivityName

# 强制停止应用
adb shell am force-stop com.package.name

# 清除应用数据
adb shell pm clear com.package.name

# 安装 APK
adb install -r app.apk

# 卸载应用
adb uninstall com.package.name
```

### 更多输入命令
```bash
# 长按（通过 swipe 实现，相同坐标）
adb shell input swipe 500 500 500 500 1000

# 拖拽
adb shell input swipe <x1> <y1> <x2> <y2> <duration>

# 按键（完整列表搜索 Android KeyEvent）
adb shell input keyevent <keycode>
```

---

## 注意事项

1. **开启 USB 调试**
   - 需要在平板的开发者选项中开启「USB 调试」
   - 首次连接需要在平板上授权

2. **坐标问题**
   - 不同设备分辨率不同，坐标不通用
   - 建议先用 `get_screen_size.sh` 查看
   - 建议先用 `screenshot.sh` 确认

3. **权限问题**
   - 某些操作可能需要 root
   - 但大部分基础操作不需要

4. **速度问题**
   - 脚本中可以适当加 `sleep` 等待界面加载
   - 滑动的 duration 参数可以调整手感

---

## 参考资料

- [Android Debug Bridge (ADB)](https://developer.android.com/studio/command-line/adb)
- [Input Events](https://developer.android.com/reference/android/view/KeyEvent)
- [ADB Shell Input](https://stackoverflow.com/questions/7789826/adb-shell-input-events)
