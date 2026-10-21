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
3. **Arquitectura de Rutas y Seguridad Robusta en el Backend:**
   - Se respeta la convención real del proyecto (`/api/...`):
     - `/api/story/health`, `/api/story/analyze-drawing`, `/api/story/generate` (protegidos con `X-WonderLoop-Key` para la app Fire TV).
     - `/api/tv-session` (con alias `/v1/tv-session` por compatibilidad) para crear y consultar sesiones de emparejamiento QR desde el Fire TV.
     - `/upload/:sessionId` en la raíz para que el código QR sea corto, limpio y fácil de escanear por cualquier cámara móvil. La subida desde el móvil se valida mediante el token temporal `sessionId` (expira a los 15 minutos), sin exponer jamás `X-WonderLoop-Key` en el navegador público.
   - **Hardening y Protección DoS/Fuga:** Rate limiting separado para generación IA, polling de sesiones TV y subida de dibujos (`express-rate-limit`), validación estricta de mimetypes en Multer (`jpeg`, `png`, `webp` hasta 10MB), tope de tamaño Base64, decodificación con `sharp` limitada a 25 megapíxeles (`limitInputPixels`, protege frente a "bombas de descompresión"), cabeceras de seguridad `helmet()` y ofuscación de excepciones internas.
   - **Secreto de la app sin valores por defecto:** el servidor no arranca sin `APP_SECRET_KEY` (≥ 32 caracteres, comparación en tiempo constante). La app Fire TV recibe la misma clave en compilación desde `local.properties` (`wonderloop.appKey` → `BuildConfig` → `platformAppSecretKey()`); nunca está en el código. `dotenv` se carga como primer import (`import 'dotenv/config'`) para que ningún módulo lea `process.env` antes de tiempo.
   - **Proxy y despliegue:** `TRUST_PROXY` (0 en LAN, 1 detrás de proxy/balanceador) para que el rate limiting vea la IP real del cliente. Como todos los dispositivos de una casa comparten IP pública, el polling de sesiones TV admite 1500 peticiones cada 5 min (TV + móvil a la vez), la creación de sesiones va aparte con 40 cada 5 min, y cada 429 se registra en los logs (`🚦 [RateLimit:...]`). Con `PUBLIC_BASE_URL`, ni el banner ni `/`, `/health` o las respuestas de sesión exponen la IP interna; los logs no incluyen el nombre del niño. Las sesiones QR viven en memoria → el backend debe correr en **una sola instancia**.
   - **Superficie mínima:** eliminadas las rutas `/api/revenuecat` y `/api/subscription` (webhook sin autenticar y cupones canjeables desde fuera); los cupones se validan en local en la app. `npm audit` → 0 vulnerabilidades (eliminados `ts-node` / `ts-node-dev`).
   - **Privacidad Infantil (COPPA) y Gestión de Memoria:** el buffer del dibujo (`session.imageBase64`) se purga de la RAM 60 s después del primer poll del Fire TV en estado `ready`, y las sesiones caducan a los 15 minutos, impidiendo la persistencia de dibujos infantiles en el servidor.
4. **Proporciones de Imagen (`16:9` en Escenas vs. `1:1` en Retratos):**
   - **Retrato del Héroe / Compañero (`generateCharacterPortrait`):** Formato cuadrado `1:1` (`1024x1024`).
   - **Ilustraciones de las 5 Escenas (`generateSceneIllustration`):** Formato panorámico cinematográfico **`16:9` (`1280x720`)** optimizado para televisores de salón (+42 pulgadas).
