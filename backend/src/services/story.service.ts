import { IVisionAdapter, GeminiVisionAdapter } from '../adapters/vision.adapter.js';
import { IStoryAdapter, GeminiStoryAdapter } from '../adapters/story.adapter.js';
import { IImageAdapter, NanobananaImageAdapter } from '../adapters/image.adapter.js';
import { IAudioAdapter, GeminiAudioAdapter, VoiceStyle } from '../adapters/audio.adapter.js';
import { ArtStyle, CharacterProfile, FullStoryResponse, StoryContinuationContext, StoryScene } from '../types/story.types.js';

export class StoryOrchestratorService {
  constructor(
    private visionAdapter: IVisionAdapter = new GeminiVisionAdapter(),
    private storyAdapter: IStoryAdapter = new GeminiStoryAdapter(),
    private imageAdapter: IImageAdapter = new NanobananaImageAdapter(),
    private audioAdapter: IAudioAdapter = new GeminiAudioAdapter()
  ) {}

  /**
   * Step 1: Analyze child's drawing and return recognized hero profile with reinterpreted portrait
   */
  async analyzeDrawing(
    imageBuffer: Buffer,
    mimeType: string,
    artStyle: ArtStyle = 'watercolor'
  ): Promise<CharacterProfile> {
    console.log(`🔍 [StoryService] Analyzing drawing (${imageBuffer.length} bytes, ${mimeType}, style: ${artStyle})...`);
    const profile = await this.visionAdapter.analyzeDrawing(imageBuffer, mimeType);

    const isBlank =
      profile.isValidDrawing === false ||
      (typeof profile.confidence === 'number' && profile.confidence < 0.55) ||
      profile.name.toLowerCase().includes('vacío') ||
      profile.species.toLowerCase().includes('sin dibujo') ||
      profile.species.toLowerCase().includes('vacía');

    if (isBlank) {
      console.log(`ℹ️ [StoryService] Drawing classified as blank/unclear (confidence: ${profile.confidence}). Skipping portrait generation to save quota.`);
      return {
        ...profile,
        isValidDrawing: false,
        name: profile.name || 'Sin dibujo',
        species: profile.species || 'Superficie sin dibujo',
        originalDrawingBase64: `data:${mimeType};base64,${imageBuffer.toString('base64')}`,
        reinterpretedImageUrl: undefined
      };
    }

    // Generate portrait based on the analyzed traits and chosen artStyle
    const reinterpretedImageUrl = await this.imageAdapter.generateCharacterPortrait(imageBuffer, mimeType, profile, artStyle);

    return {
      ...profile,
      isValidDrawing: true,
      originalDrawingBase64: `data:${mimeType};base64,${imageBuffer.toString('base64')}`,
      reinterpretedImageUrl
    };
  }

