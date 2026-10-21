import { GoogleGenAI } from '@google/genai';
import {
  BedrockRuntimeClient,
  ConverseCommand,
  ContentBlock,
  ImageFormat
} from '@aws-sdk/client-bedrock-runtime';
import { withRetry } from '../utils/retry.js';
import { safeParseJson } from '../utils/safe-json.js';

/**
 * Provider-agnostic "prompt (+ optional image) -> JSON object" client shared by the vision and story adapters.
 * Prompts and post-processing live in the adapters; only the model call differs per provider.
 */
export interface LlmJsonRequest {
  prompt: string;
  image?: { bytes: Buffer; mimeType: string };
  temperature: number;
  maxTokens: number;
  timeoutMs: number;
  label: string;
  /**
   * JSON Schema of the expected object. Bedrock uses it as a forced tool's input schema (structured output);
   * Gemini relies on the prompt plus responseMimeType=application/json.
   */
  schema?: Record<string, unknown>;
  /** Extra semantic check (e.g. "has scenes"). A rejected answer triggers the fallback provider. */
  validate?: (parsed: any) => boolean;
}

export interface ILlmClient {
  readonly name: string;
  generateJson(request: LlmJsonRequest): Promise<any>;
}

const JSON_SYSTEM_INSTRUCTION =
  'You are a JSON API. Reply with exactly one valid JSON object that follows the requested structure. ' +
  'No markdown fences, no comments, no text before or after the JSON. ' +
  'Delimit every key and string value with straight ASCII double quotes ("), never typographic quotes (“ ” or « »); ' +
  'typographic quotes are only allowed inside the text of a value.';

const SUBMIT_TOOL_NAME = 'submit_result';

function parseAndValidate(raw: string, request: LlmJsonRequest, provider: string): any {
  // Tolerate stray prose around the object by keeping the outermost {...}
  const start = raw.indexOf('{');
  const end = raw.lastIndexOf('}');
  const candidate = start >= 0 && end > start ? raw.slice(start, end + 1) : raw;
  const parsed = safeParseJson(candidate);
  if (request.validate && !request.validate(parsed)) {
    throw new Error(`${provider} returned JSON that failed validation for ${request.label}`);
  }
  return parsed;
}

/** Amazon Nova 2 Lite (multimodal: text + image in, text out) through the Bedrock Converse API. */
export class BedrockNovaLlmClient implements ILlmClient {
  readonly name: string;
  private client: BedrockRuntimeClient;
  private modelId: string;
  private circuitOpenUntil = 0;
  private static readonly CIRCUIT_OPEN_MS = 5 * 60 * 1000;

  constructor() {
    // Geo inference profile: requests from us-east-1 are served in us-east-1 / us-east-2 / us-west-2
    this.modelId = process.env.BEDROCK_TEXT_MODEL_ID || 'us.amazon.nova-2-lite-v1:0';
    this.name = `Amazon Bedrock (${this.modelId})`;
    this.client = new BedrockRuntimeClient({
      region: process.env.AWS_REGION || 'us-east-1',
      maxAttempts: 3
    });
  }

  private static imageFormat(mimeType: string): ImageFormat {
    const m = mimeType.toLowerCase();
    if (m.includes('png')) return 'png';
    if (m.includes('webp')) return 'webp';
    if (m.includes('gif')) return 'gif';
    return 'jpeg';
  }

  private static isConfigurationError(error: any): boolean {
    const name = String(error?.name || '');
    return (
      name === 'CredentialsProviderError' ||
      name === 'AccessDeniedException' ||
      name === 'UnrecognizedClientException' ||
      name === 'ResourceNotFoundException' ||
      /could not load credentials|security token|not authorized|model access/i.test(String(error?.message || ''))
    );
  }

