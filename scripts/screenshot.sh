#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SCREENSHOT_DIR="$PROJECT_ROOT/screenshots"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
FILENAME="screenshot_${TIMESTAMP}.png"

echo "Checking connected devices..."
adb devices -l

# 创建截图目录
mkdir -p "$SCREENSHOT_DIR"

echo "Taking screenshot..."
adb shell screencap -p /sdcard/screenshot.png

echo "Pulling screenshot to computer..."
adb pull /sdcard/screenshot.png "$SCREENSHOT_DIR/$FILENAME"

echo "Cleaning up temporary file..."
adb shell rm /sdcard/screenshot.png

echo ""
echo "✅ Screenshot saved!"
echo "   Location: $SCREENSHOT_DIR/$FILENAME"

# 如果是macOS，尝试打开截图
if [[ "$(uname)" == "Darwin" ]]; then
  echo ""
  echo "Opening screenshot..."
  open "$SCREENSHOT_DIR/$FILENAME"
fi
