#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK_PATH="$PROJECT_ROOT/app/build/outputs/apk/debug/app-debug.apk"

echo "Checking connected devices..."
adb devices -l

if [[ ! -f "$APK_PATH" ]]; then
  echo "❌ APK not found at: $APK_PATH"
  echo "   Please run scripts/build_debug.sh first"
  exit 1
fi

echo "Installing debug APK..."
adb install -r "$APK_PATH"

echo "✅ Install complete!"
