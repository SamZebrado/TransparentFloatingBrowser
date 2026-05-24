#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <keycode>"
  echo ""
  echo "Common keycodes:"
  echo "  3   - HOME"
  echo "  4   - BACK"
  echo "  66  - ENTER"
  echo "  67  - BACKSPACE"
  echo "  187 - APP_SWITCH / RECENT_APPS"
  echo "  26  - POWER"
  echo "  24  - VOLUME_UP"
  echo "  25  - VOLUME_DOWN"
  echo ""
  echo "Example: $0 3"
  echo "         Presses HOME key"
  exit 1
fi

KEYCODE="$1"

KEYNAMES=(
  [3]="HOME"
  [4]="BACK"
  [66]="ENTER"
  [67]="BACKSPACE"
  [187]="APP_SWITCH"
  [26]="POWER"
  [24]="VOLUME_UP"
  [25]="VOLUME_DOWN"
)

KEYNAME="${KEYNAMES[$KEYCODE]:-UNKNOWN ($KEYCODE)}"

echo "Checking connected devices..."
adb devices -l

echo "Pressing key: $KEYNAME"
adb shell input keyevent "$KEYCODE"

echo "✅ Key press done!"
