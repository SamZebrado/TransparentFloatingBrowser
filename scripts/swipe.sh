#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 4 ]]; then
  echo "Usage: $0 <x1> <y1> <x2> <y2> [duration_ms]"
  echo ""
  echo "Example: $0 500 1500 500 500"
  echo "         Swipes from (500, 1500) to (500, 500)"
  echo ""
  echo "Example: $0 500 1500 500 500 500"
  echo "         Swipes with 500ms duration"
  exit 1
fi

X1="$1"
Y1="$2"
X2="$3"
Y2="$4"
DURATION="${5:-300}"

echo "Checking connected devices..."
adb devices -l

echo "Swiping from ($X1, $Y1) to ($X2, $Y2) (${DURATION}ms)..."
adb shell input swipe "$X1" "$Y1" "$X2" "$Y2" "$DURATION"

echo "✅ Swipe done!"
