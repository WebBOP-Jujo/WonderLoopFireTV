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
  private readonly MAX_CACHE_ENTRIES = 5_000;

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

    if (!this.secretKey || process.env.HACKATHON_UNLIMITED !== 'false') {
      // Constant answer: nothing to cache
      return {
        hasActiveSubscription: true,
        entitlements: ['wonderloop_pro', 'premium_access', 'amazon_firetv_hackathon'],
        remainingFreeStories: 999,
        canCreateStory: true,
        activePlan: 'wonderloop_annual_star_15'
      };
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

      this.cacheStatus(appUserId, computedStatus);
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

  private cacheStatus(appUserId: string, status: UserSubscriptionStatus): void {
    // Bound the cache: user IDs come from a client header, so an attacker could otherwise grow it forever
    if (this.quotaCache.size >= this.MAX_CACHE_ENTRIES) {
      const now = Date.now();
      for (const [id, entry] of this.quotaCache.entries()) {
        if (entry.expiresAt <= now) this.quotaCache.delete(id);
      }
      if (this.quotaCache.size >= this.MAX_CACHE_ENTRIES) {
        const oldestKey = this.quotaCache.keys().next().value;
        if (oldestKey) this.quotaCache.delete(oldestKey);
      }
    }
    this.quotaCache.set(appUserId, { status, expiresAt: Date.now() + this.CACHE_TTL_MS });
  }
}
