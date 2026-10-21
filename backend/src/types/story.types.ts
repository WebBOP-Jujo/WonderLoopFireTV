export interface ColorDef {
  name: string;
  hex: string;
}

export interface CharacterProfile {
  name: string;
  namePhonetic?: string;
  species: string;
  primaryColors: string[];
  colorPalette?: ColorDef[];
  colorHexes?: string[];
  shapeLanguage?: string;
  mood?: string;
  distinctiveFeatures: string[];
  personality: string;
  confidence?: number;
  isValidDrawing?: boolean;
  /** Short English description of the look, used by every illustration prompt (consistency across languages). */
  visualDescriptionEn?: string;
  originalDrawingBase64?: string | null;
  reinterpretedImageUrl?: string | null;
}

export interface StoryScene {
  pageNumber: number;
  textContent: string;
  visualPrompt: string;
  illustrationUrl?: string | null;
  audioBase64?: string | null;
  /** Start time (ms) of each whitespace-separated word of textContent in the narration (Polly speech marks). */
  wordTimingsMs?: number[];
}

export type ChallengeSubject = 'friend' | 'accessory' | 'shelter' | 'vehicle' | 'snack' | 'place';

export interface CreativeChallenge {
  prompt: string;
  targetSubject: ChallengeSubject;
  encouragement: string;
  audioBase64?: string;
}

export type ArtStyle = 'watercolor' | '3d_animation';

export interface StoryContinuationContext {
  previousStoryId: string;
  previousStoryTitle: string;
  previousCharacter: CharacterProfile;
  challengePrompt: string;
  lastSceneText?: string;
  chapterIndex?: number;
  sagaId?: string;
  originalHero?: CharacterProfile;
  previousCompanionNames?: string[];
  /** 1-2 sentence summaries of every previous chapter (saga memory). */
  previousChapterSummaries?: string[];
  /** World of the previous chapter (catalog id) to allow continuity. */
  worldId?: string;
  artStyle?: ArtStyle;
}

export interface FullStoryResponse {
  storyId: string;
  title: string;
  childName: string;
  childAge: number;
  character: CharacterProfile;
  scenes: StoryScene[];
  creativeChallenge: CreativeChallenge;
  createdAt: string;
  continuation?: StoryContinuationContext;
  chapterIndex?: number;
  sagaId?: string;
  nextStoryId?: string;
  companionName?: string;
  nextStoryTitle?: string;
  artStyle?: ArtStyle;
  settingBible?: string;
  chapterSummary?: string;
  worldId?: string;
}

export interface CreateStoryRequest {
  childName: string;
  childAge: number;
  theme?: string;
  drawingImageBase64?: string;
  characterOverride?: Partial<CharacterProfile>;
  language?: 'es' | 'en';
  continuation?: StoryContinuationContext;
  artStyle?: ArtStyle;
}

export interface StoryGenerateRequest {
  character: CharacterProfile;
  childName: string;
  childAge: number;
  theme?: string;
  language?: 'es' | 'en';
  continuation?: StoryContinuationContext;
  artStyle?: ArtStyle;
}

