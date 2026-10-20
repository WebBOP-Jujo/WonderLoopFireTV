import { GoogleGenAI } from '@google/genai';
import { CharacterProfile, ColorDef } from '../types/story.types.js';
import { withRetry } from '../utils/retry.js';
import { safeParseJson } from '../utils/safe-json.js';

export interface IVisionAdapter {
  analyzeDrawing(imageBuffer: Buffer, mimeType: string): Promise<CharacterProfile>;
}

const COLOR_MAP_FALLBACKS: Record<string, string> = {
  azul: '#3A86FF',
  celeste: '#4CC9F0',
  turquesa: '#00B4D8',
  rojo: '#E63946',
  coral: '#D9384A',
  rosa: '#FF70A6',
  verde: '#0E7F64',
  amarillo: '#FFD166',
  dorado: '#FFB703',
  naranja: '#FB8500',
  morado: '#7209B7',
  violeta: '#8338EC',
  blanco: '#F8F9FA',
  gris: '#ADB5BD',
  negro: '#212529',
  marron: '#8D5B4C',
  marrón: '#8D5B4C'
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

export class GeminiVisionAdapter implements IVisionAdapter {
  private ai: GoogleGenAI;

  constructor() {
    const apiKey = process.env.GEMINI_API_KEY || '';
    this.ai = new GoogleGenAI({ apiKey });
  }

  async analyzeDrawing(imageBuffer: Buffer, mimeType: string): Promise<CharacterProfile> {
    console.log(`🔍 [GeminiVisionAdapter] Analyzing drawing with gemini-3.5-flash-lite (${imageBuffer.length} bytes, ${mimeType})...`);

    try {
      const prompt = `
Eres el experto analizador de creatividad infantil y visión artística de WonderLoop. 
Analiza la fotografía de este dibujo hecho a mano por un niño en una hoja de papel.

DIRECTRICES DE ANÁLISIS:
1. Concéntrate EXCLUSIVAMENTE en el personaje u objeto dibujado por el niño. Ignora por completo el fondo (mesa, manos, sombras, estuche de lápices, textura del mantel).
2. Si el dibujo es abstracto o esquemático, interpreta su intención con enorme ternura, fantasía e imaginación infantil.
3. DETECCIÓN DE VALIDEZ DEL DIBUJO:
   - "isValidDrawing": true si hay trazos infantiles visibles de un personaje, criatura u objeto.
   - "isValidDrawing": false si la foto está completamente vacía, oscura, desenfocada, o solo apunta a una mesa sin ningún dibujo.
   - "confidence": número entre 0.0 y 1.0 indicando la nitidez y claridad del personaje dibujado.
4. CONSISTENCIA DE COLOR (PALETA HEX):
   - Extrae de 2 a 4 colores dominantes presentes en los trazos de cera, rotulador o lápiz del dibujo.
   - Asigna a cada uno su nombre descriptivo en español y su código Hexadecimal exacto (ej. "#3A86FF", "#FFD166").
5. FORMA Y EXPRESIÓN:
   - Describe el "shapeLanguage" (silueta geométrica: ej. "cuerpo redondeado y esponjoso con orejitas triangulares", "silueta cuadrada de robot con antenas").
   - Describe el "mood" (ej. "alegre, curioso y entusiasta").
6. FONÉTICA INFANTIL:
   - Proporciona "namePhonetic": pronunciación silábica simplificada para motores de voz TTS (ej. "ni-lo", "chis-pi-ta", "po-pi").
7. SEGURIDAD INFANTIL (COPPA / Child-Safe): Si detectas cualquier elemento inapropiado, reinterpreta siempre de forma 100% positiva, pacífica y amigable para niños de 3 a 10 años.

Devuelve estrictamente un objeto JSON válido con la siguiente estructura:
{
  "isValidDrawing": true,
  "confidence": 0.95,
  "name": "Nombre original, dulce y sonoro acorde al dibujo (ej. Nilo, Chispita, Pipo, Blub)",
  "namePhonetic": "ni-lo",
  "species": "Especie o descripción mágica corta (ej. Dragón volador esponjoso, Robot explorador, Monstruito bondadoso)",
  "shapeLanguage": "cuerpo redondeado con alas pequeñas y patitas cortas",
  "mood": "alegre, curioso y juguetón",
  "colorPalette": [
    { "name": "azul celeste", "hex": "#4CC9F0" },
    { "name": "amarillo dorado", "hex": "#FFD166" }
  ],
  "distinctiveFeatures": ["alas redondas de dragón", "ojos grandes y expresivos", "manchitas doradas en la espalda"],
  "personality": "Descripción breve, tierna y divertida de su personalidad (aventurero, curioso, amigo leal)"
}
`;

      const response = await withRetry(
        () =>
          this.ai.models.generateContent({
            model: 'gemini-3.5-flash-lite',
            contents: [
              { text: prompt },
              {
                inlineData: {
                  mimeType: mimeType || 'image/jpeg',
                  data: imageBuffer.toString('base64')
                }
              }
            ],
            config: {
              responseMimeType: 'application/json',
              temperature: 0.35
            }
          }),
        { retries: 3, delayMs: 1500, label: 'Drawing Vision Analysis' }
      );

      const responseText = response.text || '{}';
      const parsed = safeParseJson(responseText);

      // Process and normalize color palette & hex codes
      const rawPalette: any[] = Array.isArray(parsed.colorPalette) ? parsed.colorPalette : [];
      const colorPalette: ColorDef[] = [];
      const colorHexes: string[] = [];
      const primaryColors: string[] = [];

      if (rawPalette.length > 0) {
        for (const item of rawPalette) {
          const colorName = typeof item === 'string' ? item : (item.name || 'color mágico');
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
        colorPalette.push({ name: 'azul mágico', hex: '#3A86FF' }, { name: 'dorado brillante', hex: '#FFD166' });
        colorHexes.push('#3A86FF', '#FFD166');
        primaryColors.push('azul mágico', 'dorado brillante');
      }

      const isValidDrawing = typeof parsed.isValidDrawing === 'boolean' ? parsed.isValidDrawing : true;
      const confidence = typeof parsed.confidence === 'number' ? Math.max(0, Math.min(1, parsed.confidence)) : 0.9;

      return {
        name: parsed.name?.trim().slice(0, 40) || 'Compañero Mágico',
        namePhonetic: parsed.namePhonetic?.trim().toLowerCase().slice(0, 40) || undefined,
        species: parsed.species?.trim().slice(0, 60) || 'Criatura de cuento',
        shapeLanguage: parsed.shapeLanguage?.trim().slice(0, 100) || 'figura redondeada y amigable',
        mood: parsed.mood?.trim().slice(0, 60) || 'alegre y curioso',
        primaryColors: primaryColors.slice(0, 4),
        colorPalette: colorPalette.slice(0, 4),
        colorHexes: colorHexes.slice(0, 4),
        distinctiveFeatures: Array.isArray(parsed.distinctiveFeatures) && parsed.distinctiveFeatures.length > 0
          ? parsed.distinctiveFeatures.map((f: any) => String(f).trim().slice(0, 80)).slice(0, 5)
          : ['ojos curiosos y expresivos', 'gran sonrisa amistosa'],
        personality: parsed.personality?.trim().slice(0, 200) || 'Alegre, bondadoso y listo para vivir grandes aventuras.',
        confidence,
        isValidDrawing,
        originalDrawingBase64: `data:${mimeType};base64,${imageBuffer.toString('base64')}`
      };
    } catch (error: any) {
      console.error('⚠️ [GeminiVisionAdapter] Error analyzing drawing with gemini-3.5-flash-lite:', error?.message || error);
      throw new Error(`Fallo en el análisis del dibujo con Gemini 3.5 Flash-Lite: ${error?.message || 'Error desconocido'}`);
    }
  }
}

