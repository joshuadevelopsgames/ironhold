package kingdom.smp;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

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
            .defineInRange("kangarudeIdleTimeoutSeconds", 25, 5, 600);

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
            .comment("OpenAI Whisper model. whisper-1 is the standard transcription model.")
            .define("openaiWhisperModel", "whisper-1");

    /** How long (ms) of mic silence ends a player's utterance and triggers STT. */
    public static final ModConfigSpec.IntValue STT_SILENCE_MS = BUILDER
            .comment("Milliseconds of silence after a player stops speaking before sending the buffered audio to STT.")
            .defineInRange("sttSilenceMs", 1000, 200, 5000);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(Identifier.parse(itemName));
    }
}
