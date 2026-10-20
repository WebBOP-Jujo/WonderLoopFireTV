import { Request, Response, NextFunction } from 'express';
import crypto from 'crypto';

// Extend Express Request interface to include userId
declare global {
  namespace Express {
    interface Request {
      userId?: string;
    }
  }
}

const MIN_SECRET_LENGTH = 32;
let expectedSecretDigest: Buffer | null = null;

function sha256(value: string): Buffer {
  return crypto.createHash('sha256').update(value, 'utf8').digest();
}

/**
 * Validates APP_SECRET_KEY at startup. There is intentionally NO fallback key:
 * the server refuses to boot without a strong secret so a public default can never leak into a deploy.
 * Generate one with: node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
 */
export function assertAppSecretConfigured(): void {
  const configured = process.env.APP_SECRET_KEY?.trim();
  if (!configured || configured.length < MIN_SECRET_LENGTH) {
    throw new Error(
      `APP_SECRET_KEY is missing or shorter than ${MIN_SECRET_LENGTH} characters. ` +
        `Set a random secret in backend/.env (and the same value as wonderloop.appKey in local.properties).`
    );
  }
  expectedSecretDigest = sha256(configured);
}

function isValidAppKey(candidate: string): boolean {
  if (!expectedSecretDigest) assertAppSecretConfigured();
  // Compare fixed-length digests in constant time to avoid timing side channels
  return crypto.timingSafeEqual(sha256(candidate), expectedSecretDigest!);
}

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
  if (typeof appKey !== 'string' || !appKey || !isValidAppKey(appKey)) {
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
