#!/usr/bin/env bash
set -euo pipefail

echo "Showing crash logs for TransparentFloatingBrowser..."
echo "Press Ctrl+C to stop logging"

adb logcat -v time | grep -E \
  'FATAL EXCEPTION|AndroidRuntime|FloatingWebViewService|FloatingWebViewController|TransparentStyleInjector|FloatingWindowConfig|FloatingWindowInstance|OverlayControlBubble'
