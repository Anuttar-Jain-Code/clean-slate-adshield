# 🛡️ Clean Slate AdShield

**Clean Slate AdShield** is a professional Android DNS-filtering VPN app that helps reduce ads, trackers, analytics, telemetry, and unwanted network requests across Android apps and browsers.

It runs locally on the device, does not require root access, and includes a simple live dashboard for blocked, allowed, cache, error, total, and rule counters.

<p align="center">
  <a href="https://github.com/Anuttar-Jain-Code/clean-slate-adshield/releases/latest"><strong>⬇️ Download Latest APK</strong></a>
  ·
  <a href="PRIVACY_POLICY.md">Privacy Policy</a>
  ·
  <a href="DISCLAIMER.md">Disclaimer</a>
  ·
  <a href="APP_STORE_NOTES.md">Publishing Notes</a>
</p>

---

## 🚀 Download

### Recommended download

Download the latest APK from GitHub Releases:

➡️ **[Download Clean Slate AdShield APK](https://github.com/Anuttar-Jain-Code/clean-slate-adshield/releases/latest)**

If no release is available yet, use GitHub Actions:

1. Open the **Actions** tab.
2. Select **Android CI**.
3. Open the latest successful run.
4. Download the artifact named **`clean-slate-adshield-debug-apk`**.
5. Extract the ZIP and install **`app-debug.apk`** on your Android device.

> For public users, a GitHub Release is the easiest download method. APK files should be attached to **Releases**, not committed directly into source folders.

---

## ✨ Key features

- ✅ Local Android `VpnService` based DNS filtering
- ✅ Blocks known ad, tracker, analytics, telemetry, and fingerprinting hostnames
- ✅ Works without root access
- ✅ Live in-app dashboard
- ✅ Privacy Shield toggle
- ✅ Strict Mode toggle for stronger filtering
- ✅ Local counters for blocked, allowed, cache hits, errors, total events, and rules loaded
- ✅ In-app three-dot menu with Privacy Policy, Contact Us, and About
- ✅ Foreground service notification with protection status
- ✅ Local-first design: no browsing-history upload to the developer

---

## 📱 App interface

The app keeps the UI simple:

- **Start** — starts the local DNS filtering VPN
- **Stop** — stops protection
- **Reload** — reloads filtering rules
- **Privacy Shield** — enables privacy/tracker protection
- **Strict Mode** — stronger filtering that may block more unwanted requests but can break some media playback
- **Live dashboard** — shows protection counters directly inside the app
- **⋮ menu** — Privacy Policy, Contact Us, and About

---

## 🔐 Privacy-first design

Clean Slate AdShield is designed to keep filtering local to the Android device.

The app does **not** require account creation and does **not** collect names, email addresses, contacts, photos, messages, precise location, payment information, or device identifiers for external tracking.

DNS hostnames are processed locally to decide whether to allow or block requests. The app does not upload browsing history, DNS history, or visited website lists to the developer.

Read the full policy here: **[PRIVACY_POLICY.md](PRIVACY_POLICY.md)**

---

## ⚠️ Important limitation

No DNS-based mobile app can honestly guarantee blocking every advertisement across every platform.

Apps such as YouTube and other video platforms may serve ads from the same infrastructure used for normal video content. DNS-level filtering can reduce tracking and some ad-related requests, but it cannot safely guarantee removal of every YouTube ad without risking broken playback or violating platform rules.

Clean Slate AdShield does **not** bypass subscriptions, paid access, DRM, app protections, or platform protections.

---

## 🏗️ Project structure

```text
app/src/main/java/com/cleanslate/adshield/
  MainActivity.kt          Simple app UI, dashboard, privacy menu
  AdShieldVpnService.kt    Android VPN DNS-filter service
  Blocklist.kt             Domain matching rules
  DnsCache.kt              Local DNS response cache
  DnsMessage.kt            DNS query parser and response builder
  Packet.kt                IPv4/UDP packet parser and response writer

app/src/main/assets/
  blocklist.txt            Default ad/tracker/privacy block rules

.github/workflows/
  android.yml              CI build and APK artifact upload
```

---

## 🧪 Build from source

1. Clone this repository.
2. Open it in Android Studio.
3. Let Gradle sync finish.
4. Select the `app` run configuration.
5. Select an Android phone or emulator.
6. Click **Run**.
7. Tap **Start** in the app and approve the Android VPN permission prompt.

To generate a local APK:

```text
Build → Generate App Bundles or APKs → Generate APK
```

The APK will be generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📦 GitHub release workflow

For easy public downloads, upload APKs to **GitHub Releases**.

Recommended release tag format:

```text
v1.0.0
```

Recommended release title:

```text
Clean Slate AdShield v1.0.0
```

Attach the generated APK as a release asset.

---

## 🔒 Copyright and permissions

Copyright © 2026 **Anuttar Jain**. All rights reserved.

This project is source-available for review and personal testing. Unless written permission is granted by the copyright owner, users may not copy, modify, redistribute, sell, rebrand, sublicense, or publish derivative versions of this software.

See **[LICENSE](LICENSE)** for the full copyright terms.

> GitHub visitors cannot directly edit this repository unless the owner gives them collaborator or maintainer access. They may still view or download public files. License terms control what they are legally allowed to do with the code.

---

## 📬 Contact

For support, privacy questions, or permission requests:

**anuttar209@gmail.com**

---

## 📄 Documents

- [Privacy Policy](PRIVACY_POLICY.md)
- [Disclaimer](DISCLAIMER.md)
- [Release Notes](RELEASE_NOTES.md)
- [App Store Publishing Notes](APP_STORE_NOTES.md)
- [License](LICENSE)