5. **Estrategia de Proveedores de IA y Dispositivo Objetivo:**
   - **Dispositivo principal de prueba:** **Amazon Fire TV Stick 4K** real (IP en la LAN para ADB: `192.168.0.38`).
   - **Backend de producción:** `https://13-217-171-0.sslip.io` en **Amazon EC2** (`t3.small`, Amazon Linux 2023, `us-east-1`, IP elástica `13.217.171.0`), con Docker Compose (Caddy para HTTPS automático + backend en una sola réplica) y el rol IAM `wonderloop-backend-role`, así que en el servidor no hay claves de AWS. La app lo usa por defecto (`wonderloop.backendUrl` en `local.properties` permite apuntar a un backend LAN para desarrollo).
   - **Proveedores actuales (Fase 5 implementada):**
     - **Visión del dibujo y texto del cuento:** **Amazon Nova 2 Lite** en **Amazon Bedrock** (`us.amazon.nova-2-lite-v1:0`, región `us-east-1`, API Converse con salida estructurada por *tool use* y esquema JSON explícito). Fallback automático a `gemini-3.5-flash-lite`.
     - **Narración:** **Amazon Polly** con voces neurales **Lucia** (`es-ES`) y **Ruth** (`en-US`), elegidas tras escuchar muestras; el estilo de voz (`soft`/`normal`/`lively`) se mapea a velocidad SSML. Fallback por página a `gemini-3.8-flash-lite-tts`.
     - **Ilustraciones (retrato 1:1 y escenas 16:9):** se mantienen en `gemini-3.1-flash-lite-image`. Motivo: **Amazon Nova Canvas llegó a EOL el 30/09/2026**, **Titan Image Generator G1 v2** está pasado de su EOL (30/06/2026) y los modelos Legacy no están disponibles para clientes nuevos, y **Nova 2 Omni** sigue en preview cerrada. Además, Titan solo ofrece consistencia de personaje mediante fine-tuning, incompatible con un dibujo nuevo por niño.
     - Interruptores: `LLM_PROVIDER=bedrock|gemini`, `TTS_PROVIDER=polly|gemini`. Permisos IAM mínimos en [`backend/aws/wonderloop-iam-policy.json`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/aws/wonderloop-iam-policy.json).
   - Objetivo: elegibilidad para el premio **AWS Builder ($5,000 + $5,000 AWS Credits)** con fiabilidad total gracias al fallback.
6. **Estrategia de Monetización y Cumplimiento Fire TV (Sin Google Play Services):**
   - Fire OS carece de Google Play Services; invocar Google Billing produce excepciones inmediatas.
   - Para el Hackathon se activa cuota ilimitada (`HACKATHON_UNLIMITED`). Para producción en Amazon Appstore, la monetización se basa en el **Amazon In-App Purchasing (IAP) SDK** (suscripciones familiares y monedas/créditos de cuentos), habiéndose purgado y aislado las librerías móviles de Google Play Billing y OneSignal del target de TV.
   - Eliminación estricta de permisos irrelevantes para televisores en el manifiesto (`android.permission.CAMERA` y `POST_NOTIFICATIONS`), cumpliendo al 100% las directrices de certificación Leanback de Amazon.

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
  - [`llm.client.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/adapters/llm.client.ts): Cliente LLM común "prompt (+ imagen) → objeto JSON": `BedrockNovaLlmClient` (Nova 2 Lite, Converse + tool use forzado con esquema, timeout por petición, circuit breaker de 5 min ante errores de credenciales), `GeminiLlmClient` y `FallbackLlmClient` (cae a Gemini ante error, timeout o JSON que no valida). Instancia compartida entre visión y cuento.
  - [`story-catalog.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/adapters/story-catalog.ts): Catálogo narrativo concreto: 12 mundos reconocibles con afinidad por especie, 15 problemas cotidianos, 8 tonos y ~50 objetos **dibujables** para los retos (barco, paraguas, escalera, perro…). Selección aleatoria con `crypto.randomInt` (antes era un hash del nombre del héroe: mismo nombre → mismo cuento).
  - [`vision.adapter.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/adapters/vision.adapter.ts) (`LlmVisionAdapter`): Análisis multimodal del dibujo con el esquema `CHARACTER_PROFILE_SCHEMA`. Devuelve los textos en el idioma del perfil (`language` en `/analyze-drawing`), un nombre original (lista de nombres sobreusados prohibida) y `visualDescriptionEn` para que las ilustraciones sean consistentes en cualquier idioma.
  - [`story.adapter.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/adapters/story.adapter.ts) (`LlmStoryAdapter`): Prompt en inglés con hoja de estilo por idioma, espina narrativa de 5 páginas (planteamiento → intento fallido → idea → solución con un rasgo del dibujo → final tranquilo + reto), frases por página según la edad, memoria de saga (`chapterSummary` / `previousChapterSummaries`, `worldId`) y revisión de calidad automática con un reintento guiado. El reto se elige entre 3 candidatos dibujables (`subjectId` en el esquema). Sin `minLength` en el esquema: con Nova multiplica la latencia por 3,5. Medición: [`src/eval-stories.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/eval-stories.ts) (retos concretos del 38 % al 100 %, páginas en rango de palabras del 50 % al 100 %, 0 clichés, ~6 s por cuento).
  - [`image.adapter.ts`] (prompts): sin marcas registradas, estilo propio del adaptador, `visualDescriptionEn` + paleta HEX + referencias del héroe y del compañero, plano y luz distintos por página (mañana → noche estrellada) y arte a sangre sin márgenes.
  - [`image.adapter.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/adapters/image.adapter.ts): Generación con `gemini-3.1-flash-lite-image` (`1024x1024` para retratos `1:1` y `1280x720` para escenas `16:9` en `watercolor` o `3d_animation`).
  - [`polly-audio.adapter.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/adapters/polly-audio.adapter.ts): Narración con Amazon Polly (MP3 `data:audio/mpeg`, SSML con pausas entre párrafos, circuit breaker).
  - [`audio.adapter.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/adapters/audio.adapter.ts): Gemini TTS (`gemini-3.8-flash-lite-tts`), `FallbackAudioAdapter` (pide al fallback solo las páginas que faltan) y la factoría `createAudioAdapter()`.

