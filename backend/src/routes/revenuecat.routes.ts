import { Router, Request, Response } from 'express';
import { RevenueCatService } from '../services/revenuecat.service.js';

export function createRevenueCatRouter(revenueCatService: RevenueCatService): Router {
  const router = Router();

  /**
   * Check entitlement and story creation quota
   */
  router.get('/quota/:userId', async (req: Request, res: Response): Promise<void> => {
    try {
      const userId = String(req.params.userId || '');
      if (!userId) {
        res.status(400).json({ error: 'Missing userId parameter' });
        return;
      }

      const status = await revenueCatService.checkUserQuota(userId);
      res.json({ success: true, ...status });
    } catch (error: any) {
      console.error('Error checking quota:', error);
      res.status(500).json({ error: 'Failed to verify quota', details: error.message });
    }
  });

  /**
   * Webhook handler for RevenueCat Server-to-Server Notifications
   */
  router.post('/webhook', async (req: Request, res: Response): Promise<void> => {
    try {
      await revenueCatService.handleWebhook(req.body);
      res.status(200).json({ received: true });
    } catch (error: any) {
      console.error('Error handling webhook:', error);
      res.status(500).json({ error: 'Webhook processing error' });
    }
  });

  /**
   * Redeem promo/coupon code for discounts or VIP access
   */
  router.post('/redeem', async (req: Request, res: Response): Promise<void> => {
    try {
      const { userId, code } = req.body;
      if (!userId || !code) {
        res.status(400).json({ success: false, message: 'Faltan parámetros userId o code' });
        return;
      }

      const result = await revenueCatService.redeemPromoCode(String(userId), String(code));
      if (!result.success) {
        res.status(400).json(result);
        return;
      }

      res.json(result);
    } catch (error: any) {
      console.error('Error redeeming code:', error);
      res.status(500).json({ success: false, message: 'Error al canjear el código', details: error.message });
    }
  });

  return router;
}
