# IntentGuard

**Context-Aware Android Privacy & Permission Risk Analyzer**

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-34-blue.svg)](https://developer.android.com/about/versions/14)

---

## 🛡️ Overview

**IntentGuard** is a proactive privacy auditing tool designed for modern Android devices. While standard Android permission managers only show a flat list of permissions granted to apps, **IntentGuard** evaluates permissions in **context**. 

It distinguishes between legitimate application requirements (e.g., WhatsApp requesting microphone and camera for calls) and suspicious permission abuse (e.g., a flashlight, calculator, or offline game requesting SMS, Contacts, Microphone, and Background Services).

---

## ✨ Features

- 🔍 **Full Device Privacy Audit**: Discovers and scans all installed user and system applications using coroutines.
- 🧠 **Context-Aware Heuristics (`RiskAnalyzer`)**: Cross-references app categories and legitimate use cases with requested runtime permissions.
- 🚨 **Risk Scoring & Severity Levels**:
  - `HIGH`: Alarming permission combinations (e.g., utility app requesting SMS reading, background location, or call logs).
  - `MEDIUM`: Questionable permissions that warrant user verification.
  - `LOW`: Permissions consistent with expected functionality.
- 🎨 **Modern Jetpack Compose UI**: Built entirely with declarative UI and Material 3 design tokens, featuring real-time scanning progress and expandable audit cards.
- ⚡ **Direct Access to App Settings**: One-tap shortcut to native Android App Settings to quickly revoke permissions or uninstall suspicious apps.
- 🔒 **100% Offline & Private**: Zero internet telemetry, zero external trackers. All scans run strictly on-device.

---

## 🔬 How the Risk Analysis Works

IntentGuard does not just count permissions; it analyzes **intent**:

1. **Categorization**: Groups apps into functional categories (Communication, Finance, Utility, Social, Media, Gaming, etc.).
2. **Legitimate Use-Case Verification**: Checks permissions against known expected behavior profiles.
3. **Pattern Matching**: Detects risky combinations (e.g., SMS + Contacts + Microphone in non-communication apps).
4. **Scoring**: Computes an actionable risk score with human-readable explanations.

---

## 📂 Project Structure

```text
IntentGuard/
├── app/
│   ├── Logo/                      # App logos and branding assets
│   ├── src/main/
│   │   ├── AndroidManifest.xml   # Manifest & QUERY_ALL_PACKAGES declaration
│   │   └── java/com/intentguard/intentguard/
│   │       ├── logic/
│   │       │   └── RiskAnalyzer.kt    # Heuristic scoring and use-case matrix
│   │       ├── model/
│   │       │   ├── AppInfo.kt         # App data models & categorization
│   │       │   └── RiskResult.kt      # Issues, severity, and analysis types
│   │       ├── ui/theme/              # Material 3 colors, typography, theme
│   │       └── MainActivity.kt        # Compose UI & ScanViewModel
│   └── build.gradle.kts           # App-level build configurations & dependencies
├── build.gradle.kts               # Root build configuration
└── settings.gradle.kts            # Project repositories and module setup

```
## 🚀 Getting Started

### Prerequisites

- Android Studio: Iguana (2023.2.1) or newer
- JDK: Version 17+
- Android SDK: API 34 (Android 14)
- Minimum OS Support: Android 8.0 (API Level 26)

### Clone & Run

Clone the repository:

```bash
git clone https://github.com/Shubhk7/IntentGuard.git
cd IntentGuard
```

Open the project in Android Studio and let Gradle sync.

### Build via Terminal

**Linux / macOS:**

```bash
./gradlew assembleDebug
```

**Windows:**

```powershell
.\gradlew.bat assembleDebug
```

### Install to Device

```bash
./gradlew installDebug
```
## 🔑 Permissions & Transparency

IntentGuard declares the following permission:

```xml
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

`QUERY_ALL_PACKAGES` is used to inspect installed applications and analyze their requested and granted permissions on supported Android versions.

### 🔒 Privacy Guarantee

IntentGuard never transmits your installed apps or device data.

- No internet telemetry
- No external trackers
- No cloud processing
- All analysis is performed locally on the device

  
## 🤝 Contributing

Contributions, issues, and feature requests are welcome!

1. Fork the Project
2. Create your Feature Branch:
   ```bash
   git checkout -b feature/AmazingFeature
   ```
3. Commit your Changes:
   ```bash
   git commit -m "Add some AmazingFeature"
   ```
4. Push to the Branch:
   ```bash
   git push origin feature/AmazingFeature
   ```
5. Open a Pull Request
