# Validation record — 2026-11-21

## Local automated checks

Windows, Eclipse Temurin JDK 17, Gradle 8.13, Android SDK 36. Android integration tests ran on a dedicated Pixel 7 profile with the installed Android 16 / API 36.1 x86_64 Play Store emulator image.

```text
gradlew.bat :app:connectedDebugAndroidTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
BUILD SUCCESSFUL
```

- JVM: **8 tests, 0 failures/errors** — settings boundaries/system scale, shared marquee newline normalization, bounded/truncated/invalid font streams, actual report fields and Unicode JSON.
- Android: **5 tests, 0 failures/errors** — real font parsing/import/restore/rejected-import preservation and effective 700 weight in both engines; shared Unicode text and 1.6 preview scale through Activity recreation; real dark Context configuration and JSON clipboard export; independent input preservation through menu and tab changes; marquee viewport/layout separation and overflow.
- Debug APK: compiled and installed by Android integration tests.
- Android Lint: **0 errors, 17 warnings**. Warnings include intentionally pinned dependency/SDK versions, the API-29-named inlined break-strategy constant whose value 0 is accepted by the API-23 TextView setter, synchronous preference persistence on the IO dispatcher, diagnostic allocations, backup compatibility suggestions, KTX/style suggestions and untranslated experiment labels. No blanket Lint suppression or baseline was added.
- Static review findings (Typeface weight resolution, marquee width/content normalization, edit state lifetime, failed font promotion) were corrected and rechecked. UI tests also caught a repeated-layout loop caused by recreating the Typeface font-family wrapper; the style is now remembered.

The initial Compose-transitive Espresso 3.5.0 failed on the available API 36/37 system images while reflecting InputManager.getInstance. The test configuration now explicitly uses Espresso 3.7.0; the final run above passed. API 26–35 and Samsung One UI hardware were not exercised.

## GitHub checks

The workflow independently compiles the project, runs JVM tests and Lint, compiles the Android test APK, and uploads the debug APK. It does **not** run emulator tests. The exact published commit's workflow URL and outcome are recorded in the pull request and publication report; local success alone is not evidence of GitHub build success.

## Limits

No real Samsung/OEM/FlipFont/SESL verification, remote toolbar service tracing, screenshot pixel comparison, performance measurement, or automated selection-handle/menu visual comparison was performed. See the manual matrix in effects-selection-toolbar.md. The original ChatGPT sandbox archive was unavailable, so this is a reimplementation of its documented feature scope.
