<p align="center">
  <img src="app/src/main/res/drawable/ic_p2pchat_logo.webp" width="128" height="128" alt="P2Chat Penguin Logo" />
</p>

<h1 align="center">P2Chat</h1>

<p align="center">
  <strong>Decentralized, Serverless & Offline Peer-to-Peer Mesh Messenger for Android</strong>
</p>

<p align="center">
  <a href="https://github.com/AmritRai1234/P2Chat/blob/main/LICENSE">
    <img src="https://img.shields.io/badge/License-GPLv3-orange.svg" alt="License GPLv3" />
  </a>
  <a href="https://developer.android.com/">
    <img src="https://img.shields.io/badge/Platform-Android%208.0%2B%20%28API%2026%2B%29-black.svg" alt="Android Platform" />
  </a>
  <a href="#">
    <img src="https://img.shields.io/badge/APK%20Size-1.6%20MB-brightgreen.svg" alt="APK Size" />
  </a>
  <a href="#">
    <img src="https://img.shields.io/badge/Security-AES--256--GCM-blue.svg" alt="AES-256-GCM E2EE" />
  </a>
  <a href="https://AmritRai1234.github.io/P2Chat/">
    <img src="https://img.shields.io/badge/Website-Live%20Demo-ff9900.svg" alt="Website Live Demo" />
  </a>
</p>

---

## 🌟 Overview

**P2Chat** is a 100% serverless, zero-internet offline messaging application engineered for Android. It connects nearby smartphones directly using **Bluetooth Low Energy, Bluetooth Classic, and Wi-Fi Direct** via Google Nearby Connections (`P2P_CLUSTER`).

P2Chat operates with **zero cellular data, zero servers, zero cloud dependency, and zero central metadata tracking**.

🌐 **Live Promotional Website**: [https://AmritRai1234.github.io/P2Chat/](https://AmritRai1234.github.io/P2Chat/)

---

## 🔥 Key Features

- 📡 **Zero-Internet P2P Mesh Network**: Multi-hop peer relay powered by Google Nearby Connections (`P2P_CLUSTER`). Messages automatically hop across nearby smartphones.
- 🔒 **AES-256-GCM End-to-End Encryption (E2EE)**: Zero-trust payload security with 128-bit authentication tags and fresh random 12-byte IVs. Group secret keys are derived using `PBKDF2WithHmacSHA256` from group IDs and secret invite codes.
- 💾 **SQLite Store-and-Forward Queueing (Room v4)**: Messages sent to offline peers automatically queue in local SQLite database files and flush over the air as soon as the recipient reconnects.
- ⚡ **Ultra-Compact 1.6 MB APK**: Aggressively shrunk via 5-pass R8 bytecode minification, resource shrinking, and WebP compression. Transfers over the air in under 1.5 seconds.
- 📲 **Offline P2P Self-Replication**: Extract its own running APK at runtime and transmit `P2Chat.apk` directly over Nearby Connections to nearby phones without Google Play or internet access.
- 📎 **100 MB File & Photo Attachments**: Transfer photos, PDFs, videos, and files using high-speed zero-copy `FILE` payloads.
- 📷 **Scannable QR Code Invitations**: Generate 512x512 QR code bitmaps via ZXing for instant in-person group joining and app sharing.
- ☀️/🌙 **Dark & Light Theme Engine**: Built-in reactive theme switcher persisted via `SharedPreferences`.

---

## 🛡️ Security Architecture & Threat Model

| Threat Vector | Defense Mechanism |
|---|---|
| **Eavesdropping / Radio Sniffing** | Payload contents encrypted via `AES-256-GCM`. Over-the-air packets appear as undecryptable binary noise (`E2EE:...`). |
| **Message Tampering / Forgery** | GCM GMAC integrity tags. Modified payloads fail authentication and are dropped immediately. |
| **Payload Flooding / DoS** | Thread-safe sliding window rate limiter (`P2PRateLimiter.kt`) drops spam over 15 msgs/sec per peer. |
| **Path Traversal Attacks** | File path sanitizer (`sanitizeFileName`) strips `../` and directory injection vectors on incoming file transfers. |
| **Group Eavesdropping** | High-entropy 12-character cryptographic invite codes (`K9X2-M7W4-P9L3`) generated via `SecureRandom`. |

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 1.9+
- **UI Framework**: Jetpack Compose (Material 3, Glassmorphism design system)
- **Architecture**: MVVM + Clean Architecture (Domain, Data, UI layers)
- **Dependency Injection**: Hilt (Dagger)
- **Local Persistence**: Room SQLite DB v4 (`messages`, `peers`, `groups`, `mesh_peers`, `message_queue`, `network_stats`)
- **P2P Mesh Stack**: Google Nearby Connections API (`P2P_CLUSTER` strategy)
- **Crypto Engine**: Java Cryptography Architecture (`Cipher`, `SecretKeyFactory`, `SecureRandom`, `AES/GCM/NoPadding`)
- **QR Engine**: ZXing (`QRCodeWriter`)

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or JDK 17+
- Android SDK 35
- Physical Android device with Bluetooth and Wi-Fi enabled (API 26+)

### Build Commands

```bash
# Clone the repository
git clone https://github.com/AmritRai1234/P2Chat.git
cd P2Chat

# Build Debug APK
./gradlew assembleDebug

# Build Ultra-Shrunk 1.6 MB Release APK (R8 Minified)
./gradlew assembleRelease
```

### Install via ADB

```bash
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

---

## 📄 License

P2Chat is released under the **GNU General Public License v3.0 (GPLv3)**.  
See the [LICENSE](LICENSE) file for more information.