### 4.2. App Kotlin Multiplatform para Fire TV (`composeApp/`)
- **Navegación y Estado Global:** [`App.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/App.kt) y [`Screen.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/navigation/Screen.kt).
- **Cliente HTTP y Sesión TV:** [`WonderLoopApi.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/data/WonderLoopApi.kt) y [`Models.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/data/Models.kt).
- **URL del backend inyectada en compilación:** `wonderloop.backendUrl` (o la variable `WONDERLOOP_BACKEND_URL`) → `BuildConfig.WONDERLOOP_BACKEND_URL` → `platformDefaultBackendUrl()`; por defecto `https://13-217-171-0.sslip.io`. En el diálogo de servidor de la pantalla QR, una IP se normaliza a `http://<ip>:3000` y un nombre de dominio a `https://<dominio>`.
- **Clave de la app inyectada en compilación:** `wonderloop.appKey` en `local.properties` (o la variable `WONDERLOOP_APP_KEY`) → `BuildConfig.WONDERLOOP_APP_KEY` → [`PlatformConfig.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/platform/PlatformConfig.kt) / `PlatformConfig.android.kt`. Debe coincidir con `APP_SECRET_KEY` del backend.
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

### FASE 4.5: Auditoría Profunda de TV, Refuerzo de Seguridad, Estabilidad de Foco D-Pad y Despliegue en Hardware Real [COMPLETADA]
1. **Refuerzo de Seguridad, Privacidad Infantil y Gestión de Memoria en Backend:**
   - Rate limiting con `express-rate-limit` en `/api/tv-session` (`sessionLimiter`: 30 req/min) y en la subida de dibujos (`uploadLimiter`: 10 req/min) para mitigar abusos y ataques DoS.
   - Destrucción de imagen en memoria: una vez el Fire TV descarga el dibujo infantil (`status === "ready"`), el backend libera inmediatamente `session.imageBase64 = undefined`, evitando consumo de RAM en el servidor y protegiendo la privacidad infantil (COPPA).
   - `fileFilter` estricto en Multer (`image/jpeg`, `image/png`, `image/webp`), cota de 10MB en subida multipart y cota máxima de 15MB en decodificación Base64 antes de invocar `sharp`.
   - Protección con `helmet()`, CORS restringido a la app y ofuscación de trazas internas (`error.message` nunca se expone en respuestas HTTP hacia el exterior).
2. **Perfeccionamiento del Foco D-Pad y Navegación TV:**
   - **Consumo atómico de KeyDown y KeyUp:** En [`TvFocusModifiers.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/components/TvFocusModifiers.kt), se consumen ambos eventos de teclado para `DPAD_CENTER` y `ENTER`, eliminando pulsaciones dobles, desincronizaciones de foco y parpadeos en el lector.
   - **Retención de foco en Zona de Padres:** Resuelta la pérdida de foco al conmutar el interruptor de efectos de sonido (SFX) en [`ParentsZoneScreen.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/ParentsZoneScreen.kt) mediante `sfxRowRequester` y gestión reactiva del estado.
   - **Unificación de Safe Zone:** Margen estandarizado a `48.dp` horizontal y `24.dp` vertical en todas las pantallas para garantizar cumplimiento del >5% de overscan en televisores (resolución lógica `960x540dp`).
   - **Estabilidad de foco en arranque frío:** Retardo de inicialización unificado a `120-150ms` (`LaunchedEffect`) para asegurar que el `FocusRequester` se ancle correctamente tras la composición inicial.
   - **Navegación controlada en el lector:** Corrección del evento `DPAD_UP` en [`StoryReaderScreen.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/StoryReaderScreen.kt) para evitar saltos erráticos fuera del libro.
