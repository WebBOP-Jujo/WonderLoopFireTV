import { Router, Request, Response } from 'express';
import os from 'os';
import crypto from 'crypto';
import sharp from 'sharp';

export type TvSessionStatus = 'waiting' | 'connected' | 'uploading' | 'ready' | 'retry';

export interface TvSession {
  sessionId: string;
  shortCode: string;
  status: TvSessionStatus;
  language: 'es' | 'en';
  imageBase64?: string;
  mimeType?: string;
  createdAt: number;
  expiresAt: number;
}

const SESSION_TTL_MS = 15 * 60 * 1000; // 15 minutes
const MAX_SESSIONS = 500; // Cap concurrent memory footprint
const MAX_DRAWING_INPUT_PIXELS = 25_000_000;
const sessions = new Map<string, TvSession>();

// Periodic cleanup of expired sessions every 2 minutes
setInterval(() => {
  const now = Date.now();
  for (const [id, session] of sessions.entries()) {
    if (session.expiresAt <= now) {
      sessions.delete(id);
    }
  }
}, 2 * 60 * 1000).unref();

/**
 * Detects the best local Wi-Fi / LAN IPv4 address of the host machine,
 * filtering out loopback (127.x), link-local APIPA (169.254.x), and virtual
 * adapters (WSL, Hyper-V, vEthernet, Docker, VirtualBox, VMware).
 */
export function detectLanIp(): string {
  if (process.env.LAN_IP && process.env.LAN_IP.trim().length > 0) {
    return process.env.LAN_IP.trim();
  }

  const interfaces = os.networkInterfaces();
  const candidates: { name: string; address: string; score: number }[] = [];

  for (const [name, nets] of Object.entries(interfaces)) {
    if (!nets) continue;
    const lowerName = name.toLowerCase();

    // Skip virtual network adapters
    if (
      lowerName.includes('vethernet') ||
      lowerName.includes('wsl') ||
      lowerName.includes('hyper-v') ||
      lowerName.includes('docker') ||
      lowerName.includes('virtualbox') ||
      lowerName.includes('vbox') ||
      lowerName.includes('vmware') ||
      lowerName.includes('loopback')
    ) {
      continue;
    }

    for (const net of nets) {
      const isIPv4 = net.family === 'IPv4' || (net.family as unknown as number) === 4;
      if (!isIPv4 || net.internal) continue;

      const addr = net.address;
      // Skip loopback, link-local APIPA, and VirtualBox host-only subnet
      if (
        addr.startsWith('127.') ||
        addr.startsWith('169.254.') ||
        addr.startsWith('192.168.56.')
      ) {
        continue;
      }

      let score = 10;
      if (lowerName.includes('wi-fi') || lowerName.includes('wifi') || lowerName.includes('wlan')) {
        score += 50;
      } else if (lowerName.includes('ethernet') || lowerName.includes('eth')) {
        score += 30;
      }

      if (addr.startsWith('192.168.')) {
        score += 25;
      } else if (addr.startsWith('10.')) {
        score += 15;
      }

      candidates.push({ name, address: addr, score });
    }
  }

  candidates.sort((a, b) => b.score - a.score);
  return candidates[0]?.address || '192.168.0.35';
}

function generateSessionCodes(): { sessionId: string; shortCode: string } {
  const digits = Math.floor(1000 + Math.random() * 9000).toString();
  const suffix = crypto.randomBytes(4).toString('hex').toUpperCase();
  return {
    sessionId: `WL${digits}${suffix}`,
    shortCode: `WL-${digits}`
  };
}

function resolveBaseUrlForQr(req: Request, lanIp: string): string {
  if (process.env.PUBLIC_BASE_URL && process.env.PUBLIC_BASE_URL.trim().length > 0) {
    return process.env.PUBLIC_BASE_URL.trim().replace(/\/+$/, '');
  }

  const port = process.env.PORT || 3000;
  const hostHeader = (req.headers.host || '').trim();
  const hostname = hostHeader.split(':')[0] || '';

  // If the request came via localhost or Android emulator alias (10.0.2.2),
  // substitute the real Wi-Fi LAN IP so a physical phone can scan the QR code.
  if (
    !hostname ||
    hostname === 'localhost' ||
    hostname === '127.0.0.1' ||
    hostname === '10.0.2.2' ||
    hostname === '0.0.0.0'
  ) {
    return `http://${lanIp}:${port}`;
  }

  const protocol = req.headers['x-forwarded-proto'] || req.protocol || 'http';
  return `${protocol}://${hostHeader}`;
}

