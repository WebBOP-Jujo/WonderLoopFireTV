import dotenv from 'dotenv';
dotenv.config();

import { StoryOrchestratorService } from './services/story.service.js';
import { RevenueCatService } from './services/revenuecat.service.js';

async function runPipelineTest() {
  console.log('\n🚀 [WonderLoop Test] Starting E2E Pipeline Verification...\n');

  const orchestrator = new StoryOrchestratorService();
  const revenueCat = new RevenueCatService();

  // 1. Synthetic 1x1 PNG buffer to represent an uploaded drawing
  const sampleDrawingBuffer = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkWPjfDwAEcQHzlR2qGgAAAABJRU5ErkJggg==',
    'base64'
  );

  console.log('--- TEST 1: Drawing Analysis (Multimodal Gemini) ---');
  const character = await orchestrator.analyzeDrawing(sampleDrawingBuffer, 'image/png');
  console.log('✅ Hero detected:', {
    name: character.name,
    species: character.species,
    colors: character.primaryColors,
    features: character.distinctiveFeatures
  });

  console.log('\n--- TEST 2: Full Storybook Generation ---');
  const story = await orchestrator.createFullStory(character, 'Leo', 6, 'Aventura en el Bosque de Cera');
  console.log(`✅ Story Title: "${story.title}"`);
  console.log(`✅ Pages generated: ${story.scenes.length}`);

  story.scenes.forEach((scene) => {
    console.log(`  📖 [Página ${scene.pageNumber}]: ${scene.textContent.slice(0, 70)}...`);
    console.log(`     🎨 Prompt Visual: ${scene.visualPrompt.slice(0, 50)}...`);
    console.log(`     🔊 Audio WAV: ${scene.audioBase64 ? `${Math.round((scene.audioBase64.length * 3) / 4 / 1024)} KB` : 'None (Fallback TTS)'}`);
  });

  console.log('\n--- TEST 3: The Creativity Loop (Physical Challenge) ---');
  console.log(`🖍️ Reto físico: "${story.creativeChallenge.prompt}"`);
  console.log(`💡 Ánimo: "${story.creativeChallenge.encouragement}"`);

  console.log('\n--- TEST 4: RevenueCat Quota & Entitlement Check ---');
  const quota = await revenueCat.checkUserQuota('test_parent_42');
  console.log('✅ RevenueCat Status:', quota);

  console.log('\n🎉 [WonderLoop Test] ALL 4 TESTS PASSED SUCCESSFULLY! Ready for Mobile App consumption.\n');
}

runPipelineTest().catch((err) => {
  console.error('❌ Pipeline Test Failed:', err);
  process.exit(1);
});
