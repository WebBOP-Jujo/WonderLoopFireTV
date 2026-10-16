# 📺 WONDERLOOP TV — DOCUMENTO MAESTRO DE CONTEXTO, ARQUITECTURA Y HOJA DE RUTA (AMAZON FIRE TV HACKATHON 2026)

> **Propósito de este documento:** Este archivo contiene **todo el contexto histórico, funcional, técnico y estratégico** del proyecto para que cualquier sesión de IA o desarrollador que abra el workspace `C:\Users\jorge.arcicollar\Documents\WonderLoopTV` comprenda al 100% de dónde venimos, qué está ya construido y probado en el código actual, qué decisiones de arquitectura y diseño están cerradas y cómo implementamos **WonderLoop TV** paso a paso para **Amazon Fire TV (Fire TV Stick 4K)**.

---

## 1. DE DÓNDE VENIMOS (ORIGEN DEL PROYECTO)

### 1.1. ¿Qué es WonderLoop?
**WonderLoop** nació como una aplicación interactiva (desarrollada en **Kotlin Multiplatform + Compose Multiplatform** con un backend propio en **Node.js + TypeScript + Express**) pensada para familias con niños de **3 a 10 años**.

La premisa mágica de **WonderLoop** es:
1. **Del papel a la pantalla:** El niño dibuja en un papel un personaje inventado (un dragón, un robot, un perrito aventurero, un monstruo simpático...).
2. **Visión + Transformación Artística Consistente:** El sistema analiza el dibujo con IA multimodal (`visionAdapter`) extrayendo una ficha técnica visual muy precisa (`CharacterProfile`: `name`, `species`, `primaryColors`, `distinctiveFeatures`, `personality`) y genera un retrato oficial (`imageAdapter`) en uno de los **2 estilos artísticos oficiales**:
   - **`watercolor` (*Acuarela de Cuento*)**
   - **`3d_animation` (*Animación 3D estilo Pixar / Clay*)**
3. **Sagas Interactivas por Capítulos con Retos de Dibujo ("El Bucle Mágico"):**
   - El personaje creado protagoniza un cuento dividido en **5 escenas ilustradas (`SagaPage.ScenePage`)**.
   - Al terminar la 5.ª escena de cada capítulo, aparece la **Misión / Reto de Dibujo (`SagaPage.ChallengePromptPage`)**: la historia se detiene en un momento emocionante y le pide al niño que **dibuje en papel un nuevo elemento, vehículo o compañero** para ayudar al héroe a superar el reto y desbloquear el siguiente episodio.
   - Cuando el niño escanea ese nuevo dibujo, el backend genera el **Capítulo 2 (y sucesivos)** manteniendo toda la memoria de la saga (`continuation`, `sagaHero`, `previousCompanionNames`) e integrando tanto al héroe fundador como al nuevo compañero en las ilustraciones y en la trama. En el lector, los capítulos quedan unidos en un mismo libro continuo con una página especial de celebración **`SagaPage.RetoSuperadoPage`** (con confeti y medalla) entre capítulo y capítulo.
