import { Request, Response, NextFunction } from 'express';

// Extend Express Request interface to include userId
declare global {
  namespace Express {
    interface Request {
      userId?: string;
    }
  }
}

const CONFIGURED_SECRET = process.env.APP_SECRET_KEY?.trim();
if (!CONFIGURED_SECRET) {
  if (process.env.NODE_ENV === 'production') {
    console.error('🚨 [Security CRITICAL] APP_SECRET_KEY is not defined in production environment!');
  } else {
    console.warn('⚠️ [Security Notice] APP_SECRET_KEY is not set in .env. Using fallback development key.');
  }
}
const EXPECTED_SECRET_KEY = CONFIGURED_SECRET || 'change-me-to-a-long-random-secret';

/**
 * Middleware that authenticates incoming requests from the WonderLoop mobile application.
 * Verifies the application signature/key and extracts the anonymous user identifier.
 */
export function authenticateApp(req: Request, res: Response, next: NextFunction): void {
  // Allow health checks and public root without authentication
  if (req.path === '/health' || req.path === '/' || req.path === '/api/story/health') {
    return next();
  }

  const appKey = req.headers['x-wonderloop-key'] || req.headers['authorization']?.replace('Bearer ', '');
  const userId = req.headers['x-user-id'] || req.body?.userId || req.query?.userId;

  // 1. Verify App Secret Key (prevents unauthorized external scripts calling Gemini AI)
  if (!appKey || appKey !== EXPECTED_SECRET_KEY) {
    console.warn(`⛔ [Auth] Unauthorized access attempt from IP: ${req.ip} to ${req.originalUrl}`);
    res.status(401).json({
      error: 'UNAUTHORIZED',
      message: 'Cabecera de aplicación inválida o ausente. Acceso denegado.'
    });
    return;
  }

  // 2. Verify and extract User ID (COPPA-compliant anonymous UUID)
  if (!userId || typeof userId !== 'string' || userId.trim() === '') {
    console.warn(`⚠️ [Auth] Missing X-User-Id header from ${req.ip}`);
    res.status(400).json({
      error: 'MISSING_USER_ID',
      message: 'Falta la identificación de usuario en la cabecera (X-User-Id).'
    });
    return;
  }

  req.userId = userId.trim();
  next();
}
