import { PollyClient, SynthesizeSpeechCommand, Engine, VoiceId, LanguageCode } from '@aws-sdk/client-polly';
import type { IAudioAdapter, NarrationTrack, VoiceStyle } from './audio.adapter.js';

/**
 * Amazon Polly narration adapter.
 *
 * - Neural voices by default: Lucia (es-ES) and Ruth (en-US), overridable via POLLY_VOICE_ES / POLLY_VOICE_EN.
 * - Voice style (soft / normal / lively) is mapped to SSML speaking rate; paragraph breaks become SSML pauses.
 * - Returns MP3 data URIs (audio/mpeg), the same contract as GeminiAudioAdapter, so the Fire TV app
 *   plays and caches them without changes.
 * - Credentials come from the default AWS provider chain (EC2 instance role in production,
 *   env vars or ~/.aws locally). Nothing is hardcoded.
 */

// Polly bills up to 3000 characters per SynthesizeSpeech request; keep headroom for the SSML tags.
const MAX_TEXT_CHARS = 2800;
// After a credentials/config error, stop calling Polly for a while so every story doesn't pay the timeout.
const CIRCUIT_OPEN_MS = 5 * 60 * 1000;

const STYLE_RATES: Record<VoiceStyle, string> = {
  soft: '88%',
  normal: '96%',
  lively: '106%'
};

function normalizeToken(word: string): string {
  return word.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9ñ]/g, '');
}

/**
 * Aligns Polly word speech marks to the words shown on screen (text split on whitespace), so the app can
 * highlight word i once playback reaches timings[i]. Tokens without a mark (dashes, lone punctuation)
 * inherit the previous time.
 */
export function alignWordTimings(displayText: string, marks: Array<{ time: number; value: string }>): number[] {
  const words = displayText.split(/\s+/).filter(Boolean);
  const timings: number[] = [];
  let next = 0;
  let last = 0;
  for (const word of words) {
    const w = normalizeToken(word);
    if (w) {
      for (let k = next; k < Math.min(marks.length, next + 4); k++) {
        const v = normalizeToken(marks[k].value);
        if (v && (w === v || w.startsWith(v) || v.startsWith(w) || w.includes(v))) {
          last = marks[k].time;
          next = k + 1;
          break;
        }
      }
    }
    timings.push(last);
  }
  return timings;
}

function escapeXml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

function isConfigurationError(error: any): boolean {
  const name = String(error?.name || '');
  const msg = String(error?.message || '');
  return (
    name === 'CredentialsProviderError' ||
    name === 'UnrecognizedClientException' ||
    name === 'AccessDeniedException' ||
    name === 'InvalidSignatureException' ||
    /could not load credentials|security token|not authorized/i.test(msg)
  );
}

export class PollyAudioAdapter implements IAudioAdapter {
  private client: PollyClient;
  private engine: Engine;
  private voices: Record<'es' | 'en', VoiceId>;
  private circuitOpenUntil = 0;

  constructor() {
    this.client = new PollyClient({
      region: process.env.AWS_REGION || 'us-east-1',
      maxAttempts: 2
    });
    this.engine = (process.env.POLLY_ENGINE as Engine) || 'neural';
    this.voices = {
      es: (process.env.POLLY_VOICE_ES as VoiceId) || 'Lucia',
      en: (process.env.POLLY_VOICE_EN as VoiceId) || 'Ruth'
    };
  }

  private buildSsml(rawText: string, voiceStyle: VoiceStyle): string {
    const paragraphs = rawText
      .split(/\r?\n\s*\r?\n/)
      .map((p) => escapeXml(p.replace(/\s+/g, ' ').trim()))
      .filter(Boolean);
    const body = paragraphs.join('<break time="700ms"/>');
    return `<speak><prosody rate="${STYLE_RATES[voiceStyle] || '96%'}">${body}</prosody></speak>`;
  }

