package kingdom.smp.entity;

import net.minecraft.util.RandomSource;

/**
 * Profession for Kingdom Villagers. Each profession defines whether the villager
 * can speak (via OpenRouter) or is silent (expresses through emotes/behavior only).
 */
public enum VillagerProfession {
    // ── Silent professions (thought bubbles + behavioral AI) ─────────────────
    BLACKSMITH("blacksmith", "Blacksmith", false,
        "A hardworking metalworker who forges and repairs equipment.",
        new String[]{
            "The blacksmith examines your gear with a critical eye.",
            "Sparks fly as the blacksmith hammers away.",
            "The blacksmith grunts and gestures at your equipment."
        }),
    FARMER("farmer", "Farmer", false,
        "A humble farmer who tends crops and provides food.",
        new String[]{
            "The farmer wipes sweat from their brow and offers you some produce.",
            "The farmer hums while checking their crops.",
            "The farmer gestures toward the fields."
        }),
    GUARD("guard", "Guard", false,
        "A vigilant protector who watches over the village.",
        new String[]{
            "The guard nods at you and grips their weapon tighter.",
            "The guard scans the horizon for threats.",
            "The guard stands at attention."
        }),
    MERCHANT("merchant", "Merchant", false,
        "A shrewd trader who buys and sells goods.",
        new String[]{
            "The merchant rubs their hands together expectantly.",
            "The merchant arranges their wares carefully.",
            "The merchant eyes your coinpurse."
        }),
    ALCHEMIST("alchemist", "Alchemist", false,
        "A mysterious brewer of potions and elixirs.",
        new String[]{
            "The alchemist peers at you through bubbling vapors.",
            "Strange liquids bubble and hiss on the alchemist's table.",
            "The alchemist sniffs the air curiously."
        }),

    // ── Talking professions (full AI dialogue) ───────────────────────────────
    WIZARD("wizard", "Wizard", true,
        "An arcane scholar who studies forbidden magic and enchantments. "
            + "Speaks in cryptic, layered sentences. References ancient tomes and constellations. "
            + "Fascinated by the player's class abilities. Remembers what spells and enchantments "
            + "have been discussed before.",
        new String[]{
            "The stars whisper of your arrival... though they rarely agree on what it means.",
            "Ah, another seeker. The last one turned into a newt. Temporarily.",
            "Knowledge is a flame. I merely decide who gets burned."
        }),
    PRIEST("priest", "Priest", true,
        "A spiritual guide who channels divine energy for healing and blessings. "
            + "Speaks with warmth or stern authority depending on temperament. Comments on the "
            + "player's moral choices. Notices when players have been fighting or helping others. "
            + "Stronger connection with Cleric class players.",
        new String[]{
            "The light sees all, child. Even what you hide from yourself.",
            "I sense weariness in your soul. Come, let me ease your burden.",
            "Every scar tells a story. Yours tell... quite a few."
        }),
    LIBRARIAN("librarian", "Librarian", true,
        "A meticulous scholar and keeper of forbidden knowledge. "
            + "Speaks precisely, often corrects people. Has opinions about everything and isn't "
            + "shy about sharing them. Remembers what topics have been discussed. Gets excited "
            + "about obscure lore. Slightly condescending but means well.",
        new String[]{
            "Actually, the correct term is — oh never mind, you wouldn't understand.",
            "I've been cataloging the dreams of endermen. Fascinating and terrible.",
            "Another visitor. Please don't touch the restricted section. Again."
        }),
    BARD("bard", "Bard", true,
        "A charismatic performer and storyteller who weaves magic through music and verse. "
            + "Speaks dramatically, sometimes in rhyme or meter. Tells stories about server events "
            + "and other players. Generates unique song lines. Loves an audience. Gets moody "
            + "without attention.",
        new String[]{
            "A hero approaches! Or a villain. The song hasn't decided yet.",
            "Listen well, for I sing of battles won and meals half-eaten!",
            "The road goes ever on, and frankly, my feet are killing me."
        });

    private final String id;
    private final String displayName;
    private final boolean canTalk;
    private final String llmFlavor;
    private final String[] fallbackLines;

    VillagerProfession(String id, String displayName, boolean canTalk,
                       String llmFlavor, String[] fallbackLines) {
        this.id = id;
        this.displayName = displayName;
        this.canTalk = canTalk;
        this.llmFlavor = llmFlavor;
        this.fallbackLines = fallbackLines;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public boolean canTalk() { return canTalk; }
    public String llmFlavor() { return llmFlavor; }
    public String[] fallbackLines() { return fallbackLines; }

    /** Pick a random fallback line for when LLM is unavailable or on cooldown. */
    public String randomFallback(RandomSource rand) {
        return fallbackLines[rand.nextInt(fallbackLines.length)];
    }

    public static VillagerProfession random(RandomSource rand) {
        VillagerProfession[] values = values();
        return values[rand.nextInt(values.length)];
    }

    public static VillagerProfession fromId(String id) {
        for (VillagerProfession p : values()) {
            if (p.id.equals(id)) return p;
        }
        return FARMER;
    }

    // ── Name pools per profession ────────────────────────────────────────────

    private static final String[] BLACKSMITH_NAMES = {
        "Grimjaw", "Ironhand", "Torven", "Brokk", "Hilda", "Slag", "Ember", "Anvilda"
    };
    private static final String[] FARMER_NAMES = {
        "Barley", "Turnip", "Midge", "Clover", "Hayseed", "Bramble", "Parsnip", "Thatch"
    };
    private static final String[] GUARD_NAMES = {
        "Sentinel", "Bulwark", "Grim", "Shieldwall", "Vigil", "Ironsides", "Bastion", "Ward"
    };
    private static final String[] MERCHANT_NAMES = {
        "Coinsworth", "Haggle", "Goldtooth", "Barter", "Fleece", "Tally", "Margin", "Markup"
    };
    private static final String[] ALCHEMIST_NAMES = {
        "Fizzwick", "Bubbles", "Tincture", "Mordant", "Flask", "Reagent", "Pipette", "Sublimate"
    };
    private static final String[] WIZARD_NAMES = {
        "Thalindor", "Mystara", "Grimoire", "Ashwick", "Vortaine", "Spelldust", "Noctum", "Aethel"
    };
    private static final String[] PRIEST_NAMES = {
        "Brother Aldric", "Sister Maren", "Father Cael", "Mother Vesper",
        "Abbot Theron", "Deacon Lira", "Prior Ambrose", "Seraph"
    };
    private static final String[] LIBRARIAN_NAMES = {
        "Index", "Margin", "Footnote", "Quillon", "Dewey", "Codex", "Errata", "Tome"
    };
    private static final String[] BARD_NAMES = {
        "Lyric", "Cadence", "Stanza", "Riff", "Coda", "Tempo", "Ballad", "Encore"
    };

    /** Generate a random name appropriate for this profession. */
    public String randomName(RandomSource rand) {
        String[] pool = switch (this) {
            case BLACKSMITH -> BLACKSMITH_NAMES;
            case FARMER     -> FARMER_NAMES;
            case GUARD      -> GUARD_NAMES;
            case MERCHANT   -> MERCHANT_NAMES;
            case ALCHEMIST  -> ALCHEMIST_NAMES;
            case WIZARD     -> WIZARD_NAMES;
            case PRIEST     -> PRIEST_NAMES;
            case LIBRARIAN  -> LIBRARIAN_NAMES;
            case BARD       -> BARD_NAMES;
        };
        return pool[rand.nextInt(pool.length)];
    }
}
