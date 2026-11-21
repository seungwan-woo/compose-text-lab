# Compose Text Lab

Reconstruct the feature scope of the referenced ChatGPT prototype and publish a reproducible Android project. The old sandbox ZIP was unavailable in this session; this is a new implementation of that documented scope, not an assertion that its original bytes were recovered.

One Compose Activity hosts controls and four experiments: rendering, effects, selection/input, and reports. Immutable settings feed both engines. Platform widgets use AndroidView and a configuration/themed Context. The preview's Compose density is derived from that same Context, preserving Android's font scaling conversion. Controls retain normal system scaling. Built-in and imported fonts use the same Android Typeface. Experiments expose explicit normalized properties and a separate theme-default probe.

TTF/OTF import uses the system document picker, bounded private storage, and Typeface validation without storage permissions. Theme offers system/light/dark/dynamic. Font scale follows the system or overrides only the preview configuration at 0.8–2.0; the app links to system display settings. Effects: shadow, gradient, stroke, and marquee. Selection: SelectionContainer, BasicTextField, selectable TextView, EditText, and optional View ActionMode extension with a bounded event log. Do not claim to force or detect a hidden remote toolbar service.

Reports export real layout/widget dimensions, line counts, baselines, ellipsis/overflow observations, configuration and font identity as JSON via clipboard or document creation. Measurements belong to the current configuration; do not report synthetic equivalence or stale results. Text changes in the two input probes remain independent.

Use the prototype's pinned AGP 8.13.2, Kotlin 2.2.21, Compose BOM 2025.12.00, Gradle 8.13, JDK 17, compile/target SDK 36, minSdk 26. These are a reproduction baseline, not a claim to be the latest release. No SESL dependency or hidden SDK usage. Unit tests cover data boundaries, font import safety, and reports; Android tests cover UI controls and configuration parity. Build/lint locally, and verify a GitHub Actions debug APK artifact. OEM appearance and remote-service internals require separate One UI device testing.

Approaches considered: port the ZIP (unavailable); implement the documented scope (chosen to fulfill the user's explicit publication request); stop for reattachment (unnecessary for a fully specified app). Publish a bootstrap on the empty main branch and the app as a reviewable PR through the connected GitHub plugin. No prior repository files existed at inspection.
