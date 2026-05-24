#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "Usage: $0 <x> <y>"
  echo ""
  echo "Example: $0 500 1000"
  echo "         Taps at coordinates (500, 1000)"
  exit 1
fi

X="$1"
Y="$2"

echo "Checking connected devices..."
adb devices -l

echo "Tapping at ($X, $Y)..."
adb shell input tap "$X" "$Y"

echo "✅ Tap done!"
