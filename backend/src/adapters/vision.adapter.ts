import { CharacterProfile, ColorDef } from '../types/story.types.js';
import { ILlmClient, getLlmClient } from './llm.client.js';

export interface IVisionAdapter {
  analyzeDrawing(imageBuffer: Buffer, mimeType: string, language?: 'es' | 'en'): Promise<CharacterProfile>;
}

const CHARACTER_PROFILE_SCHEMA = {
  type: 'object',
  properties: {
    isValidDrawing: { type: 'boolean' },
    confidence: { type: 'number', description: '0.0 - 1.0' },
    name: { type: 'string' },
    namePhonetic: { type: 'string' },
    species: { type: 'string' },
    shapeLanguage: { type: 'string' },
    mood: { type: 'string' },
    colorPalette: {
      type: 'array',
      items: {
        type: 'object',
        properties: { name: { type: 'string' }, hex: { type: 'string', description: '#RRGGBB' } },
        required: ['name', 'hex']
      }
    },
    distinctiveFeatures: { type: 'array', items: { type: 'string' } },
    personality: { type: 'string' },
    visualDescriptionEn: { type: 'string', description: 'English, 20-40 words, what the character looks like' }
  },
  required: ['isValidDrawing', 'confidence', 'name', 'species', 'colorPalette', 'distinctiveFeatures', 'personality', 'visualDescriptionEn']
};

const COLOR_MAP_FALLBACKS: Record<string, string> = {
  azul: '#3A86FF', blue: '#3A86FF',
  celeste: '#4CC9F0', 'light blue': '#4CC9F0', sky: '#4CC9F0',
  turquesa: '#00B4D8', turquoise: '#00B4D8',
  rojo: '#E63946', red: '#E63946',
  coral: '#D9384A',
  rosa: '#FF70A6', pink: '#FF70A6',
  verde: '#0E7F64', green: '#0E7F64',
  amarillo: '#FFD166', yellow: '#FFD166',
  dorado: '#FFB703', gold: '#FFB703',
  naranja: '#FB8500', orange: '#FB8500',
  morado: '#7209B7', purple: '#7209B7',
  violeta: '#8338EC', violet: '#8338EC',
  blanco: '#F8F9FA', white: '#F8F9FA',
  gris: '#ADB5BD', gray: '#ADB5BD', grey: '#ADB5BD', silver: '#ADB5BD', plateado: '#ADB5BD',
  negro: '#212529', black: '#212529',
  marron: '#8D5B4C', marrón: '#8D5B4C', brown: '#8D5B4C'
};

function sanitizeHex(hexCandidate?: string, colorName: string = ''): string {
  if (hexCandidate && /^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})$/.test(hexCandidate.trim())) {
    return hexCandidate.trim().toUpperCase();
  }
  const lowerName = colorName.toLowerCase();
  for (const [key, hex] of Object.entries(COLOR_MAP_FALLBACKS)) {
    if (lowerName.includes(key)) {
      return hex;
    }
  }
  return '#3A86FF';
}

/** Names the models fall back to again and again: banned so every drawing gets a fresh name. */
const OVERUSED_NAMES = ['Felicito', 'Mimí', 'Mimi', 'Misu', 'Misi', 'Pipo', 'Nilo', 'Chispita', 'Blub', 'Lulú', 'Lulu', 'Pingui', 'Sparky', 'Bolita'];

function buildVisionPrompt(language: 'es' | 'en'): string {
  const outLanguage = language === 'en' ? 'ENGLISH' : 'SPANISH (from Spain)';
  return `You analyze a photo of a drawing a child (3-10 years old) made on paper. The result becomes the hero of a bedtime story shown on the family TV.

1. FOCUS only on what the child drew. Ignore the table, hands, shadows, pencils or paper texture.
2. VALIDITY:
   - isValidDrawing = true if there are visible child strokes of a character, creature or object (even very simple or abstract ones).
   - isValidDrawing = false if the photo is blank, dark, blurry or shows no drawing at all.
   - confidence: 0.0-1.0, how clearly the drawn character can be seen.
3. INTERPRET with kindness: if it is abstract or scribbly, read it the way the child probably intended. If anything looks inappropriate, reinterpret it in a fully positive, gentle way for young children.
4. COLORS: 2-4 dominant colors actually used in the strokes, each with a short name and its hex code (#RRGGBB) as close as possible to the real crayon/marker color.
5. TEXT FIELDS IN ${outLanguage}: name, species, shapeLanguage, mood, colorPalette names, distinctiveFeatures and personality.
   - species: what it is, in 2-5 plain words a child understands (a little dragon, a robot with wheels, a smiling cat). No "magical" labels.
   - distinctiveFeatures: 2-4 concrete VISIBLE features from the drawing (long curly tail, three eyes, big wheels, striped legs). They will be used in the story, so make them specific.
   - personality: one short sentence a child would like, consistent with the facial expression.
   - shapeLanguage: the silhouette in a few words (round fluffy body with triangle ears).
6. NAME: an ORIGINAL name of 2-3 syllables, easy to say for a small child, inspired by the drawing's look, color or sound (a round orange fish could be "Mango"; a tall green robot "Tuli"). Do not use these overused names: ${OVERUSED_NAMES.join(', ')}.
7. namePhonetic: simple syllables for text-to-speech (e.g. "man-go").
8. visualDescriptionEn: ALWAYS IN ENGLISH, 20-40 words describing exactly how the character looks (body shape, colors, features, expression) so an illustrator can redraw it consistently.`;
}