export function createTvSessionApiRouter(): Router {
  const router = Router();

  /**
   * POST /api/tv-session (or /v1/tv-session)
   * Creates a new QR pairing session for Fire TV.
   */
  router.post('/', (req: Request, res: Response) => {
    const rawLang = String(req.body?.language || req.query?.lang || 'es').toLowerCase();
    const language: 'es' | 'en' = rawLang.startsWith('en') ? 'en' : 'es';

    const { sessionId, shortCode } = generateSessionCodes();
    const now = Date.now();

    // Enforce max capacity and evict expired or oldest session if map is full
    if (sessions.size >= MAX_SESSIONS) {
      for (const [id, s] of sessions.entries()) {
        if (s.expiresAt <= now || s.status === 'ready') {
          sessions.delete(id);
        }
      }
      if (sessions.size >= MAX_SESSIONS) {
        const oldestKey = sessions.keys().next().value;
        if (oldestKey) sessions.delete(oldestKey);
      }
    }

    const session: TvSession = {
      sessionId,
      shortCode,
      status: 'waiting',
      language,
      createdAt: now,
      expiresAt: now + SESSION_TTL_MS
    };

    sessions.set(sessionId, session);

    const lanIp = detectLanIp();
    const baseUrl = resolveBaseUrlForQr(req, lanIp);
    const uploadUrl = `${baseUrl}/upload/${sessionId}`;

    console.log(`📺 [TvSession] Created session ${sessionId} (${shortCode}) -> ${uploadUrl}`);

    res.status(201).json({
      success: true,
      sessionId,
      shortCode,
      uploadUrl,
      lanIp,
      expiresInSeconds: Math.round(SESSION_TTL_MS / 1000)
    });
  });

  /**
   * GET /api/tv-session/:sessionId
   * Polled by Fire TV every ~1.2s to check mobile connection & receive the drawing.
   */
  router.get('/:sessionId', (req: Request, res: Response) => {
    const sessionId = String(req.params.sessionId || '').trim().toUpperCase();
    const session = sessions.get(sessionId);

    if (!session || session.expiresAt <= Date.now()) {
      res.status(404).json({
        success: false,
        error: 'SESSION_NOT_FOUND',
        message: 'La sesión de TV ha expirado o no existe.'
      });
      return;
    }

    const expiresInSeconds = Math.max(0, Math.round((session.expiresAt - Date.now()) / 1000));

    res.json({
      success: true,
      sessionId: session.sessionId,
      shortCode: session.shortCode,
      status: session.status,
      language: session.language,
      imageBase64: session.status === 'ready' ? session.imageBase64 : undefined,
      mimeType: session.status === 'ready' ? (session.mimeType || 'image/jpeg') : undefined,
      expiresInSeconds
    });

    if (session.status === 'ready' && session.imageBase64) {
      // Schedule delayed cleanup of multi-MB base64 buffer from RAM to avoid memory bloat
      if (!(session as any)._cleanupScheduled) {
        (session as any)._cleanupScheduled = true;
        setTimeout(() => {
          session.imageBase64 = undefined;
          console.log(`🧹 [TvSession] Evicted image buffer for completed session ${session.sessionId} from RAM`);
        }, 60000).unref();
      }
    }
  });

  /**
   * POST /api/tv-session/:sessionId/connected
   * Triggered automatically when the mobile browser opens the QR link.
   */
  router.post('/:sessionId/connected', (req: Request, res: Response) => {
    const sessionId = String(req.params.sessionId || '').trim().toUpperCase();
    const session = sessions.get(sessionId);

    if (!session || session.expiresAt <= Date.now()) {
      res.status(404).json({ success: false, error: 'SESSION_NOT_FOUND' });
      return;
    }

    if (session.status === 'waiting') {
      session.status = 'connected';
      console.log(`📱 [TvSession] Mobile connected to session ${sessionId}`);
    }

    res.json({ success: true, status: session.status });
  });

  /**
   * POST /api/tv-session/:sessionId/status
   * Updates intermediate status (e.g., 'connected' | 'uploading' | 'retry').
   */
  router.post('/:sessionId/status', (req: Request, res: Response) => {
    const sessionId = String(req.params.sessionId || '').trim().toUpperCase();
    const session = sessions.get(sessionId);

    if (!session || session.expiresAt <= Date.now()) {
      res.status(404).json({ success: false, error: 'SESSION_NOT_FOUND' });
      return;
    }

    const nextStatus = req.body?.status as TvSessionStatus | undefined;
    if (nextStatus) {
      if (nextStatus === 'retry') {
        session.status = 'retry';
        session.imageBase64 = undefined;
        session.mimeType = undefined;
        (session as any)._cleanupScheduled = false;
        session.expiresAt = Date.now() + SESSION_TTL_MS;
        console.log(`🔄 [TvSession] Session ${sessionId} status -> retry (cleared previous drawing)`);
      } else if ((nextStatus === 'connected' || nextStatus === 'uploading') && session.status !== 'ready') {
        session.status = nextStatus;
        console.log(`🔄 [TvSession] Session ${sessionId} status -> ${session.status}`);
      }
    }

    res.json({ success: true, status: session.status });
  });

  /**
   * POST /api/tv-session/:sessionId/reset
   * Resets an active session back to 'connected' or 'waiting' and clears the previous drawing.
   * Allows the mobile phone and TV to reuse the same session smoothly without refreshing or closing the browser.
   */
  router.post('/:sessionId/reset', (req: Request, res: Response) => {
    const sessionId = String(req.params.sessionId || '').trim().toUpperCase();
    const session = sessions.get(sessionId);

    if (!session || session.expiresAt <= Date.now()) {
      res.status(404).json({ success: false, error: 'SESSION_NOT_FOUND' });
      return;
    }

    const lanIp = detectLanIp();
    const baseUrl = resolveBaseUrlForQr(req, lanIp);
    const uploadUrl = `${baseUrl}/upload/${session.sessionId}`;

    session.status = 'connected';
    session.imageBase64 = undefined;
    session.mimeType = undefined;
    (session as any)._cleanupScheduled = false;
    session.expiresAt = Date.now() + SESSION_TTL_MS; // extend session

    console.log(`🔄 [TvSession] Session ${sessionId} reset for new drawing upload.`);
    res.json({
      success: true,
      sessionId: session.sessionId,
      shortCode: session.shortCode,
      uploadUrl,
      lanIp,
      status: session.status,
      expiresInSeconds: Math.round(SESSION_TTL_MS / 1000)
    });
  });

  /**
   * POST /api/tv-session/:sessionId/drawing
   * Receives the child's drawing from the mobile web app, normalizes it, and marks the session 'ready'.
   */
  router.post('/:sessionId/drawing', async (req: Request, res: Response): Promise<void> => {
    const sessionId = String(req.params.sessionId || '').trim().toUpperCase();
    const session = sessions.get(sessionId);

    if (!session || session.expiresAt <= Date.now()) {
      res.status(404).json({
        success: false,
        error: 'SESSION_NOT_FOUND',
        message: 'La sesión de TV ha expirado. Escanea el nuevo código QR en tu televisor.'
      });
      return;
    }

    try {
      const rawImage = String(req.body?.imageBase64 || '').trim();
      const rawMime = String(req.body?.mimeType || 'image/jpeg').trim().toLowerCase();

      if (!rawImage || rawImage.length < 100) {
        res.status(400).json({
          success: false,
          error: 'INVALID_IMAGE',
          message: 'No se recibió una imagen válida del dibujo.'
        });
        return;
      }

      if (rawImage.length > 7 * 1024 * 1024) {
        res.status(400).json({
          success: false,
          error: 'IMAGE_TOO_LARGE',
          message: 'La imagen excede el límite máximo permitido (5MB).'
        });
        return;
      }

      const validMimes = new Set(['image/jpeg', 'image/png', 'image/webp']);
      if (!validMimes.has(rawMime)) {
        res.status(400).json({
          success: false,
          error: 'INVALID_MIME_TYPE',
          message: 'Formato de imagen no soportado. Usa JPEG, PNG o WebP.'
        });
        return;
      }

      session.status = 'uploading';

      // Extract raw base64 payload (strip data:image/...;base64, prefix if present)
      const base64Clean = rawImage.replace(/^data:[^;]+;base64,/, '');
      let finalBase64 = base64Clean;
      let finalMime = rawMime || 'image/jpeg';

      // Normalize & auto-rotate (EXIF orientation) via sharp so phone camera photos are always upright
      try {
        const inputBuf = Buffer.from(base64Clean, 'base64');
        // limitInputPixels guards against decompression bombs (a tiny PNG declaring e.g. 16k x 16k pixels).
        // The mobile page already downscales to 1024px, so 25 MP is generous for any real phone photo.
        const optimizedBuf = await sharp(inputBuf, { limitInputPixels: MAX_DRAWING_INPUT_PIXELS })
          .rotate() // Honors EXIF orientation from mobile cameras
          .resize({ width: 1024, height: 1024, fit: 'inside', withoutEnlargement: true })
          .jpeg({ quality: 85, mozjpeg: true })
          .toBuffer();

        if (optimizedBuf.length > 0) {
          finalBase64 = optimizedBuf.toString('base64');
          finalMime = 'image/jpeg';
        }
      } catch (sharpErr: any) {
        console.warn(`⚠️ [TvSession] sharp failed to decode drawing for ${sessionId}: ${sharpErr?.message}`);
        res.status(400).json({
          success: false,
          error: 'INVALID_IMAGE_PAYLOAD',
          message: 'No se pudo decodificar la imagen enviada. Asegúrate de que sea un archivo de imagen válido.'
        });
        return;
      }

      session.imageBase64 = finalBase64;
      session.mimeType = finalMime;
      session.status = 'ready';

      const kbSize = Math.round((finalBase64.length * 3) / 4 / 1024);
      console.log(`✨ [TvSession] Drawing received for session ${sessionId} (${kbSize} KB JPEG). Ready for Fire TV!`);

      res.json({
        success: true,
        sessionId: session.sessionId,
        status: session.status
      });
    } catch (err: any) {
      console.error(`❌ [TvSession] Error processing drawing for ${sessionId}:`, err);
      res.status(500).json({
        success: false,
        error: 'UPLOAD_FAILED',
        message: 'Error al procesar el dibujo. Inténtalo de nuevo.'
      });
    }
  });

  return router;
}

