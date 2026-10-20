# 📺 Guía de Pruebas WonderLoop TV en Amazon Fire TV Stick 4K (y Móvil Companion QR)

**WonderLoop TV** está optimizado para ejecutarse en **Amazon Fire TV Stick 4K** con navegación 100% por mando a distancia (D-Pad) y escaneo de dibujos infantiles desde cualquier smartphone a través de un puente QR en red local Wi-Fi.

---

## ⚡ 1. Despliegue Rápido en Amazon Fire TV Stick 4K (Vía Wi-Fi ADB)

### Requisitos:
- **Dispositivo Fire TV Stick 4K** encendido y conectado a la misma red Wi-Fi que el PC (ej. IP `192.168.0.38`).
- En el Fire TV: **Opciones para desarrolladores** activadas (*Depuración ADB* y *Apps de fuentes desconocidas* en `ON`).
- Backend Node.js de WonderLoop corriendo en el PC en el puerto 3000 (`npm run dev`).

### 🚀 Comandos de Despliegue en 3 Pasos (PowerShell):
```powershell
# Paso 1: Conectar ADB por Wi-Fi al Fire TV Stick 4K
adb connect 192.168.0.38:5555

# Paso 2: Instalar o actualizar el APK de WonderLoop TV
adb -s 192.168.0.38:5555 install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk

# Paso 3: Arrancar la app en la pantalla del televisor
adb -s 192.168.0.38:5555 shell am start -n org.wonderloop.app/.MainActivity
```

### 🎮 Cómo Probar la Experiencia Completa de Salón:
1. **Navegación con Mando D-Pad:**
   - La pantalla de inicio enfoca automáticamente el botón principal *"Crear un nuevo cuento mágico"*.
   - Explora las tarjetas 10-foot *"Ver Biblioteca"* (colección de cuentos) y *"Ver todos"* (galería de amigos) usando la cruceta del mando Fire TV.
2. **Escaneo de Dibujo vía Código QR:**
   - Pulsa el botón central del mando en *"Crear un nuevo cuento"*.
   - Aparece la pantalla `QrScanScreen` con un código QR gigante y nítido.
   - Con cualquier teléfono móvil conectado a la misma Wi-Fi, abre la cámara y escanea el QR (`http://<IP_PC>:3000/upload/<sessionId>`).
   - Sube una foto de un dibujo en papel. El televisor reacciona automáticamente sin tocar nada más, generando el cuento de 5 escenas 16:9 ilustradas y narradas.
3. **¿Sin móvil a mano? (Modo Demo con Mando):**
   - En `QrScanScreen`, baja con el D-Pad a la sección *"¿Sin móvil a mano? Elige un dibujo de prueba con el mando"*.
   - Elige entre los 4 dibujos de prueba precargados (*Dragón Nilo*, *Robot Sparky*, *Perrito Toby*, *Monstruo Chomp*) pulsando el botón central del mando.

---

