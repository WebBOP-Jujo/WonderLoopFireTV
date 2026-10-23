# Friction Log: Developer Experience Porting to Amazon Fire TV Stick 4K

**Hackathon Track:** *Build, Ship, Shape: Amazon Developer Hackathon (2026)*  
**Project:** WonderLoop TV — *AI-Powered Illustrated Bedtime Stories for Children on Fire TV*  
**Developer Persona:** Full-Stack & Mobile Multiplatform Engineer  
**Target Hardware:** Amazon Fire TV Stick 4K (3rd Gen / Max, Fire OS 7.6 / Fire OS 8, 1.5–2GB RAM)  
**Host Environment:** Windows 11, VS Code + Antigravity AI Agent, Claude Code, Android SDK API 35 (Android 15), ADB over Wi-Fi  
**AWS Services Used:** Amazon Bedrock (Amazon Nova 2 Lite, Converse API, `us-east-1`), Amazon Polly (neural voices), AWS IAM  
**Submission Goal:** In-depth Developer Experience (DX) Friction Log (+10% Hackathon Bonus)  
**Overall DX Score:** **8.8 / 10**

---

## 1. Executive Summary

WonderLoop was originally created as a Kotlin Multiplatform mobile application where children draw a character on paper, snap a photo using their phone camera, and receive a customized bedtime adventure.

Transitioning this concept to **Amazon Fire TV Stick 4K** presented unique platform challenges:
1. **The TV Camera Dilemma:** Televisions do not have built-in cameras.
2. **10-Foot UI & Remote Navigation:** Replacing touch-based scrolling with a 3-meter living room D-Pad navigation model.
3. **Hardware Constraints:** Optimizing audio synthesis, image decoders, and memory consumption on streaming stick SoCs (quad-core ARM, limited thermal and heap envelope).
4. **Moving the AI pipeline to AWS:** Replacing our original Gemini text, vision and TTS calls with Amazon Bedrock (Nova 2 Lite) and Amazon Polly, while keeping Gemini as an automatic fallback (Frictions 6–9).
5. **A reading experience for the 10-foot screen:** Using TV focus components from shared Kotlin Multiplatform UI, and showing story text without hiding the full-screen artwork (Frictions 10–12).

This friction log chronicles our end-to-end journey, identifying key stumbling blocks, platform delights, and actionable feedback for Amazon's Developer Tooling team.

---

## 2. Friction Points Logged (Chronological Analysis)

### 🔴 Friction 1: The "Camera on TV" Paradigm & Absence of a First-Party Mobile-to-TV Bridge
* **Category:** Architecture & Platform Capabilities  
* **Severity:** **HIGH** (Friction Rating: 4.5 / 5)  
* **Context:** WonderLoop requires digitizing physical paper drawings. Smart TVs and Fire TV Sticks do not possess cameras. Existing TV solutions either require purchasing third-party USB webcams (unrealistic for families) or completely drop the drawing scanning feature.
* **The Struggle:** 
  - There is currently no out-of-the-box Amazon SDK to pair a Fire TV screen with an ambient smartphone camera (e.g., similar to how Apple AirDrop or specialized Chromecast controls work).
  - As a third-party developer, we had to architect an entirely custom local Wi-Fi bridging solution: an in-memory session manager (`backend/src/routes/tv.routes.ts`), an on-screen QR code generator (`QrCodeCanvas.kt`), a mobile-optimized upload web app (`/upload/:sessionId`), and a polling engine on the Fire TV app (`QrScanScreen.kt`).
* **Delight Factor:** Once built, scanning a QR code with any smartphone (iOS or Android) without downloading an auxiliary app felt like sheer magic in the living room.
* **Actionable Feedback for Amazon:**
  > **Feature Request:** Launch an official **Amazon Fire TV Mobile Companion SDK**. A lightweight library where the TV calls `AmazonCompanion.requestInput(type = CAMERA_SCAN)` and Fire OS automatically renders a secure QR code on screen. When scanned with any mobile browser, Amazon's cloud/local bridge handles image transfer directly back to the TV app.

