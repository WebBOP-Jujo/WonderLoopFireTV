import { GoogleGenAI } from '@google/genai';
import sharp from 'sharp';
import { ArtStyle, CharacterProfile } from '../types/story.types.js';
import { withRetry } from '../utils/retry.js';

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
    artStyle?: ArtStyle
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
   * JPEG (quality 82, max 1024px) of ~110-140 KB (-85% network payload & disk usage).
   */
  private async compressToJpegDataUri(base64Data: string, fallbackMime: string, label: string): Promise<string> {
    try {
      const rawBuf = Buffer.from(base64Data, 'base64');
      const jpegBuf = await sharp(rawBuf)
        .resize({ width: 1024, height: 1024, fit: 'inside', withoutEnlargement: true })
        .jpeg({ quality: 82, mozjpeg: true })
        .toBuffer();

      if (jpegBuf.length > 0 && jpegBuf.length < rawBuf.length) {
        const inKb = Math.round(rawBuf.length / 1024);
        const outKb = Math.round(jpegBuf.length / 1024);
        console.log(`    🗜️ [GeminiImageAdapter] ${label}: compressed ${inKb} KB -> ${outKb} KB JPEG`);
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
    console.log(`🎨 [GeminiImageAdapter] Generating character portrait for "${character.name}" (style: ${artStyle}) with gemini-3.1-flash-lite-image...`);

    const prompt = is3D
      ? `
3D animated movie character portrait in charming Pixar / Disney & tactile soft clay animation style.
Reinterpret the child's drawing provided in the image into a finished, adorable 3D animated movie protagonist.
Character Name: "${character.name}"
Species / Creature: "${character.species}"
Primary Colors: ${character.primaryColors.join(', ')}
Distinctive Features: ${character.distinctiveFeatures.join(', ')}
Personality: ${character.personality}

CRITICAL ARTISTIC GUIDELINES:
- Capture the spirit, warmth, colors, and exact visual identity of the child's original drawing, translated into a high-end 3D animated character.
- Cute rounded 3D proportions, expressive eyes, soft tactile fabric/skin/clay textures, subsurface scattering.
- Center the character with an endearing, friendly expression against a clean, soft warm cinematic bokeh background vignette.
- Absolutely NO text, no letters, no labels, no watermark.
`.trim()
      : `
Children's storybook character portrait in charming watercolor style.
Reinterpret the child's drawing provided in the image into a finished, beautiful storybook protagonist.
Character Name: "${character.name}"
Species / Creature: "${character.species}"
Primary Colors: ${character.primaryColors.join(', ')}
Distinctive Features: ${character.distinctiveFeatures.join(', ')}
Personality: ${character.personality}

CRITICAL ARTISTIC GUIDELINES:
- Capture the spirit, warmth, and exact visual identity of the child's original drawing.
- Center the character with an endearing, friendly expression on a clean, soft warm pastel watercolor background vignette.
- Soft watercolor textures, whimsical picture book art, warm magical lighting.
- Absolutely NO text, no letters, no labels, no watermark.
`.trim();

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
              responseModalities: ['IMAGE']
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
    artStyle: ArtStyle = 'watercolor'
  ): Promise<string> {
    const hasCompanion = Boolean(companionImageBase64);
    const is3D = artStyle === '3d_animation';
    console.log(`🎨 [GeminiImageAdapter] Generating illustration for page ${pageNumber} (style: ${artStyle}, hasRef: ${Boolean(characterImageBase64)}, hasCompanion: ${hasCompanion}): "${visualPrompt.slice(0, 70)}..."`);

    const characterPart = this.parseBase64Part(characterImageBase64);
    const companionPart = this.parseBase64Part(companionImageBase64);

    let consistencyDirective = '';
    if (characterPart && companionPart) {
      consistencyDirective = `CRITICAL DUAL-CHARACTER CONSISTENCY DIRECTIVE:
You MUST depict BOTH characters together in this scene:
- Primary Character (from Reference Image 1): Keep facial expressions, colors, anatomy, and identifying features completely consistent with this hero.
- Companion Character (from Reference Image 2): Keep colors, anatomy, and identifying features completely consistent with this companion.
Show them interacting as a warm, affectionate team in the scene.`;
    } else if (characterPart) {
      consistencyDirective = `CRITICAL CONSISTENCY DIRECTIVE: You MUST depict the exact same protagonist shown in the attached reference image. Keep the character's facial expression, colors, anatomy, and identifying features completely consistent with this hero.`;
    }

    const enrichedPrompt = is3D
      ? `
3D animated movie scene illustration for page ${pageNumber} in charming Pixar / Disney & tactile soft clay animation style.
${consistencyDirective}
Scene: ${visualPrompt}
Style: High-end 3D animated feature film render, cute rounded 3D character proportions, soft tactile textures, subsurface scattering, cinematic warm volumetric lighting, shallow depth of field, vibrant joyful colors.
Absolutely NO text, no words, no letters, no watermark.
`.trim()
      : `
Children's storybook watercolor illustration for page ${pageNumber}.
${consistencyDirective}
Scene: ${visualPrompt}
Style: Whimsical children's picture book watercolor, soft gentle textures, warm magical lighting, pastel tones.
Absolutely NO text, no words, no letters, no watermark.
`.trim();

    const contents: any[] = [{ text: enrichedPrompt }];
    if (characterPart) contents.push(characterPart);
    if (companionPart) contents.push(companionPart);

    const response = await withRetry(
      () =>
        this.ai.models.generateContent({
          model: 'gemini-3.1-flash-lite-image',
          contents,
          config: {
            responseModalities: ['IMAGE']
          }
        }),
      { retries: 3, delayMs: 1500, label: `Scene Illustration (Page ${pageNumber})` }
    );

    const candidate = response.candidates?.[0];
    const imagePart = candidate?.content?.parts?.find((p: any) => p.inlineData?.data);

    if (imagePart?.inlineData?.data) {
      const mimeType = imagePart.inlineData.mimeType || 'image/png';
      const base64Data = imagePart.inlineData.data;
      console.log(`✅ [GeminiImageAdapter] Page ${pageNumber} illustration generated successfully (${mimeType})`);
      return await this.compressToJpegDataUri(base64Data, mimeType, `Page ${pageNumber}`);
    }

    throw new Error(`La respuesta de Gemini Image para la página ${pageNumber} no contenía datos de imagen.`);
  }
}

