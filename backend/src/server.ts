import dotenv from 'dotenv';
dotenv.config();

import express from 'express';
import cors from 'cors';
import compression from 'compression';
import rateLimit from 'express-rate-limit';
import helmet from 'helmet';
import { StoryOrchestratorService } from './services/story.service.js';
import { RevenueCatService } from './services/revenuecat.service.js';
import { createStoryRouter } from './routes/story.routes.js';
import { createRevenueCatRouter } from './routes/revenuecat.routes.js';
import { createTvSessionApiRouter, createTvUploadWebRouter, detectLanIp } from './routes/tv.routes.js';

const app = express();
const PORT = process.env.PORT || 3000;

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

// 1. Protective rate limiter for heavy Generative AI workloads (Cloud Run anti-abuse)
const aiRateLimiter = rateLimit({
  windowMs: 10 * 60 * 1000, // 10 minutes
  max: 30, // 30 requests per 10 minutes per IP
  standardHeaders: true,
  legacyHeaders: false,
  skip: (req) => req.path === '/health' || req.path === '/',
  message: {
    error: 'RATE_LIMIT_EXCEEDED',
    message: 'Has alcanzado el límite temporal de generación de cuentos. Espera unos minutos.'
  }
});

// 2. Protective rate limiter for TV session polling and session creation (prevents RAM flooding)
const tvSessionRateLimiter = rateLimit({
  windowMs: 5 * 60 * 1000, // 5 minutes
  max: 350, // Allows normal 1.2s polling frequency for active sessions
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: 'TOO_MANY_REQUESTS',
    message: 'Demasiadas peticiones a la sesión de TV. Inténtalo más tarde.'
  }
});

// 3. Rate limiter for mobile uploads
const tvUploadRateLimiter = rateLimit({
  windowMs: 5 * 60 * 1000, // 5 minutes
  max: 30, // Max 30 photo attempts per 5 minutes per IP
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: 'TOO_MANY_REQUESTS',
    message: 'Límite de subidas alcanzado. Por favor, espera unos minutos.'
  }
});

// Routes
app.use('/api/story', aiRateLimiter, createStoryRouter(storyOrchestrator, revenueCatService));
app.use('/api/tv-session', tvSessionRateLimiter, tvSessionApiRouter);
app.use('/v1/tv-session', tvSessionRateLimiter, tvSessionApiRouter);
app.use('/upload', tvUploadRateLimiter, tvUploadWebRouter);
app.use('/api/revenuecat', createRevenueCatRouter(revenueCatService));
app.use('/api/subscription', createRevenueCatRouter(revenueCatService));

// Public health endpoint alias
app.get('/health', (_req, res) => {
  res.json({
    status: 'ok',
    service: 'WonderLoop TV Backend',
    lanIp: detectLanIp(),
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
    lanIp: detectLanIp(),
    models: {
      language: 'gemini-3.5-flash-lite',
      image: 'gemini-3.1-flash-lite-image',
      speech: 'gemini-3.8-flash-lite-tts'
    }
  });
});

const server = app.listen(Number(PORT), '0.0.0.0', () => {
  const lanIp = detectLanIp();
  console.log(`
  ✨ WonderLoop TV Gateway Online!
  ===========================================
  📡 Local Server:   http://localhost:${PORT}
  📺 Fire TV Stick:  http://${lanIp}:${PORT}
  📱 QR Upload Base: http://${lanIp}:${PORT}/upload/<sessionId>
  🎯 Target Event:   Build, Ship, Shape — Amazon Fire TV Hackathon
  🤖 AI Models:
     - Text & Vision: gemini-3.5-flash-lite
     - Image Gen:     gemini-3.1-flash-lite-image (16:9 Scenes / 1:1 Portraits)
     - Speech (TTS):  gemini-3.8-flash-lite-tts
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
