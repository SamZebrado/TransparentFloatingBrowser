#!/usr/bin/env bash
set -euo pipefail

echo "Checking connected devices..."
adb devices -l

echo ""
echo "Getting screen resolution..."
adb shell wm size

echo ""
echo "Getting screen density..."
adb shell wm density

echo ""
echo "Getting display info (for exact pixel coordinates)..."
adb shell dumpsys window displays | grep -E 'mCurrentDisplayRect|mDisplayRect' -A 2

echo ""
echo "✅ Done!"
