#!/usr/bin/env bash
set -euo pipefail

adb shell wm size 1080x2340
adb shell wm density 360
./gradlew --no-daemon :app:connectedDebugAndroidTest

capture_dir=build/ci-pages/screenshots
mkdir -p "$capture_dir"
shopt -s globstar nullglob
for scene in render dark-font-scale gradient compose-selection view-selection; do
  matches=(app/build/outputs/connected_android_test_additional_output/**/"ci-$scene.png")
  if (( ${#matches[@]} != 1 )); then
    echo "Expected one Gradle test output for $scene, found ${#matches[@]}" >&2
    exit 1
  fi
  cp "${matches[0]}" "$capture_dir/$scene.png"
done
