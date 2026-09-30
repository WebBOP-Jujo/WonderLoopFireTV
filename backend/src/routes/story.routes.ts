import { Router, Request, Response } from 'express';
import multer from 'multer';
import { StoryOrchestratorService } from '../services/story.service.js';
import { RevenueCatService } from '../services/revenuecat.service.js';
import { authenticateApp } from '../middleware/auth.middleware.js';
import { requireQuota } from '../middleware/quota.middleware.js';

const upload = multer({
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB max image
  storage: multer.memoryStorage()
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
          const base64Data = req.body.imageBase64.replace(/^data:image\/\w+;base64,/, '');
          buffer = Buffer.from(base64Data, 'base64');
          if (req.body.mimeType) mimeType = req.body.mimeType;
        } else {
          res.status(400).json({ error: 'Missing drawing image (file or imageBase64 required)' });
          return;
        }

        const artStyle = req.body.artStyle === '3d_animation' ? '3d_animation' : 'watercolor';
        const character = await orchestrator.analyzeDrawing(buffer, mimeType, artStyle);
        res.json({ success: true, character });
      } catch (error: any) {
        console.error('Error in /analyze-drawing:', error);
        res.status(500).json({ error: 'Failed to analyze drawing', details: error.message });
      }
    }
  );

  /**
   * Step 2: Generate full story from an existing character profile
   */
  router.post('/generate', quotaGuard, async (req: Request, res: Response): Promise<void> => {
    try {
      const { character, childName, childAge, theme, language, continuation, voiceStyle, artStyle } = req.body;

      if (!character || !childName || !childAge) {
        res.status(400).json({ error: 'Missing required parameters (character, childName, childAge)' });
        return;
      }

      const normalizedVoice = voiceStyle === 'normal' || voiceStyle === 'lively' ? voiceStyle : 'soft';
      const normalizedArtStyle =
        artStyle === '3d_animation'
          ? '3d_animation'
          : artStyle === 'watercolor'
            ? 'watercolor'
            : undefined;

      const story = await orchestrator.createFullStory(
        character,
        childName,
        childAge,
        theme,
        language === 'en' ? 'en' : 'es',
        continuation,
        normalizedVoice,
        normalizedArtStyle
      );

      res.json({ success: true, story });
    } catch (error: any) {
      console.error('Error in /generate:', error);
      res.status(500).json({ error: 'Failed to generate story', details: error.message });
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
          const base64Data = req.body.imageBase64.replace(/^data:image\/\w+;base64,/, '');
          buffer = Buffer.from(base64Data, 'base64');
          if (req.body.mimeType) mimeType = req.body.mimeType;
        } else {
          res.status(400).json({ error: 'Missing drawing image' });
          return;
        }

        const childName = req.body.childName || 'Pequeño Explorador';
        const childAge = Number(req.body.childAge) || 6;
        const theme = req.body.theme;
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
        console.error('Error in /create-from-drawing:', error);
        res.status(500).json({ error: 'Failed to create story from drawing', details: error.message });
      }
    }
  );

  return router;
}
