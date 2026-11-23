#!/usr/bin/env bash
set -euo pipefail

android_release=$(adb shell getprop ro.build.version.release | tr -d '\r')
runtime_api=$(adb shell getprop ro.build.version.sdk_full | tr -d '\r')
if [[ -z "$runtime_api" ]]; then
  api_major=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
  api_minor=$(adb shell getprop ro.build.version.sdk_minor | tr -d '\r')
  runtime_api="$api_major.${api_minor:-0}"
elif [[ "$runtime_api" != *.* ]]; then
  runtime_api+=.0
fi
if [[ "$runtime_api" != "$ANDROID_TEST_API" ]]; then
  echo "Expected Android API $ANDROID_TEST_API, but the booted emulator reports $runtime_api" >&2
  exit 1
fi
page_dir=build/ci-pages
mkdir -p "$page_dir"
printf 'Android %s · API %s 에뮬레이터\n' "$android_release" "$runtime_api" > "$page_dir/runtime.txt"
printf 'Testing Android %s, API %s (%s-byte pages)\n' "$android_release" "$runtime_api" "$(adb shell getconf PAGE_SIZE | tr -d '\r')"

adb shell wm size 2400x1900
adb shell wm density 320
./gradlew --no-daemon :app:connectedDebugAndroidTest

capture_dir="$page_dir/screenshots"
mkdir -p "$capture_dir"
shopt -s globstar nullglob
copy_test_output() {
  local source_name=$1 destination=$2
  local matches=(app/build/outputs/connected_android_test_additional_output/**/"$source_name")
  if (( ${#matches[@]} != 1 )); then
    echo "Expected one Gradle test output for $source_name, found ${#matches[@]}" >&2
    exit 1
  fi
  cp "${matches[0]}" "$destination"
}
for scene in render dark-font-scale gradient compose-selection view-selection \
  compose-right-click view-right-click overflow-ellipsis overflow-clip marquee-start marquee-moving tablet-selection +  multilingual-lines; do
  copy_test_output "ci-$scene.png" "$capture_dir/$scene.png"
done
copy_test_output rich-text-comparison.png "$capture_dir/rich-text-comparison.png"
copy_test_output ci-multilingual-lines.json "$page_dir/multilingual-lines.json"