3. **Rediseño 10-Foot de la Pantalla de Inicio (`HomeScreen.kt`):**
   - Eliminación de botones redundantes `+ Nuevo cuento` y `+ Nuevo amigo`.
   - Nuevas tarjetas de acción a pantalla completa: **"Ver Biblioteca"** (acceso a todos los cuentos con conteo exacto en singular/plural) y **"Ver todos"** (galería completa de personajes creados).
   - Corrección del solapamiento y corte de texto en la tarjeta destacada *"Seguir leyendo"* ajustando el balance de pesos (`1.4f` vs `0.85f`), limitando a `maxLines = 1` y usando una cabecera compacta en pastilla única.
4. **Resiliencia en el Polling QR (`QrScanScreen.kt`):**
   - Detección de estado caducado (`status === "expired"` o TTL >15 min): la app regenera automáticamente una nueva sesión y código QR sin bloquear al usuario en pantalla.
   - Diálogo de configuración de IP del servidor y botón de reintento completamente accesibles y enfocables con D-Pad.
5. **Verificación en Dispositivo Real (Amazon Fire TV Stick 4K):**
   - Instalación y validación en vivo sobre hardware real conectado por Wi-Fi (`adb connect 192.168.0.38:5555`).
   - Capturas de pantalla de verificación obtenidas y validadas (`home_verified2.png`, `focus_card1-4.png`, `sfx_on.png`, `sfx_off.png`), confirmando legibilidad a 3 metros, contraste accesible y respuesta instantánea del mando físico.

