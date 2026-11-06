#!/usr/bin/env bash
set -euo pipefail
APK="$GITHUB_WORKSPACE/pandora24-app/app-debug.apk"
PKG="com.jarvis.kisisel.trial24"
test -s "$APK"
adb wait-for-device
adb install -r "$APK"
# Real users grant these through Android's permission UI.
adb shell pm grant "$PKG" android.permission.RECORD_AUDIO
adb shell pm grant "$PKG" android.permission.BLUETOOTH_CONNECT
adb logcat -c
adb shell am start -W -n "$PKG/com.jarvis.kisisel.MainActivity"
sleep 12
pid=$(adb shell pidof "$PKG" | tr -d '\r')
if [[ -z "$pid" ]]; then
  echo "::error::PANDORA activity crashed or exited before startup test"
  adb logcat -d -v brief -s AndroidRuntime:E ActivityManager:E | tail -120
  exit 1
fi
echo "PANDORA emulator process: $pid"
adb shell uiautomator dump /sdcard/window.xml >/dev/null
adb pull /sdcard/window.xml /tmp/pandora24-ui.xml >/dev/null
if ! grep -q "PANDORA" /tmp/pandora24-ui.xml; then
  echo "::error::Expected PANDORA interface not present"
  exit 1
fi
if adb logcat -d -v brief -s AndroidRuntime:E | grep -q "FATAL EXCEPTION"; then
  echo "::error::AndroidRuntime fatal error detected"
  adb logcat -d -v brief -s AndroidRuntime:E | tail -120
  exit 1
fi
adb exec-out screencap -p > "$GITHUB_WORKSPACE/pandora24-android13.png" || true
echo "Android 13 launch, UI and no-headset idle smoke test: OK"
