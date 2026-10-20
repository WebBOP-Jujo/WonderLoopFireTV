import { GoogleGenAI } from '@google/genai';
import { ArtStyle, ChallengeSubject, CharacterProfile, CreativeChallenge, StoryContinuationContext, StoryScene } from '../types/story.types.js';
import { withRetry } from '../utils/retry.js';
import { safeParseJson } from '../utils/safe-json.js';

export interface GeneratedStoryContent {
  title: string;
  scenes: StoryScene[];
  creativeChallenge: CreativeChallenge;
  settingBible?: string;
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

interface MagicalWorld {
  id: string;
  nameEs: string;
  nameEn: string;
  descriptionEs: string;
  descriptionEn: string;
  visualAnchor: string;
}

const MAGICAL_WORLDS: MagicalWorld[] = [
  {
    id: 'toy_space',
    nameEs: 'El Espacio de las Estrellas',
    nameEn: 'The Starry Space',
    descriptionEs: 'Un viaje entre estrellas doradas que brillan, la luna sonriente y planetas redondos de colores.',
    descriptionEn: 'A fun journey among bright golden stars, a smiling moon, and colorful round planets.',
    visualAnchor: 'Cheerful starry space setting with round colorful planets, a smiling crescent moon, and bright golden twinkling stars'
  },
  {
    id: 'coral_reef',
    nameEs: 'El Fondo del Mar',
    nameEn: 'Under the Sea',
    descriptionEs: 'Un mundo bajo el agua azul con peces de colores, caracolas en la arena y burbujas que suben bailando.',
    descriptionEn: 'A bright underwater world with colorful fish, pretty seashells in golden sand, and dancing bubbles.',
    visualAnchor: 'Sunny underwater ocean setting with friendly colorful fish, pink starfish, golden sand, and floating bubbles'
  },
  {
    id: 'whispering_canopy',
    nameEs: 'El Bosque Encantado',
    nameEn: 'The Enchanted Forest',
    descriptionEs: 'Un bosque verde y soleado con árboles grandes, setas de colores, flores alegres y pajaritos cantores.',
    descriptionEn: 'A sunny green forest with big friendly trees, colorful mushrooms, wildflowers, and singing birds.',
    visualAnchor: 'Sunny green storybook forest setting with big leafy trees, red spotted mushrooms, wildflowers, and warm sunlight'
  },
  {
    id: 'cotton_cloud_kingdom',
    nameEs: 'El Reino de las Nubes',
    nameEn: 'The Cloud Kingdom',
    descriptionEs: 'Un reino suave en el cielo con nubes esponjosas como almohadas, un sol brillante y un arcoíris de colores.',
    descriptionEn: 'A soft kingdom in the sky with fluffy white clouds, a warm smiling sun, and a bright colorful rainbow.',
    visualAnchor: 'Bright blue sky setting with fluffy white clouds, a colorful arching rainbow, and warm golden sunshine'
  },
  {
    id: 'friendly_jurassic',
    nameEs: 'La Isla de los Dinosaurios',
    nameEn: 'Dinosaur Island',
    descriptionEs: 'Una isla verde y divertida con dinosaurios buenos y juguetones, palmeras altas y una laguna de agua limpia.',
    descriptionEn: 'A fun green island with friendly gentle dinosaurs, tall palm trees, and a clear blue lagoon.',
    visualAnchor: 'Lush green island setting with friendly smiling dinosaurs, tall green palm trees, and a sparkling blue lagoon'
  },
  {
    id: 'clockwork_garden',
    nameEs: 'El Jardín de las Flores Gigantes',
    nameEn: 'The Giant Flower Garden',
    descriptionEs: 'Un jardín mágico donde las flores son más altas que una casa, con mariposas de colores y mariquitas simpáticas.',
    descriptionEn: 'A magical garden where flowers are taller than houses, with colorful butterflies and friendly ladybugs.',
    visualAnchor: 'Sunny garden setting with giant colorful daisies and sunflowers, fluttering butterflies, and soft green grass'
  },
  {
    id: 'crystal_echo_cavern',
    nameEs: 'La Cueva de las Piedras Brillantes',
    nameEn: 'The Glowing Crystal Cave',
    descriptionEs: 'Una cueva acogedora e iluminada con piedras de colores que brillan suavemente como lucecitas de noche.',
    descriptionEn: 'A cozy warm cave illuminated with colorful gemstones that glow softly like friendly nightlights.',
    visualAnchor: 'Cozy warm cave setting with glowing colorful gemstones, soft magical sparkles, and smooth clean stone paths'
  }
];

const CHALLENGE_CATEGORIES: Array<{
  category: ChallengeSubject;
  nameEs: string;
  nameEn: string;
  examplesEs: string[];
  examplesEn: string[];
}> = [
  {
    category: 'friend',
    nameEs: 'Un nuevo amiguito o criatura compañera',
    nameEn: 'A new little creature or animal companion',
    examplesEs: [
      '¡Dibuja un pájaro simpático con pico amarillo y plumas brillantes para vigilar desde arriba!',
      '¡Dibuja una oruga luminosa con patitas redondas para iluminar el camino!',
      '¡Dibuja un pececillo saltarín con aletas doradas para guiar el nado!'
    ],
    examplesEn: [
      'Draw a cheerful bird with a yellow beak and bright feathers to watch from above!',
      'Draw a glowing caterpillar with round tiny feet to light up the path!',
      'Draw a bouncy little fish with golden fins to guide the swim!'
    ]
  },
  {
    category: 'accessory',
    nameEs: 'Una herramienta o accesorio mágico',
    nameEn: 'A magical tool or clever accessory',
    examplesEs: [
      '¡Dibuja una linterna mágica con forma de estrella que lance un rayo de luz cálida!',
      '¡Dibuja una llave dorada con alas para abrir la puerta de piedra!',
      '¡Dibuja una capa planeadora con estrellas dibujadas para flotar suavemente!'
    ],
    examplesEn: [
      'Draw a magic star-shaped lantern that shines a warm beam of light!',
      'Draw a golden winged key to unlock the mossy stone gate!',
      'Draw a gliding starry cape to float down gently like a feather!'
    ]
  },
  {
    category: 'shelter',
    nameEs: 'Un refugio o casita acogedora',
    nameEn: 'A cozy shelter or magical outpost',
    examplesEs: [
      '¡Dibuja una cabaña acogedora en un árbol con techo de paja y chimenea sonriente!',
      '¡Dibuja una tienda de campaña de tela de colores protegida de la brisa!',
      '¡Dibuja una casita-seta con puerta redonda para descansar juntos!'
    ],
    examplesEn: [
      'Draw a cozy treehouse cabin with a thatched roof and a cheerful chimney!',
      'Draw a colorful fabric tent sheltered from the gentle breeze!',
      'Draw a cute mushroom cottage with a round door to rest together!'
    ]
  },
  {
    category: 'vehicle',
    nameEs: 'Un vehículo o transporte fantástico',
    nameEn: 'A whimsical vehicle or ride',
    examplesEs: [
      '¡Dibuja un barco de madera con una gran vela azul para cruzar la laguna!',
      '¡Dibuja un trineo de nubes con patines curvos para deslizarse colina abajo!',
      '¡Dibuja un globo aerostático redondo con cesta tejida para volar sobre el valle!'
    ],
    examplesEn: [
      'Draw a wooden boat with a big blue sail to glide across the lagoon!',
      'Draw a cloud sleigh with curved runners to slide down the hill!',
      'Draw a round hot-air balloon with a woven basket to float over the valley!'
    ]
  },
  {
    category: 'snack',
    nameEs: 'Un alimento, fruto o pócima mágica',
    nameEn: 'A magical fruit, treat, or potion',
    examplesEs: [
      '¡Dibuja una fruta mágica brillante en forma de espiral que despierte la risa!',
      '¡Dibuja una galleta con chispas de estrellas doradas para reponer fuerzas!',
      '¡Dibuja una botellita de burbujas de colores con elixir de cosquillas!'
    ],
    examplesEn: [
      'Draw a glowing spiral fruit that makes everyone giggle happily!',
      'Draw a cookie topped with golden star sprinkles to restore energy!',
      'Draw a bottle of sparkling rainbow bubbles with tickle elixir!'
    ]
  }
];

function sanitizeString(input?: string, maxLen: number = 80): string {
  if (!input) return '';
  return input
    .replace(/[\r\n\t]/g, ' ')
    .replace(/[{}<>\\"]/g, '')
    .trim()
    .slice(0, maxLen);
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

    const sagaHero = continuation?.originalHero || continuation?.previousCharacter || character;
    const newCompanion = isContinuation ? character : null;
    const chapterNum = continuation?.chapterIndex || (isContinuation ? 2 : 1);

    const sanitizedChildName = sanitizeString(childName, 30) || (isEnglish ? 'Little Explorer' : 'Pequeño Explorador');
    const sanitizedHeroName = sanitizeString(sagaHero.name, 40) || 'Héroe Mágico';
    const sanitizedCompanionName = newCompanion ? sanitizeString(newCompanion.name, 40) : '';

    // Calculate deterministic hash from hero's name to ensure consistent but varied pseudo-randomness
    const heroHash = Array.from(sanitizedHeroName).reduce((acc, char) => acc + char.charCodeAt(0), 0);

    // 1. Transparent Rich World Selection (no UI friction required)
    const worldIndex = ((chapterNum - 1) * 3 + heroHash) % MAGICAL_WORLDS.length;
    const selectedWorld = MAGICAL_WORLDS[worldIndex];
    const sanitizedCustomTheme = sanitizeString(theme, 120);
    const effectiveTheme = sanitizedCustomTheme.length > 3
      ? sanitizedCustomTheme
      : (isEnglish ? `${selectedWorld.nameEn}: ${selectedWorld.descriptionEn}` : `${selectedWorld.nameEs}: ${selectedWorld.descriptionEs}`);
    const settingBible = selectedWorld.visualAnchor;

    // 2. Anti-Vehicle Bias: Rotating Creative Challenge Category
    const challengeCategoryIndex = ((chapterNum - 1) + heroHash) % CHALLENGE_CATEGORIES.length;
    const selectedCategory = CHALLENGE_CATEGORIES[challengeCategoryIndex];
    const challengeExample = isEnglish ? selectedCategory.examplesEn[0] : selectedCategory.examplesEs[0];

    console.log(`📖 [GeminiStoryAdapter] Generating Chapter ${chapterNum} in ${isEnglish ? 'ENGLISH' : 'SPANISH'} for "${sanitizedHeroName}" (World: "${selectedWorld.nameEn}", Challenge Category: "${selectedCategory.category}", Age: ${childAge})...`);

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

    // 5 Rotating Narrative Archetypes
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
        hook: 'Journeying beyond familiar paths into an extraordinary wonderland.',
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
        hook: 'Un viaje a un rincón asombroso y no convencional de este mundo mágico.',
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

    const archetypeIndex = ((chapterNum - 1) * 2 + heroHash) % NARRATIVE_ARCHETYPES.length;
    const selectedArchetype = NARRATIVE_ARCHETYPES[archetypeIndex];

    const heroColors = (sagaHero.primaryColors && sagaHero.primaryColors.length > 0)
      ? sagaHero.primaryColors.join(', ')
      : 'azul y dorado';
    const heroTraits = (sagaHero.distinctiveFeatures && sagaHero.distinctiveFeatures.length > 0)
      ? sagaHero.distinctiveFeatures.join(', ')
      : 'ojos expresivos y sonrisa amistosa';

    let sagaContinuationSection = '';
    let visualPromptGuideline = '';
    let titleGuidance = '';

    const stylePrefix = is3D
      ? 'Wide 16:9 horizontal widescreen 3D animated movie scene in Pixar / Disney animation style'
      : "Wide 16:9 horizontal widescreen whimsical watercolor children's picture book illustration";
    const styleSuffix = is3D
      ? '16:9 widescreen cinematic warm volumetric studio lighting, tactile 3D textures, shallow depth of field, high quality, no text, no words, no letters.'
      : '16:9 widescreen panoramic composition, soft warm paper grain, gentle pastel brushstrokes, whimsical picture book art, warm magical lighting, high quality, no text, no words, no letters.';

    if (continuation && newCompanion) {
      const companionColors = (newCompanion.primaryColors && newCompanion.primaryColors.length > 0)
        ? newCompanion.primaryColors.join(', ')
        : 'colores mágicos';
      const companionTraits = (newCompanion.distinctiveFeatures && newCompanion.distinctiveFeatures.length > 0)
        ? newCompanion.distinctiveFeatures.join(', ')
        : 'rasgos entrañables';

      const prevCompanions = continuation.previousCompanionNames && continuation.previousCompanionNames.length > 0
        ? (isEnglish
            ? `Previous saga companions: ${continuation.previousCompanionNames.join(', ')}.`
            : `Compañeros anteriores de la saga: ${continuation.previousCompanionNames.join(', ')}.`)
        : '';

      const lastSceneBridge = continuation.lastSceneText
        ? (isEnglish
            ? `CLIMAX OF THE PREVIOUS CHAPTER (MANDATORY BRIDGE):\n"${sanitizeString(continuation.lastSceneText, 300)}"\nPage 1 MUST bridge seamlessly from this ending, showing how "${sanitizedCompanionName}"'s arrival helps "${sanitizedHeroName}" advance with joy!\n`
            : `CLÍMAX DEL CAPÍTULO ANTERIOR (ENLACE OBLIGATORIO):\n"${sanitizeString(continuation.lastSceneText, 300)}"\nLa Página 1 DEBE enlazar directamente y de forma fluida con este desenlace, mostrando cómo la llegada de "${sanitizedCompanionName}" ayuda a "${sanitizedHeroName}" a continuar con entusiasmo!\n`)
        : '';

      sagaContinuationSection = isEnglish
        ? `
---
SAGA CONTINUATION MODE (CHAPTER ${chapterNum}):
SUPREME RULE: THIS STORY IS CHAPTER ${chapterNum} OF THE ONGOING SAGA OF "${sanitizedHeroName}"!

1. ROLES:
- SUPREME SAGA PROTAGONIST (FOUNDING HERO): "${sanitizedHeroName}" (${sagaHero.species}, colors: ${heroColors}, traits: ${heroTraits}, personality: ${sagaHero.personality}).
  -> "${sanitizedHeroName}" is the founding hero and active protagonist in ALL 5 pages!
- NEW CHAPTER ALLY DRAWN BY THE CHILD: "${sanitizedCompanionName}" (${newCompanion.species}, colors: ${companionColors}, traits: ${companionTraits}, personality: ${newCompanion.personality}).
  -> "${sanitizedCompanionName}" was drawn by ${sanitizedChildName} in response to the previous creative challenge ("${sanitizeString(continuation.challengePrompt, 150)}").
${prevCompanions ? `- ${prevCompanions}` : ''}

${lastSceneBridge}
2. CHAPTER DEVELOPMENT:
- PAGE 1: "${sanitizedHeroName}" continues the journey and warmly welcomes "${sanitizedCompanionName}"!
- PAGES 2, 3 and 4: Both friends explore together, overcoming playful hurdles with camaraderie and wit.
- PAGE 5: Triumphant celebration, followed by a new need that sparks the new CREATIVE CHALLENGE for ${sanitizedChildName}.
`
        : `
---
MODO DE CONTINUACIÓN DE SAGA (CAPÍTULO ${chapterNum}):
¡REGLA SUPREMA: ESTE CUENTO ES EL CAPÍTULO ${chapterNum} DE LA SAGA DE "${sanitizedHeroName}"!

1. ROLES INDISCUTIBLES:
- PROTAGONISTA SUPREMO DE LA SAGA (HÉROE FUNDADOR): "${sanitizedHeroName}" (${sagaHero.species}, colores: ${heroColors}, rasgos: ${heroTraits}, personalidad: ${sagaHero.personality}).
  -> ¡"${sanitizedHeroName}" es el HÉROE FUNDADOR de la saga y el protagonista activo en TODAS las 5 páginas! La historia se vive a través de sus ojos.
- NUEVO COMPAÑERO / AYUDANTE DIBUJADO EN ESTE CAPÍTULO: "${sanitizedCompanionName}" (${newCompanion.species}, colores: ${companionColors}, rasgos: ${companionTraits}, personalidad: ${newCompanion.personality}).
  -> "${sanitizedCompanionName}" es el nuevo aliado/criatura/objeto que el niño ${sanitizedChildName} acaba de dibujar en papel para responder al reto creativo anterior ("${sanitizeString(continuation.challengePrompt, 150)}").
${prevCompanions ? `- ${prevCompanions}` : ''}

${lastSceneBridge}
2. DESARROLLO DE LA CONTINUIDAD (CAPÍTULO ${chapterNum}):
- PÁGINA 1: "${sanitizedHeroName}" continúa su camino y se alegra enormemente al ver llegar a "${sanitizedCompanionName}", traído al mundo gracias al dibujo de ${sanitizedChildName}. ¡"${sanitizedHeroName}" lo recibe con abrazos y alivio!
- PÁGINAS 2, 3 y 4: "${sanitizedHeroName}" y "${sanitizedCompanionName}" avanzan juntos resolviendo situaciones con ingenio y apoyo mutuo.
- PÁGINA 5: Ambos celebran el éxito con una sonrisa triunfal. Justo en ese momento, surge una nueva necesidad para el siguiente paso del viaje y se lanza el RETO CREATIVO al niño ${sanitizedChildName}.
`;

      titleGuidance = isEnglish
        ? `"${sanitizedHeroName} & ${sanitizedCompanionName}: [Catchy subtitle] (Chapter ${chapterNum})"`
        : `"${sanitizedHeroName} y ${sanitizedCompanionName}: [Subtítulo divertido y sonoro] (Capítulo ${chapterNum})"`;

      visualPromptGuideline = `
EN CADA UNA DE LAS 5 PÁGINAS el "visualPrompt" DEBE describir OBLIGATORIAMENTE a AMBOS personajes interactuando juntos en composición panorámica 16:9 con el siguiente entorno ambiental:
Environment Anchor (Setting Bible): "${settingBible}"
Plantilla de escena: "${stylePrefix} of 1) ${sanitizedHeroName}, the cute ${sagaHero.species} with ${heroColors} colors and ${heroTraits}, AND 2) ${sanitizedCompanionName}, the new companion ${newCompanion.species} with ${companionColors} colors and ${companionTraits}, [characters action/interaction], set in ${settingBible}. ${styleSuffix}"
`;
    } else {
      titleGuidance = isEnglish
        ? `Suggestive, charming and musical story title featuring ${sanitizedHeroName}`
        : `Título sugerente, mágico y sonoro del cuento que incluya a ${sanitizedHeroName}`;

      visualPromptGuideline = `
En cada una de las 5 páginas, el "visualPrompt" DEBE describir con precisión al protagonista en composición panorámica 16:9 con el siguiente entorno ambiental:
Environment Anchor (Setting Bible): "${settingBible}"
Plantilla de escena: "${stylePrefix} of ${sanitizedHeroName}, a cute ${sagaHero.species} with ${heroColors} colors and ${heroTraits}, [character action], set in ${settingBible}. ${styleSuffix}"
`;
    }

    const wordCountGuideline = childAge <= 6
      ? (isEnglish
          ? 'WORD COUNT LIMIT: Strictly 40 to 55 words per page (1-2 short, breezy paragraphs). Perfect for young children and 25s voice narration.'
          : 'LÍMITE DE PALABRAS: Estrictamente 40 a 55 palabras por página (1-2 párrafos cortos y rítmicos). Perfecto para niños de 3-6 años y locución de 25s.')
      : (isEnglish
          ? 'WORD COUNT LIMIT: Strictly 65 to 85 words per page (2-3 short, dynamic paragraphs). Perfect for readers of 7-10 years and 35s voice narration.'
          : 'LÍMITE DE PALABRAS: Estrictamente 65 a 85 palabras por página (2-3 párrafos dinámicos). Perfecto para lectores de 7-10 años y locución de 35s.');

    const prompt = `
Eres un autor galardonado de literatura infantil y pedagogo creativo para WonderLoop.
Tu misión es escribir un cuento ilustrado entrañable, seguro, vibrante y NADA REPETITIVO de EXACTAMENTE 5 páginas adaptado a televisores de salón (10-Foot UI).
${continuation ? `Esta historia une al protagonista de la saga "${sanitizedHeroName}" con su nuevo aliado creado por el niño: "${sanitizedCompanionName}".` : `El protagonista del cuento es el personaje que el niño ha dibujado y creado: ${sanitizedHeroName}.`}

${languageDirective}
${sagaContinuationSection}
${visualPromptGuideline}

---
DATOS DEL PROTAGONISTA DE LA SAGA:
- Nombre: ${sanitizedHeroName}
- Especie: ${sagaHero.species}
- Silueta / Forma: ${sagaHero.shapeLanguage || 'amigable y redondeado'}
- Colores principales: ${heroColors}
- Rasgos distintivos: ${heroTraits}
- Personalidad: ${sagaHero.personality}

DATOS DEL LECTOR Y AMBIENTACIÓN:
- Niño creador: ${sanitizedChildName}
- Edad: ${childAge} años
- Pautas pedagógicas:
${ageGuidance}
- Pauta de longitud de texto para TV:
${wordCountGuideline}
- Mundo Mágico asignado: ${effectiveTheme}

---
DINÁMICA NARRATIVA ASIGNADA PARA ESTE CAPÍTULO:
- Arquetipo: "${selectedArchetype.name}"
- Detonante inicial: ${selectedArchetype.hook}
- Tono y ritmo: ${selectedArchetype.essence}

---
REGLAS DE ORO DE ESTILO (ADIÓS A LOS CLICHÉS Y LA MONOTONÍA):
1. PROHIBICIÓN DE COMIENZOS TÍPICOS: Jamás empieces con "Había una vez", "En un bosque mágico", "En un día soleado" o "Érase una vez". Comienza "in media res": con un sonido intrigante (*¡Cataplum!*), una sorpresa repentina, una pregunta divertida o una acción en marcha.
2. DIÁLOGOS VIVOS EN CADA PÁGINA: Cada página DEBE tener al menos 1 o 2 líneas de diálogo directo entre los personajes con signos de exclamación o pregunta (*—¡Agárrate fuerte, que despegamos! —exclamó...*).
3. ONOMATOPEYAS AUDIBLES (ESENCIAL PARA EL AUDIO TTS): Incluye al menos 1 onomatopeya expresiva por página (*¡Fiuuu!*, *¡Chas!*, *¡Plop!*, *¡Toc-toc!*, *¡Bip-bip!*, *¡Splash!*).
4. RITMO DINÁMICO Y SENSORIAL: Describe texturas y sensaciones divertidas acordes a ${selectedWorld.nameEs}.
5. SEGURIDAD INFANTIL (COPPA / G-Rated): Cero violencia, cero miedo angustioso, cero abandono. Siempre valores de bondad, ingenio, amistad y humor blanco.

---
DIRECTRIZ PARA EL RETO CREATIVO (PÁGINA 5 & creativeChallenge):
- Categoría de reto asignada para este capítulo: "${selectedCategory.category}" (${selectedCategory.nameEs} / ${selectedCategory.nameEn}).
- En la página 5, la historia debe plantear con enorme entusiasmo la necesidad de que el niño dibuje este elemento para desbloquear el siguiente episodio.
- Silueta limpia, formas geométricas sencillas y un color vivo identificable.
- Ejemplo de reto para esta categoría: "${challengeExample}".

---
FORMATO DE RESPUESTA:
Devuelve estrictamente un objeto JSON con la siguiente estructura:
{
  "title": "${titleGuidance}",
  "settingBible": "${settingBible}",
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
    "prompt": "${challengeExample}",
    "targetSubject": "${selectedCategory.category}",
    "encouragement": "¡Tus ceras tienen magia, dibújalo y continuaremos la aventura!"
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
              temperature: 0.65
            }
          }),
        { retries: 2, delayMs: 1200, label: `Story Text Generation (${sanitizedHeroName})` }
      );

      const responseText = response.text || '{}';
      const parsed = safeParseJson(responseText);

      if (!parsed.scenes || !Array.isArray(parsed.scenes) || parsed.scenes.length === 0) {
        throw new Error('La respuesta de Gemini no contiene escenas válidas.');
      }

      const returnedSettingBible = parsed.settingBible?.trim() || settingBible;

      // Ensure each scene has proper sequential page numbering and embedded setting bible
      const scenes: StoryScene[] = parsed.scenes.map((s: any, idx: number) => {
        let vPrompt = s.visualPrompt?.trim() || `Whimsical storybook illustration of ${sanitizedHeroName}, 16:9 widescreen, no text.`;
        if (returnedSettingBible && !vPrompt.toLowerCase().includes(returnedSettingBible.toLowerCase().slice(0, 30))) {
          vPrompt = `${vPrompt}, set in ${returnedSettingBible}`;
        }
        return {
          pageNumber: idx + 1,
          textContent: s.textContent?.trim() || `Página ${idx + 1} de la aventura de ${sanitizedHeroName}.`,
          visualPrompt: vPrompt
        };
      });

      const creativeChallenge: CreativeChallenge = {
        prompt: parsed.creativeChallenge?.prompt?.trim() || challengeExample,
        targetSubject: (parsed.creativeChallenge?.targetSubject?.trim() as ChallengeSubject) || selectedCategory.category,
        encouragement: parsed.creativeChallenge?.encouragement?.trim() || '¡Usa tus colores favoritos para darle vida!'
      };

      const fallbackTitle = continuation && newCompanion
        ? (isEnglish ? `${sanitizedHeroName} & ${sanitizedCompanionName} (Chapter ${chapterNum})` : `${sanitizedHeroName} y ${sanitizedCompanionName} (Capítulo ${chapterNum})`)
        : (isEnglish ? `The Adventures of ${sanitizedHeroName} & ${sanitizedChildName}` : `Las Aventuras de ${sanitizedHeroName} y ${sanitizedChildName}`);

      return {
        title: parsed.title?.trim() || fallbackTitle,
        scenes,
        creativeChallenge,
        settingBible: returnedSettingBible
      };
    } catch (error: any) {
      console.error('⚠️ [GeminiStoryAdapter] Error generating story with gemini-3.5-flash-lite:', error?.message || error);
      throw new Error(`Fallo en la generación del cuento con Gemini 3.5 Flash-Lite: ${error?.message || 'Error desconocido'}`);
    }
  }
}

