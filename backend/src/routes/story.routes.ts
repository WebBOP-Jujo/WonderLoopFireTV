import { Router, Request, Response } from 'express';
import multer from 'multer';
import { z } from 'zod';
import { StoryOrchestratorService } from '../services/story.service.js';
import { RevenueCatService } from '../services/revenuecat.service.js';
import { authenticateApp } from '../middleware/auth.middleware.js';
import { requireQuota } from '../middleware/quota.middleware.js';

const ALLOWED_MIME_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);

const upload = multer({
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB max image
  storage: multer.memoryStorage(),
  fileFilter: (_req, file, cb) => {
    if (ALLOWED_MIME_TYPES.has(file.mimetype.toLowerCase())) {
      cb(null, true);
    } else {
      cb(new Error('INVALID_FILE_TYPE: Solo se admiten archivos de imagen (JPEG, PNG o WEBP).'));
    }
  }
});

const GenerateStorySchema = z.object({
  character: z.object({
    name: z.string().trim().min(1, 'El personaje debe tener un nombre').max(60, 'El nombre del personaje no puede exceder 60 caracteres'),
    species: z.string().trim().max(100).optional().default('personaje mágico'),
    primaryColors: z.array(z.string().trim().max(50)).optional().default([]),
    distinctiveFeatures: z.array(z.string().trim().max(150)).optional().default([]),
    personality: z.string().trim().max(250).optional().default('amigable y curioso'),
    reinterpretedImageUrl: z.string().max(7 * 1024 * 1024).nullable().optional(),
    originalDrawingBase64: z.string().max(7 * 1024 * 1024).nullable().optional()
  }).passthrough(),
  childName: z.string().trim().min(1, 'Falta el nombre del niño/a').max(50, 'El nombre no puede exceder 50 caracteres'),
  childAge: z.preprocess(
    (v) => (typeof v === 'string' ? parseInt(v, 10) : v),
    z.number().int().min(2, 'La edad mínima recomendada es 2 años').max(14, 'La edad máxima recomendada es 14 años')
  ),
  theme: z.string().trim().max(300).nullable().optional(),
  language: z.enum(['es', 'en']).optional().default('es'),
  continuation: z.any().nullable().optional(),
  voiceStyle: z.enum(['soft', 'normal', 'lively']).optional().default('soft'),
  artStyle: z.enum(['watercolor', '3d_animation']).nullable().optional()
});

