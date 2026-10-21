/**
 * Narrative catalog for WonderLoop stories.
 *
 * Design principles (children 3-10, read aloud on a TV):
 * - CONCRETE over abstract: real, recognizable places, physical everyday problems and drawable objects.
 *   No "magic beams", "musical bridges" or other concepts a child cannot picture or draw.
 * - DRAWABLE challenges: every drawing challenge names one object/animal a 4-8 year old can draw from memory
 *   in a few minutes (a boat, an umbrella, a ladder, a dog), with at most one simple attribute (color or size).
 * - VARIETY: world, problem, tone and challenge candidates are picked at random per story (crypto RNG),
 *   with species affinity for the world so the setting fits the hero (a penguin goes to snow or the sea).
 */
import { randomInt } from 'crypto';
import { ChallengeSubject } from '../types/story.types.js';

export type StoryLanguage = 'es' | 'en';

export interface StoryWorld {
  id: string;
  nameEs: string;
  nameEn: string;
  /** Concrete details the story can use (places, props, inhabitants). */
  detailsEs: string;
  detailsEn: string;
  /** English setting description reused by every illustration of the story (environment consistency). */
  visualAnchor: string;
  /** Lower-case keywords (es + en) that make this world a natural home for a hero. */
  affinity: string[];
}

export const STORY_WORLDS: StoryWorld[] = [
  {
    id: 'seashell_beach',
    nameEs: 'La playa de las caracolas',
    nameEn: 'Seashell Beach',
    detailsEs: 'arena dorada, olas suaves, castillos de arena, cangrejos, un faro rojo y blanco y un muelle de madera',
    detailsEn: 'golden sand, gentle waves, sandcastles, little crabs, a red-and-white lighthouse and a wooden pier',
    visualAnchor: 'a sunny seaside beach with golden sand, gentle turquoise waves, sandcastles, a red-and-white lighthouse and a small wooden pier',
    affinity: ['cangrejo', 'crab', 'gaviota', 'seagull', 'tortuga', 'turtle', 'foca', 'seal', 'concha', 'shell', 'pirata', 'pirate', 'barco', 'boat']
  },
  {
    id: 'snowy_mountains',
    nameEs: 'Las montañas nevadas',
    nameEn: 'The Snowy Mountains',
    detailsEs: 'nieve blandita, pinos con gorros de nieve, un lago helado, un iglú y una colina para tirarse en trineo',
    detailsEn: 'soft snow, pine trees with snowy caps, a frozen lake, an igloo and a hill for sledding',
    visualAnchor: 'snowy mountains with soft white snow, snow-capped pine trees, a frozen blue lake, an igloo and a gentle sledding hill under a clear sky',
    affinity: ['pingüino', 'pinguino', 'penguin', 'oso polar', 'polar', 'muñeco de nieve', 'snowman', 'yeti', 'zorro', 'fox', 'reno', 'reindeer', 'hielo', 'ice', 'nieve', 'snow']
  },
  {
    id: 'valley_farm',
    nameEs: 'La granja del valle',
    nameEn: 'Valley Farm',
    detailsEs: 'un granero rojo, balas de paja, un huerto, un estanque con patos, gallinas y un tractor verde',
    detailsEn: 'a red barn, hay bales, a vegetable patch, a duck pond, hens and a green tractor',
    visualAnchor: 'a cheerful countryside farm with a red barn, golden hay bales, a vegetable patch, a duck pond and rolling green fields',
    affinity: ['vaca', 'cow', 'cerdo', 'pig', 'gallina', 'chicken', 'hen', 'pollito', 'chick', 'caballo', 'horse', 'oveja', 'sheep', 'perro', 'dog', 'gato', 'cat', 'pato', 'duck', 'tractor']
  },
  {
    id: 'oak_forest',
    nameEs: 'El bosque de los robles',
    nameEn: 'Oak Tree Forest',
    detailsEs: 'robles enormes, setas rojas con puntos, un riachuelo con piedras, ardillas y un tronco hueco',
    detailsEn: 'huge oak trees, red spotted mushrooms, a babbling brook with stepping stones, squirrels and a hollow log',
    visualAnchor: 'a sunny green forest with huge oak trees, red spotted mushrooms, a small brook with stepping stones and a hollow log',
    affinity: ['zorro', 'fox', 'oso', 'bear', 'búho', 'buho', 'owl', 'ciervo', 'deer', 'conejo', 'rabbit', 'bunny', 'ardilla', 'squirrel', 'erizo', 'hedgehog', 'lobo', 'wolf', 'duende', 'elf', 'gnomo', 'gnome']
  },
  {
    id: 'under_the_sea',
    nameEs: 'El fondo del mar',
    nameEn: 'Under the Sea',
    detailsEs: 'corales de colores, algas que se mecen, peces, un barco hundido con un cofre y burbujas',
    detailsEn: 'colorful coral, swaying seaweed, schools of fish, an old shipwreck with a treasure chest and bubbles',
    visualAnchor: 'a bright underwater world with colorful coral reefs, swaying green seaweed, friendly fish, an old wooden shipwreck and rising bubbles',
    affinity: ['pez', 'fish', 'pulpo', 'octopus', 'tiburón', 'tiburon', 'shark', 'ballena', 'whale', 'delfín', 'delfin', 'dolphin', 'sirena', 'mermaid', 'medusa', 'jellyfish', 'estrella de mar', 'starfish', 'caballito de mar', 'seahorse', 'submarino', 'submarine']
  },
  {
    id: 'rainforest',
    nameEs: 'La selva',
    nameEn: 'The Rainforest',
    detailsEs: 'lianas, hojas gigantes, una cascada, tucanes, ranas de colores y un río ancho',
    detailsEn: 'hanging vines, giant leaves, a waterfall, toucans, colorful frogs and a wide river',
    visualAnchor: 'a lush rainforest with hanging vines, giant green leaves, a sparkling waterfall, colorful flowers and a wide calm river',
    affinity: ['mono', 'monkey', 'loro', 'parrot', 'tucán', 'tucan', 'toucan', 'tigre', 'tiger', 'jaguar', 'serpiente', 'snake', 'rana', 'frog', 'perezoso', 'sloth', 'gorila', 'gorilla', 'camaleón', 'chameleon']
  },
  {
    id: 'outer_space',
    nameEs: 'El espacio',
    nameEn: 'Outer Space',
    detailsEs: 'la Luna con sus cráteres, planetas de colores, un cohete, una estación espacial y estrellas',
    detailsEn: 'the Moon and its craters, colorful planets, a rocket, a space station and twinkling stars',
    visualAnchor: 'friendly outer space with the cratered Moon, round colorful planets, a small space station and twinkling stars on a deep blue sky',
    affinity: ['robot', 'alien', 'extraterrestre', 'marciano', 'astronauta', 'astronaut', 'cohete', 'rocket', 'nave', 'spaceship', 'planeta', 'planet', 'estrella', 'star', 'luna', 'moon', 'ovni', 'ufo']
  },
  {
    id: 'savanna',
    nameEs: 'La sabana',
    nameEn: 'The Savanna',
    detailsEs: 'hierba alta y dorada, acacias, una charca, piedras grandes y un atardecer naranja',
    detailsEn: 'tall golden grass, acacia trees, a watering hole, big rocks and an orange sunset',
    visualAnchor: 'a wide African savanna with tall golden grass, flat-topped acacia trees, a watering hole and big smooth rocks',
    affinity: ['león', 'leon', 'lion', 'jirafa', 'giraffe', 'elefante', 'elephant', 'cebra', 'zebra', 'hipopótamo', 'hipopotamo', 'hippo', 'rinoceronte', 'rhino', 'guepardo', 'cheetah', 'suricata', 'meerkat']
  },
  {
    id: 'riverside_town',
    nameEs: 'El pueblo junto al río',
    nameEn: 'Riverside Town',
    detailsEs: 'casitas de colores, una panadería, un parque con columpios, un puente de piedra y bicicletas',
    detailsEn: 'colorful little houses, a bakery, a park with swings, a stone bridge and bicycles',
    visualAnchor: 'a cozy small town with colorful little houses, a bakery with an awning, a park with swings, a stone bridge over a calm river',
    affinity: ['coche', 'car', 'autobús', 'autobus', 'bus', 'tren', 'train', 'bicicleta', 'bike', 'niño', 'niña', 'kid', 'boy', 'girl', 'superhéroe', 'superheroe', 'superhero', 'muñeca', 'doll', 'peluche', 'teddy']
  },
  {
    id: 'hilltop_castle',
    nameEs: 'El castillo de la colina',
    nameEn: 'Hilltop Castle',
    detailsEs: 'torres con banderas, un puente levadizo, un foso con agua, un jardín y unas escaleras de caracol',
    detailsEn: 'towers with flags, a drawbridge, a moat, a garden and spiral stairs',
    visualAnchor: 'a friendly storybook castle on a green hill with tall towers and colorful flags, a wooden drawbridge over a moat and a flower garden',
    affinity: ['dragón', 'dragon', 'caballero', 'knight', 'princesa', 'princess', 'príncipe', 'principe', 'prince', 'rey', 'king', 'reina', 'queen', 'unicornio', 'unicorn', 'hada', 'fairy', 'mago', 'wizard', 'bruja', 'witch']
  },
  {
    id: 'dinosaur_valley',
    nameEs: 'El valle de los dinosaurios',
    nameEn: 'Dinosaur Valley',
    detailsEs: 'helechos gigantes, un volcán dormido, una laguna, huellas enormes en el barro y huevos en un nido',
    detailsEn: 'giant ferns, a sleeping volcano, a lagoon, huge footprints in the mud and eggs in a nest',
    visualAnchor: 'a green prehistoric valley with giant ferns, a sleeping volcano in the distance, a calm lagoon and big footprints in the mud',
    affinity: ['dinosaurio', 'dinosaur', 'dino', 'rex', 'triceratops', 'diplodocus', 'lagarto', 'lizard', 'cocodrilo', 'crocodile', 'dragón', 'dragon']
  },
  {
    id: 'grandmas_garden',
    nameEs: 'El jardín de la abuela',
    nameEn: "Grandma's Garden",
    detailsEs: 'flores de colores, un huerto con tomates, un estanque con nenúfares, una regadera y mariposas',
    detailsEn: 'colorful flowers, a vegetable patch with tomatoes, a lily pond, a watering can and butterflies',
    visualAnchor: 'a lovely backyard garden with colorful flowers, a vegetable patch with red tomatoes, a small lily pond and fluttering butterflies',
    affinity: ['mariquita', 'ladybug', 'mariposa', 'butterfly', 'abeja', 'bee', 'caracol', 'snail', 'gusano', 'worm', 'hormiga', 'ant', 'oruga', 'caterpillar', 'flor', 'flower', 'grillo', 'cricket', 'libélula', 'dragonfly']
  }
];

