package kingdom.smp;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

// Ironhold server config. Registered as ModConfig.Type.SERVER so the API keys
// below are NEVER synced to connecting clients (COMMON configs are). All values
// here are read server-side only.
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ── Void Invoker LLM (OpenRouter) ─────────────────────────────────────────
    // Set these on the SERVER only — never share your API key publicly.
    // Get a free key at https://openrouter.ai
    public static final ModConfigSpec.ConfigValue<String> OPENROUTER_API_KEY = BUILDER
            .comment("OpenRouter API key for the Void Invoker LLM taunts. Server-side only.")
            .define("openrouterApiKey", "");

    public static final ModConfigSpec.ConfigValue<String> OPENROUTER_MODEL = BUILDER
            .comment("OpenRouter model to use. Free options: meta-llama/llama-3.1-8b-instruct:free, mistralai/mistral-7b-instruct:free")
            .define("openrouterModel", "mistralai/mistral-7b-instruct:free");

    // ── Kangarude NPC: brain ──────────────────────────────────────────────────
    // Calls Claude (or any OpenRouter model) via the OpenRouter REST API.
    // Uses OPENROUTER_API_KEY above (env var override: OPENROUTER_API_KEY).
    public static final ModConfigSpec.ConfigValue<String> KANGARUDE_OPENROUTER_MODEL = BUILDER
            .comment("OpenRouter model id for Kangarude. Default is Claude Haiku 4.5 — "
                + "matches the rest of the voiced NPCs, lower TTFB than Grok for "
                + "Kanga's long personality prompt. Set to 'x-ai/grok-4-fast' to "
                + "revert. Find live ids at https://openrouter.ai/models")
            .define("kangarudeOpenrouterModel", "anthropic/claude-haiku-4.5");

    public static final ModConfigSpec.IntValue KANGARUDE_IDLE_TIMEOUT_SECONDS = BUILDER
            .comment("How long Kangarude waits for the player to respond before walking off.")
            .defineInRange("kangarudeIdleTimeoutSeconds", 600, 5, 600);

    // ── Kangarude NPC: ElevenLabs voice ───────────────────────────────────────
    // Server-side only. Set ELEVENLABS_API_KEY in config or via environment override.
    public static final ModConfigSpec.ConfigValue<String> ELEVENLABS_API_KEY = BUILDER
            .comment("ElevenLabs API key for Kangarude TTS. Server-side only — keep private.")
            .define("elevenlabsApiKey", "");

    public static final ModConfigSpec.ConfigValue<String> ELEVENLABS_VOICE_ID = BUILDER
            .comment("ElevenLabs voice id for Kangarude. Find ids in your voice library.")
            .define("elevenlabsVoiceId", "tdlj9WjgHdDTMKoAvBYQ");

    public static final ModConfigSpec.ConfigValue<String> ELEVENLABS_MODEL = BUILDER
            .comment("ElevenLabs model. eleven_flash_v2_5 is fastest (~200ms TTFB), eleven_multilingual_v2 is highest quality.")
            .define("elevenlabsModel", "eleven_flash_v2_5");

    // ── OpenAI Whisper STT (for Kangarude mic input) ──────────────────────────
    // Env var OPENAI_API_KEY overrides this if set.
    public static final ModConfigSpec.ConfigValue<String> OPENAI_API_KEY = BUILDER
            .comment("OpenAI API key for Whisper STT (lets players talk to NPCs via mic). Server-side only.")
            .define("openaiApiKey", "");

    public static final ModConfigSpec.ConfigValue<String> OPENAI_WHISPER_MODEL = BUILDER
            .comment("OpenAI transcription model. gpt-4o-mini-transcribe is ~2x faster and "
                + "cheaper than whisper-1 at equal-or-better accuracy. NOTE: a toml saved "
                + "before this default changed still pins whisper-1 — edit it by hand.")
            .define("openaiWhisperModel", "gpt-4o-mini-transcribe");

    /** How long (ms) of mic silence ends a player's utterance and triggers STT. */
    public static final ModConfigSpec.IntValue STT_SILENCE_MS = BUILDER
            .comment("Milliseconds of silence after a player stops speaking before sending the buffered audio to STT.")
            .defineInRange("sttSilenceMs", 1000, 200, 5000);

    // ── Spawn lobby ───────────────────────────────────────────────────────────
    // Keep lobbyEnabled false until you've built the lobby and set its spawn and
    // exit portal in-game with /lobby setspawn and /lobby setportal pos1|pos2.
    public static final ModConfigSpec.BooleanValue LOBBY_ENABLED = BUILDER
            .comment("Master switch for the spawn lobby. Leave false until the lobby spawn and exit portal are set with /lobby setspawn and /lobby setportal.")
            .define("lobbyEnabled", false);

    public static final ModConfigSpec.BooleanValue LOBBY_EVERY_JOIN = BUILDER
            .comment("true: every login routes the player into the lobby. false: only players never seen before (first join only).")
            .define("lobbyEveryJoin", false);

    static final ModConfigSpec SPEC = BUILDER.build();
}
