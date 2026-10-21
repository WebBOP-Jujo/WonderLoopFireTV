# 📺 Guía de Pruebas WonderLoop TV en Amazon Fire TV Stick 4K (y Móvil Companion QR)

**WonderLoop TV** está optimizado para ejecutarse en **Amazon Fire TV Stick 4K** con navegación 100% por mando a distancia (D-Pad) y escaneo de dibujos infantiles desde cualquier smartphone a través de un puente QR.

**Backend de producción (por defecto en la app):** `https://13-217-171-0.sslip.io`, en **Amazon EC2** (`t3.small`, `us-east-1`) detrás de Caddy (HTTPS automático). Con él **no hace falta arrancar nada en el PC**: basta con instalar el APK. El backend local (secciones 2–5) solo se necesita para desarrollar.

**Proveedores de IA actuales:**
- Visión del dibujo y texto del cuento → **Amazon Nova 2 Lite (Amazon Bedrock)**, con fallback automático a Gemini.
- Narración → **Amazon Polly** (Lucia `es-ES` / Ruth `en-US`), con fallback por página a Gemini TTS.
- Ilustraciones (retrato 1:1 + 5 escenas 16:9) → Gemini.

---

## 📋 Índice
0. [Prueba rápida con el backend de AWS](#0-prueba-rápida-con-el-backend-de-aws)
1. [Arquitectura y flujo de datos](#1-arquitectura-y-flujo-de-datos)
2. [Configuración del backend (`backend/.env`)](#2-configuración-del-backend-backendenv)
3. [Credenciales de AWS para pruebas locales](#3-credenciales-de-aws-para-pruebas-locales)
4. [Clave de la app (`local.properties`)](#4-clave-de-la-app-localproperties)
5. [Arranque del backend](#5-arranque-del-backend)
6. [Compilación e instalación en el Fire TV](#6-compilación-e-instalación-en-el-fire-tv)
7. [Checklist de pruebas E2E](#7-checklist-de-pruebas-e2e)
8. [Resolución de problemas](#8-resolución-de-problemas)
9. [Despliegue del backend en AWS EC2](#9-despliegue-del-backend-en-aws-ec2)

---

## 0. Prueba rápida con el backend de AWS

1. Compila el APK con la clave en `local.properties` (sección 4) e instálalo (sección 6). La URL del backend de AWS ya va incluida por defecto.
2. Abre la app → *Crear un nuevo cuento* → escanea el QR con **cualquier móvil**, aunque no esté en tu Wi-Fi: el enlace es `https://13-217-171-0.sslip.io/upload/<sessionId>`.
3. Comprobación del servidor desde el navegador: `https://13-217-171-0.sslip.io/` (proveedores activos) y `https://13-217-171-0.sslip.io/api/story/health`.

---

## 1. Arquitectura y flujo de datos

```text
[ Móvil (navegador, sin app) ] --escanea QR--> http://<IP_PC>:3000/upload/<sessionId>
        │ sube la foto del dibujo (comprimida a 1024px en <canvas>)
        ▼
[ Backend Gateway (PC, puerto 3000) ] <──polling cada 1,2 s── [ Fire TV Stick 4K (app Compose) ]
        │
        ├──► Amazon Bedrock · Nova 2 Lite ──► analiza el dibujo (nombre, especie, colores HEX)
        ├──► Gemini Image                 ──► retrato 1:1 del héroe
        ├──► Amazon Bedrock · Nova 2 Lite ──► cuento de 5 escenas + reto de dibujo
        ├──► Gemini Image                 ──► 5 ilustraciones 16:9
        └──► Amazon Polly                 ──► narración MP3 por página
             (cada llamada a AWS cae a Gemini automáticamente si falla)
```

---

## 2. Configuración del backend (`backend/.env`)

```powershell
cd backend
npm install
copy .env.example .env
```

Variables mínimas en `backend/.env` (el fichero está en `.gitignore`):

```env
PORT=3000
NODE_ENV=production

# Obligatoria: el servidor NO arranca sin ella (mín. 32 caracteres)
# Generar: node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
APP_SECRET_KEY=<secreto_aleatorio>

# Ilustraciones + fallback de visión, cuento y voz
GEMINI_API_KEY=<clave_de_Google_AI_Studio>

# AWS (ver sección 3)
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=<solo_para_pruebas_locales>
AWS_SECRET_ACCESS_KEY=<solo_para_pruebas_locales>
LLM_PROVIDER=bedrock
TTS_PROVIDER=polly

# 0 en red local; 1 si hay un proxy / balanceador delante
TRUST_PROXY=0
```

> Para desactivar AWS temporalmente: `LLM_PROVIDER=gemini` y/o `TTS_PROVIDER=gemini`.

---

## 3. Credenciales de AWS para pruebas locales

En producción (EC2) se usará un **rol IAM de instancia** y no hará falta ninguna clave. Para probar en el PC:

1. Consola de AWS → **IAM → Usuarios → "Crear persona"** (así se llama el botón en la consola en español) → nombre `wonderloop-local`, **sin** acceso a la consola.
2. En el usuario → pestaña **Permisos → Agregar permisos → Crear política insertada → JSON** → pegar [`backend/aws/wonderloop-iam-policy.json`](backend/aws/wonderloop-iam-policy.json) (solo `polly:SynthesizeSpeech` y `bedrock:InvokeModel` sobre Nova 2 Lite).
3. Pestaña **Credenciales de seguridad → Crear clave de acceso → "Código local"** → copiar ambas claves a `backend/.env`. La clave secreta solo se muestra una vez.
4. **Nunca** pegues las claves en el código, en un chat ni en un commit.

---

## 4. Clave de la app (`local.properties`)

La app Fire TV envía la cabecera `X-WonderLoop-Key`, cuyo valor se inyecta al compilar. Añade a `local.properties` (raíz del proyecto, fuera de git) **el mismo valor** que `APP_SECRET_KEY`:

```properties
wonderloop.appKey=<mismo_valor_que_APP_SECRET_KEY>
```

Si cambias el secreto, **recompila y reinstala el APK**: un APK antiguo recibirá `401 UNAUTHORIZED`.

Por defecto la app apunta al backend de AWS. Para usar un backend en tu PC durante el desarrollo, añade también:

```properties
wonderloop.backendUrl=http://192.168.0.35:3000
```

También se puede cambiar en tiempo de ejecución desde el diálogo de servidor de la pantalla QR: una IP se convierte en `http://<ip>:3000` y un nombre de dominio en `https://<dominio>`.

---

## 5. Arranque del backend

```powershell
cd backend
npm run build
npm run dev
```

El banner de arranque muestra la IP LAN y los proveedores activos, por ejemplo:

```text
🤖 AI Models:
   - Text & Vision: Amazon Bedrock (us.amazon.nova-2-lite-v1:0) -> fallback Gemini (gemini-3.5-flash-lite)
   - Image Gen:     gemini-3.1-flash-lite-image (16:9 Scenes / 1:1 Portraits)
   - Speech (TTS):  Amazon Polly (neural, us-east-1) -> fallback gemini-3.8-flash-lite-tts
```

Comprobación rápida desde el navegador del PC: `http://localhost:3000/api/story/health` debe devolver `{"status":"ok",...}`, y `http://localhost:3000/` devuelve los proveedores activos.

---

## 6. Compilación e instalación en el Fire TV

Requisitos en el Fire TV: **Ajustes → Mi Fire TV → Opciones para desarrolladores** con *Depuración ADB* y *Apps de fuentes desconocidas* activadas, y el Fire TV en la misma Wi-Fi que el PC.

```powershell
# 1. Compilar el APK (JDK 21)
.\gradlew.bat :composeApp:assembleDebug

# 2. Conectar ADB por Wi-Fi (IP actual del Fire TV Stick: 192.168.0.38)
adb connect 192.168.0.38:5555

# 3. Instalar o actualizar el APK
adb -s 192.168.0.38:5555 install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk

# 4. Arrancar la app en el televisor
adb -s 192.168.0.38:5555 shell am start -n org.wonderloop.app/.MainActivity
```

Si la app no encuentra el backend, revisa la dirección del servidor en el diálogo de la pantalla QR (navegable con el D-Pad). La app prueba, por este orden: la dirección guardada en ese diálogo, la de compilación (`wonderloop.backendUrl` o el backend de AWS), `10.0.2.2:3000` (emulador) y `localhost:3000`.

---

## 7. Checklist de pruebas E2E

### 🟢 1. Navegación con mando D-Pad
- La pantalla de inicio enfoca automáticamente el botón principal *"Crear un nuevo cuento mágico"*.
- Las tarjetas *"Ver Biblioteca"* y *"Ver todos"* se recorren con la cruceta y muestran el halo dorado de foco.
- `BACK` en la pantalla de inicio sale directamente al launcher del Fire TV (sin diálogo de confirmación).

### 🟢 2. Escaneo del dibujo vía QR
1. Pulsa *"Crear un nuevo cuento"* → aparece `QrScanScreen` con un QR grande.
2. Escanéalo con un móvil en la misma Wi-Fi → se abre la web de subida (`/upload/<sessionId>`).
3. Haz una foto al dibujo o elígelo de la galería → el TV pasa por *Móvil conectado → Subiendo dibujo → ¡Dibujo recibido!* sin tocar el mando.
4. **Sin móvil:** baja con el D-Pad a *"¿Sin móvil a mano?"* y elige uno de los 4 dibujos de prueba precargados.

### 🟢 3. Héroe generado (visión con Nova 2 Lite)
- La pantalla de confirmación muestra nombre, especie y colores coherentes con el dibujo y el retrato en el estilo elegido.
- En la consola del backend debe aparecer: `✅ [Bedrock] Drawing Vision Analysis via us.amazon.nova-2-lite-v1:0 (tool-use) in ~2s`.

### 🟢 4. Cuento ilustrado y narrado (Nova 2 Lite + Gemini Image + Polly)
1. Confirma el héroe y elige idioma (`es` / `en`) → pantalla de generación.
2. Consola del backend:
   - `✅ [Bedrock] Story Text Generation (...) via us.amazon.nova-2-lite-v1:0 (tool-use) in ~5-6s`
   - `✅ [PollyAudioAdapter] Page N synthesized ... (Lucia/neural ...)` (o `Ruth` en inglés) para las 5 páginas.
3. En el lector: pasa de página con `DPAD_RIGHT` / `DPAD_LEFT` (o avance/retroceso rápido) y activa la narración con `PLAY_PAUSE`. La voz debe ser Lucia (español) o Ruth (inglés).
4. El texto no debe mostrar asteriscos (`*Plof!*`).

### 🟢 5. Bucle creativo (sagas)
1. Al final de la página 5 aparece el **reto de dibujo**.
2. Escanea un nuevo dibujo → se genera el **Capítulo 2**, con el héroe original y el nuevo compañero, y la página de celebración entre capítulos.

### 🟢 6. Fallback a Gemini (opcional)
- Arranca el backend con credenciales de AWS inválidas o sin ellas: la consola muestra `⚠️ [Bedrock] Credentials/permissions/model access problem ... Bedrock paused for 5 min` y `🔁 [LLM] ... Using Gemini`, y la historia se genera igualmente.

### 🟢 7. Zona de Padres
- Se protege con una operación aritmética (puerta parental). Desde ahí se gestionan perfiles, idioma, estilo y efectos de sonido.
- Cupones locales válidos: `AMAZON2026`, `HACKATHON2026`, `DEVPOSTVIP`, `WONDERVIP` (en modo hackathon la cuota ya es ilimitada).

---

## 8. Resolución de problemas

| Problema | Causa posible | Solución |
| :--- | :--- | :--- |
| El backend se cierra al arrancar con `🚨 [Security] APP_SECRET_KEY is missing...` | Falta `APP_SECRET_KEY` o tiene menos de 32 caracteres | Genera uno (sección 2) y ponlo también en `local.properties`. |
| La app recibe `401 UNAUTHORIZED` | `wonderloop.appKey` no coincide con `APP_SECRET_KEY`, o el APK se compiló con la clave antigua | Iguala ambos valores, recompila y reinstala el APK. |
| Consola: `AccessDeniedException ... bedrock:InvokeModel` | El usuario IAM no tiene la política de Bedrock | Añade [`wonderloop-iam-policy.json`](backend/aws/wonderloop-iam-policy.json) al usuario. Mientras tanto, Gemini cubre la petición. |
| Consola: `Polly paused for 5 min` | Credenciales de AWS ausentes o sin `polly:SynthesizeSpeech` | Revisa `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` y la política; reinicia el backend. |
| El QR no abre la web en el móvil | El móvil no está en la misma Wi-Fi o la IP LAN detectada es incorrecta | Conecta el móvil a la misma red o fija `LAN_IP=<ip_del_pc>` en `.env`. |
| La subida falla con `INVALID_IMAGE_PAYLOAD` | Imagen corrupta o de más de 25 megapíxeles | Usa una foto normal; la web del móvil ya la reduce a 1024 px. |
| La app muestra *"Sin conexión"* | El Fire TV no alcanza el puerto 3000 del PC | Comprueba la IP en el diálogo de la pantalla QR y el firewall de Windows (puerto 3000). |
| En los logs del servidor aparece `🚦 [RateLimit:...]` | Demasiadas peticiones desde la misma IP pública (todos los dispositivos de casa cuentan juntos) | Es informativo. Si ocurre en uso normal, ajusta los límites en `backend/src/server.ts`. |
| La narración local no suena cuando falta el audio del servidor | Motor TTS del sistema no visible para la app (Android 11+) | El manifiesto ya declara `<queries>` para `TTS_SERVICE`; reinstala el APK actual. |
| Ver logs de la app | Depuración en el dispositivo | `adb -s 192.168.0.38:5555 logcat -s System.out:V` |

---

## 9. Despliegue del backend en AWS EC2

Infraestructura actual (región `us-east-1`):
- Instancia **EC2 `t3.small`** con Amazon Linux 2023, 20 GiB gp3, **IP elástica `13.217.171.0`**.
- **Rol IAM `wonderloop-backend-role`** con la política [`backend/aws/wonderloop-iam-policy.json`](backend/aws/wonderloop-iam-policy.json): Bedrock y Polly funcionan sin claves de AWS en el servidor.
- Grupo de seguridad: 22 (solo tu IP), 80 y 443 (abiertos).
- Contenedores (Docker Compose): `caddy` (HTTPS en :443 con certificado automático para `13-217-171-0.sslip.io`) y `backend` (:8080, una sola réplica porque las sesiones QR viven en memoria).

**Desplegar una versión nueva** (desde la carpeta `backend`, en PowerShell):

```powershell
.\deploy\deploy-ec2.ps1 -Ip 13.217.171.0 -KeyPath "C:\Users\jorge.arcicollar\Documents\wonderloop-key.pem"
```

El script genera el `.env` de producción a partir de `backend/.env` **sin las claves de AWS locales**, sube el código por SSH, ejecuta [`deploy/bootstrap.sh`](backend/deploy/bootstrap.sh) (instala Docker y Compose si faltan, swap de 2 GB, `docker compose up -d --build`) y espera a que `/api/story/health` responda por HTTPS.

**Ver los logs del servidor:**

```powershell
ssh -i "C:\Users\jorge.arcicollar\Documents\wonderloop-key.pem" ec2-user@13.217.171.0 "cd /opt/wonderloop/backend/deploy && sudo docker compose logs -f backend"
```

En los logs deben aparecer `✅ [Bedrock] ... (tool-use)` y `✅ [PollyAudioAdapter] ...`. Si aparecen `🔁 [LLM] ... Using Gemini` o `Polly paused`, revisa que la instancia tenga asociado el rol IAM.

**Coste aproximado:** ~15 $/mes la `t3.small` más el uso de Bedrock y Polly (céntimos en pruebas). Mantén la alerta de AWS Budgets activa y para o termina la instancia cuando acabe la evaluación (20/11/2026).
