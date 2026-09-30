import { GoogleGenAI } from '@google/genai';
import { CharacterProfile } from '../types/story.types.js';
import { withRetry } from '../utils/retry.js';

export interface IVisionAdapter {
  analyzeDrawing(imageBuffer: Buffer, mimeType: string): Promise<CharacterProfile>;
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
Eres el experto analizador de creatividad infantil de WonderLoop. 
Analiza la fotografía de este dibujo hecho a mano por un niño en una hoja de papel.

DIRECTRICES DE ANÁLISIS:
1. Concéntrate EXCLUSIVAMENTE en el personaje dibujado por el niño. Ignora el fondo (mesa, manos, sombras, estuche de lápices).
2. Si el dibujo es abstracto o sencillo, interpreta su intención con ternura e imaginación infantil.
3. SEGURIDAD INFANTIL (COPPA & Child-Safe): Si detectas cualquier elemento inadecuado, reinterpreta siempre de forma 100% positiva, pacífica y amigable para niños de 4 a 10 años.

Devuelve estrictamente un objeto JSON válido con la siguiente estructura:
{
  "name": "Nombre original y sonoro para el personaje acorde al dibujo (ej. Nilo, Chispita, Pipo, Blub)",
  "species": "Especie o descripción mágica corta (ej. Dragón volador esponjoso, Robot chef sonriente, Monstruito bondadoso)",
  "primaryColors": ["color dominante 1", "color 2", "color 3"],
  "distinctiveFeatures": ["rasgo 1 dibujado (ej. alas redondeadas)", "rasgo 2 (ej. gran sonrisa)", "accesorio visible si lo hay"],
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
              temperature: 0.4
            }
          }),
        { retries: 3, delayMs: 1500, label: 'Drawing Vision Analysis' }
      );

      const responseText = response.text || '{}';
      const cleanJson = responseText.replace(/```json\n?|```/g, '').trim();
      const parsed = JSON.parse(cleanJson);

      return {
        name: parsed.name?.trim() || 'Compañero Mágico',
        species: parsed.species?.trim() || 'Criatura de cuento',
        primaryColors: Array.isArray(parsed.primaryColors) && parsed.primaryColors.length > 0 
          ? parsed.primaryColors 
          : ['azul', 'dorado'],
        distinctiveFeatures: Array.isArray(parsed.distinctiveFeatures) && parsed.distinctiveFeatures.length > 0
          ? parsed.distinctiveFeatures
          : ['ojos curiosos', 'gran sonrisa'],
        personality: parsed.personality?.trim() || 'Alegre, bondadoso y listo para vivir grandes aventuras.',
        originalDrawingBase64: `data:${mimeType};base64,${imageBuffer.toString('base64')}`
      };
    } catch (error: any) {
      console.error('⚠️ [GeminiVisionAdapter] Error analyzing drawing with gemini-3.5-flash-lite:', error?.message || error);
      throw new Error(`Fallo en el análisis del dibujo con Gemini 3.5 Flash-Lite: ${error?.message || 'Error desconocido'}`);
    }
  }
}

