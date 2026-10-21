// Must be the first import: ESM evaluates imports before the module body,
// so a later dotenv.config() call would run after other modules already read process.env.
import 'dotenv/config';

import express from 'express';
import cors from 'cors';
import compression from 'compression';
import rateLimit from 'express-rate-limit';
import helmet from 'helmet';
import { StoryOrchestratorService } from './services/story.service.js';
import { RevenueCatService } from './services/revenuecat.service.js';
import { createStoryRouter } from './routes/story.routes.js';
import { createTvSessionApiRouter, createTvUploadWebRouter, detectLanIp } from './routes/tv.routes.js';
import { assertAppSecretConfigured } from './middleware/auth.middleware.js';
import { resolveTtsProvider } from './adapters/audio.adapter.js';
import { getLlmClient } from './adapters/llm.client.js';

try {
  assertAppSecretConfigured();
} catch (err: any) {
  console.error(`🚨 [Security] ${err.message}`);
  process.exit(1);
}

const SPEECH_PROVIDER_LABEL =
  resolveTtsProvider() === 'polly'
    ? `Amazon Polly (${process.env.POLLY_ENGINE || 'neural'}, ${process.env.AWS_REGION || 'us-east-1'}) -> fallback gemini-3.8-flash-lite-tts`
    : 'gemini-3.8-flash-lite-tts';

const app = express();
const PORT = process.env.PORT || 3000;
// Public URL when deployed (e.g. https://api.example.com). When set, internal/LAN addresses are never advertised.
const PUBLIC_BASE_URL = process.env.PUBLIC_BASE_URL?.trim().replace(/\/+$/, '') || '';

// Behind a reverse proxy / load balancer (Caddy, ALB, App Runner, Cloud Run) req.ip would otherwise be
// the proxy's address, making every client share a single rate-limit bucket.
// Set TRUST_PROXY to the number of proxy hops in front of the app (e.g. 1). Leave unset on a bare LAN
// server so clients cannot spoof X-Forwarded-For to dodge the rate limiters.
const trustProxyHops = Number.parseInt(process.env.TRUST_PROXY || '0', 10);
if (Number.isFinite(trustProxyHops) && trustProxyHops > 0) {
  app.set('trust proxy', trustProxyHops);
}

// Security HTTP headers via Helmet
app.use(helmet({ contentSecurityPolicy: false }));

// Enable HTTP response compression (drastically shrinks multi-MB base64 audio and images)
app.use(compression({ threshold: 1024 }));

// Configure CORS
const allowedOrigins = process.env.ALLOWED_ORIGINS
  ? process.env.ALLOWED_ORIGINS.split(',').map((o) => o.trim())
  : null;

app.use(
  cors({
    origin: (origin, callback) => {
      // Allow requests with no origin (mobile apps, Fire TV native, curl, local scripts)
      if (!origin) return callback(null, true);
      if (!allowedOrigins || allowedOrigins.includes(origin) || allowedOrigins.includes('*')) {
        return callback(null, true);
      }
      // In local LAN mode, allow connection
      return callback(null, true);
    },
    credentials: true
  })
);
app.use(express.json({ limit: '15mb' }));
app.use(express.urlencoded({ extended: true, limit: '15mb' }));

// Services
const storyOrchestrator = new StoryOrchestratorService();
const revenueCatService = new RevenueCatService();
const tvSessionApiRouter = createTvSessionApiRouter();
const tvUploadWebRouter = createTvUploadWebRouter();

// Logs every rejected request so rate limiting is visible in production logs.
function rateLimitHandler(name: string) {
  return (req: express.Request, res: express.Response, _next: express.NextFunction, options: { statusCode: number; message: unknown }) => {
    console.warn(`🚦 [RateLimit:${name}] ${req.ip} exceeded the limit on ${req.method} ${req.originalUrl}`);
    res.status(options.statusCode).json(options.message);
  };
}

// 1. Protective rate limiter for heavy Generative AI workloads (anti-abuse)
const aiRateLimiter = rateLimit({
  windowMs: 10 * 60 * 1000, // 10 minutes
  max: 30, // 30 requests per 10 minutes per IP
  standardHeaders: true,
  legacyHeaders: false,
  skip: (req) => req.path === '/health' || req.path === '/',
  handler: rateLimitHandler('ai'),
  message: {
    error: 'RATE_LIMIT_EXCEEDED',
    message: 'Has alcanzado el límite temporal de generación de cuentos. Espera unos minutos.'
  }
});

