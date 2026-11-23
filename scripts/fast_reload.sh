#!/usr/bin/env bash
set -e
cd /storage/emulated/10/Documents/PDFMaxing
export PATH="$HOME/Android/Sdk/platform-tools:$HOME/.android/bin:$PATH"
PROJECT_CACHE_DIR="$HOME/.cache/projects/$(basename "$PWD")-$(pwd -P | sha1sum | cut -c1-8)"

# 1. Incremental build
./gradlew assembleDebug --project-cache-dir="$PROJECT_CACHE_DIR/gradle" --quiet

# Find or connect device
DEVICE=$(adb devices | grep -E "\bdevice\b" | awk '{print $1}' | head -n1)
if [ -z "$DEVICE" ]; then
    MDNS_DEV=$(adb mdns services 2>/dev/null | grep -E "_adb-tls-connect\._tcp|_adb\._tcp" | awk '{print $NF}' | head -n1)
    if [ -n "$MDNS_DEV" ]; then
        adb connect "$MDNS_DEV"
        DEVICE=$(adb devices | grep -E "\bdevice\b" | awk '{print $1}' | head -n1)
    fi
fi

if [ -n "$DEVICE" ]; then
    # 2. Push APK via wireless ADB
    adb -s "$DEVICE" install -r -d app/build/outputs/apk/debug/PDFMaxing-debug.apk
    # 3. Restart activity with fresh code
    adb -s "$DEVICE" shell am force-stop com.droid.pdfmaxing
    adb -s "$DEVICE" shell am start -n com.droid.pdfmaxing/.MainActivity
    echo "✅ App updated and running on device ($DEVICE)!"
else
    echo "⚠️ No ADB device found, APK built at app/build/outputs/apk/debug/PDFMaxing-debug.apk"
fi