  /**
   * Step 2: Create full 5-page storybook with illustrations, audio and creative challenge
   */
  async createFullStory(
    character: CharacterProfile,
    childName: string,
    childAge: number,
    theme?: string,
    language: 'es' | 'en' = 'es',
    continuation?: StoryContinuationContext,
    voiceStyle: VoiceStyle = 'soft',
    artStyle?: ArtStyle
  ): Promise<FullStoryResponse> {
    const isContinuation = Boolean(continuation);
    const effectiveArtStyle: ArtStyle =
      artStyle ||
      continuation?.artStyle ||
      (childAge >= 7 ? '3d_animation' : 'watercolor');

    // In a saga continuation, the true hero of the story is the original saga hero (e.g. Toby the dog).
    // The newly scanned character is the companion/helper (e.g. Barquito or Pájaro).
    const sagaHero = continuation?.originalHero || continuation?.previousCharacter || character;
    const newCompanion = isContinuation ? character : undefined;

    console.log(`📖 [StoryService] Generating story (${language.toUpperCase()}, voice: ${voiceStyle}, style: ${effectiveArtStyle}) for saga hero "${sagaHero.name}" with companion "${newCompanion?.name || 'none'}" (Child: ${childName}, Age: ${childAge}, isContinuation: ${isContinuation})...`);

    const t0 = Date.now();
    // 1. Generate text story and creative challenge via LLM (with saga continuity if present)
    const generated = await this.storyAdapter.generateStory(
      character,
      childName,
      childAge,
      theme,
      language,
      continuation,
      effectiveArtStyle
    );
    const tStory = Date.now() - t0;
    console.log(`⏱️ [StoryService] Text & Story generation completed in ${tStory} ms`);

    // 2. Generate unified audio and illustrations in parallel
    console.log(`🚀 [StoryService] Launching parallel multimedia synthesis for "${sagaHero.name}" and companion "${newCompanion?.name || 'none'}" (style: ${effectiveArtStyle})...`);
    const tParallelStart = Date.now();

    // Reference 1 is ALWAYS the Saga Hero (Toby)
    const characterRefImage = (sagaHero.reinterpretedImageUrl || sagaHero.originalDrawingBase64) || undefined;
    // Reference 2 is ALWAYS the newly drawn companion (Barquito / Pájaro)
    const companionRefImage = newCompanion
      ? ((newCompanion.reinterpretedImageUrl || newCompanion.originalDrawingBase64) || undefined)
      : undefined;

    const [audioMap, illustrations] = await Promise.all([
      (async () => {
        const tAudioStart = Date.now();
        const res = await this.audioAdapter.generateFullStoryAudio(
          generated.scenes.map((s) => ({ pageNumber: s.pageNumber, textContent: s.textContent })),
          language,
          voiceStyle
        );
        const tAudio = Date.now() - tAudioStart;
        console.log(`⏱️ [StoryService] Full Story TTS Audio completed in ${tAudio} ms (${(tAudio / 1000).toFixed(1)}s)`);
        return res;
      })(),
      (async () => {
        const tImgStart = Date.now();
        const settledResults = await Promise.allSettled(
          generated.scenes.map((s) =>
            this.imageAdapter.generateSceneIllustration(
              s.visualPrompt,
              s.pageNumber,
              characterRefImage,
              companionRefImage,
              effectiveArtStyle,
              generated.settingBible,
              sagaHero
            )
          )
        );
        const illustrations: (string | undefined)[] = settledResults.map((r, idx) => {
          if (r.status === 'fulfilled') {
            return r.value;
          } else {
            console.warn(`⚠️ [StoryService] Illustration failed for scene ${idx + 1}: ${r.reason?.message || r.reason}`);
            return undefined;
          }
        });
        const tImg = Date.now() - tImgStart;
        const fulfilledCount = settledResults.filter((r) => r.status === 'fulfilled').length;
        console.log(`⏱️ [StoryService] ${fulfilledCount}/${settledResults.length} Illustrations completed in ${tImg} ms (${(tImg / 1000).toFixed(1)}s)`);
        return illustrations;
      })()
    ]);

    const tParallelTotal = Date.now() - tParallelStart;
    console.log(`⏱️ [StoryService] Parallel Multimedia Phase finished in ${tParallelTotal} ms (${(tParallelTotal / 1000).toFixed(1)}s)`);

    // 3. Assemble enriched scenes with synchronized audio and illustrations (with fallback to character portrait)
    const enrichedScenes: StoryScene[] = generated.scenes.map((scene, idx) => ({
      ...scene,
      illustrationUrl: illustrations[idx] || characterRefImage || undefined,
      audioBase64: audioMap.get(scene.pageNumber) || undefined
    }));

    const storyId = `story_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;

    return {
      storyId,
      title: generated.title,
      settingBible: generated.settingBible,
      childName,
      childAge,
      character: {
        ...character,
        reinterpretedImageUrl: undefined,
        originalDrawingBase64: undefined
      },
      companionName: newCompanion?.name,
      scenes: enrichedScenes,
      creativeChallenge: generated.creativeChallenge,
      createdAt: new Date().toISOString(),
      artStyle: effectiveArtStyle,
      continuation: continuation ? {
        ...continuation,
        artStyle: effectiveArtStyle,
        previousCharacter: {
          ...sagaHero,
          reinterpretedImageUrl: undefined,
          originalDrawingBase64: undefined
        },
        originalHero: {
          ...sagaHero,
          reinterpretedImageUrl: undefined,
          originalDrawingBase64: undefined
        },
        previousCompanionNames: [
          ...(continuation.previousCompanionNames || []),
          ...(newCompanion ? [newCompanion.name] : [])
        ]
      } : undefined,
      chapterIndex: continuation?.chapterIndex || (continuation ? 2 : 1),
      sagaId: continuation?.sagaId || continuation?.previousStoryId || storyId
    };
  }
}