// 2. TV session polling. Behind a proxy (TRUST_PROXY) every device of a household shares ONE public IP:
//    the Fire TV polls every 1.2 s (~250 req / 5 min) and the phone upload page every 1.5 s (~200 req / 5 min),
//    so the budget must cover several devices polling at once.
const tvSessionRateLimiter = rateLimit({
  windowMs: 5 * 60 * 1000, // 5 minutes
  max: 1500, // ~5 req/s per household
  standardHeaders: true,
  legacyHeaders: false,
  handler: rateLimitHandler('tv-session'),
  message: {
    success: false,
    error: 'TOO_MANY_REQUESTS',
    message: 'Demasiadas peticiones a la sesión de TV. Inténtalo más tarde.'
  }
});

// 2b. Session creation allocates server memory: much stricter than polling (prevents RAM flooding)
const tvSessionCreateLimiter = rateLimit({
  windowMs: 5 * 60 * 1000, // 5 minutes
  max: 40,
  standardHeaders: true,
  legacyHeaders: false,
  handler: rateLimitHandler('tv-session-create'),
  message: {
    success: false,
    error: 'TOO_MANY_REQUESTS',
    message: 'Demasiadas sesiones de TV creadas. Inténtalo en unos minutos.'
  }
});

// 3. Rate limiter for mobile uploads
const tvUploadRateLimiter = rateLimit({
  windowMs: 5 * 60 * 1000, // 5 minutes
  max: 30, // Max 30 photo attempts per 5 minutes per IP
  standardHeaders: true,
  legacyHeaders: false,
  handler: rateLimitHandler('upload'),
  message: {
    success: false,
    error: 'TOO_MANY_REQUESTS',
    message: 'Límite de subidas alcanzado. Por favor, espera unos minutos.'
  }
});

// Routes
app.use('/api/story', aiRateLimiter, createStoryRouter(storyOrchestrator, revenueCatService));
// Drawing uploads decode images with sharp (CPU heavy): apply the stricter upload limiter on top of the session one
app.post(['/api/tv-session/:sessionId/drawing', '/v1/tv-session/:sessionId/drawing'], tvUploadRateLimiter);
app.post(['/api/tv-session', '/v1/tv-session'], tvSessionCreateLimiter);
app.use('/api/tv-session', tvSessionRateLimiter, tvSessionApiRouter);
app.use('/v1/tv-session', tvSessionRateLimiter, tvSessionApiRouter);
app.use('/upload', tvUploadRateLimiter, tvUploadWebRouter);

// Public health endpoint alias
app.get('/health', (_req, res) => {
  res.json({
    status: 'ok',
    service: 'WonderLoop TV Backend',
    ...(PUBLIC_BASE_URL ? {} : { lanIp: detectLanIp() }),
    timestamp: new Date().toISOString()
  });
});

// Root info (Public healthcheck for Cloud Run / load balancers)
app.get('/', (_req, res) => {
  res.json({
    app: 'WonderLoop TV Backend Gateway',
    version: '1.0.0',
    status: 'healthy',
    hackathon: 'Build, Ship, Shape: Amazon Developer Hackathon (Fire TV)',
    ...(PUBLIC_BASE_URL ? { publicUrl: PUBLIC_BASE_URL } : { lanIp: detectLanIp() }),
    models: {
      language: getLlmClient().name,
      image: 'gemini-3.1-flash-lite-image',
      speech: SPEECH_PROVIDER_LABEL
    }
  });
});

const server = app.listen(Number(PORT), '0.0.0.0', () => {
  const lanIp = detectLanIp();
  const endpoints = PUBLIC_BASE_URL
    ? `  🌐 Public URL:     ${PUBLIC_BASE_URL}\n  📱 QR Upload Base: ${PUBLIC_BASE_URL}/upload/<sessionId>`
    : `  📡 Local Server:   http://localhost:${PORT}\n  📺 Fire TV Stick:  http://${lanIp}:${PORT}\n  📱 QR Upload Base: http://${lanIp}:${PORT}/upload/<sessionId>`;
  console.log(`
  ✨ WonderLoop TV Gateway Online!
  ===========================================
${endpoints}
  🎯 Target Event:   Build, Ship, Shape — Amazon Fire TV Hackathon
  🤖 AI Models:
     - Text & Vision: ${getLlmClient().name}
     - Image Gen:     gemini-3.1-flash-lite-image (16:9 Scenes / 1:1 Portraits)
     - Speech (TTS):  ${SPEECH_PROVIDER_LABEL}
  ===========================================
  `);
});

// Configure 5-minute timeout for multi-asset AI synthesis
server.setTimeout(300000);
server.keepAliveTimeout = 300000;

// Graceful shutdown handling for Cloud Run scale-to-zero
process.on('SIGTERM', () => {
  console.log('🛑 [Server] SIGTERM received, shutting down gracefully...');
  server.close(() => {
    console.log('✅ [Server] HTTP server closed.');
    process.exit(0);
  });
});