export function createStoryRouter(
  orchestrator: StoryOrchestratorService,
  revenueCatService: RevenueCatService
): Router {
  const router = Router();

  /**
   * Healthcheck (Public, unauthenticated for Cloud Run probes)
   */
  router.get('/health', (_req: Request, res: Response) => {
    res.json({
      status: 'ok',
      service: 'WonderLoop Story Engine',
      timestamp: new Date().toISOString()
    });
  });

  // Protect all downstream story creation routes with App Authentication
  router.use(authenticateApp);
  const quotaGuard = requireQuota(revenueCatService);

  /**
   * Step 1: Analyze drawing only
   */
  router.post(
    '/analyze-drawing',
    quotaGuard,
    upload.single('drawing'),
    async (req: Request, res: Response): Promise<void> => {
      try {
        let buffer: Buffer;
        let mimeType = 'image/jpeg';

        if (req.file) {
          buffer = req.file.buffer;
          mimeType = req.file.mimetype;
        } else if (req.body.imageBase64) {
          const rawBase64 = String(req.body.imageBase64).trim();
          if (rawBase64.length > 7 * 1024 * 1024) {
            res.status(400).json({ error: 'IMAGE_TOO_LARGE', message: 'La imagen excede el límite máximo permitido de 5MB.' });
            return;
          }
          const base64Data = rawBase64.replace(/^data:image\/\w+;base64,/, '');
          buffer = Buffer.from(base64Data, 'base64');
          if (req.body.mimeType && ALLOWED_MIME_TYPES.has(req.body.mimeType.toLowerCase())) {
            mimeType = req.body.mimeType.toLowerCase();
          }
        } else {
          res.status(400).json({ error: 'MISSING_IMAGE', message: 'No se recibió ninguna imagen (archivo o imageBase64 requerido).' });
          return;
        }

        const artStyle = req.body.artStyle === '3d_animation' ? '3d_animation' : 'watercolor';
        const character = await orchestrator.analyzeDrawing(buffer, mimeType, artStyle);
        res.json({ success: true, character });
      } catch (error: any) {
        console.error('❌ [StoryRouter] Error in /analyze-drawing:', error);
        res.status(500).json({
          error: 'FAILED_TO_ANALYZE_DRAWING',
          message: 'No se pudo analizar el dibujo en este momento. Inténtalo de nuevo.'
        });
      }
    }
  );

  /**
   * Step 2: Generate full story from an existing character profile
   */
  router.post('/generate', quotaGuard, async (req: Request, res: Response): Promise<void> => {
    try {
      const parseResult = GenerateStorySchema.safeParse(req.body);
      if (!parseResult.success) {
        console.error('❌ [StoryRouter] Validation failed for /generate:', parseResult.error.errors);
        res.status(400).json({
          error: 'INVALID_REQUEST',
          message: 'Parámetros de historia inválidos o incompletos',
          issues: parseResult.error.errors.map((e) => e.message)
        });
        return;
      }

      const { character, childName, childAge, theme, language, continuation, voiceStyle, artStyle } = parseResult.data;

      const story = await orchestrator.createFullStory(
        character,
        childName,
        childAge,
        theme || undefined,
        language,
        continuation || undefined,
        voiceStyle,
        artStyle || undefined
      );

      res.json({ success: true, story });
    } catch (error: any) {
      console.error('❌ [StoryRouter] Error in /generate:', error);
      res.status(500).json({
        error: 'FAILED_TO_GENERATE_STORY',
        message: 'No se pudo generar el cuento en este momento. Inténtalo de nuevo.'
      });
    }
  });

  /**
   * Step 3: Full pipeline (Drawing -> Story) in a single call
   */
  router.post(
    '/create-from-drawing',
    quotaGuard,
    upload.single('drawing'),
    async (req: Request, res: Response): Promise<void> => {
      try {
        let buffer: Buffer;
        let mimeType = 'image/jpeg';

        if (req.file) {
          buffer = req.file.buffer;
          mimeType = req.file.mimetype;
        } else if (req.body.imageBase64) {
          const rawBase64 = String(req.body.imageBase64).trim();
          if (rawBase64.length > 7 * 1024 * 1024) {
            res.status(400).json({ error: 'IMAGE_TOO_LARGE', message: 'La imagen excede el límite máximo permitido de 5MB.' });
            return;
          }
          const base64Data = rawBase64.replace(/^data:image\/\w+;base64,/, '');
          buffer = Buffer.from(base64Data, 'base64');
          if (req.body.mimeType && ALLOWED_MIME_TYPES.has(req.body.mimeType.toLowerCase())) {
            mimeType = req.body.mimeType.toLowerCase();
          }
        } else {
          res.status(400).json({ error: 'MISSING_IMAGE', message: 'No se recibió la imagen del dibujo.' });
          return;
        }

        const childName = String(req.body.childName || 'Pequeño Explorador').trim();
        const childAge = Number(req.body.childAge) || 6;
        const theme = req.body.theme ? String(req.body.theme).trim() : undefined;
        const language = req.body.language === 'en' ? 'en' : 'es';
        const normalizedVoice = req.body.voiceStyle === 'normal' || req.body.voiceStyle === 'lively' ? req.body.voiceStyle : 'soft';
        const normalizedArtStyle =
          req.body.artStyle === '3d_animation'
            ? '3d_animation'
            : req.body.artStyle === 'watercolor'
              ? 'watercolor'
              : (childAge >= 7 ? '3d_animation' : 'watercolor');

        // 1. Analyze drawing
        const character = await orchestrator.analyzeDrawing(buffer, mimeType, normalizedArtStyle);

        // 2. Generate full story
        const story = await orchestrator.createFullStory(character, childName, childAge, theme, language, undefined, normalizedVoice, normalizedArtStyle);

        res.json({ success: true, story });
      } catch (error: any) {
        console.error('❌ [StoryRouter] Error in /create-from-drawing:', error);
        res.status(500).json({
          error: 'FAILED_TO_CREATE_STORY',
          message: 'No se pudo crear el cuento a partir del dibujo. Inténtalo de nuevo.'
        });
      }
    }
  );

  return router;
}
