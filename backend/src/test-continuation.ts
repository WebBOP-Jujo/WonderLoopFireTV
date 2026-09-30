import dotenv from 'dotenv';
dotenv.config();

import { GeminiStoryAdapter } from './adapters/story.adapter.js';
import { CharacterProfile, StoryContinuationContext } from './types/story.types.js';

async function testContinuation() {
  console.log('🧪 Testing Story Continuation Pipeline...');

  const storyAdapter = new GeminiStoryAdapter();

  const originalHero: CharacterProfile = {
    name: 'Barquito Tim',
    species: 'barco explorador',
    primaryColors: ['azul', 'rojo'],
    distinctiveFeatures: ['ventanas redondas amarillas', 'chimenea con humo blanco'],
    personality: 'valiente, curioso y amable'
  };

  const newCompanion: CharacterProfile = {
    name: 'Delfín Chispa',
    species: 'delfín mágico',
    primaryColors: ['turquesa', 'dorado'],
    distinctiveFeatures: ['aleta luminosa', 'estrellitas brillantes al saltar'],
    personality: 'juguetón, veloz y gran guía marítimo'
  };

  const continuation: StoryContinuationContext = {
    previousStoryId: 'story_barquito_tim_001',
    previousStoryTitle: 'La Gran Travesía de Barquito Tim',
    previousCharacter: originalHero,
    challengePrompt: '¡Barquito Tim ha llegado a un mar con una densa niebla mágica y no ve el camino! Dibuja un amigo marino que brille para guiarlo.',
    lastSceneText: 'Barquito Tim flotaba en medio de la niebla espesa, esperando con ilusión a su nuevo amigo guía.',
    chapterIndex: 2
  };

  console.log('Sending continuation request to Gemini 3.5 Flash-Lite...');
  const result = await storyAdapter.generateStory(
    newCompanion,
    'Leo',
    8,
    'Aventura marina y amistad',
    'es',
    continuation
  );

  console.log('\n=============================================');
  console.log(`📖 TÍTULO GENERADO: "${result.title}"`);
  console.log('=============================================');
  console.log(`Escenas generadas: ${result.scenes.length}`);
  result.scenes.forEach((s) => {
    console.log(`\n📄 [PÁGINA ${s.pageNumber}]`);
    console.log(`Texto: ${s.textContent}`);
    console.log(`Visual Prompt: ${s.visualPrompt.slice(0, 120)}...`);
  });
  console.log('\n=============================================');
  console.log(`🖍️ RETO PARA CAPÍTULO 3: "${result.creativeChallenge.prompt}"`);
  console.log(`💡 Ánimo: "${result.creativeChallenge.encouragement}"`);
  console.log('=============================================\n');
}

testContinuation().catch((err) => {
  console.error('❌ Error testing continuation:', err);
  process.exit(1);
});
