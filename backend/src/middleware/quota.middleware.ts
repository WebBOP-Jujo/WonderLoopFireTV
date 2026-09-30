import { Request, Response, NextFunction } from 'express';
import { RevenueCatService } from '../services/revenuecat.service.js';

/**
 * Middleware that verifies that the user has an active entitlement or available free quota
 * before executing expensive Gemini generative AI calls.
 */
export function requireQuota(revenueCatService: RevenueCatService) {
  return async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    const userId = req.userId;
    if (!userId) {
      res.status(400).json({
        error: 'MISSING_USER_ID',
        message: 'Falta la identificación de usuario en la cabecera (X-User-Id).'
      });
      return;
    }

    try {
      const status = await revenueCatService.checkUserQuota(userId);

      if (!status.canCreateStory && status.remainingFreeStories <= 0) {
        console.warn(`💳 [Quota] User ${userId} blocked by quota gate.`);
        res.status(402).json({
          error: 'QUOTA_EXCEEDED',
          message: 'Has completado tus cuentos de bienvenida. Desbloquea cuentos ilimitados con el Plan Familiar o canjea un cupón.',
          quota: status
        });
        return;
      }

      // User has valid quota or active VIP subscription
      (req as any).subscriptionStatus = status;
      next();
    } catch (error: any) {
      console.error(`⚠️ [Quota] Error checking quota for ${userId}:`, error.message);
      // In case of transient network error with billing, fail-safe to let user continue
      next();
    }
  };
}
