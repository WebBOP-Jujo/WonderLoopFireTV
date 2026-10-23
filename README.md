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
   │  • No database: drawing purged from memory on pickup             │
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

* **Compose for TV (`androidx.tv:tv-material`):** focusable cards and buttons are built on Google's TV `Surface`, which handles D-pad focus, OK/Enter, long-press, focus scale, golden focus border and glow natively ([`ui/tv`](composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/tv/TvPrimitives.kt) primitives with an Android `actual`).
* **Cinema-mode reader:** each page shows its 16:9 illustration full-bleed with a slow Ken Burns drift and **film-style subtitles synchronized with the Amazon Polly narration**: one or two sentences at a time, the spoken word lit in gold (Polly speech marks aligned to the on-screen words), so the artwork stays visible. Controls behave like a Fire TV video player: they fade out while the narrator reads and return on any key; OK pauses, ◀ ▶ always turn the page (or step through subtitles at the child's own pace when the voice is off), and **"📖 I'll read"** shows the full page text for a grown-up reading aloud. BACK closes that panel first, then exits (progress is saved).
* **Three ways to read the same page**, so the TV adapts to the child instead of the other way round:

  | Mode | How to get there | What ◀ ▶ do |
  | :--- | :--- | :--- |
  | 🔊 **Listen** (default) | Narration starts on every page | Turn the page; subtitles follow the voice, the spoken word lit in gold |
  | 🐢 **At my own pace** | Pause the narrator (OK, or "Pause" in the controls) | Step through the subtitles one or two sentences at a time, then turn the page |
  | 📖 **I'll read** | "📖 I'll read" in the controls (BACK closes it) | Turn the page; the full page text is shown for a grown-up reading aloud, and the narrator stays quiet |

  "Listen" switches the narrator back on at any time.
* **Chapter-end pages:** the drawing mission (hero polaroid, the challenge in large type, draw → scan → next chapter steps) is **narrated by Polly too**, so pre-readers hear their mission; the "challenge completed" page shows the drawn friend and a preview of the next chapter.
* **Streaming-style home:** a wide "Hi, Leo! What story shall we create today?" card with "Scan my drawing" and a taped paper note with a drawing idea (16 ideas, a new one on every visit), next to "Continue reading" (with the story's own illustration); rails of 16:9 story covers and drawn friends as pinned polaroids; focus restored to the last opened item after BACK. Entering a rail lands on its first card, or on the one the child last left there.
* **A new profile is never an empty screen:** the rails show a focusable "Your first story" slot and a "?" polaroid ("Your first friend") that lead to scanning, beside faint placeholder cards, and the side card explains the three steps (draw → scan → watch it come alive).
* **One visual language everywhere:** a shared `WonderBackdrop` (warm cream, soft colour glows, crayon sparkles in the margins; a calmer variant for the parents' area) behind every screen, profile cards with per-child accent colours, polaroid portraits, real medals, and the phone upload page styled to match.
* **Exit dialog, as Fire TV recommends:** BACK on the main screen asks "Leaving already?" with focus on "Keep creating"; BACK inside the dialog only closes it, so there is no loop ([Controller Behavior Guidelines](https://developer.amazon.com/docs/fire-tv/controller-behavior-guidelines.html)).
* **Pure D-Pad Remote Navigation:** one focus stop per card, no focus escaping at the end of a rail, focus re-acquired after dialogs, and remote-first copy ("press OK", "☰ Menu") instead of touch wording.
* **Distinct Focus Indicators:** High-contrast golden glow focus halos (`#FFD166`), subtle element scaling (`1.04x - 1.08x`), and tactile audio feedback (`SoundEffect.POP`).
* **Screen stays on** while reading, narrating or waiting for a story (no screensaver mid-story).
* **Try it without a phone:** four built-in child-style crayon drawings can be picked with the remote on the QR screen.
* **Safe Title Overscan Margins:** Strict 48dp horizontal and 24dp vertical safe areas ensure content never clips on any television panel or projector.
* **Typography for 10 Feet:** High-contrast, dyslexia-friendly, rounded typefaces (`Fredoka` for titles, `Nunito` for story text) sized for readability from 3+ meters away.
* **Parental Gate ("Zona de Padres"):** Arithmetic challenge safeguards sensitive settings, profile management and deleting books (☰ Menu on a book) from accidental child input.
* **Bilingual Support:** Full native support for Spanish (`es-ES`) and English (`en-US`), with localized prompts, UI labels, and voice narration.

---

## 🔒 Child Safety & Privacy by Design

WonderLoop is built on **data minimisation**, the core principle behind COPPA and GDPR-K: we only touch what a bedtime story needs, and we keep almost nothing.

* **Nothing to sign up for:** no accounts, no email, no passwords, no location, no contacts, no microphone, no camera on the TV.
* **The family's data stays in the living room:** a child profile is just a first name, an age and an emoji avatar, stored **only on the Fire TV**. Stories and drawn friends are saved on the device too. Grown-ups can edit or delete any profile or book behind the parental gate.
* **No database on the server:** the backend never writes anything about a child to disk. The drawing photo lives **in memory only**, inside a QR session that expires after 15 minutes, and is purged the moment the TV picks it up (`session.imageBase64 = undefined`).
* **Only what the story needs reaches the AI:** the drawing, the child's first name and age. No surname, no voice, no face, no device identifier tied to the child. Requests go to **Amazon Bedrock**, which does not store prompts or use them to train models, and to the **Gemini API under its paid-service terms**, which also excludes training on customer content.
* **No ads, no analytics, no tracking SDKs:** the only identifier is a random install ID used for rate limiting, and the in-app purchase SDK is switched off in this free build, so the app talks to no third-party SDK at all.
* **Clean logs:** server logs never contain the child's name.
* **Grown-ups stay in charge:** settings, profiles, deleting books and leaving the app are behind a parental gate or a confirmation dialog, never one accidental click away.

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
| **Voice narration** (one MP3 per page + word timings) | **Amazon Polly** neural voices: `Lucia` (es-ES), `Ruth` (en-US); voice style mapped to SSML speaking rate; **word speech marks** drive the karaoke-style highlight on the TV | Gemini TTS, per page (no highlight) | [`polly-audio.adapter.ts`](backend/src/adapters/polly-audio.adapter.ts), [`audio.adapter.ts`](backend/src/adapters/audio.adapter.ts) |
| **Illustrations** (1:1 portrait + 5 × 16:9 scenes) | Gemini image model | — | [`image.adapter.ts`](backend/src/adapters/image.adapter.ts) |

**How the integration works**
* **Structured output with Converse tool use:** each Bedrock request declares the JSON Schema of the expected answer (character profile or storybook) as a forced tool (`toolChoice`). Bedrock returns an already-parsed object, so formatting quirks in free text can never break the pipeline.
* **Resilience:** any Bedrock error, timeout or schema-invalid answer falls through to Gemini for that request only. Polly failures fall back page by page. After a credentials/permissions error, the AWS client pauses itself for 5 minutes (circuit breaker) instead of slowing every story down.
* **Switchable providers:** `LLM_PROVIDER=bedrock|gemini` and `TTS_PROVIDER=polly|gemini`; the active providers are printed on startup and returned by `GET /`.
* **Measured on real calls (us-east-1):** drawing vision ≈ 2 s, full 5-scene story ≈ 5–6 s with Nova 2 Lite; Polly narration ≈ 0.6–1.0 s per page (≈ 60–70 KB MP3).

### 📚 Story Quality Engine

Generated stories are designed like real picture books, not free-form LLM text:

* **Concrete narrative catalog** ([`story-catalog.ts`](backend/src/adapters/story-catalog.ts)): 12 recognizable worlds chosen with species affinity (a penguin goes to the snow or the sea), 15 everyday problems (a kite stuck in a tree, rain on the way home…), 8 tones, and ~50 **drawable** challenge objects (a boat, an umbrella, a ladder, a dog) that a 4-8 year-old can draw from memory. No abstract "magic beams".
* **Story spine** for every chapter: setup → funny failed try → new idea → solution using a feature **the child actually drew** → calm bedtime ending that sets up the next drawing challenge.
* **Language style sheets** (Spain Spanish with *raya* dialogue / American English with quotes), age-based sentence targets, and **saga memory** (chapter summaries carried between chapters).
* **Automatic quality review** of every story (clichés, page openings, length per age, language leakage, drawable challenge) with one guided rewrite when needed.
* **Illustration direction per page**: different camera framing and a morning-to-starry-night lighting arc, while the hero's look (English visual description + exact hex palette + reference image) stays identical on every page.
* **Happy faces by default:** a straight or downturned mouth in a child's drawing is just how kids draw, so the vision step always reads the hero's mood as positive, and portraits and scenes ask for cheerful, curious or determined faces (at most a light "oops" when there is a problem), never sad, crying or scared ones.

Measured with [`src/eval-stories.ts`](backend/src/eval-stories.ts) (`npx tsx src/eval-stories.ts 2`, 16 stories, Amazon Nova 2 Lite):

| Metric | Before | After |
| :--- | :---: | :---: |
| Concrete, drawable challenges | 38% | **100%** |
| Unique challenges | 63% | **100%** |
| Unique titles (same hero twice) | 88% | **100%** |
| Pages within the age word target | 50% | **100%** |
| Pages with clichés ("Once upon a time"…) | 4 | **0** |
| Pages starting with a sound word | 40% | **0%** |
| Average generation time | 6.5 s | **6.0 s** |

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

## 🧪 How to Test (5 minutes)

The app talks to the live AWS backend by default, so **nothing has to run on your computer**: install the APK on a Fire TV and use the remote.

### 📦 Install the prebuilt APK

**[⬇️ Download WonderLoopTV-v1.0.0.apk](https://github.com/WebBOP-Jujo/WonderLoopFireTV/releases/latest/download/WonderLoopTV-v1.0.0.apk)** (signed release, ~12 MB, Fire OS 7+ / Android 8+).

* **With the Downloader app on the Fire TV:** Settings → My Fire TV → Developer Options → allow *Install unknown apps* for Downloader, then open this URL in Downloader:
  `https://github.com/WebBOP-Jujo/WonderLoopFireTV/releases/latest/download/WonderLoopTV-v1.0.0.apk`
* **With ADB from a computer** (enable *ADB debugging* in Developer Options; the IP is in Settings → My Fire TV → About → Network):
  ```bash
  adb connect <FIRE_TV_IP>:5555
  adb install -r WonderLoopTV-v1.0.0.apk
  ```

WonderLoop appears in **Your Apps & Channels** (sideloaded apps show their square icon on a grey tile). To build it yourself instead, see [Building & Installing](#2-building--installing-on-amazon-fire-tv).

### ▶️ Walkthrough

1. **Pick a profile** on "Who is imagining today?" (or add one), then press OK on **"Hi, {name}! · Scan my drawing"**.
2. **Bring a drawing in**, either way:
   * **With a phone:** scan the QR code on the TV with any phone camera (any network works), take a photo of a drawing on paper and send it.
   * **Without a phone:** press ▼ to **"No phone at hand?"** and choose one of the four sample drawings.
3. **Meet the hero:** the TV shows the drawing turned into a storybook character. Confirm it and choose the story language (Spanish or English).
4. **Read the story:** ◀ ▶ turn pages; Amazon Polly narrates with film-style subtitles (the spoken word lights up in gold). Press **OK** to pause the narrator: ◀ ▶ then walk through the subtitles at the child's own pace. **"📖 I'll read"** shows the whole page text for a grown-up reading aloud.
5. **Close the loop:** at the end the hero asks the child to draw something that helps in the next chapter. Scan (or pick) another drawing and **Chapter 2** continues the saga with the new friend.
6. **Grown-ups:** **Parents** (top right) opens the Parents Zone behind an arithmetic parental gate (profiles, language, art style, sound effects).

Remote keys: D-Pad to move, OK to select, BACK to go back (on Home it asks before exiting), ☰ Menu on a book to delete it (parental gate).

The full checklist, expected backend log lines and troubleshooting are in **[TESTING_GUIDE.md](TESTING_GUIDE.md)**.

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
