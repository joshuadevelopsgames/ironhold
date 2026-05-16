package kingdom.smp.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Datapack-defined "rules of engagement" for an NPC: what they sell, what
 * secrets they'll part with for coin, what gifts they love or loathe, and how
 * snobby they are about all of it. One manifest per profession (default) or
 * per named NPC (override).
 *
 * <p>JSON layout (everything except {@code tier} is optional):
 * <pre>{@code
 * {
 *   "tier": "commoner|merchant|nobility|royalty",
 *   "currency": "minecraft:emerald",
 *   "price_multiplier": 1.0,
 *   "min_rapport_for_audience": -50,
 *   "min_rapport_for_trade":    -20,
 *   "llm_flavor": "One-line color the LLM gets in its system prompt.",
 *   "wares":    [ { "item": "minecraft:iron_pickaxe", "base_price": 12, "stock": 1 } ],
 *   "secrets":  [ { "key": "iron_seam_north", "summary": "...", "base_price": 6 } ],
 *   "tastes":   [ { "item": "ironhold:high_quality_iron_ore", "rapport_delta": 8, "reaction": "..." } ],
 *   "disdains": [ { "item": "minecraft:gold_ingot", "rapport_delta": -2, "reaction": "..." } ]
 * }
 * }</pre>
 */
public record NpcManifest(
    @Nullable Identifier id,
    Tier tier,
    String currencyId,
    float priceMultiplier,
    int minRapportForAudience,
    int minRapportForTrade,
    String llmFlavor,
    List<Ware> wares,
    List<Secret> secrets,
    List<Taste> tastes,
    List<Disdain> disdains
) {
    public enum Tier {
        COMMONER, MERCHANT, NOBILITY, ROYALTY;

        public static Tier parse(String s) {
            if (s == null) return COMMONER;
            return switch (s.toLowerCase()) {
                case "merchant" -> MERCHANT;
                case "nobility", "noble" -> NOBILITY;
                case "royalty", "royal" -> ROYALTY;
                default -> COMMONER;
            };
        }

        public boolean atLeast(Tier t) { return ordinal() >= t.ordinal(); }
    }

    public record Ware(String item, int basePrice, int stock) {}
    public record Secret(String key, String summary, int basePrice) {}
    public record Taste(String item, int rapportDelta, String reaction) {}
    public record Disdain(String item, int rapportDelta, String reaction) {}

    public static final NpcManifest EMPTY = new NpcManifest(
        null, Tier.COMMONER, "minecraft:emerald", 1.0f,
        -100, -100, "",
        List.of(), List.of(), List.of(), List.of());

    public int priceFor(Ware w) { return Math.max(1, Math.round(w.basePrice() * priceMultiplier)); }
    public int priceFor(Secret s) { return Math.max(1, Math.round(s.basePrice() * priceMultiplier)); }

    public @Nullable Taste tasteFor(String itemId) {
        for (Taste t : tastes) if (t.item().equals(itemId)) return t;
        return null;
    }

    public @Nullable Disdain disdainFor(String itemId) {
        for (Disdain d : disdains) if (d.item().equals(itemId)) return d;
        return null;
    }

    public @Nullable Ware wareFor(String itemId) {
        for (Ware w : wares) if (w.item().equals(itemId)) return w;
        return null;
    }

    public @Nullable Secret secret(String key) {
        for (Secret s : secrets) if (s.key().equals(key)) return s;
        return null;
    }

    // ── JSON parser ──────────────────────────────────────────────────────────

    public static NpcManifest parse(Identifier id, JsonObject root) {
        Tier tier = Tier.parse(optStr(root, "tier", "commoner"));
        String currency = optStr(root, "currency", "minecraft:emerald");
        float mul = optFloat(root, "price_multiplier", defaultMultiplier(tier));
        int minAud = optInt(root, "min_rapport_for_audience", defaultMinAudience(tier));
        int minTrade = optInt(root, "min_rapport_for_trade", defaultMinTrade(tier));
        String flavor = optStr(root, "llm_flavor", "");

        return new NpcManifest(
            id, tier, currency, mul, minAud, minTrade, flavor,
            parseWares(root.getAsJsonArray("wares")),
            parseSecrets(root.getAsJsonArray("secrets")),
            parseTastes(root.getAsJsonArray("tastes")),
            parseDisdains(root.getAsJsonArray("disdains")));
    }

    private static float defaultMultiplier(Tier t) {
        return switch (t) {
            case COMMONER -> 1.0f;
            case MERCHANT -> 1.25f;
            case NOBILITY -> 2.0f;
            case ROYALTY  -> 4.0f;
        };
    }

    private static int defaultMinAudience(Tier t) {
        return switch (t) {
            case COMMONER -> -100;
            case MERCHANT -> -50;
            case NOBILITY -> 0;
            case ROYALTY  -> 25;
        };
    }

    private static int defaultMinTrade(Tier t) {
        return switch (t) {
            case COMMONER -> -100;
            case MERCHANT -> -20;
            case NOBILITY -> 10;
            case ROYALTY  -> 40;
        };
    }

    private static List<Ware> parseWares(@Nullable JsonArray arr) {
        if (arr == null) return List.of();
        List<Ware> out = new ArrayList<>(arr.size());
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            out.add(new Ware(
                optStr(o, "item", ""),
                optInt(o, "base_price", 1),
                optInt(o, "stock", 1)));
        }
        return List.copyOf(out);
    }

    private static List<Secret> parseSecrets(@Nullable JsonArray arr) {
        if (arr == null) return List.of();
        List<Secret> out = new ArrayList<>(arr.size());
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            out.add(new Secret(
                optStr(o, "key", ""),
                optStr(o, "summary", ""),
                optInt(o, "base_price", 1)));
        }
        return List.copyOf(out);
    }

    private static List<Taste> parseTastes(@Nullable JsonArray arr) {
        if (arr == null) return List.of();
        List<Taste> out = new ArrayList<>(arr.size());
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            out.add(new Taste(
                optStr(o, "item", ""),
                optInt(o, "rapport_delta", 0),
                optStr(o, "reaction", "")));
        }
        return List.copyOf(out);
    }

    private static List<Disdain> parseDisdains(@Nullable JsonArray arr) {
        if (arr == null) return List.of();
        List<Disdain> out = new ArrayList<>(arr.size());
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            out.add(new Disdain(
                optStr(o, "item", ""),
                optInt(o, "rapport_delta", 0),
                optStr(o, "reaction", "")));
        }
        return List.copyOf(out);
    }

    private static String optStr(JsonObject o, String key, String fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : fallback;
    }

    private static int optInt(JsonObject o, String key, int fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : fallback;
    }

    private static float optFloat(JsonObject o, String key, float fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsFloat() : fallback;
    }
}
