#!/usr/bin/env bash
set -euo pipefail

PKG="com.samzebrado.transparentfloatingbrowser"
ACTIVITY=".MainActivity"

echo "Checking connected devices..."
adb devices -l

echo "Starting app with package: $PKG"
adb shell monkey -p "$PKG" 1 || {
  echo "Monkey failed, trying explicit activity start..."
  adb shell am start -n "$PKG/$ACTIVITY"
}

echo "✅ App started!"
