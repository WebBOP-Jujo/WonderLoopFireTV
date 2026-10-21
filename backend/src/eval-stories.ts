/**
 * Story quality evaluation (text only, no images or audio).
 *
 *   npx tsx src/eval-stories.ts [runsPerHero=1] [outFile]
 *
 * Generates stories for a fixed set of heroes (the same hero appears twice on purpose to measure variety)
 * and reports objective metrics: repeated titles/challenges, formulaic page openings, banned clichés,
 * language leakage, concreteness of the drawing challenge and words per page vs. the age target.
 */
import 'dotenv/config';
import fs from 'fs';
import { LlmStoryAdapter } from './adapters/story.adapter.js';
import { BANNED_PHRASES, CHALLENGE_SUBJECTS } from './adapters/story-catalog.js';
import { CharacterProfile } from './types/story.types.js';

interface EvalCase {
  label: string;
  hero: CharacterProfile;
  childName: string;
  childAge: number;
  language: 'es' | 'en';
}

const hero = (name: string, species: string, colors: string[], features: string[], personality: string): CharacterProfile => ({
  name,
  species,
  primaryColors: colors,
  distinctiveFeatures: features,
  personality
});

const pingui = hero('Pingui', 'pingüino simpático', ['negro', 'blanco', 'naranja'], ['pico naranja', 'patas naranjas', 'barriga blanca'], 'curioso y valiente');
const felicito = hero('Felicito', 'happy fluffy cat', ['light blue', 'yellow', 'red'], ['pointy blue ears', 'curly yellow tail', 'big red smile'], 'cheerful and playful');

const CASES: EvalCase[] = [
  { label: 'Pingui#1 es/6', hero: pingui, childName: 'Leo', childAge: 6, language: 'es' },
  { label: 'Pingui#2 es/6', hero: pingui, childName: 'Leo', childAge: 6, language: 'es' },
  { label: 'Felicito#1 en/6', hero: felicito, childName: 'Lucas', childAge: 6, language: 'en' },
  { label: 'Felicito#2 en/6', hero: felicito, childName: 'Lucas', childAge: 6, language: 'en' },
  { label: 'Chispa es/8', hero: hero('Chispa', 'dragón pequeño', ['verde', 'amarillo'], ['alas pequeñas', 'cola en espiral'], 'travieso y generoso'), childName: 'Sara', childAge: 8, language: 'es' },
  { label: 'Bip en/8', hero: hero('Bip', 'little robot', ['silver', 'blue'], ['antenna with a red light', 'wheels instead of feet'], 'clever and kind'), childName: 'Emma', childAge: 8, language: 'en' },
  { label: 'Toby es/4', hero: hero('Toby', 'perrito', ['marrón', 'blanco'], ['orejas largas', 'mancha en un ojo'], 'juguetón y cariñoso'), childName: 'Hugo', childAge: 4, language: 'es' },
  { label: 'Luz en/5', hero: hero('Luz', 'unicorn', ['pink', 'white', 'purple'], ['rainbow mane', 'golden horn'], 'gentle and dreamy'), childName: 'Mia', childAge: 5, language: 'en' }
];