/**
 * Serves the self-contained Warm Storybook Mobile Web App at GET /upload/:sessionId
 */
export function createTvUploadWebRouter(): Router {
  const router = Router();

  router.get('/:sessionId', (req: Request, res: Response) => {
    const sessionId = String(req.params.sessionId || '').trim().toUpperCase();
    const session = sessions.get(sessionId);

    if (!session || session.expiresAt <= Date.now()) {
      res.status(404).send(renderExpiredSessionHtml());
      return;
    }

    // Immediately mark session as connected when the phone loads the page
    if (session.status === 'waiting') {
      session.status = 'connected';
      console.log(`📱 [TvSession] Mobile web app opened for session ${sessionId} (${session.shortCode})`);
    }

    res.setHeader('Content-Type', 'text/html; charset=utf-8');
    res.send(renderMobileUploadHtml(session));
  });

  return router;
}

function renderExpiredSessionHtml(): string {
  return `<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <title>WonderLoop TV</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Fredoka:wght@600;700&family=Nunito:wght@600;700&display=swap" rel="stylesheet">
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: 'Nunito', -apple-system, BlinkMacSystemFont, sans-serif;
      background: radial-gradient(circle at 50% 20%, #FFFDF9 0%, #FFFBF5 60%, #F7EFE4 100%);
      color: #2B253A;
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      text-align: center;
    }
    .card {
      background: #FFFDF9;
      border: 2px solid #EDE3D5;
      border-radius: 28px;
      padding: 32px 24px;
      max-width: 420px;
      width: 100%;
      box-shadow: 0 12px 32px rgba(43, 37, 58, 0.08);
    }
    h1 {
      font-family: 'Fredoka', sans-serif;
      font-size: 24px;
      color: #D9384A;
      margin: 16px 0 10px;
    }
    p {
      font-size: 16px;
      color: #655B7C;
      line-height: 1.5;
    }
  </style>
</head>
<body>
  <div class="card">
    <div style="font-size: 48px;">⏳</div>
    <h1>Sesión caducada / Session Expired</h1>
    <p>Por favor, genera un nuevo código QR en la pantalla de tu Fire TV y vuelve a escanearlo.<br><br>Please generate a new QR code on your Fire TV screen and scan it again.</p>
  </div>
</body>
</html>`;
}

