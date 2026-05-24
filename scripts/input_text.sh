#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 \"<text>\""
  echo ""
  echo "Example: $0 \"Hello World\""
  echo "         Inputs the text \"Hello World\""
  echo ""
  echo "Note: Spaces need to be escaped or in quotes"
  exit 1
fi

TEXT="$1"

echo "Checking connected devices..."
adb devices -l

echo "Inputting text: \"$TEXT\""
adb shell input text "$(echo "$TEXT" | sed 's/ /%s/g')"

echo "✅ Text input done!"
