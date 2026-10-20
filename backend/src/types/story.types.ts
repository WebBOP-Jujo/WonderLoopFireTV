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
  originalDrawingBase64?: string | null;
  reinterpretedImageUrl?: string | null;
}

export interface StoryScene {
  pageNumber: number;
  textContent: string;
  visualPrompt: string;
  illustrationUrl?: string | null;
  audioBase64?: string | null;
}

export type ChallengeSubject = 'friend' | 'accessory' | 'shelter' | 'vehicle' | 'snack' | 'place';

export interface CreativeChallenge {
  prompt: string;
  targetSubject: ChallengeSubject;
  encouragement: string;
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