## 📋 Índice para Modo Real (Sin Mocks)
1. [Resumen de Arquitectura y Flujo de Datos](#1-resumen-de-arquitectura-y-flujo-de-datos)
2. [Obtención y Configuración de API Keys Reales](#2-obtención-y-configuración-de-api-keys-reales)
   - [A. Google Gemini IA (Visión y Narrativa)](#a-google-gemini-ia-visión-y-narrativa)
   - [B. RevenueCat (Monetización y Paywall)](#b-revenuecat-monetización-y-paywall)
   - [C. OneSignal (Notificaciones de Retención)](#c-onesignal-notificaciones-de-retención)
3. [Preparación del Dispositivo Móvil Android](#3-preparación-del-dispositivo-móvil-android)
4. [Puente de Red USB (adb reverse)](#4-puente-de-red-usb-adb-reverse)
5. [Arranque del Backend en Modo Real](#5-arranque-del-backend-en-modo-real)
6. [Compilación e Instalación del APK](#6-compilación-e-instalación-del-apk)
7. [Checklist de Pruebas de Extremo a Extremo (E2E)](#7-checklist-de-pruebas-de-extremo-a-extremo-e2e)
8. [Resolución de Problemas Frecuentes](#8-resolución-de-problemas-frecuentes)

---

## 1. Resumen de Arquitectura y Flujo de Datos

```text
[ Teléfono Android Físico ]
  │
  ├─► Cámara Nativa (Foto de dibujo real en papel)
  │
  ├─► WonderLoop App (Compose Multiplatform)
  │    │
  │    ├──► OneSignal SDK ──(Push Tags: last_hero, pending_challenge)──► OneSignal Cloud
  │    │
  │    └──► RevenueCat SDK ──(Compras & Entitlements)──► RevenueCat API
  │
  ▼ (Conexión USB / adb reverse tcp:3000 tcp:3000)
[ Backend Gateway (PC Local: Puerto 3000) ]
  │
  ├──► Gemini 3.8 / 2.5 Flash Vision (Analiza dibujo físico) ──► Google AI Studio
  │
  ├──► Gemini Story Engine (Crea cuento de 5 páginas + Reto) ──► Google AI Studio
  │
  └──► RevenueCat REST API (Validación de cuotas de servidor) ──► RevenueCat Cloud
```

---

## 2. Obtención y Configuración de API Keys Reales

### A. Google Gemini IA (Visión y Narrativa)
El backend utiliza los modelos multimodales de Google para interpretar el dibujo del niño y redactar el libro digital.

1. Entra en [Google AI Studio](https://aistudio.google.com/).
2. Inicia sesión con tu cuenta de Google y pulsa en **"Get API key"** ➔ **"Create API key"**.
3. Copia tu clave (comienza por `AIzaSy...`).
4. Abre el archivo `backend/.env` en tu proyecto:
   ```env
   PORT=3000
   NODE_ENV=production
   GEMINI_API_KEY=AIzaSyTuClaveRealAqui
   MOCK_AI_SERVICES=false
   ```
   > 💡 Al fijar `MOCK_AI_SERVICES=false`, el backend deja de devolver datos simulados y llamará a Gemini directamente en cada petición.

---

### B. RevenueCat (Monetización y Paywall)
RevenueCat gestiona las suscripciones (Anual Familiar, Mensual Creador y Pack Consumible).

1. Entra en el [Dashboard de RevenueCat](https://app.revenuecat.com/) y crea un proyecto llamado `WonderLoop`.
2. **Configura los Entitlements:**
   - Ve a **Entitlements** y crea:
     - Identificador: `premium_access` (acceso a cuentos ilimitados).
     - Identificador: `family_tier` (acceso multiperfil).
3. **Configura el Offering ("default"):**
   - Crea los siguientes paquetes vinculados a tus identificadores:
     - `wonderloop_annual_family` (Plan Anual - 39,99 €)
     - `wonderloop_monthly_single` (Plan Mensual - 4,99 €)
     - `wonderloop_pack_10` (Pack 10 Cuentos - 2,99 €)
4. **Obtén las Claves:**
   - **Clave Secreta (Backend):** Ve a *Project Settings ➔ API Keys* y copia la clave secreta `sk_...`.
     - En `backend/.env`:
       ```env
       REVENUECAT_SECRET_KEY=sk_tu_clave_secreta_aqui
       MOCK_REVENUECAT=false
       ```
   - **Clave Pública de Android (App):** En *Project Settings ➔ API Keys*, copia la clave pública de Google Play (comienza por `goog_...`).
     - Abre `composeApp/src/androidMain/kotlin/org/wonderloop/app/MainActivity.kt`:
     - Sustituye `"goog_placeholder_api_key"` por tu clave real:
       ```kotlin
       Purchases.configure(
           PurchasesConfiguration.Builder(this, "goog_tu_clave_publica_aqui")
               .build()
       )
       ```

---

### C. OneSignal (Notificaciones de Retención)
OneSignal activa las notificaciones personalizadas para optar al premio de **$25,000 ("Keep Them Coming Back")**.

1. Regístrate o inicia sesión en [OneSignal Dashboard](https://dashboard.onesignal.com/).
2. Pulsa en **"New App/Website"**:
   - Nombre: `WonderLoop`.
   - Plataforma: **Google Android (FCM)**.
3. En *Settings ➔ Keys & IDs*, copia tu **OneSignal App ID** (formato UUID: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`).
4. Abre `composeApp/src/androidMain/kotlin/org/wonderloop/app/MainActivity.kt`:
   - Sustituye `"onesignal_app_id_placeholder"` por tu ID real:
     ```kotlin
     OneSignal.initWithContext(this, "tu-app-id-de-onesignal")
     ```

---

## 3. Preparación del Dispositivo Móvil Android

1. **Activar Opciones de Desarrollador:**
   - En tu teléfono Android, ve a: **Ajustes ➔ Información del teléfono**.
   - Busca **"Número de compilación"** y púlsalo **7 veces seguidas** hasta que aparezca el mensaje: *"¡Ahora eres desarrollador!"*.
2. **Activar Depuración por USB:**
   - Ve a: **Ajustes ➔ Sistema (o Ajustes adicionales) ➔ Opciones de desarrollador**.
   - Activa el interruptor **"Depuración por USB"**.
3. **Conectar el Teléfono:**
   - Conecta el teléfono a tu ordenador mediante un cable USB de datos.
   - En la pantalla del móvil aparecerá una ventana emergente: *"¿Permitir depuración por USB?"*.
   - Marca la casilla **"Permitir siempre desde este ordenador"** y pulsa **Aceptar**.
4. **Verificar que el PC reconoce el dispositivo:**
   - Abre un terminal de PowerShell en tu PC y ejecuta:
     ```powershell
     adb devices
     ```
   - Deberás ver tu dispositivo listado como `device`:
     ```text
     List of devices attached
     RFCW10XXXXX    device
     ```

---

## 4. Puente de Red USB (adb reverse)

Cuando la app corre en tu teléfono físico, `localhost` se refiere al propio teléfono, no a tu ordenador donde está el backend. Para que la app hable con el backend en tu PC sin necesidad de configurar redes complejas ni cambiar IPs:

Ejecuta en PowerShell:
```powershell
adb reverse tcp:3000 tcp:3000
```

> ✨ **Nota:** La aplicación WonderLoop incluye detección inteligente de red en `WonderLoopApi.kt`. Al ejecutar este comando, la app detectará automáticamente el túnel y se conectará en milisegundos con el backend.

---

## 5. Arranque del Backend en Modo Real

Abre un terminal en la carpeta del backend y arráncalo:

```powershell
cd c:\Users\jorge.arcicollar\Documents\AppCuentos\backend

# 1. Compilar TypeScript
npm run build

# 2. Iniciar el servidor
node dist/server.js
```

Verás en la consola:
```text
✨ WonderLoop Gateway Online!
===========================================
📡 Server listening on: http://localhost:3000
🎯 Target Event: RevenueCat Shipaton 2026
🤖 AI Engine: Gemini 3.8 Flash + Nanobanana + Gemini 3.1 TTS
💳 Monetization: RevenueCat (Production ready)
===========================================
```

Para verificar que el backend está respondiendo, puedes abrir en el navegador de tu PC:
`http://localhost:3000/api/story/health`
Debe devolver: `{"status":"ok","service":"WonderLoop Story Engine",...}`.

---

## 6. Compilación e Instalación del APK

Con el backend en marcha y el móvil conectado por USB:

```powershell
cd c:\Users\jorge.arcicollar\Documents\AppCuentos

# 1. Compilar la última versión del APK (si hiciste cambios en API keys)
.\gradlew.bat assembleDebug

# 2. Instalar el APK directamente en el teléfono
adb install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk

# 3. Abrir la app en la pantalla del móvil
adb shell am start -n org.wonderloop.app/.MainActivity
```

---

## 7. Checklist de Pruebas de Extremo a Extremo (E2E)

Sigue este recorrido completo para probar el producto de principio a fin:

### 🟢 1. Estado de Conexión
* Al abrir la app, la píldora superior debe mostrar:  
  **`🟢 Conectado con IA (Gemini 3.8 Flash)`**.

### 🟢 2. Captura Phygital Real
1. Dibuja en un folio de papel cualquier personaje con colores (ejemplo: un monstruito azul con sombrero verde).
2. Pulsa **"📸 Escanear Dibujo Ahora"**.
3. Selecciona la cámara, encuadra el dibujo y haz la foto.
4. Pulsa **"✨ Analizar con Magia"**.
5. **Observa el resultado de Gemini Vision:**
   - La pantalla de confirmación mostrará el nombre inventado, la especie detectada y los colores reales que dibujaste con tus lápices.

### 🟢 3. Creación del Cuento Digital con IA
1. Pulsa **"📖 ¡Comenzar el Cuento!"**.
2. Observa la pantalla de generación mientras Gemini escribe la historia personalizada y adapta el vocabulario a la edad seleccionada.
3. Desliza por las **5 páginas ilustradas** en el visor digital táctil.
4. Pulsa el icono **🔊** para comprobar la narración con voz.

### 🟢 4. The Creativity Loop (Misión Fuera de Pantalla)
1. Al llegar a la página 5, pulsa **"🖍️ ¡Ver Reto Físico! ➔"**.
2. Lee la misión analógica generada por IA (ejemplo: *"Dibuja al mejor amigo de tu personaje en otro papel"*).
3. Pulsa **"📚 Guardar en Mi Estantería y Salir"**.
4. Comprueba que el nuevo libro aparece guardado en la estantería de **"Mis Cuentos y Amigos"** en la pantalla principal.

### 🟢 5. Control Parental y Paywall
1. Pulsa el botón **"⭐ Premium"** en la esquina superior derecha o en el banner principal.
2. Comprueba que se abre el **Control Parental** con una multiplicación de adultos (ej. `8 × 7 = ?`).
3. Introduce un valor erróneo: comprueba que no te deja pasar y genera una nueva operación.
4. Introduce el valor correcto: comprueba que se abre el **Paywall de RevenueCat**.
5. Explora los 3 planes (Anual con 7 días gratis, Mensual y Pack de 10 cuentos).
6. Toca **"🎟️ Canjear Código"** e introduce el cupón: `SHIPATON2026`.
7. Verifica que se activa inmediatamente el estado **`🌟 Premium`**.

---

## 8. Resolución de Problemas Frecuentes

| Problema | Causa Posible | Solución |
| :--- | :--- | :--- |
| La app muestra *"🔴 Modo Local / Sin Conexión"* | El teléfono no alcanza el puerto 3000 del PC | Ejecuta `adb reverse tcp:3000 tcp:3000` en PowerShell con el móvil conectado. |
| `adb devices` aparece vacío | Cable USB solo de carga o depuración no permitida | Cambia de cable USB, revisa que la Depuración USB esté activa y acepta el mensaje de autorización en la pantalla del teléfono. |
| Error al analizar dibujo con Gemini | `GEMINI_API_KEY` vacía o inválida | Comprueba que tu API key en `backend/.env` no tiene espacios y que tu cuenta de Google AI Studio tiene cuota activa. |
| La cámara no se abre | Permiso de cámara revocado | En el teléfono, ve a *Ajustes ➔ Aplicaciones ➔ WonderLoop ➔ Permisos ➔ Cámara ➔ Permitir*. |
| Ver los logs en tiempo real para depurar | Quieres saber qué está pasando internamente | Ejecuta en terminal: `adb logcat -s WonderLoop:V System.out:V OneSignal:V Purchases:V` |