### FASE 4.7: UI/UX Fire TV profesional + Compose for TV [COMPLETADA]
1. **`androidx.tv:tv-material` (migración híbrida):** primitivas `TvClickableSurface` y `KeepScreenOn` en [`ui/tv/TvPrimitives.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/tv/TvPrimitives.kt) (`expect`) con implementación Android sobre `androidx.tv.material3.Surface` (foco, OK, pulsación larga, escala, borde dorado y glow nativos). Se usan en el lector, el inicio y sus rails. La migración completa no se hizo a propósito: la UI vive en `commonMain` (KMP), tv-material es solo Android y reescribir ~250 componentes Material3 a 2 días de la entrega era un riesgo innecesario; el resto de pantallas sigue con `tvFocusable`.
2. **Lector en modo cine** ([`StoryReaderScreen.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/StoryReaderScreen.kt)): ilustración 16:9 a sangre con Ken Burns, panel de lectura translúcido, ◀ ▶ pasan página desde cualquier punto (antes ▶ no funcionaba sobre "Siguiente"), narración automática, BACK sale sin diálogo y **resaltado palabra a palabra** con `wordTimingsMs` (speech marks de Polly alineadas por el backend con `alignWordTimings`; `PlatformAudioService.currentPositionMs()`).
3. **Inicio con rails** ([`HomeScreen.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/HomeScreen.kt)): tarjeta "crear" + "seguir leyendo" con la ilustración real, rails de portadas 16:9 y amigos, idea del día bilingüe (antes fija en español), restauración del foco al volver (`HomeFocusMemory`) y tras cerrar diálogos (`focusRestoreTick`), sin fugas de foco al final de los rails.
4. **Biblioteca y amigos:** zona segura de 48 dp, un único foco por tarjeta, foco inicial, borrado con ☰ (Menú) + puerta parental (se retiraron las papeleras del área infantil), sin el contador "999 cuentos", textos pensados para mando ("pulsa OK") en lugar de "toca".
5. **Subtítulos de cine (sustituye al panel de lectura, que tapaba ~55 % de la ilustración):** `subtitleChunks()` divide la página en bloques de 1-2 frases (≤100 caracteres, cortes en pausas, la acotación del diálogo va con su frase) sincronizados con la palabra narrada (speech marks de Polly; sin ellas, estimación de 430 ms/palabra) y con la palabra actual en dorado sobre un degradado inferior. Controles en raíl vertical a la derecha (▲ ▼ los recorren, así ◀ ▶ quedan siempre para pasar página) que se ocultan a los 3,5 s mientras narra; OK con los controles ocultos pausa (convención de reproductor Fire TV); sin voz, ◀ ▶ avanzan los subtítulos al ritmo del niño; **"Leer yo"** muestra el texto completo para que lea un adulto y BACK lo cierra antes de salir; al terminar la narración "Siguiente" late. El prompt de imagen coloca a los personajes en los dos tercios superiores y deja el cuarto inferior tranquilo.
6. **Páginas de fin de capítulo rediseñadas:** fondo con la ilustración del capítulo lavada en crema, polaroid del héroe/amigo, reto a 25 sp, pasos "Dibújalo › Escanéalo › Capítulo N", botones `TvClickableSurface` de 60 dp. **El reto se narra** (el backend sintetiza con Polly una pista extra `CHALLENGE_AUDIO_PAGE = 0` → `creativeChallenge.audioBase64`, guardada como archivo en el dispositivo; cuentos antiguos usan el TTS del sistema en español). "Reto superado" muestra el amigo dibujado y la portada del siguiente capítulo.
7. **Otros:** `KeepScreenOn` en lector, QR, análisis y generación; `<queries>` de `TTS_SERVICE`; píldora de servidor "Servidor en la nube · Cambiar"; mínimo 12 sp en todas las pantallas; los 4 "dibujos de prueba" eran JPEG en blanco de 12×12 px y ahora son dibujos infantiles reales (448×448).
6. **Verificado en el Fire TV Stick 4K** con capturas por ADB: inicio, rails, lector (paso de página con ▶, narración y resaltado sincronizado), biblioteca, puerta parental con ☰, amigos y restauración de foco.

### FASE 4.6: Auditoría de Seguridad del Backend [COMPLETADA]
1. Eliminado el secreto público por defecto del backend, la app, `.env.example` y el script de despliegue; nuevo secreto aleatorio en `backend/.env` y `local.properties` (ambos fuera de git).
2. `TRUST_PROXY`, `limitInputPixels`, limitador específico para la subida de dibujos, eliminación de las rutas de RevenueCat, caché de cuotas acotada y `npm audit` sin vulnerabilidades.
3. Verificado: arranque sin secreto rechazado, 401 con la clave antigua, 404 en las rutas eliminadas, PNG de 16.000×16.000 px rechazado; la app Android compila (`BUILD SUCCESSFUL`).

### FASE 5: Integración Principal con AWS (`Bedrock` + `Polly`) con Fallback a Gemini [EN CURSO]
1. **[HECHO]** Amazon Polly (`@aws-sdk/client-polly`) como narrador principal con fallback por página a Gemini TTS. Verificado en real: 0,6–1,0 s por página, MP3 de ~60–70 KB.
2. **[HECHO]** Amazon Bedrock (`@aws-sdk/client-bedrock-runtime`) con Nova 2 Lite para visión y cuento, con fallback a Gemini. Verificado en real: visión ~2 s, cuento de 5 escenas ~5–6 s, 6/6 llamadas correctas en `es` y `en` tras pasar a salida estructurada por tool use (en modo texto, Nova devolvía JSON inválido en inglés).
3. **[HECHO]** Friction log actualizado con las fricciones de AWS (Frictions 6–9 en [`FRICTION_LOG_AMAZON_HACKATHON.md`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/FRICTION_LOG_AMAZON_HACKATHON.md)) y README con la sección "AWS Integration (Implemented)".
4. **[HECHO]** Despliegue público con HTTPS en AWS: EC2 `t3.small` en `us-east-1` + Caddy + rol IAM con la política mínima, una sola instancia. Scripts en [`backend/deploy/`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/deploy) (`deploy-ec2.ps1` desde el PC + `bootstrap.sh` en el servidor). Verificado desde fuera: HTTPS y redirección 308, 401 sin clave, sesión QR con URL HTTPS, y un cuento completo en inglés en 12,1 s con **Bedrock** (visión 1,8 s, texto 5,2 s), 5 ilustraciones de Gemini (5,3 s) y **Polly** (5/5 páginas) usando las credenciales del rol de la instancia.
5. **[HECHO]** La app usa el backend de AWS por defecto (`BuildConfig.WONDERLOOP_BACKEND_URL`); APK de depuración instalado por ADB en el Fire TV `192.168.0.38`.
6. **[PENDIENTE]** Prueba E2E manual en el Fire TV contra el backend de AWS (QR desde un móvil → héroe → cuento ilustrado → narración con Polly) y APK firmado en GitHub Releases para los jueces.
7. **[OPCIONAL]** Bedrock Guardrails sobre el texto generado (argumento de seguridad infantil).
8. **[PENDIENTE]** Arreglos rápidos del README (ajustar las afirmaciones de privacidad y telemetría al código real), quitar "Pixar / Disney" de los prompts del backend, vídeo de menos de 3 minutos y Product Feedback.
