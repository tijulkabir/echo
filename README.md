<p align="center">
    <img src="docs/echo-logo.png" alt="Echo Logo" width="180">
</p>

# Echo — Decentralized Mesh Messaging

Echo is an Android messaging application built for secure, off-grid peer-to-peer communication over Bluetooth Low Energy (BLE) mesh networks. It enables users to communicate securely without relying on cellular data or internet infrastructure.

This project was developed as a **Mobile Application Development** course project (2nd Year, 1st Semester), focusing on creating a polished user experience on top of a decentralized networking protocol.

---

## ✨ Features & Implementation

### Direct Messaging System
Designed and implemented a dedicated full-screen Direct Messaging (DM) experience:
- **Dedicated DM Interface:** A WhatsApp-style 1-on-1 chat UI (`DirectMessageChatScreen.kt`) with message bubbles and delivery status tracking.
- **Conversation Management:** A centralized hub (`DirectMessageListScreen.kt`) showing active DM threads, unread notification badges, and peer identification.
- **Dynamic Routing:** Custom navigation state management allowing seamless transitions between global mesh channels and private DM threads.

### Custom Design System & UX
Completely overhauled the visual identity to provide a premium, modern feel:
- **Color Palette:** Implemented a custom `EchoCyan` and `EchoPurple` theme across the application, replacing the default terminal-style aesthetics.
- **Typography:** Built a scalable, consistent typography system.
- **Dynamic Theming:** Support for Dark, Light, and System-default visual modes.
- **Brand Identity:** Custom launcher icons and logo design.

### Onboarding & Identity
- **Nickname Flow:** Created a seamless first-launch onboarding experience (`UsernameSetupScreen.kt`) with validation logic to establish peer identity before entering the mesh.

### Core Enhancements & Stability
- **Memory Management Fixes:** Identified and patched a critical race condition in the app's panic-wipe functionality by ensuring the process-wide `AppStateStore` is securely cleared before UI state collectors rehydrate, preventing deleted messages from reappearing.

---

## ⚙️ Core Engine (Powered by bitchat)

Echo's underlying network, cryptography, and protocol layers are powered by the robust [bitchat-android](https://github.com/permissionlesstech/bitchat-android) open-source framework. By leveraging this engine, Echo supports:

- **BLE Mesh Networking:** Decentralized multi-hop routing using Bluetooth Low Energy.
- **End-to-End Encryption:** Secured via the Noise Protocol Framework (X25519, AES-256-GCM).
- **Store-and-Forward:** Asynchronous message delivery caching.
- **Nostr Integration:** Optional geo-spatial location channels backed by Nostr relays.
- **Media Transfer:** Support for sharing images, files, and voice notes over BLE.

---

## 🛠️ Technical Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material Design 3 |
| Architecture | MVVM with Coroutines & StateFlow |
| Network Core | bitchat mesh protocol (BLE) |
| Encryption | BouncyCastle, Noise Protocol |
| Min SDK | Android 8.0 (API 26) |

## 🚀 Build & Run

```bash
git clone https://github.com/tijulkabir/echo.git
cd echo
./gradlew assembleDebug
./gradlew installDebug
```

> **Note:** A physical Android device is highly recommended, as Android Emulators have limited Bluetooth Low Energy support.

## 📄 License & Attribution

Echo's application layer and custom UI implementations are developed by Tijul Kabir Toha. 

The underlying networking framework, protocol definitions, and core services are proudly inherited from the open-source [bitchat-android](https://github.com/permissionlesstech/bitchat-android) project. Licensed under the [MIT License](LICENSE.md).
