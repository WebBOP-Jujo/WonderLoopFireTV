import { GoogleGenAI } from '@google/genai';
import sharp from 'sharp';
import { ArtStyle, CharacterProfile } from '../types/story.types.js';
import { withRetry } from '../utils/retry.js';

const STYLE = {
  watercolor: {
    portrait: "Children's picture-book character portrait, soft warm watercolor on textured paper, gentle pastel washes, delicate ink outlines",
    scene: "Children's picture-book illustration, soft warm watercolor on textured paper, gentle pastel washes, delicate ink outlines"
  },
  '3d_animation': {
    portrait: 'Character portrait in a high-end 3D animated feature-film style, soft clay-like materials, rounded friendly proportions, subsurface scattering',
    scene: 'Scene in a high-end 3D animated feature-film style, soft clay-like materials, rounded friendly proportions, cinematic depth of field'
  }
} as const;

/** One camera framing and one light per page: visual variety, plus a day-to-night arc that suits a bedtime story. */
const PAGE_DIRECTION: Record<number, { shot: string; light: string }> = {
  1: { shot: 'wide establishing shot showing the place and the hero small in the scene', light: 'fresh bright morning light' },
  2: { shot: 'medium shot centered on the action', light: 'clear midday sunlight' },
  3: { shot: 'close-up on the faces and their emotion', light: 'warm afternoon light' },
  4: { shot: 'dynamic low-angle shot of the key moment', light: 'golden-hour glow' },
  5: { shot: 'cozy wide shot with calm space around the characters', light: 'soft dusk with the first stars, warm lights glowing' }
};

const SAFE_FINISH =
  'Friendly, gentle and safe for young children; clean well-formed anatomy and hands; no text, letters, numbers or watermarks anywhere in the image.';

function describeLook(c?: CharacterProfile): string {
  if (!c) return '';
  if (c.visualDescriptionEn) return c.visualDescriptionEn;
  const features = c.distinctiveFeatures?.length ? `, with ${c.distinctiveFeatures.join(', ')}` : '';
  return `${c.species}${features}`;
}

function palette(c?: CharacterProfile): string {
  return c?.colorHexes?.length ? `exact colors ${c.colorHexes.join(', ')}` : '';
}

export interface IImageAdapter {
  generateCharacterPortrait(
    drawingBuffer: Buffer,
    mimeType: string,
    character: CharacterProfile,
    artStyle?: ArtStyle
  ): Promise<string>;
  generateSceneIllustration(
    visualPrompt: string,
    pageNumber: number,
    characterImageBase64?: string,
    companionImageBase64?: string,
    artStyle?: ArtStyle,
    settingBible?: string,
    characterProfile?: CharacterProfile,
    companionProfile?: CharacterProfile
  ): Promise<string>;
}

export class NanobananaImageAdapter implements IImageAdapter {
  private ai: GoogleGenAI;

  constructor() {
    const apiKey = process.env.GEMINI_API_KEY || '';
    this.ai = new GoogleGenAI({ apiKey });
  }

  /**
   * Compresses raw Gemini PNG/JPEG image output (~800 KB - 1 MB) into an optimized
   * JPEG (quality 82, max dimensions configurable: 1024x1024 for 1:1 portraits, 1280x720 for 16:9 TV scenes).
   */
  private async compressToJpegDataUri(
    base64Data: string,
    fallbackMime: string,
    label: string,
    maxWidth: number = 1024,
    maxHeight: number = 1024
  ): Promise<string> {
    try {
      const rawBuf = Buffer.from(base64Data, 'base64');
      const jpegBuf = await sharp(rawBuf)
        .resize({ width: maxWidth, height: maxHeight, fit: 'inside', withoutEnlargement: true })
        .jpeg({ quality: 82, mozjpeg: true })
        .toBuffer();

      if (jpegBuf.length > 0 && jpegBuf.length < rawBuf.length) {
        const inKb = Math.round(rawBuf.length / 1024);
        const outKb = Math.round(jpegBuf.length / 1024);
        console.log(`    🗜️ [GeminiImageAdapter] ${label}: compressed ${inKb} KB -> ${outKb} KB JPEG (${maxWidth}x${maxHeight})`);
        return `data:image/jpeg;base64,${jpegBuf.toString('base64')}`;
      }
    } catch (err: any) {
      console.warn(`⚠️ [GeminiImageAdapter] Image compression skipped for ${label}: ${err?.message || err}`);
    }
    return `data:${fallbackMime};base64,${base64Data}`;
  }

