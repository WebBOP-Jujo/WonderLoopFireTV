# Instrucciones para el Agente en `WonderLoopTV`

Antes de realizar cualquier cambio o análisis en este proyecto, **lee obligatoriamente el archivo [`CONTEXTO_MAESTRO_WONDERLOOP_TV.md`](file:///C:/Users/jorge.arcicollar/Documents/WonderLoopTV/CONTEXTO_MAESTRO_WONDERLOOP_TV.md)** ubicado en la raíz de este directorio.

En ese documento se detalla:
1. De dónde venimos (la app móvil original `WonderLoop` ya copiada íntegramente en este directorio).
2. El objetivo de transición a **Amazon Fire TV (`Build, Ship, Shape: Amazon Developer Hackathon` - `https://amazonappdev2026.devpost.com/`)**.
3. El diseño exacto del flujo de **escaneo de dibujo vía Código QR desde cualquier móvil hacia el Fire TV en tiempo real**.
4. La arquitectura actual del Backend (`backend/`) y de la App en Kotlin Multiplatform + Jetpack Compose (`composeApp/`), incluyendo la regla de mantener `visionAdapter` -> `imageAdapter` secuencial para preservar la consistencia visual del personaje.
5. El plan de implementación paso a paso en 5 Fases (Manifiesto Leanback 16:9, Puente QR Móvil-TV, Generador QR en pantalla de TV, UI "10-Foot" para pantallas de +42 pulgadas con navegación por mando D-Pad, e integración opcional con AWS Bedrock/Polly para el premio adicional *AWS Builder*).
