/**
 * Helper for executing async operations with automatic retry on transient errors.
 * Designed for Google Gemini generative AI calls (503 Service Unavailable, 429 Rate Limits, socket timeouts).
 */
export async function withRetry<T>(
  operation: () => Promise<T>,
  options: {
    retries?: number;
    delayMs?: number;
    backoffMultiplier?: number;
    label?: string;
  } = {}
): Promise<T> {
  const {
    retries = 3,
    delayMs = 1500,
    backoffMultiplier = 1.6,
    label = 'Operation'
  } = options;

  let attempt = 0;
  let currentDelay = delayMs;

  while (true) {
    attempt++;
    try {
      return await operation();
    } catch (error: any) {
      const msg = error?.message || String(error);
      const status = error?.status || error?.statusCode;
      const code = error?.code;

      const isTransient =
        status === 503 ||
        status === 429 ||
        code === 'ECONNRESET' ||
        code === 'ETIMEDOUT' ||
        /overloaded|unavailable|high demand|rate limit|fetch failed|socket hang up|deadline exceeded/i.test(msg);

      if (attempt > retries || !isTransient) {
        throw error;
      }

      console.warn(
        `⚠️ [Retry] ${label} attempt ${attempt}/${retries + 1} failed with transient notice (${msg.slice(0, 90)}...). Retrying in ${currentDelay}ms...`
      );
      await new Promise((resolve) => setTimeout(resolve, currentDelay));
      currentDelay = Math.round(currentDelay * backoffMultiplier);
    }
  }
}