export interface StoryProblem {
  id: string;
  es: string;
  en: string;
}

/** Concrete, gentle, physical problems. The model adapts them to the world. */
export const STORY_PROBLEMS: StoryProblem[] = [
  { id: 'stuck_kite', es: 'la cometa de un amigo se ha quedado enganchada en lo alto de un árbol', en: "a friend's kite is stuck high up in a tree" },
  { id: 'rain', es: 'empieza a llover de repente y hay que llegar a casa sin empaparse', en: 'it suddenly starts to rain and everyone must get home without getting soaked' },
  { id: 'lost_little_one', es: 'un animalito pequeño se ha perdido y quiere volver con su familia', en: 'a little animal got lost and wants to go back to its family' },
  { id: 'river_crossing', es: 'tienen que cruzar un río y no hay ningún puente', en: 'they need to cross a river and there is no bridge' },
  { id: 'runaway_ball', es: 'una pelota se ha escapado rodando colina abajo', en: 'a ball rolled away down the hill' },
  { id: 'fallen_chick', es: 'un pajarito se ha caído del nido y hay que devolverlo arriba', en: 'a baby bird fell out of its nest and must be put back up' },
  { id: 'birthday_gift', es: 'es el cumpleaños de un amigo y todavía no tienen regalo', en: "it is a friend's birthday and they still have no present" },
  { id: 'heavy_load', es: 'tienen que llevar una caja muy pesada hasta el otro lado', en: 'they must carry a very heavy box all the way to the other side' },
  { id: 'dark_path', es: 'se está haciendo de noche y no se ve bien el camino de vuelta', en: 'night is falling and the way back is hard to see' },
  { id: 'lost_hat', es: 'el viento se ha llevado el sombrero favorito de un amigo', en: "the wind blew away a friend's favorite hat" },
  { id: 'picnic', es: 'la cesta del picnic se ha quedado al otro lado del camino', en: 'the picnic basket was left on the other side of the path' },
  { id: 'broken_toy', es: 'el juguete favorito de un amigo se ha roto y está muy triste', en: "a friend's favorite toy broke and they feel very sad" },
  { id: 'hungry_friend', es: 'un amigo tiene muchísima hambre y no encuentran nada de merendar', en: 'a friend is very hungry and they cannot find anything for a snack' },
  { id: 'too_high', es: 'algo importante está demasiado alto para alcanzarlo', en: 'something important is too high up to reach' },
  { id: 'race', es: 'hay una carrera divertida y nadie sabe cómo llegar a la meta a tiempo', en: 'there is a fun race and nobody knows how to reach the finish line in time' }
];

