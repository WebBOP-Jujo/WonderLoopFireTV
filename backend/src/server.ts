import dotenv from 'dotenv';
dotenv.config();

import express from 'express';
import cors from 'cors';
import compression from 'compression';
import rateLimit from 'express-rate-limit';
import { StoryOrchestratorService } from './services/story.service.js';
import { RevenueCatService } from './services/revenuecat.service.js';
import { createStoryRouter } from './routes/story.routes.js';
import { createRevenueCatRouter } from './routes/revenuecat.routes.js';
import { createTvSessionApiRouter, createTvUploadWebRouter, detectLanIp } from './routes/tv.routes.js';

const app = express();
const PORT = process.env.PORT || 3000;

// Enable HTTP response compression (drastically shrinks multi-MB base64 audio and images)
app.use(compression({ threshold: 1024 }));

app.use(cors());
app.use(express.json({ limit: '15mb' }));
app.use(express.urlencoded({ extended: true, limit: '15mb' }));

// Services
const storyOrchestrator = new StoryOrchestratorService();
const revenueCatService = new RevenueCatService();
const tvSessionApiRouter = createTvSessionApiRouter();
const tvUploadWebRouter = createTvUploadWebRouter();

// Protective rate limiter for heavy Generative AI workloads (Cloud Run anti-abuse)
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

// Routes
app.use('/api/story', aiRateLimiter, createStoryRouter(storyOrchestrator, revenueCatService));
app.use('/api/tv-session', tvSessionApiRouter);
app.use('/v1/tv-session', tvSessionApiRouter);
app.use('/upload', tvUploadWebRouter);
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
