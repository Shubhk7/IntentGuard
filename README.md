<div align="center">

  <img src="app/Logo/Logo_name.jpeg" alt="IntentGuard Logo" width="360" />

  # IntentGuard

  **Context-Aware Android Privacy & Permission Risk Analyzer**

  [![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
  [![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com)
  [![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
  [![Target SDK](https://img.shields.io/badge/Target%20SDK-34-blue.svg)](https://developer.android.com/about/versions/14)

</div>

---

## 🛡️ Overview

**IntentGuard** is a proactive privacy auditing tool designed for modern Android devices. While standard Android permission managers only show a flat list of permissions granted to apps, **IntentGuard** evaluates permissions in **context**. 

It distinguishes between legitimate application requirements (e.g., WhatsApp requesting microphone and camera for calls) and suspicious permission abuse (e.g., a flashlight, calculator, or offline game requesting SMS, Contacts, Microphone, and Background Services).

---

## ✨ Features

- 🔍 **Full Device Privacy Audit**: Discovers and scans all installed user and system applications using high-performance coroutines.
- 🧠 **Context-Aware Heuristics (`RiskAnalyzer`)**: Cross-references app categories and legitimate use cases with requested runtime permissions.
- 🚨 **Risk Scoring & Severity Levels**:
  - `HIGH`: Alarming permission combinations (e.g., utility requesting SMS reading, background location, or call logs).
  - `MEDIUM`: Questionable permissions that warrant user verification.
  - `LOW`: Permissions consistent with expected functionality.
- 🎨 **Modern Jetpack Compose UI**: Built entirely with declarative UI and Material 3 design tokens, including animated scan progress and expandable audit cards.
- ⚡ **Direct Access to App Settings**: One-tap shortcut directly to Android's native App Settings to quickly revoke granted permissions or uninstall suspicious apps.
- 🔒 **100% Offline & Private**: Zero internet telemetry, zero external trackers. All scans run strictly on-device.

---

## 🔬 How the Risk Analysis Works

IntentGuard does not just count permissions; it analyzes **intent**:
