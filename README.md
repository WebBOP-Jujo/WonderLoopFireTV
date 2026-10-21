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
| **AWS Builder** | Drawing vision and story writing run on **Amazon Nova 2 Lite via Amazon Bedrock** (structured output through the Converse API), and every page is narrated by **Amazon Polly** neural voices — each with an automatic, per-request fallback to Gemini. See [AWS Integration](#️-aws-integration-implemented). |

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
   │  1. Vision Analysis    ──► traits, colors & validity             │
   │                            Amazon Nova 2 Lite (Bedrock)          │
   │  2. Character Genesis  ──► 1:1 portrait (Watercolor / 3D)        │
   │                            Gemini image model                    │
   │  3. Narrative Engine   ──► 5-page story + Setting Bible          │
   │                            Amazon Nova 2 Lite (Bedrock)          │
   │  4. Widescreen Art     ──► 5 x 16:9 cinematic TV illustrations   │
   │                            Gemini image model                    │
   │  5. Voice Narration    ──► one MP3 per page                      │
   │                            Amazon Polly (Lucia / Ruth)           │
   │                                                                  │
   │  Every AWS call falls back to Gemini automatically on failure.   │
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

## 🛡️ Backend Security Hardening

* **No default secrets:** the server refuses to start without a random `APP_SECRET_KEY` (≥ 32 chars). The Fire TV app receives the same key at build time from the git-ignored `local.properties` (injected through `BuildConfig`), never from source code. Keys are compared in constant time.
* **Real client IPs behind proxies:** `TRUST_PROXY` makes rate limiting work per client behind a load balancer or reverse proxy, while staying off on a bare LAN server so `X-Forwarded-For` cannot be spoofed.
* **Abuse protection:** separate rate limits for AI generation, TV session polling and drawing uploads; uploads are decoded with a 25-megapixel cap (`limitInputPixels`) to block decompression-bomb images; strict MIME and size validation.
* **Minimal attack surface:** unused billing/webhook routes removed; `npm audit` reports 0 vulnerabilities.
* **Least-privilege AWS access:** the backend only needs `bedrock:InvokeModel` on Nova 2 Lite and `polly:SynthesizeSpeech` — see [`backend/aws/wonderloop-iam-policy.json`](backend/aws/wonderloop-iam-policy.json). Credentials come from the standard AWS provider chain (an instance role in production), never from code.

---

## ☁️ AWS Integration (Implemented)

| Capability | Primary provider (AWS) | Fallback | Code |
| :--- | :--- | :--- | :--- |
| **Drawing vision** (name, species, hex palette, distinctive features, blank-sheet detection) | **Amazon Nova 2 Lite** on Amazon Bedrock (`us.amazon.nova-2-lite-v1:0`, Converse API, image sent as bytes) | Gemini | [`llm.client.ts`](backend/src/adapters/llm.client.ts), [`vision.adapter.ts`](backend/src/adapters/vision.adapter.ts) |
| **Story writing** (5 scenes + English illustration prompts + drawing challenge, `es` / `en`) | **Amazon Nova 2 Lite** on Amazon Bedrock | Gemini | [`llm.client.ts`](backend/src/adapters/llm.client.ts), [`story.adapter.ts`](backend/src/adapters/story.adapter.ts) |
| **Voice narration** (one MP3 per page) | **Amazon Polly** neural voices: `Lucia` (es-ES), `Ruth` (en-US); voice style mapped to SSML speaking rate | Gemini TTS, per page | [`polly-audio.adapter.ts`](backend/src/adapters/polly-audio.adapter.ts), [`audio.adapter.ts`](backend/src/adapters/audio.adapter.ts) |
| **Illustrations** (1:1 portrait + 5 × 16:9 scenes) | Gemini image model | — | [`image.adapter.ts`](backend/src/adapters/image.adapter.ts) |

**How the integration works**
* **Structured output with Converse tool use:** each Bedrock request declares the JSON Schema of the expected answer (character profile or storybook) as a forced tool (`toolChoice`). Bedrock returns an already-parsed object, so formatting quirks in free text can never break the pipeline.
* **Resilience:** any Bedrock error, timeout or schema-invalid answer falls through to Gemini for that request only. Polly failures fall back page by page. After a credentials/permissions error, the AWS client pauses itself for 5 minutes (circuit breaker) instead of slowing every story down.
* **Switchable providers:** `LLM_PROVIDER=bedrock|gemini` and `TTS_PROVIDER=polly|gemini`; the active providers are printed on startup and returned by `GET /`.
* **Measured on real calls (us-east-1):** drawing vision ≈ 2 s, full 5-scene story ≈ 5–6 s with Nova 2 Lite; Polly narration ≈ 0.6–1.0 s per page (≈ 60–70 KB MP3).

**Why illustrations still use Gemini:** we evaluated the Amazon image models available on Bedrock. Amazon Nova Canvas reached end of life on September 30, 2026, Titan Image Generator G1 v2 is past its published end-of-life date (Legacy models are not available to new customers), and Amazon Nova 2 Omni is still in limited preview. WonderLoop also needs per-request character consistency from a reference image (the child's drawing), which Titan only offers through fine-tuning. The image adapter is isolated behind `IImageAdapter`, so an Amazon image model can be plugged in as soon as one is generally available.

---

## 🌐 Live Backend on AWS

The production backend runs on **Amazon EC2** (`t3.small`, `us-east-1`) and is publicly reachable over HTTPS:

**`https://13-217-171-0.sslip.io`** — `GET /` lists the active AI providers, `GET /api/story/health` is the health check.

```
Fire TV app / phone browser ──HTTPS──► EC2 (Elastic IP)
                                        ├─ Caddy  :443  automatic TLS certificate, HTTP→HTTPS redirect
                                        └─ backend :8080 Docker container (single replica)
                                             └─ Amazon Bedrock + Amazon Polly through the EC2 instance role
                                                (no AWS keys stored on the server)
```

* The Fire TV app uses this URL **by default** (injected at build time), so the QR upload link works from any phone, on any network.
* AWS credentials come from an IAM role attached to the instance with the least-privilege policy in [`backend/aws/wonderloop-iam-policy.json`](backend/aws/wonderloop-iam-policy.json).
* Deployment is one command from a Windows PC — [`backend/deploy/deploy-ec2.ps1`](backend/deploy/deploy-ec2.ps1) packs the source, uploads it over SSH and runs [`deploy/bootstrap.sh`](backend/deploy/bootstrap.sh) (Docker, Compose, Caddy, build, start):
  ```powershell
  cd backend
  .\deploy\deploy-ec2.ps1 -Ip <ELASTIC_IP> -KeyPath <path\to\key.pem> [-Domain api.example.com]
  ```
  Without `-Domain`, the free `<ip-with-dashes>.sslip.io` hostname is used, so no domain purchase is needed for HTTPS.

---

## 🚀 Quick Start Guide

### Prerequisites
* **Android Development:** Android Studio Ladybug or newer, **JDK 21**.
* **Backend:** **Node.js 20.9+** (tested with Node 22) and npm.
* **AWS account** with access to Amazon Bedrock (Nova 2 Lite) and Amazon Polly in `us-east-1`, plus a **Gemini API key** (illustrations and fallback).
* **Hardware:** Amazon Fire TV Stick / Fire TV Cube connected to the same local Wi-Fi, or an Android TV emulator (API 28-35).

---

### 1. Launching the Backend Gateway

```bash
cd backend

# Install dependencies
npm install

# Configure environment variables
cp .env.example .env
```

Edit `backend/.env` (see the comments in [`.env.example`](backend/.env.example)):

| Variable | Required | Purpose |
| :--- | :---: | :--- |
| `APP_SECRET_KEY` | ✅ | Shared key between the Fire TV app and the backend (≥ 32 chars). Generate one with `node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"`. The server does not start without it. |
| `GEMINI_API_KEY` | ✅ | Illustrations + automatic fallback for vision, story and narration. |
| `AWS_REGION` | ✅ | `us-east-1` (Nova 2 Lite geo inference profile + Polly neural voices). |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | Local only | An IAM user limited to [`aws/wonderloop-iam-policy.json`](backend/aws/wonderloop-iam-policy.json). On EC2, use an instance role instead and leave these unset. |
| `LLM_PROVIDER` / `TTS_PROVIDER` | Optional | `bedrock` / `polly` by default; set to `gemini` to bypass AWS. |
| `TRUST_PROXY` | Optional | `1` when running behind a reverse proxy / load balancer, `0` on a LAN. |

```bash
# Compile TypeScript
npm run build

# Start the development server
npm run dev
```

The server automatically detects your local LAN IP and starts listening on port `3000` (e.g., `http://192.168.1.50:3000`). The startup banner shows which providers are active.

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

3. **Set the app key (and optionally the backend URL):** add to the git-ignored `local.properties` in the project root (or export `WONDERLOOP_APP_KEY` / `WONDERLOOP_BACKEND_URL`):
   ```properties
   wonderloop.appKey=<same value as the backend's APP_SECRET_KEY>
   # Optional: defaults to the public AWS backend. Use your PC's LAN address for a local backend:
   # wonderloop.backendUrl=http://192.168.1.50:3000
   ```
   The backend address can also be changed at runtime from the server dialog on the QR screen (an IP becomes `http://<ip>:3000`, a hostname becomes `https://<hostname>`).

4. **Build and Install the Debug APK:**
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
│   │   ├── adapters/               # AI Engine Adapters
│   │   │   ├── llm.client.ts       # Amazon Bedrock (Nova 2 Lite) + Gemini fallback
│   │   │   ├── vision.adapter.ts   # Drawing -> CharacterProfile
│   │   │   ├── story.adapter.ts    # 5-scene saga writer
│   │   │   ├── polly-audio.adapter.ts # Amazon Polly narration
│   │   │   ├── audio.adapter.ts    # Gemini TTS + per-page fallback
│   │   │   └── image.adapter.ts    # Portrait & 16:9 illustrations
│   │   ├── middleware/             # App Secret Auth & quota guard
│   │   ├── routes/
│   │   │   ├── tv.routes.ts        # QR Pairing & Upload Portal
│   │   │   └── story.routes.ts     # Generation Pipeline REST Endpoints
│   │   ├── services/               # Story Orchestrator & Session Manager
│   │   └── server.ts               # Express Entrypoint
│   ├── aws/wonderloop-iam-policy.json  # Least-privilege IAM policy (Bedrock + Polly)
│   ├── deploy/                     # EC2 deployment: deploy-ec2.ps1, bootstrap.sh, docker-compose.yml, Caddyfile
│   ├── Dockerfile                  # Multi-stage Node 22 image (runs as non-root)
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