---

### 🟡 Friction 2: Contradictory BACK-Button Guidance (Android TV vs. Fire TV)
* **Category:** Certification & OS Guidelines  
* **Severity:** **MEDIUM** (Friction Rating: 3 / 5)  
* **Task attempted:** Decide what `BACK` should do on the app's main screen: exit straight to the launcher, or ask *"¿Seguro que quieres salir?"* first.
* **The Struggle:**
  - Google's Android TV navigation guide says the BACK button should not be "gated by confirmation screens".
  - Amazon's Fire TV [Controller Behavior Guidelines](https://developer.amazon.com/docs/fire-tv/controller-behavior-guidelines.html) recommend the opposite: on the main screen, capture BACK to show an *"Are you sure you want to exit the XYZ app?"* dialog (while avoiding a close/reopen loop).
  - Because Fire OS is an Android fork, most Compose/TV material we found online follows Google's rule. We first removed our exit dialog believing it was an Appstore rejection risk, and only later found Amazon's own guideline recommending it.
* **Resolution:**
  - `BACK` on the home screen (and on the root profile picker) opens a child-friendly exit dialog with focus on **"Keep creating"**, so an accidental BACK + OK never closes the app. BACK inside the dialog only closes it, so there is no loop. The splash screen still exits directly.
* **Actionable Feedback for Amazon:**
  > **Docs:** Add a short "Fire TV vs. Android TV differences" box to the controller guidelines (BACK on the main screen, exit dialogs, Menu key), so developers arriving from Google's docs know which rule applies on Fire OS.

---

### 🟡 Friction 3: AudioTrack Allocation & Thread Churn on Quad-Core Streaming SoCs
* **Category:** Performance & Resource Management  
* **Severity:** **MEDIUM** (Friction Rating: 3.5 / 5)  
* **Context:** A mobile phone with 8GB RAM can easily spawn an ephemeral `AudioTrack` and worker thread whenever a sound effect plays. On a Fire TV remote, users press D-Pad arrows rapidly (10–30 clicks per minute) to browse horizontal rails.
* **The Struggle:**
  - In our initial implementation, every D-Pad navigation click generated an in-memory PCM wave, instantiated a new `AudioTrack`, and launched a native thread with `sleep(durationMs)`.
  - On the Fire TV Stick 4K (MediaTek MT8696 / Amlogic SoC), creating 20 threads in quick succession triggered frequent Garbage Collection (GC) pauses and noticeable UI frame drops (jank) during focus transitions.
* **Resolution:**
  - We refactored `PlatformAudioService.android.kt` to pre-cache all 4 synthesized sound effects (`POP`, `MAGIC_CHIME`, `PAGE_TURN`, `CELEBRATION`) in `AudioTrack.MODE_STATIC` during app warmup.
  - Replays now execute on a single reusable daemon thread (`sfxExecutor`) with zero runtime memory allocations.
* **Actionable Feedback for Amazon:**
  > **Documentation Tip:** Include a dedicated "High-Frequency Remote SFX Best Practices" section in the Fire TV Audio Guidelines, recommending `MODE_STATIC` or SoundPool over runtime `AudioTrack` creation.

---

### 🟡 Friction 4: Local Wi-Fi Subnet Cleartext Blocking on Android 9+ (Fire OS 7/8)
* **Category:** Network Security & Prototyping  
* **Severity:** **MEDIUM** (Friction Rating: 3 / 5)  
* **Context:** During development and local hackathon judging, the Fire TV Stick 4K communicates with the developer's workstation via LAN IP (`http://192.168.x.x:3000`).
* **The Struggle:**
  - Android 9+ (API 28+) disables unencrypted cleartext HTTP by default.
  - Simply enabling `android:usesCleartextTraffic="true"` globally in `AndroidManifest.xml` triggers security warnings and risks Appstore certification issues for production releases.
  - Finding the exact configuration for `<network-security-config>` that permits local IP subnets while strictly enforcing HTTPS for cloud endpoints required manual trial and error.
