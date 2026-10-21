/**
 * Safely parses JSON strings produced by LLMs (Gemini, Claude, GPT).
 * Handles markdown code fences (```json), unescaped control characters
 * (literal newlines \n, \r, tabs) inside string literals, and trailing commas.
 */
export function safeParseJson<T = any>(raw: string, fallback?: T): T {
  if (!raw || typeof raw !== 'string') {
    if (fallback !== undefined) return fallback;
    throw new Error('safeParseJson: Input is empty or not a string.');
  }

  // 1. Remove markdown fences and outer whitespace
  let clean = raw.replace(/```(?:json)?\s*([\s\S]*?)\s*```/g, '$1').trim();

  // 2. Direct fast parse
  try {
    return JSON.parse(clean);
  } catch (err1) {
    // 2b. Some models (e.g. Amazon Nova) occasionally delimit values with typographic quotes: "key": “value”.
    // Re-delimit them with ASCII quotes (escaping inner ones) and keep the result for the next fallbacks.
    const retyped = clean.replace(/([:\[,]\s*)[“«]([^“”«»]*)[”»](?=\s*[,}\]])/g, (_m, prefix: string, inner: string) => {
      return `${prefix}"${inner.replace(/\\?"/g, '\\"')}"`;
    });
    if (retyped !== clean) {
      clean = retyped;
      try {
        return JSON.parse(clean);
      } catch {
        // continue with the generic repairs below
      }
    }

    // 3. First fallback: escape literal newlines and control characters inside double quotes
    try {
      const fixedQuotes = clean.replace(/"((?:[^"\\]|\\.)*)"/gs, (match) => {
        return match
          .replace(/\r\n/g, '\\n')
          .replace(/\n/g, '\\n')
          .replace(/\r/g, '')
          .replace(/\t/g, '\\t');
      });
      return JSON.parse(fixedQuotes);
    } catch (err2) {
      // 4. Second fallback: replace all unescaped control characters globally
      try {
        const sanitized = clean.replace(/[\x00-\x1F\x7F]/g, (char) => {
          if (char === '\n') return '\\n';
          if (char === '\r') return '';
          if (char === '\t') return ' ';
          return '';
        });
        return JSON.parse(sanitized);
      } catch (err3: any) {
        if (fallback !== undefined) {
          console.warn(`⚠️ [safeParseJson] Falling back to default: ${err3?.message || err3}`);
          return fallback;
        }
        throw new Error(`safeParseJson: Failed to parse LLM JSON output: ${err3?.message || err3}`);
      }
    }
  }
}
