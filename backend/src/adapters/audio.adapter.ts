import { GoogleGenAI } from '@google/genai';
import { Mp3Encoder } from '@breezystack/lamejs';
import { withRetry } from '../utils/retry.js';
import { PollyAudioAdapter } from './polly-audio.adapter.js';

export type VoiceStyle = 'soft' | 'normal' | 'lively';

/** Narration of one page: the audio (data URI) and, when the provider supports it, the start time (ms) of every
 * whitespace-separated word of the page text, used by the TV reader to highlight words as they are spoken. */
export interface NarrationTrack {
  audio: string;
  wordTimingsMs?: number[];
}

export interface IAudioAdapter {
  generateFullStoryAudio(
    scenes: Array<{ pageNumber: number; textContent: string }>,
    language?: 'es' | 'en',
    voiceStyle?: VoiceStyle
  ): Promise<Map<number, NarrationTrack>>;
}

export class GeminiAudioAdapter implements IAudioAdapter {
  private ai: GoogleGenAI;

  constructor() {
    const apiKey = process.env.GEMINI_API_KEY || '';
    this.ai = new GoogleGenAI({ apiKey });
  }

  private getStyleInstruction(language: 'es' | 'en', voiceStyle: VoiceStyle): string {
    const isEnglish = language === 'en';
    if (voiceStyle === 'soft') {
      return isEnglish
        ? 'soft, calm, soothing and gentle bedtime storybook narrator in English. Speak slowly, peacefully and warmly as if reading a child to sleep.'
        : 'narrador de cuentos infantiles en español de España (acento castellano peninsular de España, pronunciación española con distinción de c/z y s). Tono suave, calmado, pausado y relajante para antes de dormir. Habla con dulzura y ritmo tranquilo.';
    }
    if (voiceStyle === 'lively') {
      return isEnglish
        ? 'cheerful, lively, enthusiastic and animated daytime storybook narrator in English. Speak with joyful energy, expressive intonation and playful rhythm for children.'
        : 'narrador de cuentos infantiles en español de España (acento castellano peninsular de España, pronunciación española con distinción de c/z y s). Tono alegre, animado, entusiasta y expresivo de día. Habla con energía divertida y entonación vivaz para niños.';
    }
    return isEnglish
      ? 'warm, gentle, affectionate and expressive storybook narrator in English. Speak clearly with natural cadence for children.'
      : 'narrador de cuentos infantiles en español de España (acento castellano peninsular de España, pronunciación española con distinción de c/z y s). Tono cálido, afectuoso y natural. Habla con claridad y ritmo natural para niños.';
  }