  private parseBase64Part(base64OrDataUrl?: string): any {
    if (!base64OrDataUrl) return null;
    const match = base64OrDataUrl.match(/^data:([^;]+);base64,(.+)$/);
    if (match) {
      return {
        inlineData: {
          mimeType: match[1],
          data: match[2]
        }
      };
    } else if (base64OrDataUrl.length > 100 && !base64OrDataUrl.startsWith('http')) {
      return {
        inlineData: {
          mimeType: 'image/jpeg',
          data: base64OrDataUrl
        }
      };
    }
    return null;
  }

  /**
   * Generates a polished storybook portrait reinterpreting the child's drawing.
   */
  async generateCharacterPortrait(
    drawingBuffer: Buffer,
    mimeType: string,
    character: CharacterProfile,
    artStyle: ArtStyle = 'watercolor'
  ): Promise<string> {
    const is3D = artStyle === '3d_animation';
    console.log(`🎨 [GeminiImageAdapter] Generating character portrait for "${character.name}" (style: ${artStyle}, hex: ${character.colorHexes?.join(', ') || 'n/a'}) with gemini-3.1-flash-lite-image...`);

    const style = STYLE[is3D ? '3d_animation' : 'watercolor'].portrait;
    const prompt = [
      `${style}.`,
      `Redraw the child's drawing in the attached photo as a finished, lovable storybook hero, keeping its identity: the same body shape, the same features and the same colors (${palette(character) || character.primaryColors.join(', ')}).`,
      `The hero is ${character.name}: ${describeLook(character)}.`,
      character.shapeLanguage ? `Silhouette: ${character.shapeLanguage}.` : '',
      `Expression and mood: ${character.mood || 'happy and curious'}; personality: ${character.personality}.`,
      'Center the character, full body, facing the viewer with a warm smile, on a simple soft background vignette.',
      SAFE_FINISH
    ].filter(Boolean).join('\n');

    try {
      const response = await withRetry(
        () =>
          this.ai.models.generateContent({
            model: 'gemini-3.1-flash-lite-image',
            contents: [
              { text: prompt },
              {
                inlineData: {
                  mimeType: mimeType || 'image/jpeg',
                  data: drawingBuffer.toString('base64')
                }
              }
            ],
            config: {
              responseModalities: ['IMAGE'],
              imageConfig: {
                aspectRatio: '1:1'
              }
            }
          }),
        { retries: 3, delayMs: 1500, label: `Character Portrait (${character.name})` }
      );

      const candidate = response.candidates?.[0];
      const imagePart = candidate?.content?.parts?.find((p: any) => p.inlineData?.data);

      if (imagePart?.inlineData?.data) {
        const outMimeType = imagePart.inlineData.mimeType || 'image/png';
        const base64Data = imagePart.inlineData.data;
        console.log(`✅ [GeminiImageAdapter] Character portrait for "${character.name}" generated successfully (${outMimeType})`);
        return await this.compressToJpegDataUri(base64Data, outMimeType, `Portrait (${character.name})`);
      }

      throw new Error('No image data returned for character portrait.');
    } catch (error: any) {
      console.warn(`⚠️ [GeminiImageAdapter] Character portrait fallback to original drawing: ${error?.message?.slice(0, 120)}...`);
      return character.originalDrawingBase64 || `data:${mimeType};base64,${drawingBuffer.toString('base64')}`;
    }
  }