4. **Narración por Voz IA + Efectos de Sonido Procedurales:**
   - Cada escena viene acompañada de locución generada en el backend por TTS (`audioAdapter`), más fallback a Android `TextToSpeech` local en [`PlatformAudioService.android.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/androidMain/kotlin/org/wonderloop/app/platform/PlatformAudioService.android.kt).
   - El lector incluye auto-narración al pasar de página (`autoNarrateSession`) y efectos de sonido procedurales sintetizados por `AudioTrack` PCM (`PAGE_TURN`, `CELEBRATION`, `MAGIC_CHIME`, `POP`).
5. **Zona de Padres y Multi-Perfil Infantil (Bilingüe Español / Inglés):**
   - Soporte para múltiples perfiles de niños (ej. *Lucas*, *Laura*), cada uno con su edad, sus propios personajes, su propia biblioteca de cuentos, su estilo artístico preferido (`watercolor` | `3d_animation`) y **su propio idioma de cuento independiente** en los **2 idiomas soportados**:
     - **`es` (*Español de España*)**
     - **`en` (*English*)**

### 1.2. ¿Por qué pivotamos de móvil a Amazon Fire TV (`amazonappdev2026.devpost.com`)?
- Inicialmente preparamos la versión móvil para el *RevenueCat Shipaton 2026*, pero las cuentas personales de Google Play Console creadas después de noviembre de 2023 exigen una prueba cerrada obligatoria de **12 testers durante 14 días continuos** antes de desbloquear el paso a Producción.
- Descubrimos el hackathon oficial de Amazon: **[Build, Ship, Shape: Amazon Developer Hackathon](https://amazonappdev2026.devpost.com/)**:
  - **Fecha límite de entrega:** **Viernes 23 de octubre de 2026 a las 12:00 PM PT (21:00h hora española)**.
  - **SIN bloqueo de tiendas:** Las reglas oficiales establecen explícitamente que **NO es necesario publicar la app en la tienda pública**; se evalúa mediante el **repositorio de GitHub** y un **vídeo de demostración (< 3 minutos)** corriendo en un dispositivo Fire TV real (**Amazon Fire TV Stick 4K**) o simulador.
  - **Soporte nativo para nuestro stack (Kotlin + Jetpack Compose):** Las reglas del track **Fire TV** aceptan explícitamente apps nativas Android en **Kotlin + Jetpack Compose** corriendo sobre **Fire OS**.
  - **Encaje perfecto con los criterios del jurado de Amazon:**
    En las reglas oficiales del Track Fire TV (*Sección 6 - Judging Criteria*), Amazon prioriza explícitamente:
    - **Family Entertainment** (entretenimiento familiar compartido en el salón)
    - **Multi-Modal UX** (combinar segunda pantalla móvil vía QR + mando D-Pad + narración por voz en pantalla grande)
    - **Computer Vision** (visión por computador aplicada al dibujo físico del niño)
    - **AI-Enhanced Viewing** (generación de historias e ilustraciones panorámicas `16:9` adaptadas al televisor)

---

## 2. DECISIONES ARQUITECTÓNICAS Y DE DISEÑO CERRADAS PARA `WONDERLOOP TV`

1. **Estilos Artísticos e Idiomas Oficiales:**
   - **2 Estilos Artísticos:** `watercolor` (*Acuarela*) y `3d_animation` (*Animación 3D*).
   - **2 Idiomas:** `es` (*Español de España*) y `en` (*English*).
2. **Coherencia Visual Total ("Warm Storybook Palette"):**
   - Tanto la app de **Fire TV** como la **Web App móvil del portal QR (`/upload/:sessionId`)** comparten la paleta cálida de cuento clásico definida en [`Theme.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/theme/Theme.kt):
     - Fondo Crema Cálido: `#FFFBF5` (`WonderCream`) y `#FFFDF9` (`WonderPaper`)
     - Coral Cálido: `#D9384A` (`WonderCoral`)
     - Dorado Mágico (foco D-Pad y acentos): `#FFD166` (`WonderGold`)
     - Verde Menta: `#0E7F64` (`WonderMint`)
     - Texto Ciruela Oscuro de alto contraste: `#2B253A` (`WonderTextPrimary`)
     - Tipografías redondeadas y legibles a 3 metros: `Fredoka` (títulos) y `Nunito` (lectura).
3. **Arquitectura de Rutas y Seguridad en el Backend:**
   - Se respeta la convención real del proyecto (`/api/...`):
     - `/api/story/health`, `/api/story/analyze-drawing`, `/api/story/generate` (protegidos con `X-WonderLoop-Key` para la app Fire TV).
     - `/api/tv-session` (con alias `/v1/tv-session` por compatibilidad) para crear y consultar sesiones de emparejamiento QR desde el Fire TV.
     - `/upload/:sessionId` en la raíz para que el código QR sea corto, limpio y fácil de escanear por cualquier cámara móvil. La subida desde el móvil se valida mediante el token temporal `sessionId` (expira a los 15 minutos), sin exponer jamás `X-WonderLoop-Key` en el navegador público.
4. **Proporciones de Imagen (`16:9` en Escenas vs. `1:1` en Retratos):**
   - **Retrato del Héroe / Compañero (`generateCharacterPortrait`):** Formato cuadrado `1:1` (`1024x1024`).
   - **Ilustraciones de las 5 Escenas (`generateSceneIllustration`):** Formato panorámico cinematográfico **`16:9` (`1280x720`)** optimizado para televisores de salón (+42 pulgadas).
