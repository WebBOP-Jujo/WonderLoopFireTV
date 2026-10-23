# 📺 WonderLoop TV Testing Guide — Amazon Fire TV Stick 4K (+ QR Phone Companion)

**WonderLoop TV** runs on the **Amazon Fire TV Stick 4K** with 100% remote (D-Pad) navigation, and children's paper drawings are scanned from any smartphone through a QR bridge.

**Production backend (the app's default):** `https://13-217-171-0.sslip.io`, on **Amazon EC2** (`t3.small`, `us-east-1`) behind Caddy (automatic HTTPS). With it **nothing needs to run on your PC**: just install the APK. The local backend (sections 2–5) is only needed for development.

**AI providers:**
- Drawing vision and story text → **Amazon Nova 2 Lite (Amazon Bedrock)**, with automatic fallback to Gemini.
- Narration → **Amazon Polly** (Lucia `es-ES` / Ruth `en-US`), with per-page fallback to Gemini TTS.
- Illustrations (1:1 portrait + five 16:9 scenes) → Gemini.

---

## 📋 Contents
0. [Quick test with the AWS backend](#0-quick-test-with-the-aws-backend)
1. [Architecture and data flow](#1-architecture-and-data-flow)
2. [Backend configuration (`backend/.env`)](#2-backend-configuration-backendenv)
3. [AWS credentials for local testing](#3-aws-credentials-for-local-testing)
4. [App key (`local.properties`)](#4-app-key-localproperties)
5. [Starting the backend](#5-starting-the-backend)
6. [Building and installing on the Fire TV](#6-building-and-installing-on-the-fire-tv)
7. [End-to-end test checklist](#7-end-to-end-test-checklist)
8. [Troubleshooting](#8-troubleshooting)
9. [Backend deployment on AWS EC2](#9-backend-deployment-on-aws-ec2)

---

## 0. Quick test with the AWS backend

1. Build the APK with the key in `local.properties` (section 4) and install it (section 6). The AWS backend URL is already the default.
2. Open the app → *Scan my drawing* → scan the QR code with **any phone**, even one that is not on your Wi-Fi: the link is `https://13-217-171-0.sslip.io/upload/<sessionId>`.
3. No phone at hand? On the same screen, move down to *"No phone at hand?"* and pick one of the four built-in sample drawings.
4. Server check from a browser: `https://13-217-171-0.sslip.io/` (active providers) and `https://13-217-171-0.sslip.io/api/story/health`.

---

## 1. Architecture and data flow

```text
[ Phone (browser, no app) ] --scans QR--> https://<backend>/upload/<sessionId>
        │ uploads the drawing photo (downscaled to 1024 px in a <canvas>)
        ▼
[ Backend gateway (EC2, or a PC on port 3000) ] <──polls every 1.2 s── [ Fire TV Stick 4K (Compose app) ]
        │
        ├──► Amazon Bedrock · Nova 2 Lite ──► analyses the drawing (name, species, HEX colours)
        ├──► Gemini Image                 ──► 1:1 hero portrait
        ├──► Amazon Bedrock · Nova 2 Lite ──► 5-scene story + drawing challenge
        ├──► Gemini Image                 ──► five 16:9 illustrations
        └──► Amazon Polly                 ──► MP3 narration per page
             (every AWS call falls back to Gemini automatically on failure)
```

---

## 2. Backend configuration (`backend/.env`)

```powershell
cd backend
npm install
copy .env.example .env
```

Minimum variables in `backend/.env` (the file is git-ignored):

```env
PORT=3000
NODE_ENV=production

# Required: the server does NOT start without it (at least 32 characters)
# Generate: node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
APP_SECRET_KEY=<random_secret>

# Illustrations + fallback for vision, story and voice
GEMINI_API_KEY=<Google_AI_Studio_key>

# AWS (see section 3)
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=<local_testing_only>
AWS_SECRET_ACCESS_KEY=<local_testing_only>
LLM_PROVIDER=bedrock
TTS_PROVIDER=polly

# 0 on a LAN; 1 behind a proxy / load balancer
TRUST_PROXY=0
```

> To switch AWS off temporarily: `LLM_PROVIDER=gemini` and/or `TTS_PROVIDER=gemini`.

---

## 3. AWS credentials for local testing

In production (EC2) an **IAM instance role** is used and no keys are needed. To test on a PC:

1. AWS console → **IAM → Users → Create user** → name it `wonderloop-local`, **without** console access.
2. In the user → **Permissions → Add permissions → Create inline policy → JSON** → paste [`backend/aws/wonderloop-iam-policy.json`](backend/aws/wonderloop-iam-policy.json) (only `polly:SynthesizeSpeech` and `bedrock:InvokeModel` on Nova 2 Lite).
3. **Security credentials → Create access key → "Local code"** → copy both keys into `backend/.env`. The secret key is shown only once.
4. **Never** paste the keys into code, a chat or a commit.

---

## 4. App key (`local.properties`)

The Fire TV app sends an `X-WonderLoop-Key` header whose value is injected at build time. Add **the same value** as `APP_SECRET_KEY` to `local.properties` (project root, git-ignored):

```properties
wonderloop.appKey=<same_value_as_APP_SECRET_KEY>
```

If you change the secret, **rebuild and reinstall the APK**: an old APK will get `401 UNAUTHORIZED`.

By default the app talks to the AWS backend. To use a backend on your PC during development, also add:

```properties
wonderloop.backendUrl=http://192.168.0.35:3000
```

It can also be changed at runtime from the server dialog on the QR screen: an IP becomes `http://<ip>:3000` and a hostname becomes `https://<hostname>`.

---

## 5. Starting the backend

```powershell
cd backend
npm run build
npm run dev
```

The startup banner shows the LAN IP and the active providers, for example:

```text
🤖 AI Models:
   - Text & Vision: Amazon Bedrock (us.amazon.nova-2-lite-v1:0) -> fallback Gemini (gemini-3.5-flash-lite)
   - Image Gen:     gemini-3.1-flash-lite-image (16:9 Scenes / 1:1 Portraits)
   - Speech (TTS):  Amazon Polly (neural, us-east-1) -> fallback gemini-3.8-flash-lite-tts
```

Quick check from the PC's browser: `http://localhost:3000/api/story/health` must return `{"status":"ok",...}`, and `http://localhost:3000/` lists the active providers.

---

## 6. Building and installing on the Fire TV

On the Fire TV: **Settings → My Fire TV → Developer Options** with *ADB debugging* and *Apps from Unknown Sources* turned on (if Developer Options is hidden, open **About** and click the device name 7 times). The Fire TV must be on the same Wi-Fi as the PC.

```powershell
# 1. Build the APK (JDK 21)
.\gradlew.bat :composeApp:assembleDebug

# 2. Connect ADB over Wi-Fi (use your Fire TV's IP: Settings → My Fire TV → About → Network)
adb connect <FIRE_TV_IP>:5555

# 3. Install or update the APK
adb -s <FIRE_TV_IP>:5555 install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk

# 4. Launch the app on the TV
adb -s <FIRE_TV_IP>:5555 shell am start -n org.wonderloop.app/.MainActivity
```

If the app cannot reach the backend, check the server address in the dialog on the QR screen (D-Pad navigable). The app tries, in this order: the address saved in that dialog, the build-time one (`wonderloop.backendUrl` or the AWS backend), `10.0.2.2:3000` (emulator) and `localhost:3000`.

> Sideloaded apps show their square icon on a grey tile in the Fire TV "Your apps" row; that is how Fire OS displays every app installed outside the Appstore.

---

## 7. End-to-end test checklist

### 🟢 1. D-Pad navigation
- Home focuses the *"Hi, {name}! · Scan my drawing"* card automatically, with the "💡 Today's idea" note (it changes every time you come back to Home and is always fully readable, never cut with "…").
- The *"Open library"* and *"All friends"* cards are reachable with the D-Pad and show the golden focus halo; in "My friends" the pushpin stays visible on top of the focused polaroid.
- On a new profile (no stories yet) the rails show "＋ Your first story" and the "?" polaroid "Your first friend": ▼ from the red card walks through them, OK opens scanning and ▶ does not move focus out of the row.
- "Add profile" (profile picker and Parents Zone) gets a golden border and no grey veil when focused.
- `BACK` on Home opens the "Leaving already?" dialog with focus on "Keep creating"; BACK again closes it, and "Exit WonderLoop" returns to the Fire TV launcher.

### 🟢 2. Scanning the drawing through the QR code
1. Press *"Scan my drawing"* → `QrScanScreen` shows a large QR code.
2. Scan it with any phone → the upload page opens (`/upload/<sessionId>`).
3. Take a photo of the drawing or pick it from the gallery → the TV goes through *Phone connected → Sending the drawing → Drawing received!* without touching the remote.
4. **Without a phone:** move down with the D-Pad to *"No phone at hand?"* and choose one of the 4 preloaded sample drawings.

### 🟢 3. Generated hero (vision with Nova 2 Lite)
- The confirmation screen shows a name, species and colours consistent with the drawing, and the portrait in the chosen style, always with a friendly, happy face.
- The backend console shows: `✅ [Bedrock] Drawing Vision Analysis via us.amazon.nova-2-lite-v1:0 (tool-use) in ~2s`.

### 🟢 4. Illustrated, narrated story (Nova 2 Lite + Gemini Image + Polly)
1. Confirm the hero and choose the language (`es` / `en`) → generation screen.
2. Backend console:
   - `✅ [Bedrock] Story Text Generation (...) via us.amazon.nova-2-lite-v1:0 (tool-use) in ~5-6s`
   - `✅ [PollyAudioAdapter] Page N synthesized ... (Lucia/neural ...)` (or `Ruth` in English) for all 5 pages.
3. In the reader: turn pages with `DPAD_RIGHT` / `DPAD_LEFT` (or fast-forward / rewind); narration starts automatically and `PLAY_PAUSE` (or OK while the controls are hidden) pauses it. The voice must be Lucia (Spanish) or Ruth (English), with film-style subtitles where the spoken word lights up in gold.
4. **Reading modes** (same page, three ways):
   - **Listen** (default): ◀ ▶ turn the page while the subtitles follow the voice.
   - **At my own pace:** press OK (or "Pause") to stop the narrator; now ◀ ▶ step through the subtitles one or two sentences at a time and, after the last one, turn the page. "Listen" switches the voice back on.
   - **"📖 I'll read":** shows the full page text for a grown-up and silences the narrator; ◀ ▶ still turn the page and BACK closes the panel before leaving the story.
5. The text must not show asterisks (`*Plop!*`).

### 🟢 5. Creative loop (sagas)
1. After page 5 the **drawing challenge** appears (narrated as well).
2. Scan a new drawing → **Chapter 2** is generated with the original hero and the new companion, plus the celebration page between chapters.

### 🟢 6. Gemini fallback (optional)
- Start the backend with invalid or no AWS credentials: the console shows `⚠️ [Bedrock] Credentials/permissions/model access problem ... Bedrock paused for 5 min` and `🔁 [LLM] ... Using Gemini`, and the story is still generated.

### 🟢 7. Parents Zone
- Protected by an arithmetic question (parental gate). From there you manage profiles, language, art style and sound effects.
- Valid local coupons: `AMAZON2026`, `HACKATHON2026`, `DEVPOSTVIP`, `WONDERVIP` (in hackathon mode the quota is already unlimited).

---

## 8. Troubleshooting

| Problem | Likely cause | Fix |
| :--- | :--- | :--- |
| The backend exits at startup with `🚨 [Security] APP_SECRET_KEY is missing...` | `APP_SECRET_KEY` is missing or shorter than 32 characters | Generate one (section 2) and put it in `local.properties` too. |
| The app gets `401 UNAUTHORIZED` | `wonderloop.appKey` does not match `APP_SECRET_KEY`, or the APK was built with an old key | Make both values equal, rebuild and reinstall the APK. |
| Console: `AccessDeniedException ... bedrock:InvokeModel` | The IAM user lacks the Bedrock policy | Attach [`wonderloop-iam-policy.json`](backend/aws/wonderloop-iam-policy.json) to the user. Gemini covers the request meanwhile. |
| Console: `Polly paused for 5 min` | AWS credentials missing or without `polly:SynthesizeSpeech` | Check `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` and the policy; restart the backend. |
| The QR code does not open the page on the phone (local backend) | The phone is not on the same Wi-Fi, or the detected LAN IP is wrong | Join the same network or set `LAN_IP=<pc_ip>` in `.env`. With the AWS backend any network works. |
| Upload fails with `INVALID_IMAGE_PAYLOAD` | Corrupt image or larger than 25 megapixels | Use a normal photo; the phone page already downsizes it to 1024 px. |
| The app shows *"Offline"* | The Fire TV cannot reach the backend (port 3000 on a local PC) | Check the address in the QR screen dialog and the Windows firewall (port 3000). |
| Server logs show `🚦 [RateLimit:...]` | Too many requests from the same public IP (all devices at home count together) | Informational. If it happens in normal use, tune the limits in `backend/src/server.ts`. |
| Local narration is silent when server audio is missing | The system TTS engine is not visible to the app (Android 11+) | The manifest already declares `<queries>` for `TTS_SERVICE`; reinstall the current APK. |
| App logs | On-device debugging | `adb -s <FIRE_TV_IP>:5555 logcat -s System.out:V` |

---

## 9. Backend deployment on AWS EC2

Current infrastructure (region `us-east-1`):
- **EC2 `t3.small`** instance with Amazon Linux 2023, 20 GiB gp3, **Elastic IP `13.217.171.0`** (the address survives stop/start).
- **IAM role `wonderloop-backend-role`** with the policy [`backend/aws/wonderloop-iam-policy.json`](backend/aws/wonderloop-iam-policy.json): Bedrock and Polly work with no AWS keys on the server.
- Security group: 22 (your IP only), 80 and 443 (open).
- Containers (Docker Compose): `caddy` (HTTPS on :443 with an automatic certificate for `13-217-171-0.sslip.io`, kept in a persistent volume) and `backend` (:8080, a single replica because QR sessions live in memory). Both restart automatically when the instance boots.

**Deploy a new version** (from the `backend` folder, in PowerShell):

```powershell
.\deploy\deploy-ec2.ps1 -Ip 13.217.171.0 -KeyPath "<path\to\key.pem>"
```

The script builds the production `.env` from `backend/.env` **without the local AWS keys**, uploads the code over SSH, runs [`deploy/bootstrap.sh`](backend/deploy/bootstrap.sh) (installs Docker and Compose if missing, 2 GB swap, `docker compose up -d --build`) and waits until `/api/story/health` answers over HTTPS.

**Server logs:**

```powershell
ssh -i "<path\to\key.pem>" ec2-user@13.217.171.0 "cd /opt/wonderloop/backend/deploy && sudo docker compose logs -f backend"
```

The logs should show `✅ [Bedrock] ... (tool-use)` and `✅ [PollyAudioAdapter] ...`. If you see `🔁 [LLM] ... Using Gemini` or `Polly paused`, check that the IAM role is attached to the instance.

**Approximate cost:** ~$15/month for the `t3.small` plus Bedrock and Polly usage (cents while testing). Keep an AWS Budgets alert on and stop or terminate the instance when judging ends (11/20/2026).
