#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

# 设置 JDK 为 Android Studio 自带的
if [[ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  echo "Using Android Studio JDK: $JAVA_HOME"
elif [[ -d "/Applications/Android Studio.app/Contents/jre/Contents/Home" ]]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jre/Contents/Home"
  echo "Using Android Studio JRE: $JAVA_HOME"
fi

echo "Building debug APK..."
./gradlew clean assembleDebug

echo "✅ Build complete!"
echo "   APK location: $PROJECT_ROOT/app/build/outputs/apk/debug/app-debug.apk"