5. **Estrategia de Proveedores de IA y Dispositivo Objetivo:**
   - **Dispositivo principal de prueba:** **Amazon Fire TV Stick 4K** real conectado a la misma red Wi-Fi LAN que el backend y el teléfono móvil.
   - **Fases 1 a 4:** Mantenemos los modelos actuales de Gemini (`gemini-3.5-flash-lite`, `gemini-3.1-flash-lite-image`, `gemini-3.8-flash-lite-tts`) para validar toda la experiencia Fire TV y el puente QR.
   - **Fase 5 (AWS Builder):** Añadiremos **Amazon Bedrock + Amazon Polly** como proveedores principales con **fallback automático y transparente a Gemini**, garantizando elegibilidad para el premio **Best AWS Builder ($5,000 + $5,000 AWS Credits)** y fiabilidad del 100%.

---

## 3. FLUJO FUNCIONAL Y EXPERIENCIA DE USUARIO EN FIRE TV

### 3.1. El Flujo Estrella: Escaneo de Dibujo vía Código QR (Móvil $\leftrightarrow$ Backend $\leftrightarrow$ Fire TV Stick 4K)
Como los televisores con Fire TV **no tienen cámara**:
1. La familia está sentada en el sofá frente a la TV manejando la app con el **mando a distancia del Fire TV (D-Pad)**.
2. Cuando pulsan en **"Crear Nuevo Héroe / Escanear Dibujo"** (o cuando llegan al **Reto de Dibujo al final de un capítulo**):
   - La TV solicita al backend crear una **sesión de emparejamiento QR** (`POST /api/tv-session`).
   - La pantalla del Fire TV muestra una **pantalla panorámica (`QrScanScreen`)** con:
     - Un **Código QR grande y nítido** generado nativamente en Compose.
     - Instrucciones claras en 3 pasos y código de sesión visible.
     - Indicador en tiempo real del estado del móvil (*Esperando escaneo* $\rightarrow$ *📱 Móvil conectado* $\rightarrow$ *⬆️ Subiendo dibujo* $\rightarrow$ *✨ ¡Dibujo recibido!*).
     - Un selector alternativo navegable con el mando D-Pad (*"¿Sin móvil a mano? Elige un dibujo de prueba con el mando"*) para pruebas rápidas o evaluación en emulador.
3. **En el teléfono móvil (al escanear el QR):**
   - El QR abre `http://<LAN_IP>:3000/upload/<sessionId>` en el navegador del móvil (sin instalar nada).
   - Aparece una **Web App móvil con la estética cálida de cuento de WonderLoop (`#FFFBF5`, `#D9384A`, `#FFD166`, `#0E7F64`)**, bilingüe (`es`/`en`), con dos botones grandes:
     - 📸 **Hacer foto al dibujo** (`capture="environment"`)
     - 🖼️ **Elegir de la galería**
   - Al elegir la foto, el navegador móvil la comprime en `<canvas>` (máx. `1024px` JPEG `0.85`) y la envía al backend (`POST /api/tv-session/:sessionId/drawing`).
4. **Reacción instantánea en el Fire TV Stick 4K:**
   - El Fire TV detecta `status === "ready"` en su polling (`GET /api/tv-session/:sessionId`), recibe el `imageBase64` automáticamente y lanza el análisis de visión y creación del héroe/capítulo sin tocar ningún botón adicional.

### 3.2. Directrices Oficiales de Fire TV ("10-Foot UI", Safe Zone y Fire OS)
Siguiendo la documentación oficial de Amazon Fire TV:
- **Resolución lógica y Safe Zone (Overscan 5%):** En Fire TV 1080p (`1920x1080px` a `320dpi` xhdpi) equivale a **`960x540dp`**. Se respeta un margen seguro (*Overscan Safe Zone*) para que ningún botón ni texto quede cortado en los bordes del televisor.
- **Sin dependencia de Google Play Services:** Fire OS no incluye Google Play Services. Todos los servicios están protegidos con bloques `try/catch` seguros y el modo Hackathon desbloquea cuotas ilimitadas sin depender de Google Play Billing.
- **Navegación 100% con Mando Fire TV (D-Pad):** Cruceta direccional (`DPAD_UP`, `DPAD_DOWN`, `DPAD_LEFT`, `DPAD_RIGHT`), selección (`DPAD_CENTER` / `ENTER`), botón `BACK` y botón `PLAY_PAUSE`.

---

## 4. ARQUITECTURA TÉCNICA DEL PROYECTO