  /**
   * Compresses a 16-bit PCM WAV buffer (typically 24 kHz mono = 384 kbps from Gemini TTS)
   * into a compact 64 kbps mono MP3 buffer (~83% smaller, ~200 KB per page instead of ~1.3 MB)
   * with transparent spoken-word audio quality.
   */
  private convertWavToMp3Base64(
    wavBase64: string,
    targetKbps = 64
  ): { base64: string; mimeType: string; wavBytes: number; outBytes: number } {
    const wavBuf = Buffer.from(wavBase64, 'base64');
    try {
      let numChannels = 1;
      let sampleRate = 24000;
      let bitsPerSample = 16;
      let dataOffset = 44;
      let dataByteLength = Math.max(0, wavBuf.length - 44);

      if (
        wavBuf.length > 44 &&
        wavBuf.toString('ascii', 0, 4) === 'RIFF' &&
        wavBuf.toString('ascii', 8, 12) === 'WAVE'
      ) {
        let offset = 12;
        while (offset + 8 <= wavBuf.length) {
          const chunkId = wavBuf.toString('ascii', offset, offset + 4);
          const chunkSize = wavBuf.readUInt32LE(offset + 4);
          const payloadStart = offset + 8;

          if (chunkId === 'fmt ' && payloadStart + 16 <= wavBuf.length) {
            numChannels = wavBuf.readUInt16LE(payloadStart + 2) || 1;
            sampleRate = wavBuf.readUInt32LE(payloadStart + 4) || 24000;
            bitsPerSample = wavBuf.readUInt16LE(payloadStart + 14) || 16;
          } else if (chunkId === 'data') {
            dataOffset = payloadStart;
            const available = Math.max(0, wavBuf.length - dataOffset);
            dataByteLength = chunkSize > 0 && chunkSize <= available ? chunkSize : available;
            break;
          }

          offset = payloadStart + chunkSize + (chunkSize % 2);
        }
      } else {
        dataOffset = 0;
        dataByteLength = wavBuf.length;
      }

      if (bitsPerSample !== 16 || dataByteLength < 2) {
        return { base64: wavBase64, mimeType: 'audio/wav', wavBytes: wavBuf.length, outBytes: wavBuf.length };
      }

      const numSamples = Math.floor(dataByteLength / 2);
      const pcmSamples = new Int16Array(numSamples);
      for (let i = 0; i < numSamples; i++) {
        pcmSamples[i] = wavBuf.readInt16LE(dataOffset + i * 2);
      }

      const monoSamples =
        numChannels === 2
          ? (() => {
              const m = new Int16Array(Math.floor(numSamples / 2));
              for (let i = 0; i < m.length; i++) {
                m[i] = Math.round((pcmSamples[i * 2] + pcmSamples[i * 2 + 1]) / 2);
              }
              return m;
            })()
          : pcmSamples;

      const encoder = new Mp3Encoder(1, sampleRate, targetKbps);
      const mp3Chunks: Buffer[] = [];
      const blockSize = 1152;

      for (let i = 0; i < monoSamples.length; i += blockSize) {
        const slice = monoSamples.subarray(i, i + blockSize);
        const encoded = encoder.encodeBuffer(slice);
        if (encoded.length > 0) {
          mp3Chunks.push(Buffer.from(encoded.buffer, encoded.byteOffset, encoded.byteLength));
        }
      }

      const flushed = encoder.flush();
      if (flushed.length > 0) {
        mp3Chunks.push(Buffer.from(flushed.buffer, flushed.byteOffset, flushed.byteLength));
      }

      const mp3Buffer = Buffer.concat(mp3Chunks);
      if (mp3Buffer.length > 0 && mp3Buffer.length < wavBuf.length) {
        return {
          base64: mp3Buffer.toString('base64'),
          mimeType: 'audio/mp3',
          wavBytes: wavBuf.length,
          outBytes: mp3Buffer.length
        };
      }
    } catch (err: any) {
      console.warn(`⚠️ [GeminiAudioAdapter] MP3 encoding skipped, falling back to WAV: ${err?.message || err}`);
    }

    return { base64: wavBase64, mimeType: 'audio/wav', wavBytes: wavBuf.length, outBytes: wavBuf.length };
  }

  /**
   * Synthesizes ready-to-play audio for a single storybook page using gemini-3.8-flash-lite-tts
   * and compresses the resulting WAV buffer to 64 kbps mono MP3 (~83% smaller payload).
   */
  private async synthesizeSceneAudio(
    scene: { pageNumber: number; textContent: string },
    language: 'es' | 'en' = 'es',
    voiceStyle: VoiceStyle = 'soft'
  ): Promise<string | undefined> {
    const rawText = scene.textContent?.trim();
    if (!rawText) return undefined;

    // Replace paragraph breaks with Gemini 3.8 TTS inline pause tag
    const transcript = rawText.replace(/\r?\n\s*\r?\n/g, ' <short pause> ');
    const styleInstruction = this.getStyleInstruction(language, voiceStyle);

    try {
      const t0 = Date.now();
      const response = await withRetry(
        () =>
          this.ai.models.generateContent({
            model: 'gemini-3.8-flash-lite-tts',
            contents: [
              {
                role: 'user',
                parts: [
                  {
                    text: transcript,
                    speechMetadata: {
                      style: styleInstruction
                    }
                  }
                ]
              }
            ],
            config: {
              responseModalities: ['AUDIO'],
              speechConfig: {
                languageCode: language === 'en' ? 'en-US' : 'es-ES',
                voiceConfig: {
                  prebuiltVoiceConfig: {
                    voiceName: 'Puck'
                  }
                }
              }
            }
          }),
        { retries: 2, delayMs: 1200, label: `TTS Page ${scene.pageNumber}` }
      );

      const candidate = response.candidates?.[0];
      const audioPart = candidate?.content?.parts?.find((p) => p.inlineData?.data);
      const base64Audio = audioPart?.inlineData?.data;

      if (base64Audio) {
        // Yield to event loop to allow concurrent HTTP polling requests to be processed
        await new Promise((resolve) => setImmediate(resolve));
        const compressed = this.convertWavToMp3Base64(base64Audio, 64);
        const tSec = ((Date.now() - t0) / 1000).toFixed(1);
        const wavKb = Math.round(compressed.wavBytes / 1024);
        const outKb = Math.round(compressed.outBytes / 1024);
        console.log(
          `    ✅ [GeminiAudioAdapter] Page ${scene.pageNumber} synthesized in ${tSec}s (${wavKb} KB WAV -> ${outKb} KB ${compressed.mimeType})`
        );
        return `data:${compressed.mimeType};base64,${compressed.base64}`;
      }

      console.warn(`⚠️ [GeminiAudioAdapter] No audio data returned for Page ${scene.pageNumber}`);
    } catch (error: any) {
      console.warn(`⚠️ [GeminiAudioAdapter] TTS error for Page ${scene.pageNumber}: ${error?.message || error}. Falling back to client TTS if needed.`);
    }

    return undefined;
  }