function renderMobileUploadHtml(session: TvSession): string {
  const initialLang = session.language === 'en' ? 'en' : 'es';

  return `<!DOCTYPE html>
<html lang="${initialLang}">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <title>WonderLoop TV — Portal Mágico</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Fredoka:wght@600;700&family=Nunito:wght@600;700;800&display=swap" rel="stylesheet">
  <style>
    :root {
      --cream: #FFFBF5;
      --paper: #FFFDF9;
      --warm-card: #F7EFE4;
      --coral: #D9384A;
      --coral-light: #FF6B6B;
      --gold: #FFD166;
      --mint: #0E7F64;
      --mint-soft: #E5F9F5;
      --text-primary: #2B253A;
      --text-secondary: #655B7C;
      --border: #EDE3D5;
    }
    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      -webkit-tap-highlight-color: transparent;
    }
    body {
      font-family: 'Nunito', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      background: radial-gradient(circle at 50% 12%, #FFF3E3 0%, var(--cream) 55%, #F7EFE4 100%);
      color: var(--text-primary);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 18px 16px 36px;
    }
    .top-bar {
      width: 100%;
      max-width: 440px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 16px;
    }
    .brand-pill {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      background: var(--paper);
      border: 2px solid var(--gold);
      border-radius: 999px;
      padding: 6px 14px;
      box-shadow: 0 4px 12px rgba(217, 56, 74, 0.08);
    }
    .brand-title {
      font-family: 'Fredoka', sans-serif;
      font-weight: 700;
      font-size: 17px;
      color: var(--coral);
    }
    .session-badge {
      font-size: 12px;
      font-weight: 800;
      background: var(--warm-card);
      color: var(--text-secondary);
      padding: 3px 8px;
      border-radius: 999px;
    }
    .lang-toggle {
      background: var(--paper);
      border: 1.5px solid var(--border);
      border-radius: 999px;
      padding: 6px 12px;
      font-family: 'Nunito', sans-serif;
      font-weight: 800;
      font-size: 13px;
      color: var(--text-primary);
      cursor: pointer;
    }
    .card {
      width: 100%;
      max-width: 440px;
      background: var(--paper);
      border: 2.5px solid var(--border);
      border-radius: 28px;
      padding: 24px 20px;
      box-shadow: 0 14px 34px rgba(43, 37, 58, 0.08);
      text-align: center;
      position: relative;
      overflow: hidden;
    }
    .tv-connected-banner {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: var(--mint-soft);
      color: var(--mint);
      border: 1.5px solid rgba(14, 127, 100, 0.25);
      border-radius: 999px;
      padding: 6px 14px;
      font-size: 13px;
      font-weight: 800;
      margin-bottom: 14px;
    }
    .pulse-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--mint);
      animation: pulse 1.6s infinite;
    }
    @keyframes pulse {
      0%, 100% { transform: scale(1); opacity: 1; }
      50% { transform: scale(1.35); opacity: 0.55; }
    }
    h1 {
      font-family: 'Fredoka', sans-serif;
      font-size: 25px;
      line-height: 1.2;
      color: var(--text-primary);
      margin-bottom: 8px;
    }
    .subtitle {
      font-size: 15px;
      line-height: 1.45;
      color: var(--text-secondary);
      margin-bottom: 20px;
    }
    .drawing-frame {
      width: 100%;
      aspect-ratio: 4 / 3;
      max-height: 240px;
      background: var(--warm-card);
      border: 3px dashed var(--gold);
      border-radius: 22px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      margin-bottom: 20px;
      overflow: hidden;
      position: relative;
    }
    .drawing-frame img {
      width: 100%;
      height: 100%;
      object-fit: contain;
      display: none;
      background: #FFFDF9;
    }
    .placeholder-icon {
      font-size: 46px;
      margin-bottom: 8px;
    }
    .placeholder-hint {
      font-size: 14px;
      font-weight: 700;
      color: var(--text-secondary);
      padding: 0 16px;
    }
    .btn-group {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .btn {
      width: 100%;
      border: none;
      border-radius: 18px;
      padding: 16px 20px;
      font-family: 'Fredoka', sans-serif;
      font-size: 18px;
      font-weight: 600;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      cursor: pointer;
      transition: transform 0.12s ease, opacity 0.15s ease;
    }
    .btn:active {
      transform: scale(0.98);
    }
    .btn-primary {
      background: linear-gradient(135deg, var(--coral) 0%, var(--coral-light) 100%);
      color: #FFFFFF;
      box-shadow: 0 8px 20px rgba(217, 56, 74, 0.28);
    }
    .btn-secondary {
      background: var(--paper);
      color: var(--mint);
      border: 2.5px solid var(--mint);
    }
    .status-box {
      display: none;
      margin-top: 16px;
      padding: 16px;
      border-radius: 18px;
      font-weight: 700;
      font-size: 15px;
      line-height: 1.4;
    }
    .status-uploading {
      display: block;
      background: #FFF6DF;
      border: 2px solid var(--gold);
      color: #8A5A00;
    }
    .status-success {
      display: block;
      background: var(--mint-soft);
      border: 2px solid var(--mint);
      color: var(--mint);
    }
    .status-error {
      display: block;
      background: #FDECEE;
      border: 2px solid var(--coral);
      color: var(--coral);
    }
    .status-retry {
      display: block;
      background: #FFF8E7;
      border: 2px solid var(--gold);
      color: #925C00;
    }
    input[type="file"] {
      display: none;
    }
    .footer-note {
      margin-top: 18px;
      font-size: 13px;
      color: var(--text-secondary);
      text-align: center;
    }
  </style>
</head>
<body>
  <div class="top-bar">
    <div class="brand-pill">
      <span>✨</span>
      <span class="brand-title">WonderLoop TV</span>
      <span class="session-badge">${session.shortCode}</span>
    </div>
    <button type="button" class="lang-toggle" id="langBtn">🇪🇸 ES / 🇬🇧 EN</button>
  </div>

  <div class="card">
    <div class="tv-connected-banner">
      <span class="pulse-dot"></span>
      <span id="tvBadgeText">Conectado con tu Fire TV</span>
    </div>

    <h1 id="titleText">¡Envía tu dibujo a la tele!</h1>
    <p class="subtitle" id="subtitleText">Haz una foto al dibujo en papel de tu peque y mira cómo cobra vida en la pantalla del salón.</p>

    <div class="drawing-frame" id="previewFrame">
      <div id="placeholderContent">
        <div class="placeholder-icon">🎨🖍️</div>
        <div class="placeholder-hint" id="placeholderHint">Coloca el dibujo sobre una mesa bien iluminada</div>
      </div>
      <img id="previewImg" alt="Vista previa del dibujo" />
    </div>

    <div class="btn-group" id="actionButtons">
      <button type="button" class="btn btn-primary" id="cameraBtn">
        <span id="cameraBtnIcon">📸</span>
        <span id="cameraBtnText">Hacer foto al dibujo</span>
      </button>
      <button type="button" class="btn btn-secondary" id="galleryBtn">
        <span id="galleryBtnIcon">🖼️</span>
        <span id="galleryBtnText">Elegir de la galería</span>
      </button>
    </div>

    <div id="statusBox" class="status-box"></div>

    <!-- Hidden file inputs for Camera and Gallery -->
    <input type="file" id="cameraInput" accept="image/*" capture="environment" />
    <input type="file" id="galleryInput" accept="image/*" />
  </div>

  <p class="footer-note" id="footerNote">✨ La magia comenzará automáticamente en tu televisor</p>

  <script>
    (function() {
      const SESSION_ID = ${JSON.stringify(session.sessionId)};
      let currentLang = ${JSON.stringify(initialLang)};

      const STRINGS = {
        es: {
          tvBadge: "Conectado con tu Fire TV",
          title: "¡Envía tu dibujo a la tele!",
          subtitle: "Haz una foto al dibujo en papel de tu peque y mira cómo cobra vida en la pantalla del salón.",
          placeholder: "Coloca el dibujo sobre una mesa bien iluminada",
          cameraBtn: "Hacer foto al dibujo",
          galleryBtn: "Elegir de la galería",
          uploading: "⬆️ Enviando dibujo a tu Fire TV...",
          success: "✨ ¡Dibujo recibido en tu Fire TV! Mira la pantalla del salón.",
          sendAnother: "Hacer otra foto",
          retryNotice: "🔍 ¡Casi lo tenemos! La tele no ha detectado un dibujo claro en tu foto anterior. Enfoca bien tu dibujo en el papel con buena luz y vuelve a disparar.",
          retryBtn: "Repetir foto del dibujo",
          errorGeneric: "No se pudo enviar el dibujo. Inténtalo de nuevo.",
          sessionExpired: "📺 Tu televisor ha renovado el código QR. Apunta tu cámara a la tele para reconectar.",
          footer: "✨ La magia comenzará automáticamente en tu televisor"
        },
        en: {
          tvBadge: "Connected to your Fire TV",
          title: "Send your drawing to the TV!",
          subtitle: "Take a photo of your child's paper drawing and watch it come alive on your living room screen.",
          placeholder: "Place the drawing on a well-lit table",
          cameraBtn: "Take photo of drawing",
          galleryBtn: "Choose from gallery",
          uploading: "⬆️ Sending drawing to your Fire TV...",
          success: "✨ Drawing received on your Fire TV! Look at your TV screen.",
          sendAnother: "Take another photo",
          retryNotice: "🔍 Almost there! The TV couldn't detect a clear drawing in your previous photo. Focus closely on the paper drawing with good light and try again.",
          retryBtn: "Retake drawing photo",
          errorGeneric: "Could not send the drawing. Please try again.",
          sessionExpired: "📺 Your TV has generated a new QR code. Point your camera at the TV to reconnect.",
          footer: "✨ The magic will start automatically on your TV"
        }
      };

      const tvBadgeText = document.getElementById('tvBadgeText');
      const titleText = document.getElementById('titleText');
      const subtitleText = document.getElementById('subtitleText');
      const placeholderHint = document.getElementById('placeholderHint');
      const cameraBtnIcon = document.getElementById('cameraBtnIcon');
      const cameraBtnText = document.getElementById('cameraBtnText');
      const galleryBtnIcon = document.getElementById('galleryBtnIcon');
      const galleryBtnText = document.getElementById('galleryBtnText');
      const footerNote = document.getElementById('footerNote');
      const langBtn = document.getElementById('langBtn');
      const cameraBtn = document.getElementById('cameraBtn');
      const galleryBtn = document.getElementById('galleryBtn');
      const cameraInput = document.getElementById('cameraInput');
      const galleryInput = document.getElementById('galleryInput');
      const previewImg = document.getElementById('previewImg');
      const placeholderContent = document.getElementById('placeholderContent');
      const statusBox = document.getElementById('statusBox');

      function applyLanguage() {
        const t = STRINGS[currentLang] || STRINGS.es;
        tvBadgeText.textContent = t.tvBadge;
        titleText.textContent = t.title;
        subtitleText.textContent = t.subtitle;
        placeholderHint.textContent = t.placeholder;
        cameraBtnText.textContent = t.cameraBtn;
        galleryBtnText.textContent = t.galleryBtn;
        footerNote.textContent = t.footer;
        langBtn.textContent = currentLang === 'es' ? '🇪🇸 ES | 🇬🇧 EN' : '🇬🇧 EN | 🇪🇸 ES';
      }

      langBtn.addEventListener('click', function() {
        currentLang = currentLang === 'es' ? 'en' : 'es';
        applyLanguage();
      });

      applyLanguage();

      // Notify backend that mobile is connected
      fetch('/api/tv-session/' + encodeURIComponent(SESSION_ID) + '/connected', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      }).catch(function() {});

      cameraBtn.addEventListener('click', function() {
        cameraInput.value = '';
        cameraInput.click();
      });
      galleryBtn.addEventListener('click', function() {
        galleryInput.value = '';
        galleryInput.click();
      });

      function showStatus(type, text) {
        statusBox.className = 'status-box status-' + type;
        statusBox.textContent = text;
      }

      function compressAndSendFile(file) {
        if (!file) return;
        const t = STRINGS[currentLang] || STRINGS.es;
        showStatus('uploading', t.uploading);

        cameraBtn.disabled = true;
        galleryBtn.disabled = true;
        cameraBtn.style.opacity = '0.6';
        galleryBtn.style.opacity = '0.6';
        if (cameraBtnIcon) cameraBtnIcon.textContent = "⏳";
        cameraBtnText.textContent = currentLang === 'es' ? "Enviando..." : "Sending...";

        // Notify TV immediately that upload/processing started
        fetch('/api/tv-session/' + encodeURIComponent(SESSION_ID) + '/status', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ status: 'uploading' })
        }).catch(function() {});

        const reader = new FileReader();
        reader.onload = function(e) {
          const dataUrl = e.target.result;
          previewImg.src = dataUrl;
          previewImg.style.display = 'block';
          placeholderContent.style.display = 'none';

          const img = new Image();
          img.onload = function() {
            const maxDim = 1024;
            let w = img.width;
            let h = img.height;
            if (w > maxDim || h > maxDim) {
              if (w >= h) {
                h = Math.round((h * maxDim) / w);
                w = maxDim;
              } else {
                w = Math.round((w * maxDim) / h);
                h = maxDim;
              }
            }
            const canvas = document.createElement('canvas');
            canvas.width = w;
            canvas.height = h;
            const ctx = canvas.getContext('2d');
            ctx.fillStyle = '#FFFFFF';
            ctx.fillRect(0, 0, w, h);
            ctx.drawImage(img, 0, 0, w, h);

            const compressedDataUrl = canvas.toDataURL('image/jpeg', 0.85);
            uploadBase64(compressedDataUrl, 'image/jpeg');
          };
          img.onerror = function() {
            uploadBase64(dataUrl, file.type || 'image/jpeg');
          };
          img.src = dataUrl;
        };
        reader.readAsDataURL(file);
      }

      function uploadBase64(dataUrl, mimeType) {
        const t = STRINGS[currentLang] || STRINGS.es;
        fetch('/api/tv-session/' + encodeURIComponent(SESSION_ID) + '/drawing', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            imageBase64: dataUrl,
            mimeType: mimeType
          })
        })
        .then(function(res) { return res.json(); })
        .then(function(data) {
          cameraBtn.disabled = false;
          galleryBtn.disabled = false;
          cameraBtn.style.opacity = '1';
          galleryBtn.style.opacity = '1';
          cameraInput.value = '';
          galleryInput.value = '';

          if (cameraBtnIcon) cameraBtnIcon.textContent = "📸";
          if (data && data.success) {
            showStatus('success', t.success);
            cameraBtnText.textContent = t.sendAnother;
          } else {
            showStatus('error', (data && data.message) || t.errorGeneric);
            cameraBtnText.textContent = t.cameraBtn;
          }
        })
        .catch(function() {
          cameraBtn.disabled = false;
          galleryBtn.disabled = false;
          cameraBtn.style.opacity = '1';
          galleryBtn.style.opacity = '1';
          if (cameraBtnIcon) cameraBtnIcon.textContent = "📸";
          cameraInput.value = '';
          galleryInput.value = '';
          showStatus('error', t.errorGeneric);
          cameraBtnText.textContent = t.cameraBtn;
        });
      }

      cameraInput.addEventListener('change', function(e) {
        if (e.target.files && e.target.files[0]) {
          compressAndSendFile(e.target.files[0]);
        }
      });
      galleryInput.addEventListener('change', function(e) {
        if (e.target.files && e.target.files[0]) {
          compressAndSendFile(e.target.files[0]);
        }
      });

      // Background polling: check if TV session is renewed, reset or requests retry
      setInterval(function() {
        fetch('/api/tv-session/' + encodeURIComponent(SESSION_ID))
          .then(function(res) {
            if (res.status === 404) {
              const t = STRINGS[currentLang] || STRINGS.es;
              showStatus('error', t.sessionExpired);
              tvBadgeText.textContent = currentLang === 'es' ? "⚠️ Código QR renovado en TV" : "⚠️ TV QR Code renewed";
              const pulseDot = document.querySelector('.pulse-dot');
              if (pulseDot) pulseDot.style.background = '#D9384A';
            } else if (res.ok) {
              return res.json();
            }
          })
          .then(function(data) {
            if (data && data.success) {
              const t = STRINGS[currentLang] || STRINGS.es;
              if (data.status === 'retry') {
                showStatus('retry', t.retryNotice);
                if (cameraBtnIcon) cameraBtnIcon.textContent = "📸";
                cameraBtnText.textContent = t.retryBtn;
                cameraBtn.disabled = false;
                galleryBtn.disabled = false;
                cameraBtn.style.opacity = '1';
                galleryBtn.style.opacity = '1';
              } else if (data.status === 'connected' && !data.imageBase64 && (statusBox.className.includes('status-success') || statusBox.className.includes('status-retry'))) {
                statusBox.className = 'status-box';
                statusBox.style.display = 'none';
                if (cameraBtnIcon) cameraBtnIcon.textContent = "📸";
                cameraBtnText.textContent = t.cameraBtn;
                cameraBtn.disabled = false;
                galleryBtn.disabled = false;
                cameraBtn.style.opacity = '1';
                galleryBtn.style.opacity = '1';
              }
            }
          })
          .catch(function() {});
      }, 1500);
    })();
  </script>
</body>
</html>`;
}