  private async synthesizeSceneAudio(
    scene: { pageNumber: number; textContent: string },
    language: 'es' | 'en',
    voiceStyle: VoiceStyle
  ): Promise<NarrationTrack | undefined> {
    const rawText = scene.textContent?.trim();
    if (!rawText) return undefined;
    if (rawText.length > MAX_TEXT_CHARS) {
      console.warn(`⚠️ [PollyAudioAdapter] Page ${scene.pageNumber} text too long for one request (${rawText.length} chars), deferring to fallback.`);
      return undefined;
    }
    if (Date.now() < this.circuitOpenUntil) return undefined;

    const t0 = Date.now();
    try {
      const response = await this.client.send(
        new SynthesizeSpeechCommand({
          Engine: this.engine,
          VoiceId: this.voices[language],
          LanguageCode: (language === 'en' ? 'en-US' : 'es-ES') as LanguageCode,
          OutputFormat: 'mp3',
          SampleRate: '24000',
          TextType: 'ssml',
          Text: this.buildSsml(rawText, voiceStyle)
        })
      );

      const bytes = await response.AudioStream?.transformToByteArray();
      if (!bytes || bytes.length === 0) {
        console.warn(`⚠️ [PollyAudioAdapter] Empty audio stream for Page ${scene.pageNumber}`);
        return undefined;
      }

      // Word speech marks (same SSML) for karaoke-style highlighting. Optional: narration works without them.
      let wordTimingsMs: number[] | undefined;
      try {
        const marksResponse = await this.client.send(
          new SynthesizeSpeechCommand({
            Engine: this.engine,
            VoiceId: this.voices[language],
            LanguageCode: (language === 'en' ? 'en-US' : 'es-ES') as LanguageCode,
            OutputFormat: 'json',
            SpeechMarkTypes: ['word'],
            TextType: 'ssml',
            Text: this.buildSsml(rawText, voiceStyle)
          })
        );
        const raw = Buffer.from((await marksResponse.AudioStream?.transformToByteArray()) || []).toString('utf8');
        const marks = raw
          .split('\n')
          .filter((line) => line.trim())
          .map((line) => JSON.parse(line))
          .filter((m) => m.type === 'word')
          .map((m) => ({ time: Number(m.time) || 0, value: String(m.value || '') }));
        if (marks.length > 0) wordTimingsMs = alignWordTimings(rawText, marks);
      } catch (marksError: any) {
        console.warn(`⚠️ [PollyAudioAdapter] Speech marks unavailable for Page ${scene.pageNumber}: ${marksError?.name || marksError}`);
      }

      const kb = Math.round(bytes.length / 1024);
      const tSec = ((Date.now() - t0) / 1000).toFixed(1);
      console.log(`    ✅ [PollyAudioAdapter] Page ${scene.pageNumber} synthesized in ${tSec}s (${this.voices[language]}/${this.engine}, ${kb} KB MP3)`);
      return { audio: `data:audio/mpeg;base64,${Buffer.from(bytes).toString('base64')}`, wordTimingsMs };
    } catch (error: any) {
      if (isConfigurationError(error)) {
        this.circuitOpenUntil = Date.now() + CIRCUIT_OPEN_MS;
        console.warn(`⚠️ [PollyAudioAdapter] AWS credentials/permissions unavailable (${error?.name}). Polly paused for ${CIRCUIT_OPEN_MS / 60000} min.`);
      } else {
        console.warn(`⚠️ [PollyAudioAdapter] TTS error for Page ${scene.pageNumber}: ${error?.name || ''} ${error?.message || error}`);
      }
      return undefined;
    }
  }

  async generateFullStoryAudio(
    scenes: Array<{ pageNumber: number; textContent: string }>,
    language: 'es' | 'en' = 'es',
    voiceStyle: VoiceStyle = 'soft'
  ): Promise<Map<number, NarrationTrack>> {
    const audioMap = new Map<number, NarrationTrack>();
    if (!scenes || scenes.length === 0) return audioMap;

    console.log(`🚀 [PollyAudioAdapter] Synthesizing ${scenes.length} pages with Amazon Polly (${this.voices[language]}, ${this.engine}) in ${language.toUpperCase()} [voiceStyle=${voiceStyle}]...`);
    const tStart = Date.now();

    const results = await Promise.all(
      scenes.map(async (scene) => ({
        pageNumber: scene.pageNumber,
        track: await this.synthesizeSceneAudio(scene, language, voiceStyle)
      }))
    );

    for (const { pageNumber, track } of results) {
      if (track) audioMap.set(pageNumber, track);
    }

    const totalSec = ((Date.now() - tStart) / 1000).toFixed(1);
    console.log(`🎉 [PollyAudioAdapter] ${audioMap.size}/${scenes.length} pages narrated by Amazon Polly in ${totalSec}s`);
    return audioMap;
  }
}