* **Resolution:**
  - Created [`network_security_config.xml`](composeApp/src/androidMain/res/xml/network_security_config.xml) with explicit trust anchors and local subnet exception rules.
* **Actionable Feedback for Amazon:**
  > **CLI / MCP Tool:** Provide an `amazon-devices-network-config` tool in the MCP server that auto-generates a certified network security XML file with local development profiles.

---

### 🟡 Friction 5: Jetpack Compose D-Pad Focus Collisions (`Surface` vs `tvFocusable`)
* **Category:** UI/UX & Compose Framework  
* **Severity:** **MEDIUM** (Friction Rating: 3 / 5)  
* **Context:** Modern TV development has shifted to Jetpack Compose. However, standard Material 3 components were designed for touchscreens.
* **The Struggle:**
  - Using `androidx.compose.material3.Surface(onClick = ...)` adds an implicit touch click listener and mobile ripple effect.
  - When combined with a custom `.tvFocusable(...)` modifier designed to draw an animated 3.5dp golden TV halo and 1.05x scale zoom, the two systems clash: click events fire twice, and focus visual states desynchronize from the remote control.
* **Resolution:**
  - Systematically removed `onClick` parameters from all `Surface` containers, allowing `tvFocusable` to exclusively handle `DirectionCenter`, `Enter`, and focus states.
* **Actionable Feedback for Amazon:**
  > **Framework Guidance:** Accelerate official Amazon Fire TV reference guides for Jetpack Compose Multiplatform, showing how to implement TV focus indicators without touch ripple interference.

---