### 4.1. Backend (`backend/`)
- **Punto de entrada:** [`backend/src/server.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/server.ts)
  - Rutas principales:
    - `GET /api/story/health` $\rightarrow$ Healthcheck + IP LAN detectada.
    - `POST /api/story/analyze-drawing` $\rightarrow$ Ejecuta secuencialmente `visionAdapter.analyzeDrawing` $\rightarrow$ `imageAdapter.generateCharacterPortrait`.
    - `POST /api/story/generate` $\rightarrow$ Genera el cuento de 5 escenas + 5 ilustraciones `16:9` + 5 locuciones TTS en paralelo.
    - `POST /api/tv-session`, `GET /api/tv-session/:sessionId`, `GET /upload/:sessionId` $\rightarrow$ Puente QR en tiempo real entre móvil y Fire TV.
- **Orquestador de Historias:** [`backend/src/services/story.service.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/services/story.service.ts)
  - **REGLA DE ORO DE CONSISTENCIA VISUAL (INMUTABLE):** `visionAdapter.analyzeDrawing` y `imageAdapter.generateCharacterPortrait` se ejecutan **SIEMPRE DE FORMA SECUENCIAL** para garantizar que el retrato y las 5 escenas mantengan la identidad visual exacta del dibujo del niño.
- **Adaptadores de IA (`backend/src/adapters/`):**
  - [`vision.adapter.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/adapters/vision.adapter.ts): Análisis multimodal con `gemini-3.5-flash-lite`.
  - [`story.adapter.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/adapters/story.adapter.ts): Generación narrativa en `es` y `en` con `gemini-3.5-flash-lite` y prompts visuales panorámicos `16:9`.
  - [`image.adapter.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/adapters/image.adapter.ts): Generación con `gemini-3.1-flash-lite-image` (`1024x1024` para retratos `1:1` y `1280x720` para escenas `16:9` en `watercolor` o `3d_animation`).
  - [`audio.adapter.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/adapters/audio.adapter.ts): Síntesis de voz con `gemini-3.8-flash-lite-tts` (`es` y `en`).

### 4.2. App Kotlin Multiplatform para Fire TV (`composeApp/`)
- **Navegación y Estado Global:** [`App.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/App.kt) y [`Screen.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/navigation/Screen.kt).
- **Cliente HTTP y Sesión TV:** [`WonderLoopApi.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/data/WonderLoopApi.kt) y [`Models.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/data/Models.kt).
- **Modo Hackathon Ilimitado:** [`SubscriptionManager.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/data/SubscriptionManager.kt) y [`quota.middleware.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/middleware/quota.middleware.ts) configurados con cuota desbloqueada para que ningún flujo en Fire TV se vea interrumpido por paywalls.

---

## 5. HOJA DE RUTA EN 5 FASES

### FASE 1: Configuración de Proyecto para Fire TV (16:9 Landscape + Leanback + Ilustraciones 16:9 + Cuota Ilimitada) [COMPLETADA]
1. Actualizar [`composeApp/src/androidMain/AndroidManifest.xml`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/androidMain/AndroidManifest.xml) con features `leanback`, `touchscreen=false`, `camera=false`, `sensorLandscape`, `LEANBACK_LAUNCHER` y `ic_tv_banner`.
2. Crear [`composeApp/src/androidMain/res/drawable/ic_tv_banner.xml`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/androidMain/res/drawable/ic_tv_banner.xml) (`320x180dp`) con la identidad cálida de WonderLoop TV.
3. Actualizar [`backend/src/adapters/image.adapter.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/adapters/image.adapter.ts) y [`backend/src/adapters/story.adapter.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/adapters/story.adapter.ts) para generar ilustraciones de escena en formato panorámico nativo `16:9` (`imageConfig: { aspectRatio: '16:9' }` + `1280x720` en sharp) manteniendo los retratos en `1:1` (`1024x1024`).
4. Desbloquear cuotas ilimitadas para el Hackathon en [`SubscriptionManager.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/data/SubscriptionManager.kt) y [`revenuecat.service.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/services/revenuecat.service.ts).