export interface StoryTone {
  id: string;
  en: string;
}

/** Narrative flavor (written in English: the prompt instructions are in English). */
export const STORY_TONES: StoryTone[] = [
  { id: 'adventure', en: 'a small journey with a clear destination and fun stops along the way' },
  { id: 'helping', en: 'a kind rescue where empathy and teamwork matter more than speed' },
  { id: 'mystery', en: 'a gentle everyday mystery solved by following concrete clues (footprints, crumbs, sounds)' },
  { id: 'comedy', en: 'a funny mishap full of silly physical comedy that ends well' },
  { id: 'friendship', en: 'making a new friend who is shy or different at first' },
  { id: 'celebration', en: 'getting ready for a party or celebration with a last-minute hitch' },
  { id: 'game', en: 'a playful game or race where being clever beats being fast' },
  { id: 'cozy', en: 'a calm, cozy evening errand that ends snuggled up and ready for sleep' }
];

export interface ChallengeSubjectEntry {
  id: string;
  category: ChallengeSubject;
  es: string; // with article, as used in "Dibuja <es>"
  en: string; // with article, as used in "Draw <en>"
  /** Lower-case keywords used to check the challenge text actually names the subject. */
  keywords: string[];
}

/** Things a 4-8 year old can draw from memory. Every challenge must be one of these. */
export const CHALLENGE_SUBJECTS: ChallengeSubjectEntry[] = [
  // friends / animals
  { id: 'dog', category: 'friend', es: 'un perro', en: 'a dog', keywords: ['perro', 'perrito', 'dog', 'puppy'] },
  { id: 'cat', category: 'friend', es: 'un gato', en: 'a cat', keywords: ['gato', 'gatito', 'cat', 'kitten'] },
  { id: 'bird', category: 'friend', es: 'un pájaro', en: 'a bird', keywords: ['pájaro', 'pajaro', 'pajarito', 'bird'] },
  { id: 'fish', category: 'friend', es: 'un pez', en: 'a fish', keywords: ['pez', 'pececito', 'fish'] },
  { id: 'rabbit', category: 'friend', es: 'un conejo', en: 'a rabbit', keywords: ['conejo', 'conejito', 'rabbit', 'bunny'] },
  { id: 'turtle', category: 'friend', es: 'una tortuga', en: 'a turtle', keywords: ['tortuga', 'turtle'] },
  { id: 'horse', category: 'friend', es: 'un caballo', en: 'a horse', keywords: ['caballo', 'horse', 'pony', 'poni'] },
  { id: 'butterfly', category: 'friend', es: 'una mariposa', en: 'a butterfly', keywords: ['mariposa', 'butterfly'] },
  { id: 'frog', category: 'friend', es: 'una rana', en: 'a frog', keywords: ['rana', 'frog'] },
  { id: 'owl', category: 'friend', es: 'un búho', en: 'an owl', keywords: ['búho', 'buho', 'owl'] },
  { id: 'snail', category: 'friend', es: 'un caracol', en: 'a snail', keywords: ['caracol', 'snail'] },
  { id: 'elephant', category: 'friend', es: 'un elefante', en: 'an elephant', keywords: ['elefante', 'elephant'] },
  // vehicles
  { id: 'boat', category: 'vehicle', es: 'un barco', en: 'a boat', keywords: ['barco', 'barca', 'boat', 'ship'] },
  { id: 'car', category: 'vehicle', es: 'un coche', en: 'a car', keywords: ['coche', 'car'] },
  { id: 'airplane', category: 'vehicle', es: 'un avión', en: 'an airplane', keywords: ['avión', 'avion', 'airplane', 'plane'] },
  { id: 'train', category: 'vehicle', es: 'un tren', en: 'a train', keywords: ['tren', 'train'] },
  { id: 'bicycle', category: 'vehicle', es: 'una bicicleta', en: 'a bicycle', keywords: ['bicicleta', 'bici', 'bicycle', 'bike'] },
  { id: 'rocket', category: 'vehicle', es: 'un cohete', en: 'a rocket', keywords: ['cohete', 'rocket'] },
  { id: 'balloon', category: 'vehicle', es: 'un globo aerostático', en: 'a hot-air balloon', keywords: ['globo', 'balloon'] },
  { id: 'bus', category: 'vehicle', es: 'un autobús', en: 'a bus', keywords: ['autobús', 'autobus', 'bus'] },
  { id: 'tractor', category: 'vehicle', es: 'un tractor', en: 'a tractor', keywords: ['tractor'] },
  { id: 'submarine', category: 'vehicle', es: 'un submarino', en: 'a submarine', keywords: ['submarino', 'submarine'] },
  // useful objects
  { id: 'umbrella', category: 'accessory', es: 'un paraguas', en: 'an umbrella', keywords: ['paraguas', 'umbrella'] },
  { id: 'ladder', category: 'accessory', es: 'una escalera', en: 'a ladder', keywords: ['escalera', 'ladder'] },
  { id: 'flashlight', category: 'accessory', es: 'una linterna', en: 'a flashlight', keywords: ['linterna', 'flashlight', 'torch'] },
  { id: 'basket', category: 'accessory', es: 'una cesta', en: 'a basket', keywords: ['cesta', 'cesto', 'basket'] },
  { id: 'backpack', category: 'accessory', es: 'una mochila', en: 'a backpack', keywords: ['mochila', 'backpack'] },
  { id: 'hat', category: 'accessory', es: 'un sombrero', en: 'a hat', keywords: ['sombrero', 'gorro', 'hat'] },
  { id: 'kite', category: 'accessory', es: 'una cometa', en: 'a kite', keywords: ['cometa', 'kite'] },
  { id: 'ball', category: 'accessory', es: 'una pelota', en: 'a ball', keywords: ['pelota', 'balón', 'balon', 'ball'] },
  { id: 'key', category: 'accessory', es: 'una llave', en: 'a key', keywords: ['llave', 'key'] },
  { id: 'scarf', category: 'accessory', es: 'una bufanda', en: 'a scarf', keywords: ['bufanda', 'scarf'] },
  { id: 'gift', category: 'accessory', es: 'una caja de regalo', en: 'a gift box', keywords: ['regalo', 'gift', 'present'] },
  // places / shelters
  { id: 'house', category: 'shelter', es: 'una casa', en: 'a house', keywords: ['casa', 'casita', 'house'] },
  { id: 'tent', category: 'shelter', es: 'una tienda de campaña', en: 'a tent', keywords: ['tienda', 'tent'] },
  { id: 'treehouse', category: 'shelter', es: 'una casa en un árbol', en: 'a treehouse', keywords: ['casa en un árbol', 'casa en el árbol', 'treehouse', 'tree house'] },
  { id: 'bridge', category: 'place', es: 'un puente', en: 'a bridge', keywords: ['puente', 'bridge'] },
  { id: 'igloo', category: 'shelter', es: 'un iglú', en: 'an igloo', keywords: ['iglú', 'iglu', 'igloo'] },
  { id: 'nest', category: 'shelter', es: 'un nido', en: 'a nest', keywords: ['nido', 'nest'] },
  // food
  { id: 'cake', category: 'snack', es: 'una tarta', en: 'a cake', keywords: ['tarta', 'pastel', 'cake'] },
  { id: 'apple', category: 'snack', es: 'una manzana', en: 'an apple', keywords: ['manzana', 'apple'] },
  { id: 'ice_cream', category: 'snack', es: 'un helado', en: 'an ice cream', keywords: ['helado', 'ice cream', 'ice-cream'] },
  { id: 'cookie', category: 'snack', es: 'una galleta', en: 'a cookie', keywords: ['galleta', 'cookie'] },
  { id: 'pizza', category: 'snack', es: 'una pizza', en: 'a pizza', keywords: ['pizza'] },
  { id: 'banana', category: 'snack', es: 'un plátano', en: 'a banana', keywords: ['plátano', 'platano', 'banana'] },
  { id: 'carrot', category: 'snack', es: 'una zanahoria', en: 'a carrot', keywords: ['zanahoria', 'carrot'] }
];

