import { ArtStyle, CharacterProfile, CreativeChallenge, StoryContinuationContext, StoryScene } from '../types/story.types.js';
import { ILlmClient, getLlmClient } from './llm.client.js';
import {
  BANNED_PHRASES,
  ChallengeSubjectEntry,
  STORY_PROBLEMS,
  STORY_TONES,
  StoryWorld,
  challengeNamesSubject,
  chooseChallengeCandidates,
  chooseWorld,
  findSubjectById,
  pickRandom
} from './story-catalog.js';

export interface GeneratedStoryContent {
  title: string;
  scenes: StoryScene[];
  creativeChallenge: CreativeChallenge;
  settingBible?: string;
  chapterSummary?: string;
  worldId?: string;
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

type Lang = 'es' | 'en';

const ABSTRACT_CHALLENGE_MARKERS = ['mágic', 'magic', 'brill', 'glow', 'rayo', 'beam', 'elixir', 'poción', 'potion', 'luminos', 'encantad', 'enchanted', 'musical', 'notas', 'sparkl', 'destell'];
/** A page that opens with an interjection/sound word, e.g. "¡Plaf! …", "Splash! …", "—¡Zas! …". */
const SOUND_START = /^[¡!*"“«—\-\s]*([a-zñáéíóú]+[-–]?)+!/i;

function sanitizeString(input?: string, maxLen: number = 80): string {
  if (!input) return '';
  return input
    .replace(/[\r\n\t]/g, ' ')
    .replace(/[{}<>\\"]/g, '')
    .trim()
    .slice(0, maxLen);
}

/** Removes dangling dialogue dashes models sometimes leave ("te ayudaré —." -> "te ayudaré."). */
export function tidyDialoguePunctuation(text: string): string {
  return text
    .replace(/\s+—\s*([.,;:!?])/g, '$1')
    .replace(/\s+—\s*$/gm, '')
    .replace(/—\s+—/g, '—')
    .replace(/([!?])\.(?!\.)/g, '$1');
}

function normalizeWord(word: string): string {
  return word.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
}

function wordCount(text: string): number {
  return text.trim().split(/\s+/).filter(Boolean).length;
}

/** Words per page by age: short for toddlers, richer for early readers (and ~20-40 s of narration). */
function wordTarget(age: number): [number, number] {
  if (age <= 4) return [25, 40];
  if (age <= 6) return [40, 55];
  if (age <= 8) return [55, 75];
  return [70, 90];
}

/** Sentences per page: models follow sentence counts far more reliably than word counts. */
function sentenceTarget(age: number): string {
  if (age <= 4) return '3-4';
  if (age <= 6) return '4-5';
  if (age <= 8) return '5-7';
  return '6-8';
}

function ageGuidance(age: number): string {
  if (age <= 4) return 'Toddler (2-4): very short simple sentences, lots of repetition and familiar words, one idea per sentence, zero tension.';
  if (age <= 6) return 'Preschool (5-6): short lively sentences, simple familiar vocabulary, gentle humor, tiny harmless obstacles.';
  if (age <= 8) return 'Early reader (7-8): varied sentences, a few richer words a child can guess from context, clever problem solving, playful humor.';
  return 'Confident reader (9-10): livelier pacing, witty dialogue, slightly richer vocabulary and a satisfying clever twist.';
}

function languageStyleSheet(lang: Lang): string {
  return lang === 'es'
    ? `LANGUAGE: Write title, page texts, chapterSummary and the challenge in natural SPANISH FROM SPAIN (castellano peninsular), warm and simple.
- Dialogue uses the Spanish raya, format: —[words] —[dijo/preguntó/susurró] [character]—. [more words]
- Use opening marks ¡ and ¿. Sound words in Spanish (¡plaf!, ¡zas!, ¡chof!, ¡ñam!, ¡toc, toc!).`
    : `LANGUAGE: Write title, page texts, chapterSummary and the challenge in natural, simple AMERICAN ENGLISH.
- Dialogue uses double quotes, format: "[words]," [said/asked/whispered] [character].
- NEVER use Spanish punctuation (¡ ¿) or em-dash dialogue. Sound words in English (splash, whoosh, crunch, knock-knock).`;
}

function describeCharacter(c: CharacterProfile, lang: Lang): string {
  const parts = [
    `name: ${sanitizeString(c.name, 40)}`,
    `what it is: ${sanitizeString(c.species, 80)}`,
    c.distinctiveFeatures?.length ? `drawn features: ${c.distinctiveFeatures.map((f) => sanitizeString(f, 80)).join('; ')}` : '',
    c.primaryColors?.length ? `colors: ${c.primaryColors.map((f) => sanitizeString(f, 40)).join(', ')}` : '',
    c.shapeLanguage ? `shape: ${sanitizeString(c.shapeLanguage, 100)}` : '',
    c.personality ? `personality: ${sanitizeString(c.personality, 160)}` : ''
  ].filter(Boolean);
  return parts.join(' | ') + (lang === 'en' && /[áéíóúñ]/i.test(c.species || '') ? ' (translate any Spanish descriptors naturally)' : '');
}

// No minLength on textContent: constrained decoding with minLength makes Nova ~3.5x slower (19 s vs 5.6 s).
const STORY_SCHEMA = (candidateIds: string[], pageHint: string) => ({
  type: 'object',
  properties: {
    title: { type: 'string', description: '3-7 words, concrete, includes the hero name' },
    chapterSummary: { type: 'string', description: '1-2 sentences summarizing what happened, in the story language' },
    scenes: {
      type: 'array',
      description: 'Exactly 5 pages, in order',
      items: {
        type: 'object',
        properties: {
          pageNumber: { type: 'integer' },
          textContent: { type: 'string', description: `Page text in the story language: ${pageHint}` },
          visualPrompt: { type: 'string', description: 'English, 20-45 words, only what is visible in this moment' }
        },
        required: ['pageNumber', 'textContent', 'visualPrompt']
      }
    },
    creativeChallenge: {
      type: 'object',
      properties: {
        subjectId: { type: 'string', enum: candidateIds },
        prompt: { type: 'string', description: 'One sentence, max 18 words, starts with Dibuja/Draw + the chosen subject' },
        encouragement: { type: 'string', description: 'Max 12 words, warm' }
      },
      required: ['subjectId', 'prompt', 'encouragement']
    }
  },
  required: ['title', 'chapterSummary', 'scenes', 'creativeChallenge']
});

interface StoryPlan {
  lang: Lang;
  hero: CharacterProfile;
  companion: CharacterProfile | null;
  chapterNum: number;
  childName: string;
  childAge: number;
  world: StoryWorld;
  problem: string;
  tone: string;
  candidates: ChallengeSubjectEntry[];
}

export class LlmStoryAdapter implements IStoryAdapter {
  constructor(private llm: ILlmClient = getLlmClient()) {}

  async generateStory(
    character: CharacterProfile,
    childName: string,
    childAge: number,
    theme?: string,
    language: 'es' | 'en' = 'es',
    continuation?: StoryContinuationContext,
    _artStyle: ArtStyle = 'watercolor'
  ): Promise<GeneratedStoryContent> {
    const lang: Lang = language === 'en' ? 'en' : 'es';
    const hero = continuation?.originalHero || continuation?.previousCharacter || character;
    const companion = continuation ? character : null;
    const chapterNum = continuation?.chapterIndex || (continuation ? 2 : 1);
    const heroText = [hero.species, hero.name, ...(hero.distinctiveFeatures || [])].join(' ');

    const plan: StoryPlan = {
      lang,
      hero,
      companion,
      chapterNum,
      childName: sanitizeString(childName, 30) || (lang === 'en' ? 'little artist' : 'pequeño artista'),
      childAge,
      world: chooseWorld(heroText, continuation?.worldId),
      problem: pickRandom(STORY_PROBLEMS)[lang],
      tone: pickRandom(STORY_TONES).en,
      candidates: chooseChallengeCandidates()
    };

    console.log(
      `📖 [StoryAdapter] Chapter ${chapterNum} (${lang.toUpperCase()}) for "${hero.name}": world=${plan.world.id}, tone="${plan.tone.slice(0, 40)}…", challenge candidates=${plan.candidates.map((c) => c.id).join('/')}`
    );

    const basePrompt = this.buildPrompt(plan, continuation, sanitizeString(theme, 120));
    let best = await this.generateAndReview(basePrompt, plan);
    if (best.issues.length > 0) {
      console.warn(`🔁 [StoryAdapter] Quality review failed (${best.issues.join(' | ')}). Rewriting once.`);
      const retry = await this.generateAndReview(
        `${basePrompt}\n\nREVISION REQUIRED. Your previous draft had these problems:\n- ${best.issues.join('\n- ')}\nWrite the whole story again, fixing every problem.`,
        plan
      ).catch(() => null);
      if (retry && retry.issues.length < best.issues.length) best = retry;
    }
    if (best.issues.length > 0) {
      console.warn(`⚠️ [StoryAdapter] Accepting story with minor issues after auto-fix: ${best.issues.join(' | ')}`);
    }
    return this.finalize(best.parsed, plan);
  }

  private async generateAndReview(prompt: string, plan: StoryPlan): Promise<{ parsed: any; issues: string[] }> {
    const parsed = await this.llm.generateJson({
      prompt,
      temperature: 0.85,
      maxTokens: 8000,
      timeoutMs: 75_000,
      label: `Story Text Generation (${plan.hero.name})`,
      schema: STORY_SCHEMA(
        plan.candidates.map((c) => c.id),
        `${sentenceTarget(plan.childAge)} sentences, ${wordTarget(plan.childAge)[0]}-${wordTarget(plan.childAge)[1]} words`
      ),
      validate: (p) =>
        Array.isArray(p?.scenes) &&
        p.scenes.length >= 5 &&
        // Only the first 5 pages are used: models sometimes append an empty 6th scene
        p.scenes.slice(0, 5).every((s: any) => typeof s?.textContent === 'string' && s.textContent.trim().length > 0)
    });
    return { parsed, issues: this.review(parsed, plan) };
  }

  /** Objective quality checks. Major issues trigger one rewrite; minor ones are only logged. */
  private review(parsed: any, plan: StoryPlan): string[] {
    return this.reviewDetailed(parsed, plan).major;
  }

  private reviewDetailed(parsed: any, plan: StoryPlan): { major: string[]; minor: string[] } {
    const issues: string[] = [];
    const minor: string[] = [];
    const scenes: any[] = (parsed.scenes || []).slice(0, 5);
    const texts = scenes.map((s) => String(s.textContent || '').replace(/\*+/g, ''));
    const all = `${parsed.title || ''} ${texts.join(' ')} ${parsed.creativeChallenge?.prompt || ''}`.toLowerCase();

    const banned = BANNED_PHRASES.filter((b) => all.includes(b));
    if (banned.length) issues.push(`uses banned clichés: ${banned.join(', ')}`);

    const soundStarts = texts.map((t) => SOUND_START.test(t.trim()));
    if (soundStarts.filter(Boolean).length > 1) issues.push('more than one page starts with a sound word or exclamation');
    const firstWords = texts.map((t) => t.toLowerCase().replace(/[^a-zñáéíóú ]/g, ' ').trim().split(/\s+/)[0]);
    if (firstWords.some((w, i) => i > 0 && w && w === firstWords[i - 1])) minor.push('two consecutive pages start with the same word');
    const heroFirst = normalizeWord(plan.hero.name.split(/s+/)[0] || '');
    if (heroFirst && firstWords.filter((w) => normalizeWord(w) === heroFirst).length > 2) minor.push(`${plan.hero.name} opens more than two pages`);

    if (plan.lang === 'en' && texts.some((t) => /[¡¿]|—/.test(t))) issues.push('English text contains Spanish punctuation (¡ ¿ or — dialogue)');

    const [lo, hi] = wordTarget(plan.childAge);
    const tooShort = texts.filter((t) => wordCount(t) < lo * 0.75).length;
    const tooLong = texts.filter((t) => wordCount(t) > hi * 1.3).length;
    if (tooShort > 1) issues.push(`pages are too short: word counts were ${texts.map((t, i) => `page ${i + 1}=${wordCount(t)}`).join(', ')}; every page needs ${sentenceTarget(plan.childAge)} sentences and ${lo}-${hi} words`);
    if (tooLong > 1) issues.push(`${tooLong} pages are too long (target ${lo}-${hi} words per page)`);

    const subject = plan.candidates.find((c) => c.id === parsed.creativeChallenge?.subjectId);
    const challenge = String(parsed.creativeChallenge?.prompt || '');
    if (!subject) issues.push('creativeChallenge.subjectId is not one of the candidates');
    else if (!challengeNamesSubject(challenge, subject)) issues.push(`the challenge must literally ask to draw ${subject[plan.lang]}`);
    if (ABSTRACT_CHALLENGE_MARKERS.some((m) => challenge.toLowerCase().includes(m))) issues.push('the challenge is abstract (magic/glowing/beams); ask for a plain everyday object');
    if (wordCount(challenge) > 22) issues.push('the challenge sentence is too long (max 18 words)');

    const childMentions = (all.match(new RegExp(`\\b${plan.childName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`, 'gi')) || []).length;
    if (plan.childName.length > 1 && childMentions > 3) issues.push(`the child's name appears ${childMentions} times (max 2)`);
    if (/(c[aá]mara|camera|pantalla|screen)/i.test(texts.join(' '))) minor.push('mentions a camera or screen');

    if (minor.length) console.log(`ℹ️ [StoryAdapter] Style notes: ${minor.join(' | ')}`);
    return { major: issues, minor };
  }

  private finalize(parsed: any, plan: StoryPlan): GeneratedStoryContent {
    const heroName = sanitizeString(plan.hero.name, 40);
    const companionName = plan.companion ? sanitizeString(plan.companion.name, 40) : '';

    const scenes: StoryScene[] = (parsed.scenes as any[]).slice(0, 5).map((s, idx) => ({
      pageNumber: idx + 1,
      // Strip markdown emphasis some models add: it would show on screen and confuse TTS
      textContent: tidyDialoguePunctuation(String(s.textContent || '').replace(/\*+/g, '').replace(/[ \t]+\n/g, '\n')).trim() ||
        (plan.lang === 'en' ? `Page ${idx + 1} of ${heroName}'s story.` : `Página ${idx + 1} del cuento de ${heroName}.`),
      visualPrompt: String(s.visualPrompt || '').trim() ||
        `${heroName}${companionName ? ` and ${companionName}` : ''} in ${plan.world.nameEn}, page ${idx + 1} of the story`
    }));

    // Guarantee a concrete, drawable challenge even if the model drifted
    const subject = findSubjectById(parsed.creativeChallenge?.subjectId) && plan.candidates.some((c) => c.id === parsed.creativeChallenge?.subjectId)
      ? findSubjectById(parsed.creativeChallenge.subjectId)!
      : plan.candidates[0];
    let challengePrompt = String(parsed.creativeChallenge?.prompt || '').trim();
    const abstract = ABSTRACT_CHALLENGE_MARKERS.some((m) => challengePrompt.toLowerCase().includes(m));
    if (!challengePrompt || abstract || !challengeNamesSubject(challengePrompt, subject) || wordCount(challengePrompt) > 22) {
      challengePrompt = plan.lang === 'en'
        ? `Draw ${subject.en} to help ${heroName} on the next adventure!`
        : `¡Dibuja ${subject.es} para ayudar a ${heroName} en su próxima aventura!`;
    }
    if (plan.lang === 'es' && challengePrompt.startsWith('!')) challengePrompt = `¡${challengePrompt.slice(1)}`;

    const fallbackTitle = plan.companion
      ? (plan.lang === 'en' ? `${heroName} & ${companionName}` : `${heroName} y ${companionName}`)
      : heroName;

    return {
      title: String(parsed.title || '').replace(/\*+/g, '').trim() || fallbackTitle,
      scenes,
      creativeChallenge: {
        prompt: challengePrompt,
        targetSubject: subject.category,
        encouragement: String(parsed.creativeChallenge?.encouragement || '').trim() ||
          (plan.lang === 'en' ? 'Your crayons can make it real!' : '¡Tus ceras pueden hacerlo realidad!')
      },
      settingBible: plan.world.visualAnchor,
      chapterSummary: String(parsed.chapterSummary || '').trim() || undefined,
      worldId: plan.world.id
    };
  }

  private buildPrompt(plan: StoryPlan, continuation: StoryContinuationContext | undefined, theme: string): string {
    const { lang, hero, companion, chapterNum, childName, childAge, world } = plan;
    const [lo, hi] = wordTarget(childAge);
    const heroName = sanitizeString(hero.name, 40);
    const companionName = companion ? sanitizeString(companion.name, 40) : '';
    const worldName = lang === 'en' ? world.nameEn : world.nameEs;
    const worldDetails = lang === 'en' ? world.detailsEn : world.detailsEs;
    const heroFeatures = (hero.distinctiveFeatures || []).map((f) => sanitizeString(f, 80)).filter(Boolean);

    const sagaSection = continuation && companion
      ? `
SAGA CONTEXT (chapter ${chapterNum}):
- Story so far: ${(continuation.previousChapterSummaries || []).map((s) => sanitizeString(s, 300)).join(' ') || sanitizeString(continuation.previousStoryTitle, 120)}
- Last page of the previous chapter: "${sanitizeString(continuation.lastSceneText, 300)}"
- The child answered the previous challenge ("${sanitizeString(continuation.challengePrompt, 150)}") by drawing a NEW COMPANION:
  ${describeCharacter(companion, lang)}
${continuation.previousCompanionNames?.length ? `- Friends met in earlier chapters (may appear briefly): ${continuation.previousCompanionNames.map((n) => sanitizeString(n, 40)).join(', ')}` : ''}
- Page 1 must continue naturally from that last page: ${heroName} welcomes ${companionName}, who arrives thanks to ${childName}'s drawing.
- ${heroName} is still the protagonist; ${companionName} takes part on every page and both contribute to the solution.
- Keep facts consistent with the story so far (names, places, what each character can do).`
      : '';

    return `You are an award-winning children's picture-book author. Write a read-aloud bedtime story for a family TV app: the child drew the hero on paper and will see the story illustrated on the living-room TV.

${languageStyleSheet(lang)}

STORY BRIEF
- Hero, drawn by ${childName}: ${describeCharacter(hero, lang)}
- Reader: ${childName}, ${childAge} years old. ${ageGuidance(childAge)}
- Length: EXACTLY 5 pages. EVERY page has ${sentenceTarget(childAge)} full sentences (${lo}-${hi} words). Short pages are rejected, so develop each moment with what the characters see, say and feel.
- World: ${worldName} — ${worldDetails}. Use these concrete places and things.
- Problem to solve: ${plan.problem}. Adapt it to this world; it must be visible and easy to understand.
- Tone: ${plan.tone}.
${theme ? `- The family asked for this theme: "${theme}". Blend it in if it is suitable for young children.` : ''}
${sagaSection}

STORY SPINE (one step per page)
1. Setup: show ${heroName} in a specific spot of ${worldName}; by the end of the page the problem is clear.
2. First try: ${heroName} tries something that fails in a funny, harmless way.
3. New idea: a small discovery, a helper or a feeling makes ${heroName} think of another way.
4. Solution: the problem is solved thanks to something the child drew${heroFeatures.length ? ` (one of: ${heroFeatures.join('; ')})` : ''}${companion ? ` and with ${companionName}'s help` : ''}. Use a feature only in a way that makes physical sense (a long tail can be a rope, wings can fly, big ears hear far away); if none fits, use the hero's personality or talent instead.
5. Calm ending: a warm, cozy resolution that winds down for bedtime. Then a NEW small, concrete need appears that one of the challenge candidates would solve, and the page ends by asking ${childName} to draw it.

WRITING RULES
- CONCRETE AND VISUAL: everything must be something a young child can picture or has seen: real objects, animals, food, weather, places. No abstract or invented concepts (no light trails, singing stones, musical bridges, magic beams, potions or riddles of light). Fantasy comes only from the characters (talking animals, a friendly dragon).
- VARIED OPENINGS: start pages in different ways (an action, a line of dialogue, a description, a question, a time cue). At most ONE page may start with a sound word, at most ONE page may start with ${heroName}'s name, and never start two pages with the same word.
- Sound words: 2 to 4 in the WHOLE story, inside sentences, and only for real sounds (splash when jumping into water).
- Dialogue: characters speak on at least 3 pages, and every line must make sense in the scene.
- Mention ${childName} at most twice (ideally only on page 5, when a character invites them to draw). Never mention cameras, screens, TVs or the reader looking at the story.
- Never use: ${BANNED_PHRASES.map((b) => `"${b}"`).join(', ')}. Avoid the word "magic"/"mágico".
- Gentle and safe for ages 3-10: no violence, no scary villains, nobody is in real danger or left alone.
- Title: 3-7 words, concrete, with ${heroName}${companion ? ` and ${companionName}` : ''} and a key object or place of THIS story. Avoid generic words like "adventure", "symphony", "magical".
- chapterSummary: 1-2 sentences, in the story language, saying what happened (it is used as memory for the next chapter).

ILLUSTRATION PROMPTS (visualPrompt, always in ENGLISH, 20-45 words per page)
- Describe only what is visible at that page's key moment: who (${heroName} the ${sanitizeString(hero.species, 60)}${companion ? ` and ${companionName} the ${sanitizeString(companion.species, 60)}` : ''}), the action and pose, the facial emotion (cheerful, curious, surprised or determined; never sad, crying or scared), the key props and the exact spot in ${world.nameEn}.
- Every page shows a different moment and spot.${companion ? ` Both ${heroName} and ${companionName} appear in every page.` : ''}
- Do NOT write art style, camera, lighting, "16:9" or "illustration": they are added automatically.

DRAWING CHALLENGE (creativeChallenge)
- Candidates — choose exactly ONE, the one that best fits the need on page 5:
${plan.candidates.map((c) => `  * ${c.id}: ${lang === 'en' ? c.en : c.es}`).join('\n')}
- prompt: ONE sentence, max 18 words, addressed to ${childName}. Start with "${lang === 'en' ? 'Draw' : '¡Dibuja'}" + the chosen object + at most ONE simple attribute (a color or a size) + why it helps. A plain object a child can draw from memory: never magic, glowing or special powers.
- encouragement: max 12 warm words in the story language.`;
  }
}
