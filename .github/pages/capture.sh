#!/usr/bin/env bash
set -euo pipefail

adb shell wm size 1080x2340
adb shell wm density 360
./gradlew --no-daemon :app:connectedDebugAndroidTest

capture_dir=build/ci-pages/screenshots
mkdir -p "$capture_dir"
for scene in render dark-font-scale gradient compose-selection view-selection; do
  adb exec-out run-as io.github.seungwanwoo.composetextlab cat "files/ci-captures/$scene.png" > "$capture_dir/$scene.png"
done