### FASE 2: Puente QR Móvil $\leftrightarrow$ Fire TV en el Backend y Cliente KMP [COMPLETADA]
1. Crear [`backend/src/routes/tv.routes.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/routes/tv.routes.ts) y registrarlo en [`backend/src/server.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/server.ts) (`/api/tv-session`, `/v1/tv-session`, `/upload/:sessionId`).
2. Detección automática de IP LAN (`os.networkInterfaces()`) para que el **Fire TV Stick 4K** y el móvil en la misma Wi-Fi se comuniquen sin configuración manual compleja (detectada `192.168.0.35`).
3. Web App móvil autocontenida en `GET /upload/:sessionId` con estética Warm Storybook (`#FFFBF5`, `#D9384A`, `#FFD166`, `#0E7F64`), bilingüe (`es`/`en`), compresión en `<canvas>` y envío instantáneo con rotación EXIF normalizada con `sharp`.
4. Añadir modelos y métodos `createTvSession()` y `pollTvSession()` en [`Models.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/data/Models.kt) y [`WonderLoopApi.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/data/WonderLoopApi.kt).

### FASE 3: Pantalla de Escaneo QR en la TV (`QrScanScreen`) con Generador QR Nativo [COMPLETADA]
1. Generador de código QR nativo en Compose/Android ([`QrCodeCanvas.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/components/QrCodeCanvas.kt) y [`QrCodeCanvas.android.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/androidMain/kotlin/org/wonderloop/app/ui/components/QrCodeCanvas.android.kt) con ZXing `core:3.5.3`).
2. Diseño panorámico `16:9` dividido en dos columnas en [`QrScanScreen.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/QrScanScreen.kt): Portal QR en vivo a la izquierda con polling reactivo cada 1.2s + Selector de los 2 estilos de arte (`watercolor` y `3d_animation`), galería de 4 dibujos de prueba precargados ([`DemoDrawings.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/DemoDrawings.kt)) para probar sin móvil con el mando, y modal D-Pad para configurar la IP del servidor.

### FASE 4: Adaptación Completa de la Interfaz a TV +42" (`16:9` 10-Foot UI + D-Pad Focus) [COMPLETADA]
1. Modificador `Modifier.tvFocusable(...)` en [`TvFocusModifiers.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/components/TvFocusModifiers.kt) con borde dorado `#FFD166`, escala `1.04x`–`1.08x` suave y respuesta a `DPAD_CENTER`, `ENTER` y `Click`.
2. Adaptación panorámica `16:9` con *Safe Zone* y navegación D-Pad en:
   - [`HomeScreen.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/HomeScreen.kt): Foco inicial automático en el botón CTA principal (`FocusRequester`), soporte TV en tarjeta "Continuar leyendo", carruseles "Mis cuentos" y "Mis amigos" y botones de perfil/padres.
   - [`HeroConfirmScreen.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/HeroConfirmScreen.kt): Layout a 2 columnas `16:9`, selectores de estilo (`watercolor` / `3d_animation`) e idioma (`es` / `en`), y botones de acción navegables con D-Pad.
   - [`StoryReaderScreen.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/StoryReaderScreen.kt): Paso de página horizontal con `DirectionRight` / `MediaFastForward` y `DirectionLeft` / `MediaRewind`, toggle de audio con `MediaPlayPause`, tipografía grande de lectura a 3 metros (`21.sp`), y soporte D-Pad completo en diálogo de salida, reto creativo y controles inferior/superior.
   - [`MyStoriesScreen.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/MyStoriesScreen.kt) y [`MyCharactersScreen.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/MyCharactersScreen.kt): Botones de vuelta y tarjetas de cuentos/amigos totalmente focusables con el mando Fire TV.
   - Integración completa en [`App.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/App.kt), sustituyendo la antigua cámara móvil por `QrScanScreen`.
3. Verificación de compilación: Android target compilado con éxito (`:composeApp:compileDebugKotlinAndroid` $\rightarrow$ `BUILD SUCCESSFUL`) y backend TypeScript (`npm run build` $\rightarrow$ exit code 0).

### FASE 5: Integración Principal con AWS (`Bedrock` + `Polly`) con Fallback a Gemini + `FRICTION_LOG_AND_FEEDBACK.md` [PENDIENTE]
1. Integrar `@aws-sdk/client-bedrock-runtime` y `@aws-sdk/client-polly` como proveedores primarios (cuando las credenciales AWS estén activas) y mantener Gemini como fallback automático.
2. Ejecutar `@amazon-devices/amazon-devices-buildertools-mcp` y documentar el **Product Feedback**, **Feature Requests** y **Friction Log** para obtener hasta el **+10% de bonus** en la evaluación del jurado.
