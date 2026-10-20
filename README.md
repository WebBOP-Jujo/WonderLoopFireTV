# 🌟 WonderLoop TV — Magic Bedtime Stories from Children's Hand-Drawn Art

> **Built for the Amazon Developer Hackathon 2026: *Build, Ship, Shape***  
> *Track: Fire TV | Category: Family Entertainment & Multi-Modal AI Experience*

[![Platform: Fire OS / Android TV](https://img.shields.io/badge/Platform-Fire%20OS%20%7C%20Android%20TV-FF9900.svg?logo=amazon)](https://developer.amazon.com/fire-tv)
[![UI: Jetpack Compose for TV](https://img.shields.io/badge/UI-Jetpack%20Compose%20for%20TV-4285F4.svg?logo=android)](https://developer.android.com/tv)
[![Language: Kotlin Multiplatform](https://img.shields.io/badge/Language-Kotlin%20Multiplatform-7F52FF.svg?logo=kotlin)](https://kotlinlang.org/)
[![Backend: TypeScript & Node.js](https://img.shields.io/badge/Backend-TypeScript%20%2F%20Node.js-3178C6.svg?logo=typescript)](https://www.typescriptlang.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 📖 What is WonderLoop TV?

**WonderLoop TV** transforms physical drawings made by children on paper with crayons, pencils, or markers into **living, cinematic, 5-page bedtime storybooks** displayed on the living-room television.

Instead of passive video streaming, WonderLoop fosters **active family co-creation**:
1. 🖍️ **Draw on Paper:** The child sketches a character, animal, or object on a sheet of paper.
2. 📱 **Seamless Phone Bridge:** Any family member points their smartphone camera at the dynamic QR code on the TV screen. No companion app installation required—an instant, ephemeral web upload takes the photo.
3. 📺 **Magic on Fire TV:** Within seconds, the physical drawing is analyzed by multimodal vision AI, re-imagined as an animated character (Watercolor or 3D Animation), and brought to life in a personalized 5-page widescreen storybook with voice narration.
4. 🛋️ **Shared Living-Room Storytime:** Read and listen together on the couch using the Fire TV remote control.
5. 🔄 **The Creative Loop (Sagas):** Each chapter concludes with a "Creative Challenge" asking the child to draw the next key element (a friend, a vehicle, a magic bridge), sparking a continuous multi-chapter saga!

---

## 🎯 Alignment with Amazon Developer Hackathon Priorities

Amazon's judging criteria for the **Fire TV Track** explicitly emphasizes four core pillars, which form the DNA of WonderLoop TV:

| Hackathon Pillar | How WonderLoop TV Delivers |
| :--- | :--- |
| **Family Entertainment** | Designed for the shared living-room screen. Parents and children sit together on the couch, blending physical crafting at the table with interactive digital reading on the big screen (+42"). |
| **Multi-Modal UX** | Blends physical paper drawing, smartphone camera input via ephemeral QR code synchronization, voice narration playback, and TV remote D-Pad navigation. |
| **Computer Vision** | Vision AI extracts character anatomy, distinctive traits, and color palettes directly from hand-drawn sketches, while filtering out blank sheets and accidental table photos. |
| **AI-Enhanced Viewing** | Dynamically synthesizes high-resolution **16:9 widescreen cinematic illustrations** and synchronized natural speech narration tailored to the child's age and language. |
| **AWS Builder Readiness** | Architected with clean adapter abstractions ready for **Amazon Bedrock (Amazon Nova Pro, Nova Lite, Nova Canvas)** and **Amazon Polly**. |

---

## 🛠️ Architecture & System Design

```
                     ┌────────────────────────────────────────┐
                     │          Physical World                │
                     │  Child draws hero on sheet of paper    │
                     └───────────────────┬────────────────────┘
                                         │
                                         ▼
   ┌───────────────────────┐   Camera Scan    ┌───────────────────────┐
   │    Amazon Fire TV     │ ───────────────► │   Smartphone Browser  │
   │  (Jetpack Compose TV) │   Dynamic QR     │  (Upload Web Portal)  │
   └───────────┬───────────┘                  └───────────┬───────────┘
               │                                          │
        Polls  │ Session Status                           │ Uploads Photo
               ▼                                          ▼
   ┌──────────────────────────────────────────────────────────────────┐
   │                  WonderLoop Backend Gateway                      │
   │         (Express + TypeScript + Sharp Image Pipeline)            │
   │  • Rate Limiting & Helmet Security                               │
   │  • Ephemeral Token Pairing (15-min TTL)                          │
   │  • Strict COPPA Memory Purge (Zero Child PII Retention)          │
   └───────────────────────────────┬──────────────────────────────────┘
                                   │
                                   ▼
   ┌──────────────────────────────────────────────────────────────────┐
   │                  Multimodal AI Pipeline                          │
   │                                                                  │
   │  1. Vision Analysis       ──► Extracts traits, colors & validity │
   │  2. Character Genesis     ──► 1:1 Portrait in Watercolor / 3D    │
   │  3. Narrative Engine      ──► 5-page story + Setting Bible       │
   │  4. Widescreen Art        ──► 5 x 16:9 cinematic TV illustrations│
   │  5. Voice Synthesis (TTS) ──► Synchronized warm audio narration  │
   └──────────────────────────────────────────────────────────────────┘
```

---

## 📺 Built Specifically for the "10-Foot Experience"

WonderLoop TV is **not** a scaled-up mobile app. It was designed and built from scratch for the TV living-room environment:

* **Pure D-Pad Remote Navigation:** Complete directional control with up/down/left/right and select buttons.
* **Distinct Focus Indicators:** High-contrast golden glow focus halos (`#FFD166`), subtle element scaling (`1.04x - 1.08x`), and tactile audio feedback (`SoundEffect.POP`).
* **Safe Title Overscan Margins:** Strict 48dp horizontal and 24dp vertical safe areas ensure content never clips on any television panel or projector.
* **Typography for 10 Feet:** High-contrast, dyslexia-friendly, rounded typefaces (`Fredoka` for titles, `Nunito` for story text) sized for readability from 3+ meters away.
* **Parental Gate ("Zona de Padres"):** Arithmetic challenge safeguards sensitive settings and profile management from accidental child input.
* **Bilingual Support:** Full native support for Spanish (`es-ES`) and English (`en-US`), with localized prompts, UI labels, and voice narration.

---

## 🔒 Child Safety & Privacy by Design (COPPA Compliant)

Children's safety and privacy are paramount:
* **Zero Child PII Storage:** No personal data, real names, or geolocation are ever saved.
* **Ephemeral Second-Screen Sessions:** QR codes generate temporary sessions valid for 15 minutes that automatically expire.
* **Immediate In-Memory Buffer Purge:** As soon as the Fire TV retrieves the photo, the image buffer is purged from server memory (`session.imageBase64 = undefined`), ensuring drawings are not retained or exposed.
* **No Telemetry / Trackers:** Devoid of ad networks, tracking SDKs, or third-party analytical beacons.

---

## ☁️ AWS & Amazon AI Integration Blueprint

WonderLoop TV is architected with modular service adapters, fully ready for the **Amazon AWS AI ecosystem**:

| Capability | Current Adapter | Amazon AWS Target Adapter |
| :--- | :--- | :--- |
| **Vision Analysis** | Multimodal Vision | **Amazon Nova Pro / Nova Lite** via Amazon Bedrock `ConverseCommand` |
| **Story Generation** | Story Orchestrator | **Amazon Nova Pro** (`amazon.nova-pro-v1:0`) with 300k token context |
| **Cinematic Art** | 16:9 Image Diffusion | **Amazon Nova Canvas** (`amazon.nova-canvas-v1:0`) with `conditionImage` & 1280x720 output |
| **Voice Narration** | Audio Synthesizer | **Amazon Polly** (`Lucia` / `Sergio` Neural for ES, `Ruth` Generative for EN) |

---

## 🚀 Quick Start Guide

### Prerequisites
* **Android Development:** Android Studio Ladybug / Koala or newer, JDK 17+.
* **Backend:** Node.js 18+ and npm.
* **Hardware:** Amazon Fire TV Stick / Fire TV Cube connected to the same local Wi-Fi, or an Android TV emulator (API 28-35).

---

### 1. Launching the Backend Gateway

```bash
cd backend

# Install dependencies
npm install

# Configure environment variables
cp .env.example .env
# Edit .env with your configuration

# Compile TypeScript
npm run build

# Start the development server
npm run dev
```

The server automatically detects your local LAN IP and starts listening on port `3000` (e.g., `http://192.168.1.50:3000`).

---

### 2. Building & Installing on Amazon Fire TV

1. **Enable Developer Options on your Fire TV:**
   * Go to **Settings** > **My Fire TV** > **About**.
   * Click the device name 7 times until "You are now a developer" appears.
   * Go to **Developer Options** and enable **ADB Debugging**.

2. **Connect via ADB over Wi-Fi:**
   ```bash
   adb connect <FIRE_TV_IP_ADDRESS>:5555
   # Example: adb connect 192.168.1.38:5555
   ```

3. **Build and Install the Debug APK:**
   ```bash
   # From the project root
   ./gradlew :composeApp:assembleDebug

   # Install directly onto the Fire TV
   adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk

   # Launch the application
   adb shell am start -n org.wonderloop.app/.MainActivity
   ```

---

## 📂 Project Structure

```
WonderLoopTV/
├── composeApp/                     # Fire TV Client Application (Kotlin Multiplatform)
│   ├── src/
│   │   ├── commonMain/kotlin/.../
│   │   │   ├── data/               # WonderLoopApi & Data Models
│   │   │   ├── navigation/         # D-Pad Screen Navigation Router
│   │   │   ├── platform/           # Platform Configurations
│   │   │   ├── theme/              # 10-Foot Design System (Colors, Typography, Strings)
│   │   │   └── ui/                 # Composable Screens
│   │   │       ├── HomeScreen.kt           # Main Dashboard & Story Library
│   │   │       ├── QrScanScreen.kt         # Dynamic QR Generator & Second-Screen Pair
│   │   │       ├── HeroConfirmScreen.kt    # Character Reveal & Drawing Retry
│   │   │       ├── StoryReaderScreen.kt    # 5-Page Horizontal Reader & Sagas
│   │   │       ├── MyStoriesScreen.kt      # Profile Bookshelf
│   │   │       └── ParentsZoneScreen.kt    # Parental Gate Security
│   │   └── androidMain/            # Android TV & Fire OS Native Shell
│   │       ├── AndroidManifest.xml # Leanback Launcher & Banner
│   │       └── platform/           # Hardware Audio & Key Handler
│   └── build.gradle.kts
│
├── backend/                        # Cloud & Local Gateway (Node.js & TypeScript)
│   ├── src/
│   │   ├── adapters/               # AI Engine Adapters (Vision, Story, Image, Audio)
│   │   ├── middleware/             # Rate Limiter, App Secret Auth, Security
│   │   ├── routes/
│   │   │   ├── tv.routes.ts        # QR Pairing & Upload Portal
│   │   │   └── story.routes.ts     # Generation Pipeline REST Endpoints
│   │   ├── services/               # Story Orchestrator & Session Manager
│   │   └── server.ts               # Express Entrypoint
│   ├── package.json
│   └── tsconfig.json
│
├── LICENSE                         # MIT License
└── README.md                       # Hackathon Documentation
```

---

## 🎨 Art Styles Supported

* 🎨 **Watercolor (`watercolor`):** Soft pastel tones, organic brush strokes, warm storybook paper texture, nostalgic and calming for bedtime.
* 🎬 **3D Animation (`3d_animation`):** Vibrant volumetric lighting, soft tactile textures, expressive character expressions, cinematic depth of field.

---

## ⚖️ License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Crafted with ❤️ for the <b>Amazon Developer Hackathon 2026</b>.<br>
  <i>Empowering children to see their imagination come alive on the living-room screen.</i>
</p>