  /**
   * Parallel Audio Synthesis (1 call per scene concurrently).
   * Executes all pages in parallel with Promise.all using gemini-3.8-flash-lite-tts,
   * which natively outputs complete audio/wav buffers with 44-byte RIFF headers.
   */
  async generateFullStoryAudio(
    scenes: Array<{ pageNumber: number; textContent: string }>,
    language: 'es' | 'en' = 'es',
    voiceStyle: VoiceStyle = 'soft'
  ): Promise<Map<number, NarrationTrack>> {
    const audioMap = new Map<number, NarrationTrack>();
    if (!scenes || scenes.length === 0) return audioMap;

    const pageSummary = scenes.map((s) => `P${s.pageNumber}`).join(', ');
    console.log(`🚀 [GeminiAudioAdapter] Launching parallel TTS synthesis (gemini-3.8-flash-lite-tts): ${scenes.length} pages (${pageSummary}) in ${language.toUpperCase()} [voiceStyle=${voiceStyle}]...`);
    const tStart = Date.now();

    const results = await Promise.all(
      scenes.map(async (scene) => {
        const wavDataUri = await this.synthesizeSceneAudio(scene, language, voiceStyle);
        return { pageNumber: scene.pageNumber, wavDataUri };
      })
    );

    for (const { pageNumber, wavDataUri } of results) {
      if (wavDataUri) {
        audioMap.set(pageNumber, { audio: wavDataUri });
      }
    }

    const totalSec = ((Date.now() - tStart) / 1000).toFixed(1);
    console.log(`🎉 [GeminiAudioAdapter] All ${audioMap.size}/${scenes.length} audio tracks synthesized in ${totalSec}s in parallel!`);
    return audioMap;
  }
}

/**
 * Tries the primary provider first and asks the fallback only for the pages the primary could not narrate,
 * so a single throttled or failed page never costs a full re-synthesis.
 */
export class FallbackAudioAdapter implements IAudioAdapter {
  constructor(
    private primary: IAudioAdapter,
    private fallback: IAudioAdapter,
    private label = 'Audio'
  ) {}

  async generateFullStoryAudio(
    scenes: Array<{ pageNumber: number; textContent: string }>,
    language: 'es' | 'en' = 'es',
    voiceStyle: VoiceStyle = 'soft'
  ): Promise<Map<number, NarrationTrack>> {
    let audioMap = new Map<number, NarrationTrack>();
    try {
      audioMap = await this.primary.generateFullStoryAudio(scenes, language, voiceStyle);
    } catch (error: any) {
      console.warn(`⚠️ [${this.label}] Primary narration provider failed: ${error?.message || error}`);
    }

    const missing = scenes.filter((s) => s.textContent?.trim() && !audioMap.has(s.pageNumber));
    if (missing.length === 0) return audioMap;

    console.log(`🔁 [${this.label}] Falling back for ${missing.length} page(s): ${missing.map((s) => `P${s.pageNumber}`).join(', ')}`);
    try {
      const fallbackMap = await this.fallback.generateFullStoryAudio(missing, language, voiceStyle);
      for (const [pageNumber, track] of fallbackMap) {
        audioMap.set(pageNumber, track);
      }
    } catch (error: any) {
      console.warn(`⚠️ [${this.label}] Fallback narration provider failed: ${error?.message || error}`);
    }
    return audioMap;
  }
}

export type TtsProvider = 'polly' | 'gemini';

export function resolveTtsProvider(): TtsProvider {
  return process.env.TTS_PROVIDER?.trim().toLowerCase() === 'gemini' ? 'gemini' : 'polly';
}

/**
 * TTS_PROVIDER=polly (default): Amazon Polly first, Gemini TTS for any page Polly could not narrate.
 * TTS_PROVIDER=gemini: Gemini TTS only.
 */
export function createAudioAdapter(): IAudioAdapter {
  const gemini = new GeminiAudioAdapter();
  if (resolveTtsProvider() === 'gemini') return gemini;
  return new FallbackAudioAdapter(new PollyAudioAdapter(), gemini, 'Narration');
}