/**
 * Computer-vision step: turns a photo of the child's paper drawing into a structured CharacterProfile.
 * The model call goes through ILlmClient (Amazon Nova 2 Lite on Bedrock by default, Gemini as fallback).
 */
export class LlmVisionAdapter implements IVisionAdapter {
  constructor(private llm: ILlmClient = getLlmClient()) {}

  async analyzeDrawing(imageBuffer: Buffer, mimeType: string, language: 'es' | 'en' = 'es'): Promise<CharacterProfile> {
    const lang: 'es' | 'en' = language === 'en' ? 'en' : 'es';
    console.log(`🔍 [VisionAdapter] Analyzing drawing with ${this.llm.name} (${imageBuffer.length} bytes, ${mimeType}, ${lang})...`);

    try {
      const parsed = await this.llm.generateJson({
        prompt: buildVisionPrompt(lang),
        image: { bytes: imageBuffer, mimeType: mimeType || 'image/jpeg' },
        temperature: 0.5,
        maxTokens: 1500,
        timeoutMs: 30_000,
        label: 'Drawing Vision Analysis',
        schema: CHARACTER_PROFILE_SCHEMA,
        validate: (p) => typeof p === 'object' && p !== null && (typeof p.name === 'string' || p.isValidDrawing === false)
      });

      // Process and normalize color palette & hex codes
      const rawPalette: any[] = Array.isArray(parsed.colorPalette) ? parsed.colorPalette : [];
      const colorPalette: ColorDef[] = [];
      const colorHexes: string[] = [];
      const primaryColors: string[] = [];

      if (rawPalette.length > 0) {
        for (const item of rawPalette) {
          const colorName = typeof item === 'string' ? item : (item.name || (lang === 'en' ? 'color' : 'color'));
          const hex = sanitizeHex(typeof item === 'object' ? item.hex : undefined, colorName);
          colorPalette.push({ name: colorName, hex });
          colorHexes.push(hex);
          primaryColors.push(colorName);
        }
      } else if (Array.isArray(parsed.primaryColors) && parsed.primaryColors.length > 0) {
        for (const c of parsed.primaryColors) {
          const nameStr = String(c).trim();
          const hex = sanitizeHex(undefined, nameStr);
          colorPalette.push({ name: nameStr, hex });
          colorHexes.push(hex);
          primaryColors.push(nameStr);
        }
      } else {
        const fallback = lang === 'en' ? ['blue', 'yellow'] : ['azul', 'amarillo'];
        colorPalette.push({ name: fallback[0], hex: '#3A86FF' }, { name: fallback[1], hex: '#FFD166' });
        colorHexes.push('#3A86FF', '#FFD166');
        primaryColors.push(...fallback);
      }

      const isValidDrawing = typeof parsed.isValidDrawing === 'boolean' ? parsed.isValidDrawing : true;
      const confidence = typeof parsed.confidence === 'number' ? Math.max(0, Math.min(1, parsed.confidence)) : 0.9;
      const isEn = lang === 'en';

      return {
        name: parsed.name?.trim().slice(0, 40) || (isEn ? 'Buddy' : 'Amigo'),
        namePhonetic: parsed.namePhonetic?.trim().toLowerCase().slice(0, 40) || undefined,
        species: parsed.species?.trim().slice(0, 60) || (isEn ? 'storybook friend' : 'amigo de cuento'),
        shapeLanguage: parsed.shapeLanguage?.trim().slice(0, 100) || (isEn ? 'round, friendly shape' : 'figura redondeada y amigable'),
        mood: parsed.mood?.trim().slice(0, 60) || (isEn ? 'happy and curious' : 'alegre y curioso'),
        primaryColors: primaryColors.slice(0, 4),
        colorPalette: colorPalette.slice(0, 4),
        colorHexes: colorHexes.slice(0, 4),
        distinctiveFeatures: Array.isArray(parsed.distinctiveFeatures) && parsed.distinctiveFeatures.length > 0
          ? parsed.distinctiveFeatures.map((f: any) => String(f).trim().slice(0, 80)).slice(0, 5)
          : (isEn ? ['big curious eyes', 'friendly smile'] : ['ojos grandes y curiosos', 'sonrisa amistosa']),
        personality: parsed.personality?.trim().slice(0, 200) || (isEn ? 'Cheerful, kind and ready for a new adventure.' : 'Alegre, bondadoso y con ganas de vivir una aventura.'),
        visualDescriptionEn: parsed.visualDescriptionEn?.trim().slice(0, 400) || undefined,
        confidence,
        isValidDrawing,
        originalDrawingBase64: `data:${mimeType};base64,${imageBuffer.toString('base64')}`
      };
    } catch (error: any) {
      console.error(`⚠️ [VisionAdapter] Error analyzing drawing with ${this.llm.name}:`, error?.message || error);
      throw new Error(`Fallo en el análisis del dibujo: ${error?.message || 'Error desconocido'}`);
    }
  }
}