  async generateSceneIllustration(
    visualPrompt: string,
    pageNumber: number,
    characterImageBase64?: string,
    companionImageBase64?: string,
    artStyle: ArtStyle = 'watercolor',
    settingBible?: string,
    characterProfile?: CharacterProfile,
    companionProfile?: CharacterProfile
  ): Promise<string> {
    const hasCompanion = Boolean(companionImageBase64);
    const is3D = artStyle === '3d_animation';
    console.log(`🎨 [GeminiImageAdapter] Generating illustration for page ${pageNumber} (style: ${artStyle}, hasRef: ${Boolean(characterImageBase64)}, hasCompanion: ${hasCompanion}): "${visualPrompt.slice(0, 70)}..."`);

    const characterPart = this.parseBase64Part(characterImageBase64);
    const companionPart = this.parseBase64Part(companionImageBase64);
    const direction = PAGE_DIRECTION[pageNumber] || PAGE_DIRECTION[3];

    const consistency: string[] = [];
    if (characterPart) {
      consistency.push(
        `Reference image 1 is the hero${characterProfile ? `, ${characterProfile.name}` : ''}: draw exactly the same character (same face, body shape, features and colors${palette(characterProfile) ? `: ${palette(characterProfile)}` : ''}).${characterProfile ? ` Look: ${describeLook(characterProfile)}.` : ''}`
      );
    }
    if (companionPart) {
      consistency.push(
        `Reference image 2 is the companion${companionProfile ? `, ${companionProfile.name}` : ''}: keep it identical too${palette(companionProfile) ? ` (${palette(companionProfile)})` : ''}.${companionProfile ? ` Look: ${describeLook(companionProfile)}.` : ''} Show both characters together.`
      );
    }

    const enrichedPrompt = [
      `${STYLE[is3D ? '3d_animation' : 'watercolor'].scene}, wide 16:9 landscape for a living-room TV.`,
      ...consistency,
      settingBible ? `Setting (keep it consistent across the whole story): ${settingBible}.` : '',
      `Moment to illustrate: ${visualPrompt}`,
      `Camera: ${direction.shot}. Lighting: ${direction.light}.`,
      'Composition: full-bleed artwork filling the whole frame edge to edge (no white paper margins, borders or frames); main characters clearly visible and expressive, placed in the upper two-thirds of the frame with their faces well above the bottom quarter; the bottom quarter is calm, simple ground, water or sky with no faces or key details; uncluttered background.',
      SAFE_FINISH
    ].filter(Boolean).join('\n');

    const contents: any[] = [{ text: enrichedPrompt }];
    if (characterPart) contents.push(characterPart);
    if (companionPart) contents.push(companionPart);

    const response = await withRetry(
      () =>
        this.ai.models.generateContent({
          model: 'gemini-3.1-flash-lite-image',
          contents,
          config: {
            responseModalities: ['IMAGE'],
            imageConfig: {
              aspectRatio: '16:9'
            }
          }
        }),
      { retries: 3, delayMs: 1500, label: `Scene Illustration (Page ${pageNumber})` }
    );

    const candidate = response.candidates?.[0];
    const imagePart = candidate?.content?.parts?.find((p: any) => p.inlineData?.data);

    if (imagePart?.inlineData?.data) {
      const mimeType = imagePart.inlineData.mimeType || 'image/png';
      const base64Data = imagePart.inlineData.data;
      console.log(`✅ [GeminiImageAdapter] Page ${pageNumber} 16:9 illustration generated successfully (${mimeType})`);
      return await this.compressToJpegDataUri(base64Data, mimeType, `Page ${pageNumber} (16:9)`, 1280, 720);
    }

    throw new Error(`La respuesta de Gemini Image para la página ${pageNumber} no contenía datos de imagen.`);
  }
}