### 🔴 Friction 6: No Generally Available Amazon Image Model for a New Bedrock Project
* **Category:** Amazon Bedrock — Model Availability & Lifecycle  
* **Severity:** **HIGH** (Friction Rating: 4 / 5)  
* **Task attempted:** Replace our Gemini image model (1:1 hero portrait + five 16:9 scene illustrations that must keep the child's character consistent) with a first-party Amazon model on Bedrock.
* **Steps taken:**
  1. Looked up Amazon Nova Canvas, which our design notes had targeted (1280x720 output, `conditionImage`).
  2. Checked the Bedrock *Model lifecycle (Legacy)* page: Nova Canvas (`amazon.nova-canvas-v1:0`) reached **EOL on September 30, 2026**.
  3. Evaluated Titan Image Generator G1 v2 (`amazon.titan-image-generator-v2:0`) from its feature page `titan-image-models.html`. The page describes all its features but shows no lifecycle banner; only the lifecycle tables revealed a Legacy date of Dec 30, 2025 and EOL of June 30, 2026 — and Legacy models are not available to new customers.
  4. Checked Amazon Nova 2 Omni (text + image output): announced in December 2025, still limited preview for Nova Forge customers.
* **Expected:** At least one GA Amazon image-generation model on Bedrock, or a clearly documented migration path from Nova Canvas.
* **Actual:** No usable first-party image model for a new account. Feature documentation pages for retired models still read as if the model were available.
* **Workaround:** Kept Gemini for illustrations behind the `IImageAdapter` interface, so an Amazon image model can be plugged in later without touching the orchestrator. Vision, story and narration did move to AWS (Frictions 7–9).
* **Actionable Feedback for Amazon:**
  > **Docs:** Show a lifecycle banner (Legacy / EOL date / recommended replacement) at the top of every model feature page, not only in the lifecycle tables.  
  > **Product:** Publish a GA replacement for Nova Canvas (with reference-image character consistency) or document the recommended Bedrock image model for new projects.

---

### 🟡 Friction 7: Nova 2 Lite Returns Invalid JSON in Plain-Text Mode
* **Category:** Amazon Bedrock — Model Output Reliability  
* **Severity:** **MEDIUM** (Friction Rating: 3.5 / 5)  
* **Task attempted:** Get a strict JSON object (5-scene storybook / character profile) from `us.amazon.nova-2-lite-v1:0` via the Converse API.
* **Steps taken:**
  1. System prompt: "Reply with exactly one valid JSON object… no markdown fences".
  2. Spanish stories parsed fine, but **3 of 3 English story runs failed to parse**. Logging the raw output showed the JSON wrapped in ```` ```json ```` fences and, more importantly, string values delimited with typographic quotes (`"textContent": “Look for…”`).
  3. Added an explicit "use straight ASCII double quotes" instruction plus a typographic-quote repair step in our parser — the next 3 English runs still failed.
  4. Switched to **Converse tool use** with a forced `toolChoice`. With a generic schema (`{"type": "object", "additionalProperties": true}`) the model silently returned an **empty object `{}`** in every call (vision and story).
  5. Declared the full JSON Schema of each answer (properties + `required`) as the tool's `inputSchema`.
* **Expected:** Valid JSON when the prompt demands it, or a native structured-output mode. The Nova 2 Lite model card lists *Structured outputs* as **not supported**.
* **Actual:** Unreliable JSON in text mode; empty tool input with a permissive schema and no warning.
* **Workaround:** Forced tool use with an explicit schema → **6/6 calls succeeded** (Spanish and English), returning already-parsed objects. Gemini remains as automatic fallback if validation ever fails.
* **Actionable Feedback for Amazon:**
  > **Product:** Enable Bedrock *Structured outputs* for Nova 2 Lite.  
  > **Docs:** In the tool-use guide, warn that a schema without `properties` may produce an empty `toolUse.input`, and show "forced tool as structured output" as an official pattern.

---

### 🟢 Friction 8: IAM Permissions for Cross-Region Inference Profiles
* **Category:** AWS IAM & Bedrock Onboarding  
* **Severity:** **LOW** (Friction Rating: 2 / 5)  
* **Task attempted:** Call Nova 2 Lite from `us-east-1` with a least-privilege IAM user.
* **Steps taken:** Created an IAM user with only Polly access, then called Bedrock to confirm the fallback path. The error was clear (`AccessDeniedException … not authorized to perform: bedrock:InvokeModel on resource: arn:aws:bedrock:us-east-1:<account>:inference-profile/us.amazon.nova-2-lite-v1:0`).
* **Expected vs. actual:** Granting the ARN named in the error looks sufficient, but a geo inference profile can route to us-east-1, us-east-2 and us-west-2, so the policy must also allow the underlying `foundation-model/amazon.nova-2-lite-v1:0` ARN in those Regions. Nothing in the error message points to that second requirement.
* **Workaround:** Wrote a minimal policy granting both the inference-profile ARN and the foundation-model ARN with a Region wildcard ([`backend/aws/wonderloop-iam-policy.json`](backend/aws/wonderloop-iam-policy.json)); it worked on the first try.
* **Actionable Feedback for Amazon:**
  > **Console:** Offer a "copy least-privilege policy" button on each model card that already includes the inference-profile and destination-Region foundation-model ARNs.

---

### 🟢 Friction 9: Localized IAM Console Wording ("Crear persona")
* **Category:** AWS Console UX / Localization  
* **Severity:** **LOW** (Friction Rating: 1.5 / 5)  
* **Task attempted:** Create an IAM user for local Bedrock/Polly testing with the console in Spanish.
* **Steps taken:** Opened IAM → Users. The page title says "Usuarios de IAM", but the action buttons read **"Crear persona"** / **"Eliminar persona"** ("create/delete *person*").
* **Expected:** "Crear usuario", matching the page title and the documentation.
* **Actual:** We paused to double-check that "persona" really meant an IAM user and not a different identity type.
* **Workaround:** Confirmed it was the standard *Create user* flow.
* **Actionable Feedback for Amazon:**
  > **Localization:** Use "usuario" consistently in the Spanish IAM console.

---

### 🟡 Friction 10: Compose for TV (`androidx.tv:tv-material`) Cannot Be Used from Kotlin Multiplatform Shared UI
* **Category:** UI Framework & Multiplatform  
* **Severity:** **MEDIUM** (Friction Rating: 3 / 5)  
* **Task attempted:** Move our 10-foot UI from mobile Material 3 to Google's TV components (`androidx.tv.material3.Surface`, focus scale/border/glow) as Fire TV reviewers recommend.
* **Steps taken:** Added `androidx.tv:tv-material:1.0.0` and tried to call it from the shared UI, which lives in `commonMain` (Compose Multiplatform).
* **Expected:** TV components usable from shared Compose code, like Material 3.
* **Actual:** The library is published for Android only, so `commonMain` cannot see it. A full migration would have meant moving ~15,000 lines of UI to `androidMain` or rewriting ~250 Material 3 components two days before the deadline.
* **Workaround:** Thin `expect`/`actual` primitives (`TvClickableSurface`, `KeepScreenOn`) whose Android `actual` wraps the tv-material `Surface`. The reader, home screen and chapter-end pages use them; the remaining screens keep our own focus modifier.
* **Actionable Feedback for Amazon:**
  > **Guidance:** A Fire TV sample showing Compose Multiplatform + TV focus components (or an `expect`/`actual` pattern like ours) would save every KMP team the same investigation.

---

### 🟡 Friction 11: No 10-Foot Guidance for Reading-Heavy Apps (Text vs. Full-Screen Artwork)
* **Category:** UX Guidelines  
* **Severity:** **MEDIUM** (Friction Rating: 3 / 5)  
* **Context:** Fire TV design guidance covers media browsing and video playback well, but a storybook is a mix of both: a full-screen 16:9 illustration plus 55–75 words of text per page that must be legible from the sofa.
* **The Struggle:**
  - Our first "cinema mode" put the whole page text on a white reading panel. At legible 10-foot sizes (≥ 23 sp) the panel covered **~55 % of the illustration**, including the character's body — the part children care about most.
  - Shrinking the text breaks 10-foot legibility; a side-by-side layout crops the 16:9 art to less than half the screen.
* **Resolution:** We borrowed the **Fire TV video player conventions** instead of inventing new ones:
  - Film-style subtitles (1–2 sentences, 28 sp, gold highlight on the word being narrated) synced to Amazon Polly speech marks, covering ~20 % of the frame.
  - Controls in a vertical rail that fades out while the narrator reads and returns on any key; OK pauses, like in a video app. ◀ ▶ always turn the page; an "I'll read" mode shows the full text for a grown-up.
* **Actionable Feedback for Amazon:**
  > **Docs:** Add a "reading & subtitle-style text over full-screen art" pattern to the Fire TV design guidelines (safe text band, minimum size, auto-hiding overlays, select-to-pause), so book, education and karaoke apps converge on the same remote behaviour.

---

### 🟢 Friction 12: Focus Lost After a Dialog Closes (Compose on Fire TV)
* **Category:** UI/UX & Compose Framework  
* **Severity:** **LOW** (Friction Rating: 2.5 / 5)  
* **Context:** Found only on the real device: after dismissing our parental gate dialog, no element had focus and the D-pad did nothing until the user pressed BACK.
* **Expected:** Focus returns to the element that opened the dialog, as it does with Android Views.
* **Actual:** Compose does not restore focus when a `Dialog` window closes; the same happened at the end of horizontal rails, where focus jumped to an unrelated button.
* **Workaround:** A small focus memory (last focused key + `FocusRequester` map) re-requested after dialogs close and after BACK navigation, plus key handlers that block focus from escaping the end of a rail.
* **Actionable Feedback for Amazon:**
  > **Tooling:** This is exactly what Feature Request #2 (D-Pad Focus Visualizer) would catch before certification; a "focus lost" warning in the ADB/MCP tooling would help every Compose TV app.

---

## 3. Delights: What Worked Exceptionally Well

1. **`@amazon-devices/amazon-devices-buildertools-mcp` & Skills:**
   - The `amazon-devices-scalable-ui-audit` skill was a revelation. It instantly identified that Fire TV operates under **Scaling Regime B** (constant ~960x540 dp canvas where density changes proportionally to resolution), preventing us from wasting days authoring useless `values-sw720dp/` qualifiers.
2. **Fire TV Stick 4K Hardware Performance:**
   - The hardware decoders on the Fire TV Stick 4K handled high-resolution AI watercolor illustrations, Base64 strings, and dynamic TTS narration effortlessly with zero thermal throttling.
3. **ADB over Wi-Fi:**
   - Pairing the Fire TV Stick via `adb connect 192.168.x.x:5555` allowed near-instant incremental APK installs (`assembleDebug` in <1m 20s) without needing physical USB cables hooked up to the TV.
4. **Amazon Polly as a drop-in narrator:**
   - The neural voices `Lucia` (es-ES) and `Ruth` (en-US) sounded natural for bedtime stories on the first try. Each page synthesized in **0.6–1.0 s** as a ~60–70 KB MP3 that our Fire TV player already supported, and SSML `<prosody rate>` + `<break>` mapped cleanly onto our "soft / normal / lively" narration styles.
5. **Bedrock Converse API with images:**
   - Sending the child's drawing as raw bytes in a `ContentBlock` (no S3 upload, no base64 juggling) made the vision migration a small change. Nova 2 Lite analyzed a drawing in **~2 s** and wrote a full 5-scene story in **~5–6 s**, with token usage reported in every response.
6. **Amazon Polly speech marks → word-synced subtitles:**
   - One extra `SynthesizeSpeech` call per page (`OutputFormat: json`, `SpeechMarkTypes: ["word"]`) returns the start time of every word. Aligned to the on-screen words, they drive the TV subtitles and the gold word highlight with no audio analysis on the device — the feature that makes the reader feel like a film. Polly also narrates each chapter's drawing challenge, so children who cannot read yet still hear their mission.

---

## 4. Feature Requests for the Amazon Developer Tooling & AWS Teams

| Priority | Feature Request | Target Audience | Impact |
|:---:|---|---|---|
| **#1** | **Amazon Fire TV Mobile Companion SDK** | Any TV app requiring mobile text, payment, or photo input | Eliminates 80% of cross-device pairing boilerplate for 3P developers. |
| **#2** | **Fire TV D-Pad Focus Visualizer (ADB CLI)** | Jetpack Compose / Flutter / React Native developers | Live overlay highlighting focusable elements, focus traps, and inaccessible UI nodes on the TV screen. |
| **#3** | **Pre-flight Appstore Certification Linter** | All Fire TV developers prior to submission | Scans for BACK loops, missing Leanback launchers, and unshielded cleartext traffic before submission. |
| **#4** | **Structured outputs for Amazon Nova 2 Lite** (Important) | Any Bedrock app that parses model output | Removes the need for forced-tool workarounds and makes JSON pipelines reliable in every language. |
| **#5** | **GA Amazon image model + lifecycle banners on model docs** (Critical for us) | Creative / family apps on Bedrock | A supported Nova Canvas successor with reference-image consistency would let apps like WonderLoop run 100% on AWS. |
| **#6** | **Fire TV guidance for Compose Multiplatform + reading-style UIs** | KMP teams; book, education and karaoke apps | One reference for TV focus components from shared code and for text over full-screen art (Frictions 10–11). |

---

## 5. Conclusion

Building WonderLoop TV for the **Amazon Developer Hackathon 2026** proved that the Fire TV Stick 4K is an extraordinarily capable platform for interactive, family-centric AI experiences. By addressing the friction points documented above, Amazon can further solidify Fire OS as the most developer-friendly smart TV ecosystem in the industry.
