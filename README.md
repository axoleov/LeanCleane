# LeanCleaner

[![Download APK](https://img.shields.io/badge/Download-APK%20v1.0.0-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/axoleov/LeanCleane/releases/latest/download/app-debug.apk)

> **⬇️ [Click here to download the latest APK](https://github.com/axoleov/LeanCleane/releases/latest/download/app-debug.apk)** — no account needed, just open it on your Android device and install.

Android app project built with Kotlin and Gradle.

## Build

Requires JDK 17 and Android SDK 34. Run `gradle assembleDebug` from the repository root. The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Project layout

- `app/src/main/` — Android manifest, Kotlin sources, and resources
- `app/build.gradle.kts` — Android application configuration
- `.github/workflows/build.yml` — GitHub Actions debug APK build

**Caution:** The app includes force-stop, clear-data, and package-disable functionality. Review the source and understand the effects before using those controls; clearing app data can permanently remove locally stored app data.