  async generateJson(request: LlmJsonRequest): Promise<any> {
    if (Date.now() < this.circuitOpenUntil) {
      throw new Error(`${this.name} temporarily disabled after a configuration error`);
    }

    const content: ContentBlock[] = [];
    if (request.image) {
      content.push({
        image: {
          format: BedrockNovaLlmClient.imageFormat(request.image.mimeType),
          source: { bytes: request.image.bytes }
        }
      });
    }
    content.push({ text: request.prompt });

    const t0 = Date.now();
    try {
      const response = await this.client.send(
        new ConverseCommand({
          modelId: this.modelId,
          system: [{ text: JSON_SYSTEM_INSTRUCTION }],
          messages: [{ role: 'user', content }],
          inferenceConfig: {
            temperature: request.temperature,
            maxTokens: request.maxTokens
          },
          // Structured output: forcing a tool call makes Bedrock return the JSON as an already-parsed object
          // (toolUse.input), so typographic quotes or stray prose can no longer break parsing.
          ...(request.schema
            ? {
                toolConfig: {
                  tools: [
                    {
                      toolSpec: {
                        name: SUBMIT_TOOL_NAME,
                        description: 'Submit the final answer. Fill every field following the instructions in the prompt.',
                        inputSchema: { json: request.schema as any }
                      }
                    }
                  ],
                  toolChoice: { tool: { name: SUBMIT_TOOL_NAME } }
                }
              }
            : {})
        }),
        { abortSignal: AbortSignal.timeout(request.timeoutMs) }
      );

      if (response.stopReason === 'max_tokens') {
        throw new Error('Response truncated at maxTokens');
      }

      const blocks = response.output?.message?.content || [];
      const toolInput = blocks.find((block) => block.toolUse?.name === SUBMIT_TOOL_NAME)?.toolUse?.input as any;
      let parsed: any;
      let mode: string;
      if (toolInput && typeof toolInput === 'object') {
        if (request.validate && !request.validate(toolInput)) {
          throw new Error(`${this.name} tool output failed validation for ${request.label}`);
        }
        parsed = toolInput;
        mode = 'tool-use';
      } else {
        // Safety net: the model answered in plain text instead of calling the tool
        const text = blocks.map((block) => block.text || '').join('').trim();
        if (!text) {
          throw new Error(`Empty response (stopReason: ${response.stopReason})`);
        }
        try {
          parsed = parseAndValidate(text, request, this.name);
        } catch (parseError) {
          console.warn(`⚠️ [Bedrock] Unparseable output for ${request.label} (first 300 chars): ${text.slice(0, 300).replace(/\s+/g, ' ')}`);
          throw parseError;
        }
        mode = 'text';
      }

      const usage = response.usage;
      console.log(
        `    ✅ [Bedrock] ${request.label} via ${this.modelId} (${mode}) in ${((Date.now() - t0) / 1000).toFixed(1)}s` +
          (usage ? ` (${usage.inputTokens} in / ${usage.outputTokens} out tokens)` : '')
      );
      return parsed;
    } catch (error: any) {
      if (BedrockNovaLlmClient.isConfigurationError(error)) {
        this.circuitOpenUntil = Date.now() + BedrockNovaLlmClient.CIRCUIT_OPEN_MS;
        console.warn(`⚠️ [Bedrock] Credentials/permissions/model access problem (${error?.name}). Bedrock paused for 5 min.`);
      }
      throw error;
    }
  }
}

/** Gemini text + vision model (previous primary provider, now the fallback). */
export class GeminiLlmClient implements ILlmClient {
  readonly name: string;
  private ai: GoogleGenAI;
  private model: string;

  constructor() {
    this.ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY || '' });
    this.model = process.env.GEMINI_TEXT_MODEL || 'gemini-3.5-flash-lite';
    this.name = `Gemini (${this.model})`;
  }

  async generateJson(request: LlmJsonRequest): Promise<any> {
    const contents: any[] = [{ text: request.prompt }];
    if (request.image) {
      contents.push({
        inlineData: { mimeType: request.image.mimeType || 'image/jpeg', data: request.image.bytes.toString('base64') }
      });
    }

    const t0 = Date.now();
    const response = await withRetry(
      () =>
        this.ai.models.generateContent({
          model: this.model,
          contents,
          config: {
            responseMimeType: 'application/json',
            temperature: request.temperature
          }
        }),
      { retries: 2, delayMs: 1200, label: request.label }
    );

    const parsed = parseAndValidate(response.text || '{}', request, this.name);
    console.log(`    ✅ [Gemini] ${request.label} via ${this.model} in ${((Date.now() - t0) / 1000).toFixed(1)}s`);
    return parsed;
  }
}

/** Primary provider first; any error, timeout or invalid JSON falls through to the secondary one. */
export class FallbackLlmClient implements ILlmClient {
  readonly name: string;

  constructor(private primary: ILlmClient, private fallback: ILlmClient) {
    this.name = `${primary.name} -> fallback ${fallback.name}`;
  }

  async generateJson(request: LlmJsonRequest): Promise<any> {
    try {
      return await this.primary.generateJson(request);
    } catch (error: any) {
      console.warn(`🔁 [LLM] ${request.label}: ${this.primary.name} failed (${error?.name || ''} ${error?.message || error}). Using ${this.fallback.name}.`);
      return this.fallback.generateJson(request);
    }
  }
}

export type LlmProvider = 'bedrock' | 'gemini';

export function resolveLlmProvider(): LlmProvider {
  return process.env.LLM_PROVIDER?.trim().toLowerCase() === 'gemini' ? 'gemini' : 'bedrock';
}

let sharedClient: ILlmClient | null = null;

/**
 * LLM_PROVIDER=bedrock (default): Amazon Nova 2 Lite on Bedrock, Gemini as fallback.
 * LLM_PROVIDER=gemini: Gemini only.
 * One shared instance so the vision and story steps share Bedrock's circuit breaker.
 */
export function getLlmClient(): ILlmClient {
  if (!sharedClient) {
    const gemini = new GeminiLlmClient();
    sharedClient = resolveLlmProvider() === 'gemini' ? gemini : new FallbackLlmClient(new BedrockNovaLlmClient(), gemini);
  }
  return sharedClient;
}
