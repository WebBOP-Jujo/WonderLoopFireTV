import { GoogleGenAI } from '@google/genai';
import { ArtStyle, CharacterProfile, CreativeChallenge, StoryContinuationContext, StoryScene } from '../types/story.types.js';
import { withRetry } from '../utils/retry.js';

export interface GeneratedStoryContent {
  title: string;
  scenes: StoryScene[];
  creativeChallenge: CreativeChallenge;
}

export interface IStoryAdapter {
  generateStory(
    character: CharacterProfile,
    childName: string,
    childAge: number,
    theme?: string,
    language?: 'es' | 'en',
    continuation?: StoryContinuationContext,
    artStyle?: ArtStyle
  ): Promise<GeneratedStoryContent>;
}

export class GeminiStoryAdapter implements IStoryAdapter {
  private ai: GoogleGenAI;

  constructor() {
    const apiKey = process.env.GEMINI_API_KEY || '';
    this.ai = new GoogleGenAI({ apiKey });
  }

  async generateStory(
    character: CharacterProfile,
    childName: string,
    childAge: number,
    theme?: string,
    language: 'es' | 'en' = 'es',
    continuation?: StoryContinuationContext,
    artStyle: ArtStyle = 'watercolor'
  ): Promise<GeneratedStoryContent> {
    const isEnglish = language === 'en';
    const isContinuation = Boolean(continuation);
    const is3D = artStyle === '3d_animation';

    // In a saga, the original founder hero (e.g. Toby the dog) remains the eternal protagonist.
    // The newly drawn character is the chapter's companion/helper/vehicle (e.g. Barquito, Pájaro).
    const sagaHero = continuation?.originalHero || continuation?.previousCharacter || character;
    const newCompanion = isContinuation ? character : null;
    const chapterNum = continuation?.chapterIndex || (isContinuation ? 2 : 1);

    console.log(`📖 [GeminiStoryAdapter] Generating Chapter ${chapterNum} in ${isEnglish ? 'ENGLISH' : 'SPANISH'} for saga hero "${sagaHero.name}" with companion "${newCompanion?.name || 'none'}" (Child: ${childName}, Age: ${childAge})...`);

    const ageGuidance = childAge <= 6
      ? (isEnglish
          ? `Age 4-6 bracket: Short, musical sentences with funny sounds. Joyful, sweet and comforting tone. No scary villains or distressing peril. Protagonist explores with wonder.`
          : `Franja 4-6 años: Frases cortas, rítmicas y con sonidos divertidos. Tono alegre, dulce y reconfortante. Sin villanos ni peligros angustiosos. Los personajes descubren el mundo jugando con asombro.`)
      : (isEnglish
          ? `Age 7-10 bracket: Storytelling with lively pacing, clever gentle humor, and rich vocabulary. Small puzzles, ingenuity and camaraderie. Brave decisions, witty dialogue and friendship.`
          : `Franja 7-10 años: Narrativa con mayor ritmo, humor inteligente y vocabulario rico. Pequeños enigmas, espíritu de ingenio, diálogos con chispa y camaradería. Decisiones valientes y cooperación.`);

    const languageDirective = isEnglish
      ? `LANGUAGE REQUIREMENT: Write the "title", the 5 page "textContent" scenes, and the "creativeChallenge" in charming, natural, engaging ENGLISH suited for children. (visualPrompt must remain in English as well).`
      : `DIRECTRIZ DE IDIOMA: Escribe el "title", las 5 escenas de "textContent" y el "creativeChallenge" en ESPAÑOL DE ESPAÑA (castellano peninsular natural, cálido, dulce y divertido para niños). (visualPrompt debe ser en inglés para el generador de imágenes).`;

    // 5 Rotating Narrative Archetypes to permanently prevent repetitive stories
    const NARRATIVE_ARCHETYPES = isEnglish ? [
      {
        name: 'The Whimsical Mystery',
        hook: 'A curious glowing trail, a musical whispering stone, or an upside-down riddle catches the hero by surprise.',
        essence: 'Playful investigation, funny sensory clues, laughter, and a heartwarming discovery.'
      },
      {
        name: 'The Gentle Rescue',
        hook: 'A friendly tiny creature, a lost toy, or a floating lantern needs a kind, clever hand.',
        essence: 'Pure empathy, inventive teamwork without any distressing peril, and mutual celebration.'
      },
      {
        name: 'The Wonder Expedition',
        hook: 'Journeying beyond familiar paths into an extraordinary wonderland (cotton candy clouds, singing bellflowers, glowing crystal hill).',
        essence: 'Sensory awe, vivid textures, funny sound echoes, and joyful discoveries.'
      },
      {
        name: 'The Silly Mischief & Humor',
        hook: 'A quirky playful phenomenon (tickling wind, self-bouncing bubble, a giggling shadow) creates merry chaos.',
        essence: 'Innocent physical comedy, warm banter, playful improvisation, and triumphant giggles.'
      },
      {
        name: 'The Cooperative Crossing',
        hook: 'A natural crossing or playful puzzle that requires combining two different talents to bridge the way.',
        essence: 'Brave encouragement, combining unique strengths, and joyous celebration of friendship.'
      }
    ] : [
      {
        name: 'El Misterio Asombroso',
        hook: 'Un rastro de huellas luminosas, una piedra que canta o una pista brillante despierta la curiosidad del héroe.',
        essence: 'Investigación divertida, pistas sensoriales con gracia y un hallazgo asombroso y reconfortante.'
      },
      {
        name: 'El Rescate Bondadoso',
        hook: 'Una criatura pequeña y simpática o un juguete perdido necesita una ayuda ingeniosa y cariñosa.',
        essence: 'Empatía pura, trabajo en equipo sin peligros angustiosos, y alegría compartida.'
      },
      {
        name: 'La Expedición a un Lugar Increíble',
        hook: 'Un viaje a un rincón asombroso y no convencional (el Valle de las Nubes de Algodón, las Islas de Pompas Gigantes, la Colina de las Flores Cantarinas).',
        essence: 'Asombro visual, texturas curiosas, olores ricos y exploración alegre.'
      },
      {
        name: 'La Travesura Divertida con Humor',
        hook: 'Un fenómeno simpático (un viento que hace cosquillas, pompas que rebotan sin parar, un eco juguetón) provoca risas.',
        essence: 'Comedia infantil inocente, juego dinámico y una solución disparatada pero ingeniosa.'
      },
      {
        name: 'El Gran Cruce en Equipo',
        hook: 'Un obstáculo natural o un río de estrellas que requiere combinar dos habilidades distintas para superarlo.',
        essence: 'Valentía alegre, apoyo mutuo, ánimo entusiasta y triunfo de la amistad.'
      }
    ];

    const archetypeIndex = ((chapterNum - 1) + Math.abs(sagaHero.name.length)) % NARRATIVE_ARCHETYPES.length;
    const selectedArchetype = NARRATIVE_ARCHETYPES[archetypeIndex];

    let sagaContinuationSection = '';
    let visualPromptGuideline = '';
    let titleGuidance = '';

    if (continuation && newCompanion) {
      const prevCompanions = continuation.previousCompanionNames && continuation.previousCompanionNames.length > 0
        ? `Compañeros anteriores de la saga: ${continuation.previousCompanionNames.join(', ')}.`
        : '';

      sagaContinuationSection = `
---
MODO DE CONTINUACIÓN DE SAGA (CAPÍTULO ${chapterNum}):
¡REGLA SUPREMA: ESTE CUENTO ES EL CAPÍTULO ${chapterNum} DE LA SAGA DE "${sagaHero.name}"!

1. ROLES INDISCUTIBLES:
- PROTAGONISTA SUPREMO DE LA SAGA (HÉROE FUNDADOR): "${sagaHero.name}" (${sagaHero.species}, colores: ${sagaHero.primaryColors.join(', ')}, rasgos: ${sagaHero.distinctiveFeatures.join(', ')}, personalidad: ${sagaHero.personality}).
  -> ¡"${sagaHero.name}" es el HÉROE FUNDADOR de la saga y el protagonista activo en TODAS las 5 páginas! La historia se vive a través de sus ojos.
- NUEVO COMPAÑERO / AYUDANTE DIBUJADO EN ESTE CAPÍTULO: "${newCompanion.name}" (${newCompanion.species}, colores: ${newCompanion.primaryColors.join(', ')}, rasgos: ${newCompanion.distinctiveFeatures.join(', ')}, personalidad: ${newCompanion.personality}).
  -> "${newCompanion.name}" es el nuevo aliado/vehículo/amigo que el niño ${childName} acaba de dibujar en papel para responder al reto creativo anterior ("${continuation.challengePrompt}").
${prevCompanions ? `- ${prevCompanions} (Puedes hacer un breve guiño cariñoso en la página 1 recordando el viaje recorrido, ej. dejando el barquito atado en la orilla).` : ''}

2. DESARROLLO DE LA CONTINUIDAD (CAPÍTULO ${chapterNum}):
- PÁGINA 1: "${sagaHero.name}" continúa su camino y se alegra enormemente al ver llegar a "${newCompanion.name}", traído al mundo gracias al dibujo de ${childName}. ¡"${sagaHero.name}" lo recibe con abrazos, asombro y alivio!
- PÁGINAS 2, 3 y 4: "${sagaHero.name}" y "${newCompanion.name}" avanzan juntos. "${sagaHero.name}" lidera con curiosidad mientras "${newCompanion.name}" aporta su habilidad especial o apoyo único para resolver la situación.
- PÁGINA 5: Ambos celebran el éxito con una sonrisa triunfal. Justo en ese momento, surge una nueva necesidad para el siguiente paso del viaje y se lanza el RETO CREATIVO al niño ${childName}.
`;

      titleGuidance = isEnglish
        ? `"${sagaHero.name} & ${newCompanion.name}: [Lively and exciting subtitle] (Chapter ${chapterNum})"`
        : `"${sagaHero.name} y ${newCompanion.name}: [Subtítulo emocionante, divertido y sonoro] (Capítulo ${chapterNum})"`;

      const stylePrefix = is3D
        ? "Wide 16:9 horizontal widescreen 3D animated movie scene in Pixar / Disney animation style featuring BOTH"
        : "Wide 16:9 horizontal widescreen whimsical watercolor children's picture book illustration featuring BOTH";
      const styleSuffix = is3D
        ? "16:9 widescreen cinematic warm volumetric lighting, tactile 3D textures, shallow depth of field, high quality, no text, no words, no letters."
        : "16:9 widescreen panoramic composition, soft warm lighting, pastel textures, storybook art, high quality, no text, no words, no letters.";

      visualPromptGuideline = `
COMO ES UNA CONTINUACIÓN DE SAGA, EN CADA UNA DE LAS 5 PÁGINAS el "visualPrompt" DEBE describir OBLIGATORIAMENTE a AMBOS personajes interactuando juntos en la escena panorámica 16:9:
"${stylePrefix}: 1) ${sagaHero.name}, the protagonist cute ${sagaHero.species} with ${sagaHero.primaryColors.join(' and ')} colors and ${sagaHero.distinctiveFeatures.join(', ')}, AND 2) ${newCompanion.name}, their new companion ${newCompanion.species} with ${newCompanion.primaryColors.join(' and ')} colors and ${newCompanion.distinctiveFeatures.join(', ')}, [setting and interaction]. ${styleSuffix}"
`;
    } else {
      titleGuidance = isEnglish
        ? `Suggestive, charming and musical story title featuring ${sagaHero.name}`
        : `Título sugerente, mágico y sonoro del cuento que incluya a ${sagaHero.name}`;

      const stylePrefix = is3D
        ? "Wide 16:9 horizontal widescreen 3D animated movie scene in Pixar / Disney animation style of"
        : "Wide 16:9 horizontal widescreen whimsical watercolor children's picture book illustration of";
      const styleSuffix = is3D
        ? "16:9 widescreen cinematic warm volumetric lighting, tactile 3D textures, shallow depth of field, high quality, no text, no words, no letters."
        : "16:9 widescreen panoramic composition, soft warm lighting, pastel textures, storybook art, high quality, no text, no words, no letters.";

      visualPromptGuideline = `
En cada una de las 5 páginas, el "visualPrompt" DEBE describir con precisión al protagonista en composición panorámica 16:9:
"${stylePrefix} ${sagaHero.name}, a cute ${sagaHero.species} with ${sagaHero.primaryColors.join(' and ')} colors and ${sagaHero.distinctiveFeatures.join(', ')}, in [setting/action]. ${styleSuffix}"
`;
    }

    const prompt = `
Eres un autor galardonado de literatura infantil y pedagogo creativo para WonderLoop.
Tu misión es escribir un cuento ilustrado entrañable, seguro, vibrante y NADA REPETITIVO de EXACTAMENTE 5 páginas.
${continuation ? `Esta historia une al protagonista de la saga "${sagaHero.name}" con su nuevo aliado creado por el niño: "${newCompanion?.name}".` : `El protagonista del cuento es el personaje que el niño ha dibujado y creado: ${sagaHero.name}.`}

${languageDirective}
${sagaContinuationSection}
${visualPromptGuideline}

---
DATOS DEL PROTAGONISTA DE LA SAGA:
- Nombre: ${sagaHero.name}
- Especie: ${sagaHero.species}
- Colores principales: ${sagaHero.primaryColors.join(', ')}
- Rasgos distintivos: ${sagaHero.distinctiveFeatures.join(', ')}
- Personalidad: ${sagaHero.personality}

DATOS DEL LECTOR Y CONTEXTO:
- Niño creador: ${childName}
- Edad: ${childAge} años
- Pautas pedagógicas:
${ageGuidance}
- Tema o atmósfera sugerida: ${theme || (isEnglish ? 'An exciting adventure of exploration, ingenuity and friendship' : 'Una emocionante aventura de exploración, ingenio y amistad')}

---
DINÁMICA NARRATIVA ASIGNADA PARA ESTE CAPÍTULO:
- Arquetipo: "${selectedArchetype.name}"
- Detonante inicial: ${selectedArchetype.hook}
- Tono y ritmo: ${selectedArchetype.essence}

---
REGLAS DE ORO DE ESTILO (ADIÓS A LOS CLICHÉS Y LA MONOTONÍA):
1. PROHIBICIÓN DE COMIENZOS TÍPICOS: Jamás empieces con "Había una vez", "En un bosque mágico", "En un día soleado" o "Érase una vez". Comienza "in media res": con un sonido intrigante (*¡Cataplum!*), una sorpresa repentina, una pregunta divertida o una acción en marcha.
2. DIÁLOGOS VIVOS EN CADA PÁGINA: Cada página DEBE tener al menos 1 o 2 líneas de diálogo directo entre los personajes con signos de exclamación o pregunta (*—¡Agárrate fuerte, que despegamos! —exclamó...*). Los personajes deben bromear, animarse y mostrar emociones vivas.
3. ONOMATOPEYAS AUDIBLES (ESENCIAL PARA EL AUDIO TTS): Incluye al menos 1 onomatopeya expresiva por página (*¡Fiuuu!*, *¡Chas!*, *¡Plop!*, *¡Toc-toc!*, *¡Cataplum!*, *¡Bip-bip!*, *¡Zas!*, *¡Splash!*). El motor de voz las lee con entonación cómica y hace que el cuento parezca una película animada en vez de un texto plano.
4. RITMO DINÁMICO Y SENSORIAL: Describe texturas y sensaciones divertidas (viento que hace cosquillas en las orejas, hojas crujientes como galletas, burbujas que huelen a fresa).
5. SEGURIDAD INFANTIL (COPPA / G-Rated): Cero violencia, cero miedo angustioso, cero abandono. Siempre valores de bondad, ingenio, amistad y humor blanco.

---
DIRECTRIZ CRÍTICA PARA EL RETO CREATIVO (PÁGINA 5 & creativeChallenge):
1. El reto debe pedir OBLIGATORIAMENTE un elemento MUY CLARO, ICÓNICO Y FÁCIL DE DIBUJAR para un niño de ${childAge} años, con silueta limpia, formas geométricas sencillas y un color vivo identificable.
2. EJEMPLOS PERFECTOS DE RETOS PERMITIDOS:
   - "¡Dibuja un avión amarillo con alas gigantes para cruzar el cañón!"
   - "¡Dibuja un barco de madera con una gran vela azul para navegar el lago!"
   - "¡Dibuja un pájaro simpático con pico naranja para vigilar desde las alturas!"
   - "¡Dibuja un globo aerostático redondo de rayas rojas para flotar sobre las copas de los árboles!"
   - "¡Dibuja una llave mágica dorada con forma de estrella para abrir la puerta de roca!"
   - "¡Dibuja un coche verde con ruedas grandes para recorrer el sendero de piedras!"
   - "¡Dibuja una linterna brillante que lance un rayo de luz en la cueva!"
   - "¡Dibuja un pez sonriente con aletas brillantes para mostrar el camino en el estanque!"
3. PROHIBICIÓN ESTRICTA: NUNCA pidas conceptos complejos, abstractos, o mezclas difíciles (como patinetes con alas de mariposa y hélices, artilugios con mil piezas o máquinas intrincadas). El niño dibuja con ceras en papel y el modelo de visión debe poder reconocer la silueta de inmediato (avión, barco, pájaro, pez, llave, linterna, globo, coche).

---
FORMATO DE RESPUESTA:
Devuelve estrictamente un objeto JSON con la siguiente estructura:
{
  "title": "${titleGuidance}",
  "scenes": [
    {
      "pageNumber": 1,
      "textContent": "Página 1: Entrada in media res con diálogo y onomatopeya...",
      "visualPrompt": "Prompt en inglés para ilustrar la escena 1..."
    },
    {
      "pageNumber": 2,
      "textContent": "Página 2: Avance de la aventura con diálogo activo y humor...",
      "visualPrompt": "Prompt en inglés para la escena 2..."
    },
    {
      "pageNumber": 3,
      "textContent": "Página 3: Superación del reto o investigación con complicidad...",
      "visualPrompt": "Prompt en inglés para la escena 3..."
    },
    {
      "pageNumber": 4,
      "textContent": "Página 4: Clímax de la aventura, trabajo en equipo y alegría...",
      "visualPrompt": "Prompt en inglés para la escena 4..."
    },
    {
      "pageNumber": 5,
      "textContent": "Página 5: Celebración del logro y aparición de la necesidad del reto creativo...",
      "visualPrompt": "Prompt en inglés para la escena 5..."
    }
  ],
  "creativeChallenge": {
    "prompt": "Misión creativa muy clara y sencilla para el niño (ej. ¡Dibuja un avión amarillo con alas gigantes para cruzar el gran cañón!)",
    "targetSubject": "vehicle",
    "encouragement": "Mensaje de motivación lleno de afecto (ej. ¡Tus ceras tienen magia, dale alas a nuestro vuelo!)"
  }
}
`;

    try {
      const response = await withRetry(
        () =>
          this.ai.models.generateContent({
            model: 'gemini-3.5-flash-lite',
            contents: prompt,
            config: {
              responseMimeType: 'application/json',
              temperature: 0.7
            }
          }),
        { retries: 2, delayMs: 1200, label: `Story Text Generation (${sagaHero.name})` }
      );

      const responseText = response.text || '{}';
      const cleanJson = responseText.replace(/```json\n?|```/g, '').trim();
      const parsed = JSON.parse(cleanJson);

      if (!parsed.scenes || !Array.isArray(parsed.scenes) || parsed.scenes.length === 0) {
        throw new Error('La respuesta de Gemini no contiene escenas válidas.');
      }

      // Ensure each scene has proper sequential page numbering
      const scenes: StoryScene[] = parsed.scenes.map((s: any, idx: number) => ({
        pageNumber: idx + 1,
        textContent: s.textContent?.trim() || `Página ${idx + 1} de la aventura de ${sagaHero.name}.`,
        visualPrompt: s.visualPrompt?.trim() || `A whimsical storybook illustration of ${sagaHero.name} the ${sagaHero.species}, watercolor style, no text.`
      }));

      const creativeChallenge: CreativeChallenge = {
        prompt: parsed.creativeChallenge?.prompt?.trim() || `¡${sagaHero.name} necesita tu ayuda! Coge un papel y dibuja un barco velero con una gran vela azul.`,
        targetSubject: parsed.creativeChallenge?.targetSubject?.trim() || 'vehicle',
        encouragement: parsed.creativeChallenge?.encouragement?.trim() || '¡Usa tus colores favoritos para darle vida!'
      };

      const fallbackTitle = continuation && newCompanion
        ? `${sagaHero.name} y ${newCompanion.name} (Capítulo ${chapterNum})`
        : `Las Aventuras de ${sagaHero.name} y ${childName}`;

      return {
        title: parsed.title?.trim() || fallbackTitle,
        scenes,
        creativeChallenge
      };
    } catch (error: any) {
      console.error('⚠️ [GeminiStoryAdapter] Error generating story with gemini-3.5-flash-lite:', error?.message || error);
      throw new Error(`Fallo en la generación del cuento con Gemini 3.5 Flash-Lite: ${error?.message || 'Error desconocido'}`);
    }
  }
}

