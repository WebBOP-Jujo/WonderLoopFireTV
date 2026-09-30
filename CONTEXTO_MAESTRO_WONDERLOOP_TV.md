# 📺 WONDERLOOP TV — DOCUMENTO MAESTRO DE CONTEXTO, ARQUITECTURA Y HOJA DE RUTA (AMAZON FIRE TV HACKATHON 2026)

> **Propósito de este documento:** Este archivo contiene **todo el contexto histórico, funcional, técnico y estratégico** del proyecto para que cualquier nueva sesión de IA o desarrollador que abra el workspace `C:\Users\jorge.arcicollar\Documents\WonderLoopTV` comprenda al 100% de dónde venimos, qué está ya construido y probado en el código actual, qué queremos construir exactamente para **Amazon Fire TV** y cómo debemos implementarlo paso a paso sin perder ni un detalle.

---

## 1. DE DÓNDE VENIMOS (ORIGEN DEL PROYECTO)

### 1.1. ¿Qué es WonderLoop?
**WonderLoop** nació como una aplicación móvil interactiva (desarrollada en **Kotlin Multiplatform + Compose Multiplatform** con un backend propio en **Node.js + TypeScript + Express**) pensada para familias con niños de **3 a 8 años**.

La premisa mágica de **WonderLoop** es:
1. **Del papel a la pantalla:** El niño dibuja en un papel un personaje inventado (un dragón, un robot, una princesa espacial, un monstruo simpático...).
2. **Visión + Transformación Artística Consistente:** El sistema analiza el dibujo con IA multimodal (`visionAdapter`) extrayendo una ficha técnica visual muy precisa (`visual_description`, colores exactos, accesorios, silueta) y genera un avatar oficial (`imageAdapter`) en el estilo artístico elegido por la familia:
   - *Animación 3D (estilo Pixar)*
   - *Acuarela de Cuento*
   - *Cuento Clásico Ilustrado*
   - *Anime Suave (estilo Ghibli)*
3. **Sagas Interactivas por Capítulos con Retos de Dibujo ("El Bucle Mágico"):**
   - El personaje creado protagoniza un cuento dividido en **5 escenas ilustradas (`SagaPage.ScenePage`)**.
   - Al terminar la 5.ª escena de cada capítulo, aparece la **Misión / Reto de Dibujo (`SagaPage.ChallengePromptPage`)**: la historia se detiene en un momento emocionante y le pide al niño que **dibuje en papel un nuevo elemento, objeto mágico o compañero** para ayudar al héroe a superar el reto y desbloquear el siguiente episodio.
   - Cuando el niño escanea ese nuevo dibujo, el backend genera el **Capítulo 2 (y sucesivos)** manteniendo toda la memoria de la saga (`previousEpisodes`, `sagaHero`, `characterLearnedTraits`) e integrando el nuevo dibujo en la trama. En el lector, los capítulos quedan unidos en un mismo libro continuo con una página especial de celebración **`SagaPage.RetoSuperadoPage`** (con confeti y medalla) entre capítulo y capítulo.
