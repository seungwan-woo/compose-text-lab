# Compose Text Lab Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task by task.

**Goal:** Publish a working Compose/TextView comparison app and downloadable debug APK.

**Architecture:** Pure settings and measurement models feed two rendering engines. Android-specific font import and reports remain separate from experiment UI.

**Tech Stack:** Kotlin 2.2.21, AGP 8.13.2, Compose BOM 2025.12.00, Gradle 8.13.

**Spec:** ../specs/2026-11-21-compose-text-lab-design.md

## Global Constraints
- minSdk 26; compile/target SDK 36; JDK 17 bytecode.
- Publish through connected GitHub plugin, preserve any concurrent repository work.
- Public APIs only; actual remote toolbar routing remains unverified.
- Reconstruct documented features; original ZIP is inaccessible.

## Review Focus
- Malformed/oversized font streams must fail without replacing the working font.
- FontScale/theme changes must reach both rendering contexts without stale metrics.
- Rotation/system settings return must preserve user controls and refresh measurements.
- TextView native line spacing differs from Compose line height; disclose units and origin.
- Selection and input must remain usable with IME and produce actual event observations.

### Task 1: Pure experiment and import boundaries
Files: LabSettings.kt, Measurements.kt, FontBytes.kt, unit tests.
Interfaces: LabSettings.normalized(); readFontBytes(InputStream, Int): ByteArray; buildReport(...): JSONObject.
- [x] Write unit tests for invalid values, font signatures/size/truncation, report metadata and missing measurements.
- [x] Run testDebugUnitTest and verify missing implementation fails.
- [x] Implement the boundaries; rerun to green.

### Task 2: Android experiment UI
Files: MainActivity.kt, LabApp.kt, FontRepository.kt, TextProbes.kt, SelectionProbes.kt, SettingsControls.kt, androidTest/LabUiTest.kt.
Interfaces: immutable LabSettings, FontAsset, ProbeMeasurement; callbacks return measured values.
- [x] Write Android UI tests for controls, app override/system parity, selection and rotation.
- [x] Implement preview configuration/theming, font picker, settings, effects and reports.
- [x] Run assembleDebug, lintDebug, testDebugUnitTest and connectedDebugAndroidTest where a device is available.

### Task 3: Reproducible publication
Files: wrapper, .github/workflows/android.yml, README.md, docs/effects-selection-toolbar.md.
- [x] Generate official wrapper and pin distribution checksum.
- [x] Document precise comparisons, build steps, limitations and One UI manual matrix.
- [x] Review the complete diff and fix material findings.
- [ ] Recheck empty main; publish bootstrap and feature commit using the GitHub plugin; open PR.
- [ ] Monitor the exact PR commit's build to completion and confirm its APK artifact.
