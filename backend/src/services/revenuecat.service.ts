export interface UserSubscriptionStatus {
  hasActiveSubscription: boolean;
  entitlements: string[];
  remainingFreeStories: number;
  canCreateStory: boolean;
  activePlan?: string;
}

interface CacheEntry {
  status: UserSubscriptionStatus;
  expiresAt: number;
}

export class RevenueCatService {
  private secretKey?: string;
  private quotaCache = new Map<string, CacheEntry>();
  private readonly CACHE_TTL_MS = 60_000; // 60s cache TTL

  constructor() {
    this.secretKey = process.env.REVENUECAT_SECRET_KEY;
  }

  /**
   * Check entitlement and story creation quota for a user (with 60s memory cache)
   */
  async checkUserQuota(appUserId: string): Promise<UserSubscriptionStatus> {
    const cached = this.quotaCache.get(appUserId);
    if (cached && cached.expiresAt > Date.now()) {
      return cached.status;
    }

    if (!this.secretKey) {
      console.warn(`💳 [RevenueCatService] REVENUECAT_SECRET_KEY not set. Permitting trial story for user: ${appUserId}`);
      const devStatus: UserSubscriptionStatus = {
        hasActiveSubscription: false,
        entitlements: [],
        remainingFreeStories: 1,
        canCreateStory: true
      };
      this.quotaCache.set(appUserId, { status: devStatus, expiresAt: Date.now() + this.CACHE_TTL_MS });
      return devStatus;
    }


    try {
      // RevenueCat REST API v1 endpoint for checking customer entitlements
      const response = await fetch(`https://api.revenuecat.com/v1/subscribers/${encodeURIComponent(appUserId)}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.secretKey}`,
          'Accept': 'application/json'
        }
      });

      if (!response.ok) {
        console.warn(`⚠️ [RevenueCatService] Failed to query subscriber: ${response.status}`);
        return {
          hasActiveSubscription: false,
          entitlements: [],
          remainingFreeStories: 1,
          canCreateStory: true // Permit 1st free trial story
        };
      }

      const data = await response.json() as any;
      const entitlements = data?.subscriber?.entitlements || {};
      const subscriptions = data?.subscriber?.subscriptions || {};
      const now = new Date();

      const premiumEntitlement = entitlements['wonderloop_pro'] || entitlements['premium_access'];
      const hasPremiumEntitlement = Boolean(
        premiumEntitlement &&
        (premiumEntitlement.expires_date === null || new Date(premiumEntitlement.expires_date) > now)
      );

      const activeSubIds = Object.entries(subscriptions)
        .filter(([, sub]: [string, any]) => sub?.expires_date === null || (sub?.expires_date && new Date(sub.expires_date) > now))
        .map(([subId]) => subId);

      const hasPremium = hasPremiumEntitlement || activeSubIds.length > 0;
      const rawPlanId: string | undefined =
        premiumEntitlement?.product_identifier ||
        activeSubIds[0];

      const normalizedPlan = rawPlanId
        ? rawPlanId.split(':')[0]
        : (hasPremium ? 'wonderloop_annual_star_15' : undefined);

      const monthlyQuota = normalizedPlan?.includes('basic_6') ? 6 : 15;

      const computedStatus: UserSubscriptionStatus = {
        hasActiveSubscription: hasPremium,
        entitlements: Object.keys(entitlements),
        remainingFreeStories: hasPremium ? monthlyQuota : 1,
        canCreateStory: true,
        activePlan: normalizedPlan
      };

      this.quotaCache.set(appUserId, { status: computedStatus, expiresAt: Date.now() + this.CACHE_TTL_MS });
      return computedStatus;
    } catch (error) {
      console.error('⚠️ [RevenueCatService] Error checking subscriber quota:', error);
      return {
        hasActiveSubscription: false,
        entitlements: [],
        remainingFreeStories: 1,
        canCreateStory: true
      };
    }
  }

  /**
   * Redeem a promo/coupon code (e.g. SHIPATON2026, DEVPOSTVIP)
   */
  async redeemPromoCode(appUserId: string, code: string): Promise<{ success: boolean; message: string; status?: UserSubscriptionStatus }> {
    const validCodes = ['SHIPATON2026', 'DEVPOSTVIP', 'WONDERVIP', 'HACKATHON2026'];
    const normalizedCode = (code || '').trim().toUpperCase();

    if (validCodes.includes(normalizedCode)) {
      console.log(`🎉 [RevenueCatService] Valid promo code '${normalizedCode}' redeemed for ${appUserId}`);
      const vipStatus: UserSubscriptionStatus = {
        hasActiveSubscription: true,
        entitlements: ['premium_access', 'family_tier'],
        remainingFreeStories: 9999,
        canCreateStory: true,
        activePlan: 'wonderloop_family_vip'
      };

      // Cache VIP status immediately
      this.quotaCache.set(appUserId, { status: vipStatus, expiresAt: Date.now() + (300_000) });

      return {
        success: true,
        message: '¡Código promocional activado con éxito! Tienes acceso ilimitado.',
        status: vipStatus
      };
    }

    return {
      success: false,
      message: 'Código promocional no válido o caducado.'
    };
  }

  /**
   * Handle Webhooks from RevenueCat dashboard
   */
  async handleWebhook(eventPayload: any): Promise<void> {
    const event = eventPayload?.event;
    const appUserId = event?.app_user_id;
    console.log(`🔔 [RevenueCatService] Received Webhook event: ${event?.type} for user: ${appUserId}`);

    if (appUserId) {
      // Invalidate in-memory cache so next check fetches fresh entitlement state
      this.quotaCache.delete(appUserId);
      console.log(`🧹 [RevenueCatService] Cache invalidated for user ${appUserId}`);
    }
  }
}