/** Openers and phrases the model tends to overuse; banned in the text and checked after generation. */
export const BANNED_PHRASES = ['había una vez', 'érase una vez', 'erase una vez', 'once upon a time', 'cataplum', 'in a magical'];

export function pickRandom<T>(items: readonly T[]): T {
  return items[randomInt(items.length)];
}

export function pickRandomDistinct<T>(items: readonly T[], count: number, exclude: (item: T) => boolean = () => false): T[] {
  const pool = items.filter((item) => !exclude(item));
  const picked: T[] = [];
  while (picked.length < count && pool.length > 0) {
    picked.push(pool.splice(randomInt(pool.length), 1)[0]);
  }
  return picked;
}

function normalize(text: string): string {
  return text.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
}

/** Worlds whose affinity keywords appear in the hero's species/name/features. */
export function worldsMatchingHero(heroText: string): StoryWorld[] {
  const haystack = normalize(heroText);
  return STORY_WORLDS.filter((world) => world.affinity.some((keyword) => haystack.includes(normalize(keyword))));
}

/**
 * Picks the story world:
 * - saga chapters keep the previous world half of the time (continuity) and otherwise move on;
 * - otherwise a world that fits the hero 80% of the time, a random one for variety the rest.
 */
export function chooseWorld(heroText: string, previousWorldId?: string): StoryWorld {
  const previous = previousWorldId ? STORY_WORLDS.find((w) => w.id === previousWorldId) : undefined;
  if (previous && randomInt(100) < 50) return previous;
  const fitting = worldsMatchingHero(heroText).filter((w) => w.id !== previous?.id);
  if (fitting.length > 0 && randomInt(100) < 80) return pickRandom(fitting);
  return pickRandom(STORY_WORLDS.filter((w) => w.id !== previous?.id));
}

/** Three challenge candidates from three different categories, so the model can pick the one that fits page 5. */
export function chooseChallengeCandidates(excludeIds: string[] = []): ChallengeSubjectEntry[] {
  // 'place' (bridge) is grouped with shelters: as a one-item category it would win far too often
  const group = (s: ChallengeSubjectEntry) => (s.category === 'place' ? 'shelter' : s.category);
  const groups = pickRandomDistinct(Array.from(new Set(CHALLENGE_SUBJECTS.map(group))), 3);
  return groups.map((g) => pickRandom(CHALLENGE_SUBJECTS.filter((s) => group(s) === g && !excludeIds.includes(s.id))));
}

export function challengeNamesSubject(challengeText: string, subject: ChallengeSubjectEntry): boolean {
  const text = normalize(challengeText);
  return subject.keywords.some((keyword) => text.includes(normalize(keyword)));
}

export function findSubjectById(id?: string): ChallengeSubjectEntry | undefined {
  return CHALLENGE_SUBJECTS.find((s) => s.id === id);
}