const ABSTRACT_MARKERS = ['mágic', 'magic', 'brill', 'glow', 'rayo', 'beam', 'elixir', 'spark', 'luminos', 'encantad', 'enchanted', 'poción', 'potion', 'notas musicales', 'musical'];
const SOUND_START = /^[¡!*"“«—\-\s]*([a-zñáéíóú]+[-–]?)+!/i;

function words(text: string): number {
  return text.trim().split(/\s+/).filter(Boolean).length;
}

function targetRange(age: number): [number, number] {
  if (age <= 4) return [25, 40];
  if (age <= 6) return [40, 55];
  if (age <= 8) return [55, 75];
  return [70, 90];
}

async function main() {
  const runs = Math.max(1, Number(process.argv[2] || 1));
  const outFile = process.argv[3];
  const adapter = new LlmStoryAdapter();
  const jobs = Array.from({ length: runs }).flatMap(() => CASES);

  const results: any[] = [];
  const queue = [...jobs];
  await Promise.all(
    Array.from({ length: 4 }).map(async () => {
      while (queue.length) {
        const c = queue.shift()!;
        const t0 = Date.now();
        try {
          const story = await adapter.generateStory(c.hero, c.childName, c.childAge, undefined, c.language, undefined, 'watercolor');
          results.push({ case: c, story, ms: Date.now() - t0 });
        } catch (e: any) {
          results.push({ case: c, error: e?.message, ms: Date.now() - t0 });
        }
      }
    })
  );

  const ok = results.filter((r) => r.story);
  const pages = ok.flatMap((r) => r.story.scenes.map((s: any) => ({ text: s.textContent as string, r })));
  const lower = (s: string) => s.toLowerCase();

  const titles = ok.map((r) => lower(r.story.title));
  const challenges = ok.map((r) => lower(r.story.creativeChallenge.prompt));
  const openings = pages.map((p) => lower(p.text).replace(/[^a-zñáéíóú ]/g, ' ').trim().split(/\s+/)[0] || '');
  const soundStarts = pages.filter((p) => SOUND_START.test(p.text.trim())).length;
  const consecutiveSameStartType = ok.reduce((acc, r) => {
    const starts = r.story.scenes.map((s: any) => SOUND_START.test(s.textContent.trim()));
    return acc + starts.slice(1).filter((v: boolean, i: number) => v && starts[i]).length;
  }, 0);
  const banned = pages.filter((p) => BANNED_PHRASES.some((b) => lower(p.text).includes(b))).length;
  const enLeak = ok
    .filter((r) => r.case.language === 'en')
    .flatMap((r) => r.story.scenes)
    .filter((s: any) => /[¡¿]|—/.test(s.textContent)).length;
  const concrete = ok.filter((r) => {
    const ch = lower(r.story.creativeChallenge.prompt);
    const namesSubject = CHALLENGE_SUBJECTS.some((s) => s.keywords.some((k) => ch.includes(k)));
    const abstract = ABSTRACT_MARKERS.some((m) => ch.includes(m));
    return namesSubject && !abstract && words(ch) <= 26;
  }).length;
  const inRange = pages.filter((p) => {
    const [lo, hi] = targetRange(p.r.case.childAge);
    const n = words(p.text);
    return n >= lo * 0.8 && n <= hi * 1.2;
  }).length;

  const pct = (n: number, d: number) => (d ? `${Math.round((n / d) * 100)}%` : 'n/a');
  const summary = {
    stories: `${ok.length}/${results.length}`,
    avgSeconds: ok.length ? +(ok.reduce((a, r) => a + r.ms, 0) / ok.length / 1000).toFixed(1) : 0,
    uniqueTitles: pct(new Set(titles).size, titles.length),
    uniqueChallenges: pct(new Set(challenges).size, challenges.length),
    distinctPageOpeners: pct(new Set(openings).size, openings.length),
    pagesStartingWithSound: pct(soundStarts, pages.length),
    consecutiveSoundStarts: consecutiveSameStartType,
    pagesWithBannedCliches: banned,
    englishPagesWithSpanishPunctuation: enLeak,
    concreteDrawableChallenges: pct(concrete, ok.length),
    pagesWithinWordTarget: pct(inRange, pages.length)
  };

  console.log('\n=== STORY EVAL SUMMARY ===');
  console.table(summary);
  console.log('\n=== SAMPLES ===');
  for (const r of ok) {
    console.log(`\n[${r.case.label}] "${r.story.title}"${r.story.worldId ? ` (world: ${r.story.worldId})` : ''}`);
    r.story.scenes.forEach((s: any) => console.log(`  P${s.pageNumber} (${words(s.textContent)}w): ${s.textContent.slice(0, 110)}…`));
    console.log(`  CHALLENGE: ${r.story.creativeChallenge.prompt}`);
  }
  results.filter((r) => r.error).forEach((r) => console.log(`\n[${r.case.label}] ERROR: ${r.error}`));

  if (outFile) fs.writeFileSync(outFile, JSON.stringify({ summary, results }, null, 2));
}

main();