4. **Narración por Voz IA + Efectos de Sonido Procedurales:**
   - Cada escena viene acompañada de locución generada en el backend por TTS (`audioAdapter` con `msedge-tts` y voces neuronales de cuentacuentos en los 5 idiomas, más fallback a Gemini TTS y a Android `TextToSpeech` local).
   - El lector incluye modo de auto-narración continua al pasar de página (`autoNarrateSession`) y efectos de sonido procedurales sintetizados por `AudioTrack` PCM en [`PlatformAudioService.android.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/androidMain/kotlin/org/wonderloop/app/platform/PlatformAudioService.android.kt): sonido de pasar página (`PAGE_TURN`), fanfarria de celebración en los retos (`CELEBRATION`), campana mágica (`MAGIC_CHIME`) y clic (`POP`). *(Nota: no hay música ambiental de fondo ni resaltado karaoke palabra por palabra).*
5. **Zona de Padres y Multi-Perfil Infantil:**
   - Soporte para múltiples perfiles de niños (ej. *Lucas*, *Laura*), cada uno con su edad, sus propios personajes, su propia biblioteca de cuentos, sus valores educativos prioritarios (Empatía, Valentía, Curiosidad, Paciencia, Trabajo en equipo...) y **su propio idioma de cuento independiente** (`preferredLanguage`: Español, Inglés, Francés, Alemán, Portugués).

### 1.2. ¿Por qué pivotamos de móvil a Amazon Fire TV (`amazonappdev2026.devpost.com`)?
- Inicialmente preparamos la versión móvil para el *RevenueCat Shipaton 2026*, pero las cuentas personales de Google Play Console creadas después de noviembre de 2023 exigen una prueba cerrada obligatoria de **12 testers durante 14 días continuos** antes de desbloquear el paso a Producción, y Shipaton exigía enlace público en tienda en producción.
- Descubrimos el hackathon oficial de Amazon: **[Build, Ship, Shape: Amazon Developer Hackathon](https://amazonappdev2026.devpost.com/)**:
  - **Fecha límite de entrega:** **Viernes 23 de octubre de 2026 a las 12:00 PM PT (21:00h hora española)**.
  - **SIN bloqueo de tiendas:** Las reglas oficiales establecen explícitamente que **NO es necesario publicar la app en la tienda pública**; se evalúa mediante el **repositorio de GitHub** y un **vídeo de demostración (< 3 minutos)** corriendo en un dispositivo Fire TV real o en el **emulador de Android TV / Fire TV**.
  - **Soporte nativo para nuestro stack (Kotlin + Jetpack Compose):** Las reglas del track **Fire TV** aceptan explícitamente apps nativas Android en **Kotlin + Jetpack Compose** corriendo sobre **Fire OS**.
  - **Encaje perfecto con los criterios del jurado de Amazon:**
    En las reglas oficiales del Track Fire TV (*Sección 6 - Judging Criteria*), Amazon prioriza explícitamente:
    - **Family Entertainment** (entretenimiento familiar en el salón)
    - **Multi-Modal UX** (combinar segunda pantalla/voz + mando D-Pad + experiencia visual en pantalla grande)
    - **Computer Vision** (visión por computador aplicada al dibujo del niño)
    - **AI-Enhanced Viewing** (generación de historias e ilustraciones adaptadas al hogar)

---

## 2. A QUÉ QUEREMOS TRANSICIONAR: LA VISIÓN DE `WONDERLOOP TV`

Queremos transformar el código base actual (que ya es funcional al 100% en móvil y está copiado en este directorio `C:\Users\jorge.arcicollar\Documents\WonderLoopTV`) en **WonderLoop TV**, una aplicación nativa para **Amazon Fire TV (pantallas de +42 pulgadas, formato panorámico `16:9`)**.

### 2.1. El Flujo Estrella: Escaneo de Dibujo vía Código QR (Móvil $\rightarrow$ Backend $\rightarrow$ Fire TV en Tiempo Real)
Como las televisiones con Fire TV **no tienen cámara** para hacerle una foto al dibujo en papel del niño:
1. La familia está sentada en el sofá frente a la TV (+42") manejando la app con el **mando a distancia del Fire TV (D-Pad)**.
2. Cuando pulsan en **"Crear Nuevo Héroe / Escanear Dibujo"** (o cuando llegan al **Reto de Dibujo al final de un capítulo** para desbloquear el siguiente capítulo de la saga) en la TV:
   - En vez de abrir una cámara local (que en TV no existe), la TV solicita al backend crear una **sesión de emparejamiento QR** (`POST /v1/tv-session`).
   - La pantalla del Fire TV muestra una **pantalla mágica panorámica (`QrScanScreen`)** con:
     - Un **Código QR grande y nítido** en pantalla.
     - Instrucciones claras en 3 pasos: *"1. Escanea el código QR con la cámara de tu móvil · 2. Haz una foto al dibujo en papel o elígela de tu galería · 3. ¡Mira cómo cobra vida en la tele!"*.
     - Un indicador en tiempo real: *"Esperando conexión desde tu móvil..."*.
     - Un botón alternativo navegable con el mando (*"Probar con dibujo de ejemplo"*) para que tanto nosotros como los jueces del hackathon podamos probar el flujo también con un solo clic desde el mando en el emulador si no se quiere usar un teléfono.
3. **En el teléfono móvil (al escanear el QR):**
   - El QR apunta a una URL servida directamente por nuestro propio backend (ej. `http://<IP_LAN_O_CLOUD>:3000/upload/<sessionId>`).
   - Al abrirse en el navegador del móvil (sin necesidad de instalar nada en el móvil), aparece una **Web App móvil con el diseño mágico de WonderLoop** donde el padre/niño tiene dos botones grandes:
     - 📸 **Hacer foto al dibujo** (abre la cámara trasera del móvil vía `<input type="file" accept="image/*" capture="environment">`).
     - 🖼️ **Elegir de la galería** (abre la galería de fotos del móvil vía `<input type="file" accept="image/*">`).
   - Al seleccionar la foto, la web móvil la redimensiona/comprime en un `<canvas>` (máx. 1024px JPEG base64) y la envía a `POST /v1/tv-session/:sessionId/drawing`.
   - En cuanto empieza a subir, el móvil avisa a `POST /v1/tv-session/:sessionId/status` (`uploading`) y al terminar queda en `ready` con el `imageBase64`.
4. **Reacción instantánea en la TV (Magia en el salón):**
   - La app del Fire TV, que está consultando el estado de la sesión cada `1.2s` (`GET /v1/tv-session/:sessionId`), detecta automáticamente que el dibujo ha llegado (`status === "ready"`).
   - **Sin que el usuario tenga que tocar ningún botón del mando**, la TV muestra el dibujo recién recibido con un efecto de chispas mágicas (*"¡Dibujo recibido! Transformando tu héroe..."*) y pasa automáticamente a `MagicLoadingScreen` $\rightarrow$ `HeroConfirmScreen` $\rightarrow$ `StoryGeneratingScreen` $\rightarrow$ `StoryReaderScreen` en pantalla grande.

### 2.2. Experiencia de Usuario "10-Foot UI" para Pantallas de +42 Pulgadas (`16:9` + Mando D-Pad)
Una app de Fire TV se ve a **2,5–3 metros de distancia** desde el sofá y se controla exclusivamente con el **mando a distancia (Cruceta D-Pad: Arriba, Abajo, Izquierda, Derecha, Botón Central `OK/Select` y Botón `Atrás`)**:
1. **Formato Panorámico `16:9` (Landscape fijo):**
   - Eliminamos el diseño vertical de móvil (`9:16`) y la barra inferior (`BottomBar`).
   - Usamos una **barra de navegación superior estilo Fire TV** (*Inicio*, *Mis Héroes*, *Biblioteca de Cuentos*, *Cambiar Niño*, *Zona Padres*) y distribución en **2 columnas o raíles horizontales panorámicos**.
2. **Navegación 100% por D-Pad con Foco Visual Inconfundible:**
   - En TV es crítico saber siempre qué elemento está seleccionado con el mando.
   - Todos los botones, tarjetas de personajes, cuentos y estilos artísticos deben usar un sistema de foco (`Modifier.onFocusChanged` + `Modifier.focusable`) que al estar enfocado muestre:
     - **Borde brillante de 3dp** en color Dorado Mágico (`#FFD54F`) o Coral/Lavanda.
     - **Ligero zoom suave (`scale(1.04f)`)** y elevación/resplandor.
     - **Foco inicial automático (`FocusRequester`)** al entrar en cada pantalla para que el usuario nunca tenga que pulsar varias veces para "encontrar" el foco.
3. **Reproductor de Cuentos Cinematográfico en TV (`StoryReaderScreen`):**
   - `StoryReaderScreen.kt` **ya cuenta con una base `isLandscape` a doble página** (ilustración a la izquierda y texto + botón de narración a la derecha) que potenciaremos para pantallas de +42":
     - **Navegación directa con el mando del Fire TV:** Pulsar **Flecha Derecha (`Key.DirectionRight`)** pasa a la siguiente página/escena del cuento, **Flecha Izquierda (`Key.DirectionLeft`)** vuelve a la página anterior, y el botón **OK / Play** activa o pausa la narración por voz.
     - **Tipografía grande de lectura desde el sofá** (`22sp`–`26sp` con interlineado amplio) para que padres e hijos puedan leer juntos cómodamente a 3 metros de distancia.
     - **Página de Reto de Dibujo (`ChallengePromptPage`) adaptada a TV:** Al llegar al final del capítulo, el botón **"📸 Escanear dibujo del reto"** abrirá directamente el portal QR (`QrScanScreen`) en el televisor para que el niño escanee su nuevo dibujo con el móvil y desbloquee el siguiente capítulo en vivo.
4. **Simplificación para el Hackathon de Amazon (Sin Paywalls que bloqueen el flujo):**
   - En este hackathon de Amazon **no se evalúa RevenueCat ni la monetización por suscripción**, sino la **experiencia en Fire TV, la UX multimodal, la visión por computador y la integración con AWS**.
   - Por tanto, en `WonderLoop TV` debemos dejar **acceso ilimitado / desbloqueado por defecto** (sin saltar al Paywall ni bloquear la creación de cuentos ni estilos artísticos), para que tanto el vídeo de demo como cualquier prueba de los jueces de Amazon fluya sin fricciones.

---

## 3. QUÉ HEMOS HECHO HASTA AHORA Y CÓMO ESTÁ CONSTRUIDO EL CÓDIGO ACTUAL

El código que ya reside en `C:\Users\jorge.arcicollar\Documents\WonderLoopTV` es la versión completa y funcional que construimos y depuramos. Aquí tienes el mapa exacto de cómo funciona cada pieza:

### 3.1. Arquitectura del Backend (`backend/`)
- **Punto de entrada:** [`backend/src/server.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/server.ts)
  - Servidor Express en TypeScript (`npm run dev` ejecuta `tsx watch src/server.ts` en el puerto `3000`).
  - Límite de body JSON configurado a `15mb` (`express.json({ limit: '15mb' })`) para recibir fotos de dibujos y devolver imágenes/audio en Base64.
  - Endpoints actuales:
    - `GET /health` $\rightarrow$ devuelve `{ status: 'ok', timestamp }`.
    - `POST /v1/stories/analyze` $\rightarrow$ recibe `{ imageBase64, mimeType, artStyle, language }`, ejecuta primero `visionAdapter.analyzeDrawing` y acto seguido `imageAdapter.generateCharacterAvatar`, devolviendo el `VisionAnalysisResult` con la descripción del dibujo y el `avatarImageUrl` (`data:image/jpeg;base64,...`).
    - `POST /v1/stories/generate` $\rightarrow$ recibe el análisis del dibujo, nombre del personaje, edad, estilo artístico, idioma, valores educativos, historial de capítulos previos (`previousEpisodes`) y `chapterNumber`, y ejecuta el pipeline completo de cuento + 5 ilustraciones + 5 pistas de narración TTS.
- **Orquestador de Historias:** [`backend/src/services/story.service.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/services/story.service.ts)
  - **REGLA DE ORO DE CONSISTENCIA VISUAL (NO CAMBIAR):** En `analyzeDrawing` y `generateCompleteStory`, el paso 1 (`visionAdapter.analyzeDrawing`) y el paso 2 (`imageAdapter.generateCharacterAvatar`) se ejecutan **SIEMPRE DE FORMA SECUENCIAL**. ¿Por qué? Porque `visionAdapter` extrae la descripción anatómica, colores exactos y accesorios del dibujo infantil, y se la pasa a `imageAdapter` y a `storyAdapter`. Si se paralelizaran, el avatar y las 5 escenas tendrían personajes distintos.
  - Una vez generado el guion de las 5 escenas (`storyAdapter.generateStory`), las **5 ilustraciones de escena + las 5 locuciones de audio TTS sí se generan en paralelo** (`Promise.all`) para máxima velocidad.
- **Adaptadores de IA (`backend/src/adapters/`):**
  - [`vision.adapter.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/adapters/vision.adapter.ts): Usa Gemini (`gemini-2.5-flash`) para analizar el dibujo infantil y devolver JSON estructurado (`character_type`, `colors`, `notable_features`, `personality_vibe`, `visual_description`, `suggested_name`, `confidence`).
  - [`story.adapter.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/adapters/story.adapter.ts): Usa Gemini (`gemini-2.5-flash`) con *system prompt* especializado en literatura infantil (3–8 años), continuidad de sagas (`previousEpisodes`), evolución del personaje (`new_character_trait`), reto de dibujo al final de cada capítulo (`drawing_challenge`) y soporte nativo para 5 idiomas (`es`, `en`, `fr`, `de`, `pt`).
  - [`image.adapter.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/adapters/image.adapter.ts): Genera el retrato del héroe y las 5 ilustraciones de las escenas manteniendo el estilo artístico (`3d`, `watercolor`, `storybook`, `anime`) y la descripción visual del personaje, con *fallback* automático multi-proveedor.
  - [`audio.adapter.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/adapters/audio.adapter.ts): Genera la narración hablada en MP3/WAV (`data:audio/mp3;base64,...`) con voces expresivas de cuentacuentos en el idioma correspondiente (`es-ES-ElviraNeural`, `en-US-AnaNeural`, `fr-FR-DeniseNeural`, `de-DE-AmalaNeural`, `pt-BR-FranciscaNeural` vía `msedge-tts`, con fallback a Gemini TTS).

### 3.2. Arquitectura de la App Kotlin Multiplatform (`composeApp/`)
- **Navegación y Estado Global:** [`composeApp/src/commonMain/kotlin/org/wonderloop/app/App.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/App.kt) y [`Screen.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/navigation/Screen.kt).
  - Gestiona la pila de navegación (`backStack`), el perfil infantil activo, el estado de conexión con el backend (`api.isOnline`), el observador de ciclo de vida (`AppLifecycleObserver`) para reconexión automática al volver a primer plano, y la sincronización automática de idioma por perfil.
- **Cliente HTTP y Reconexión Rápida:** [`composeApp/src/commonMain/kotlin/org/wonderloop/app/data/WonderLoopApi.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/data/WonderLoopApi.kt)
  - Configurado con `connectTimeoutMillis = 3_000` (falla en solo 3 segundos si el backend está apagado, lanzando `BackendOfflineException` para no hacer esperar al usuario) y `requestTimeoutMillis = 180_000` / `socketTimeoutMillis = 180_000` (para permitir la generación completa de IA cuando sí hay conexión).
  - Auto-reconecta llamando a `checkHealth()` antes de cada operación si `isOnline != true`.
- **Modelos y Persistencia Local Sin Límites:**
  - [`Models.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/data/Models.kt): Define `Story`, `StoryScene`, `SavedCharacter`, `ChildProfile` (incluyendo `preferredLanguage`), `ParentalSettings` y DTOs del backend.
  - [`StoryRepository.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/data/StoryRepository.kt) y [`ProfileRepository.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/data/ProfileRepository.kt): Persisten cuentos, personajes y perfiles infantiles aislados por cada niño (`stories_<profileId>`, `characters_<profileId>`).
  - [`PlatformStorage.android.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/androidMain/kotlin/org/wonderloop/app/platform/PlatformStorage.android.kt): Almacenamiento híbrido que guarda automáticamente las claves pesadas (`stories_*`, `characters_*` con imágenes y audios en Base64) en archivos individuales dentro de `context.filesDir/wonderloop_store/` para no saturar nunca la memoria de `SharedPreferences`.
- **Narración por Voz y Efectos de Sonido Procedurales:**
  - [`PlatformAudioService.android.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/androidMain/kotlin/org/wonderloop/app/platform/PlatformAudioService.android.kt): Reproduce la locución MP3/WAV de cada escena (`MediaPlayer`) generada por el backend (con fallback a `TextToSpeech` nativo de Android en el idioma activo) y sintetiza en tiempo real efectos de sonido procedurales (`AudioTrack` PCM: `POP`, `MAGIC_CHIME`, `PAGE_TURN`, `CELEBRATION`).
- **Internacionalización Completa (5 idiomas):**
  - [`AppStrings.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/theme/AppStrings.kt): Todos los textos de la interfaz traducidos a Español (`es`), Inglés (`en`), Francés (`fr`), Alemán (`de`) y Portugués (`pt`), cambiando dinámicamente según el perfil del niño activo.

---

## 4. QUÉ QUEREMOS CONSTRUIR Y CÓMO HACERLO PASO A PASO (HOJA DE RUTA TÉCNICA)

Cuando abras la nueva conversación en este proyecto (`C:\Users\jorge.arcicollar\Documents\WonderLoopTV`), este es el plan de implementación exacto en **5 Fases**:

### FASE 1: Configuración de Proyecto para Fire TV / Android TV (16:9 Landscape + Leanback)
1. **Actualizar [`composeApp/src/androidMain/AndroidManifest.xml`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/androidMain/AndroidManifest.xml):**
   - Declarar compatibilidad con TV sin exigir hardware móvil:
     ```xml
     <uses-feature android:name="android.software.leanback" android:required="false" />
     <uses-feature android:name="android.hardware.touchscreen" android:required="false" />
     <uses-feature android:name="android.hardware.camera" android:required="false" />
     ```
   - Configurar `MainActivity` en orientación horizontal fija de TV (`android:screenOrientation="sensorLandscape"`) y añadir el intent-filter `android.intent.category.LEANBACK_LAUNCHER` junto con `LAUNCHER` para que aparezca en el carrusel principal de apps de Fire TV / Android TV.
   - Añadir un drawable `ic_tv_banner.xml` (`320x180dp`) referenciado en `android:banner="@drawable/ic_tv_banner"`.
2. **Actualizar [`backend/src/adapters/image.adapter.ts`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/backend/src/adapters/image.adapter.ts):**
   - Ajustar las dimensiones/aspect ratio de las ilustraciones de escena de los cuentos a **formato panorámico `16:9` (`1280x720`)** para que luzcan cinematográficas en televisores de +42 pulgadas (manteniendo los avatares de personaje en `1:1`).

---

### FASE 2: Puente QR Móvil $\leftrightarrow$ Fire TV en el Backend y Web Móvil de Subida
1. **Nuevos endpoints en el Backend (`backend/src/routes/tv.routes.ts` registrado en `server.ts`):**
   - **`POST /v1/tv-session`**:
     - Crea una sesión en memoria con un `sessionId` corto de 6 caracteres (ej. `WL-4829`) + UUID, estado inicial `status: 'waiting'`, y detecta automáticamente la IP local de red LAN de la máquina (`os.networkInterfaces()`, ej. `192.168.1.X`) o el host público si está desplegado.
     - Devuelve `{ sessionId, uploadUrl: "http://<LAN_IP>:3000/upload/<sessionId>", expiresInSeconds: 900 }`.
   - **`GET /upload/:sessionId`**:
     - Sirve una **página web HTML5 + CSS + JS autocontenida** con la estética mágica nocturna de WonderLoop (fondo `#0F0A2C`, estrellas, botones dorados/violetas).
     - Al abrirla desde el móvil tras escanear el QR:
       1. Notifica inmediatamente al backend (`POST /v1/tv-session/:sessionId/connected`) para que en la TV aparezca *"📱 ¡Móvil conectado! Seleccionando foto del dibujo..."*.
       2. Ofrece dos botones grandes: **"📸 Hacer foto al dibujo"** y **"🖼️ Elegir de la galería"**.
       3. Redimensiona la foto en el propio navegador del móvil mediante `<canvas>` a máx. `1024px` JPEG (`0.85` quality) para que la subida sea instantánea (< 0.5s).
       4. Envía el Base64 a `POST /v1/tv-session/:sessionId/drawing` y muestra en el móvil: *"✨ ¡Dibujo enviado a la TV! Mira la pantalla de tu salón"*.
   - **`GET /v1/tv-session/:sessionId`**:
     - Consultado cada `1.2s` por la pantalla del Fire TV.
     - Devuelve `{ sessionId, status: 'waiting' | 'connected' | 'uploading' | 'ready', imageBase64?: string, mimeType?: string }`.
2. **Métodos en [`WonderLoopApi.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/data/WonderLoopApi.kt):**
   - Añadir `createTvSession(): TvSessionResponse` y `pollTvSession(sessionId: String): TvSessionStatusResponse`.

---

### FASE 3: Pantalla de Escaneo QR en la TV (`QrScanScreen.kt`) con Generador QR Nativo en Compose
1. **Crear un generador de Código QR en Kotlin/Compose (`QrCodeCanvas.kt` o vía ZXing en `androidMain`):**
   - Dibujar el código QR nítido sobre un tarjeta blanca con borde dorado en la pantalla del televisor.
2. **Rediseñar el paso de escaneo para Fire TV (`CameraScanScreen` $\rightarrow$ `QrScanScreen`):**
   - En lugar de abrir la cámara del dispositivo, muestra un layout panorámico `16:9` dividido en dos mitades:
     - **Mitad Izquierda (El Portal QR):** Código QR grande + URL corta + indicador de estado en vivo (*"Esperando que escanees el QR con el móvil..."* $\rightarrow$ *"📱 ¡Móvil conectado! Elige la foto..."* $\rightarrow$ *"✨ ¡Dibujo recibido!"*).
     - **Mitad Derecha (Cómo funciona + Alternativa con Mando):**
       - Ilustración/pasos visuales de cómo escanear con el móvil.
       - Selector de dibujos de demostración o galería local navegable con el **mando D-Pad** (*"¿Sin móvil a mano? Elige un dibujo de prueba con el mando"*), lo cual además garantiza que cualquier juez de Amazon pueda probar el flujo en el emulador en 1 clic si no usa su móvil.

---

### FASE 4: Adaptación Completa de la Interfaz a TV +42" (`16:9` 10-Foot UI + D-Pad Focus)
1. **Sistema de Foco para Mando a Distancia (`composeApp/src/commonMain/kotlin/org/wonderloop/app/ui/components/TvFocusModifiers.kt`):**
   - Crear un modificador estándar `Modifier.tvFocusable(...)` que añada:
     - Borde brillante de `3.dp` (`WonderGold` `#FFD54F`) cuando `isFocused == true`.
     - Animación de escala suave (`1.0f` $\rightarrow$ `1.04f`) cuando recibe el foco del D-Pad.
     - Soporte directo para clic con el botón central del mando (`Key.DirectionCenter` / `Key.Enter`).
2. **Layout Principal de TV en [`App.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/App.kt):**
   - Sustituir la `BottomNavigationBar` de móvil por una **Barra Superior de TV (`TvTopNavBar`)** siempre accesible con la flecha Arriba del mando:
     - Logo `WonderLoop TV` + Indicador de estado Online
     - Pestañas: `🏠 Salón Principal` | `🦸 Mis Héroes` | `📚 Mis Cuentos`
     - Selector rápido de Perfil Infantil (`Lucas`, `Laura`...) + Idioma + `🔒 Zona Padres`.
3. **Pantallas adaptadas a `16:9` (1920x1080 / 960x540dp):**
   - **`HomeScreen.kt` (Diseño TV):**
     - **Hero Banner panorámico superior:** Muestra el último héroe o saga activa a gran tamaño con dos botones principales enormes para el mando: **"📱 Escanear Nuevo Dibujo (QR)"** y **"▶️ Continuar Saga"**.
     - **Raíles horizontales inferiores (`LazyRow` navegables con Izquierda/Derecha):** Raíl de *Tus Héroes Mágicos* y Raíl de *Últimos Episodios en Familia*.
   - **`HeroConfirmScreen.kt` (Diseño TV 2 Columnas):**
     - Izquierda: Comparativa grande *Dibujo original enviado desde el móvil* $\rightarrow$ *Héroe transformado por IA*.
     - Derecha: Nombre del héroe, selector de estilo artístico (4 tarjetas grandes navegables con D-Pad), selector de idioma del cuento y botón **"✨ ¡Crear Episodio en la TV!"**.
   - **`StoryReaderScreen.kt` (Modo Cine Familiar 16:9):**
     - Aprovechar el layout `isLandscape` a doble página que ya tiene `StoryReaderScreen.kt` y optimizarlo para TV (+42"):
       - **Izquierda:** Ilustración de la escena a gran tamaño con badge de progreso (`✨ 1 / 5`).
       - **Derecha:** Tarjeta de lectura con **tipografía grande (`22sp`–`26sp`)**, botón de narración por voz (`AudioNarrationPill`) y paso de página directo con las **Flechas Izquierda / Derecha del mando del Fire TV**.
       - **Final del Capítulo (`ChallengePromptPage`):** Muestra la Misión de Dibujo a doble columna con el botón **"📱 Escanear nuevo dibujo con QR para el Capítulo siguiente"** enfocado automáticamente para el mando.
   - **Desbloqueo de Cuotas para Modo Hackathon:**
     - En [`SubscriptionManager.kt`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/composeApp/src/commonMain/kotlin/org/wonderloop/app/data/SubscriptionManager.kt), activar modo ilimitado para que todos los estilos artísticos, perfiles y capítulos estén desbloqueados sin mostrar el Paywall durante la evaluación del jurado de Amazon.

---

### FASE 5: Herramientas Oficiales de Amazon, Mini-Challenge `AWS Builder` ($5,000 + $5,000 AWS) y Bonus del +10% (`Friction Log`)

Según las reglas oficiales ([`amazonappdev2026.devpost.com/rules`](https://amazonappdev2026.devpost.com/rules) y [`/resources`](https://amazonappdev2026.devpost.com/resources)), así es exactamente cómo funcionan los requisitos técnicos, las herramientas de Amazon y los bonus de puntuación:

#### 5.1. ¿Qué SDKs o Frameworks exige el Track Principal de `Fire TV`?
- **Regla oficial literal:** *"Fire TV is the exception: Any framework or language is acceptable (React Native, Android/Kotlin/Java, web technologies, etc.). The requirement is that the project runs on Fire OS or Vega OS. The demo video must show the project running on an actual Fire TV device or the Fire TV/Vega simulator."*
- **Repositorio de referencia oficial de Amazon para Kotlin + Jetpack Compose:** [`https://github.com/AmazonAppDev/hello-world-fire-tv`](https://github.com/AmazonAppDev/hello-world-fire-tv).
- **Herramienta oficial de desarrollo recomendada por Amazon (`Amazon Devices Builder Tools`):**
  - Paquete oficial de Amazon que instala un **MCP Server + Agent Skills + Steering Docs** para Fire OS y Vega OS:
    ```bash
    npx -y @amazon-devices/amazon-devices-buildertools-mcp@latest init-context
    ```
  - Instalar y usar este paquete en el proyecto nos permite tanto aprovechar las guías de Fire OS como documentar nuestra experiencia real con él en el **Product Feedback** y en el **Friction Log** (por ejemplo, detallando qué tal funciona su soporte para Fire OS con Kotlin/Compose frente a Vega OS).

#### 5.2. ¿Qué exige exactamente el Mini-Challenge `AWS Builder` ($5,000 en metálico + $5,000 en créditos AWS)?
- **Regla oficial literal:** *"Any primary track project that incorporates AWS services (i.e. Amazon Bedrock, AgentCore, Strands SDK, Kiro Crew, SageMaker, etc.) with documented integrations. Kiro Crew qualifies on its own as a development tool used during the hackathon — a submission does not need to also call a runtime AWS service (Bedrock, AgentCore, SageMaker, Strands SDK) to count for this mini-challenge."*
- **Créditos gratuitos para construir:** Se pueden solicitar **$150 en AWS Promotional Credits** rellenando el formulario oficial antes del 21 de octubre: `https://forms.gle/5hyhr1u6x3fuV2aW7`.
- **Qué SDKs de AWS integraremos en nuestro Backend (`backend/package.json`):**
  1. **`@aws-sdk/client-bedrock-runtime` (Amazon Bedrock):**
     - Integrado en nuestros adaptadores (`vision.adapter.ts`, `story.adapter.ts`, `image.adapter.ts`) para poder usar modelos alojados en **Amazon Bedrock** (como **Amazon Nova Pro / Lite** o **Claude 3.5 Sonnet** para visión + generación de cuentos, y **Amazon Nova Canvas / Titan Image Generator** para ilustraciones) junto con nuestro proveedor actual, controlado por variable de entorno.
  2. **`@aws-sdk/client-polly` (Amazon Polly Neural TTS):**
     - Integrado en `audio.adapter.ts` para generar la narración hablada de los cuentos con las voces neuronales infantiles/cuentacuentos de Amazon (`Lucia` / `Mia` en español, `Joanna` / `Kevin` en inglés, `Lea` en francés, `Vicki` en alemán, `Camila` en portugués).
  - *(Nota de las FAQs oficiales: El jurado evalúa entre el **9 y el 20 de noviembre de 2026**, por lo que no hace falta tener recursos de AWS de pago encendidos permanentemente antes de esa fecha; las llamadas bajo demanda a Bedrock/Polly consumen apenas céntimos de los $150 de crédito promocional).*

#### 5.3. Cómo conseguir el Bonus de hasta +10% en la Nota del Jurado (`Product Feedback` + `Feature Requests` + `Friction Log`)
En el formulario de entrega de Devpost hay **3 bloques de feedback** que van directos a los equipos de ingeniería de Amazon y otorgan **hasta un +10% extra en la puntuación del jurado**:
1. **Product Feedback (Obligatorio para cada herramienta, API o SDK de Amazon usado):**
   - Para cada herramienta usada (ej. *Fire OS / Android TV Leanback*, *Amazon Devices Builder Tools MCP (`@amazon-devices/amazon-devices-buildertools-mcp`)*, *Amazon Bedrock*, *Amazon Polly*), responder a estas 5 preguntas:
     1. *Which developer tools, APIs and SDKs did you use and for what?*
     2. *What worked well? (setup, documentation, testing, performance, reliability)*
     3. *What needs work? (system errors, docs sections, missing features, compatibility issues, tool limitations, required workarounds)*
     4. *How was your onboarding experience (getting from zero to hello world)?*
     5. *Would you build with these devices and services again? Yes/No and why?*
2. **Feature Requests (Opcional — suma puntos):**
   - Por cada sugerencia de mejora para Fire TV / ADBT / Bedrock:
     - **Descripción** de la funcionalidad solicitada.
     - **Por qué importa** para nuestro proyecto (`WonderLoop TV`).
     - **Prioridad:** `Critical` / `Important` / `Nice-to-have`.
3. **Friction Log Entries (Opcional — otorga hasta un +10% de bonus directo en la evaluación):**
   - Mientras construimos `WonderLoop TV`, iremos anotando en un archivo `FRICTION_LOG_AND_FEEDBACK.md` cada obstáculo o fricción técnica real con este formato exacto exigido por las bases:
     - **Specific task attempted** (Tarea específica intentada)
     - **Steps taken** (Pasos seguidos)
     - **Expected vs. actual result** (Resultado esperado vs. resultado real)
     - **Severity rating** (Gravedad: Critical / High / Medium / Low)
     - **Workaround used** (Solución alternativa que implementamos)
     - **Actionable suggestion** (Sugerencia concreta de mejora para el equipo de Amazon)

---

## 5. CÓMO ARRANCAR EN LA NUEVA CONVERSACIÓN

Cuando abras una nueva conversación de Antigravity directamente en el workspace `C:\Users\jorge.arcicollar\Documents\WonderLoopTV`, simplemente dile al asistente:

> *"Lee el archivo `CONTEXTO_MAESTRO_WONDERLOOP_TV.md` en la raíz del proyecto para ponerte al día de todo y empecemos a implementar la transición a Fire TV paso a paso comenzando por la Fase 1 y Fase 2."*
