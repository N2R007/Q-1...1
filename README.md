# Q Scan - Ultra-Fast Real-Time Trading Scanner

## 🚀 Overview

Q Scan is an ultra-low-latency, real-time Computer Vision & Quantitative Trading Assistant for Android. It operates by scanning financial charts directly through the device camera at hardware maximum speeds (20ms per cycle / 50 FPS).

## ⚡ Key Features

- **Ultra-Fast 20ms (50 FPS) Screen Detection** - Hardware maximum speed scanning
- **Pure Kinetic Tick Velocity Engine** - Real-time momentum analysis (3X/3X, 2X/2X power levels)
- **Auto-Trade Execution** - Multi-mode connectivity (USB, Wi-Fi, Mobile Hotspot)
- **Zero-Crash Shield** - Self-healing watchdogs and memory management
- **On-Device OCR** - Google ML Kit text recognition without cloud dependency
- **Fast Flip Recovery Engine** - Dual-tier post-entry reversal protection

## 🛠️ Tech Stack

- **UI**: Jetpack Compose (Material 3)
- **Camera**: CameraX
- **Async**: Kotlin Coroutines + Flow
- **Networking**: OkHttp + WebSocket
- **OCR**: Google ML Kit Text Recognition
- **Build**: Gradle (Kotlin DSL)

## 📱 Prerequisites

- Android Studio Hedgehog or later
- Android SDK API 28+ (Android 9.0+)
- Kotlin 1.9+
- Gradle 8.3+

## 🔧 Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/q-scan-trading-app.git
   cd q-scan-trading-app
   ```

2. **Open in Android Studio**
   - File → Open → Select the project directory
   - Let Android Studio sync Gradle files

3. **Configure API Keys (Optional)**
   - Create `.env` file in project root
   - Add your Gemini API key (if using cloud mode)
   ```
   GEMINI_API_KEY=your_api_key_here
   ```

4. **Build and Run**
   - Connect Android device or start emulator
   - Click Run button or press Shift+F10

## 🔒 Security Notes

- API keys are managed via BuildConfig (Secrets Gradle Plugin)
- No hardcoded keys in source code
- Local OCR mode works without any API keys
- WebSocket connections use cleartext for local network testing

## 📊 Architecture

```
Camera → OCR Engine → Kinetic Analysis → Auto-Trade Dispatch → Multiple Relay Channels
```

**Trading Flow:**
1. Camera captures frames at 20ms intervals
2. LocalQuantVisionEngine extracts price data via ML Kit OCR
3. PureKineticTickEngine calculates momentum power (3X/3X, 2X/2X)
4. MainViewModel validates safety checks (2.5s golden window, single-entry enforcement)
5. Trade dispatches via 3 channels: WebSocket, AutoTradeBridge, HTTP Webhook

## 🌐 Multi-Mode Connectivity

- **USB Cable (ADB Reverse)**: `ws://127.0.0.1:8765`
- **Wi-Fi LAN**: Auto-discovers local gateway
- **Mobile Hotspot**: Auto-detects connected laptop via ARP table

## ⚠️ Important Notes

- **Auto-trade function is protected** - Do not modify without explicit user permission
- **Connection settings are preserved** - USB/Wi-Fi/Hotspot modes remain intact
- **Safety checks are mandatory** - 2.5s golden window, single-entry enforcement, power validation

## 📝 Development Guidelines

See `AGENTS.md` for detailed development rules and `APP_ARCHITECTURE_AND_SYSTEM_MAP.md` for complete system documentation.

## 🤝 Contributing

This is a personal trading application. For suggestions or issues, please refer to the documentation files.

## 📄 License

Personal use only. See individual component licenses for dependencies.

## 👤 Author

**MD.NURAALAM** - Trading Application Developer

---

**Disclaimer**: This application is for educational and personal trading purposes only. Trading involves financial risk. Use at your own discretion.