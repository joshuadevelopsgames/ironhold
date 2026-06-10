package kingdom.smp.entity;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * The 23 butterfly species (roster mirrored from the Luminous Butterflies concept —
 * names only; all models/textures are Ironhold-original). Each species is the single
 * source of truth for its fishing-bait stats AND its Butterfly Encyclopedia entry, and —
 * crucially — those entries are <em>truthful by construction</em>:
 *
 * <ul>
 *   <li>{@link #rarity()} is derived from {@link #baitPower} (the fishing ladder), so the
 *       encyclopedia rarity tier and the spawn-weight ladder can never disagree.</li>
 *   <li>{@link #location()} is derived from {@link #biome} — the actual vanilla biome the
 *       species spawns in (see {@link ButterflyEntity#finalizeSpawn}) — so the advertised
 *       location is literally where you'll catch it.</li>
 *   <li>{@link #likesItem} is the item the live entity is tempted by, paired with the
 *       {@link #likes} label shown in the book.</li>
 * </ul>
 *
 * <p>{@code baitPower} follows a Terraria-style ladder: common meadow species 10–14%, up to
 * dimensional species 40–50%. {@code themeId} (nullable) biases themed fishing loot;
 * {@code glowing} marks luminous species that emit a particle/light trail.
 */
public enum ButterflySpecies {
    MONARCH            ("monarch",             "Monarch",             10, null,     false, ModelShape.STANDARD,    Biomes.FOREST,           "Poppies",             Items.POPPY),
    BUCKEYE            ("buckeye",             "Buckeye",             12, null,     false, ModelShape.STANDARD,    Biomes.DARK_FOREST,      "Lilacs",              Items.LILAC),
    RINGLET            ("ringlet",             "Ringlet",             12, null,     false, ModelShape.SMALL,       Biomes.SAVANNA,          "Dandelions",          Items.DANDELION),
    LITTLE_WOOD        ("little_wood",         "Little Wood",         12, null,     false, ModelShape.SMALL,       Biomes.SNOWY_TAIGA,      "Dandelions",          Items.DANDELION),
    ORANGETIP          ("orangetip",           "Orangetip",           14, null,     false, ModelShape.SMALL,       Biomes.SNOWY_PLAINS,     "Lily of the Valley",  Items.LILY_OF_THE_VALLEY),
    WHITE_HAIRSTREAK   ("white_hairstreak",    "White Hairstreak",    14, null,     false, ModelShape.SWALLOWTAIL, Biomes.BIRCH_FOREST,     "White Tulips",        Items.WHITE_TULIP),
    SPRING_AZURE       ("spring_azure",        "Spring Azure",        15, null,     false, ModelShape.SMALL,       Biomes.FLOWER_FOREST,    "Alliums",             Items.ALLIUM),
    HAIRSTREAK         ("hairstreak",          "Hairstreak",          18, null,     false, ModelShape.SWALLOWTAIL, Biomes.DARK_FOREST,      "Peonies",             Items.PEONY),
    BLACK_SWALLOWTAIL  ("black_swallowtail",   "Black Swallowtail",   20, null,     false, ModelShape.SWALLOWTAIL, Biomes.FOREST,           "Oxeye Daisies",       Items.OXEYE_DAISY),
    YELLOW_SWALLOWTAIL ("yellow_swallowtail",  "Yellow Swallowtail",  20, null,     false, ModelShape.SWALLOWTAIL, Biomes.SUNFLOWER_PLAINS, "Sunflowers",          Items.SUNFLOWER),
    MOURNING_CLOAK     ("mourning_cloak",      "Mourning Cloak",      22, null,     false, ModelShape.STANDARD,    Biomes.SWAMP,            "Blue Orchids",        Items.BLUE_ORCHID),
    CHERRY_ROSE        ("cherry_rose",         "Cherry Rose",         22, null,     false, ModelShape.STANDARD,    Biomes.CHERRY_GROVE,     "Pink Petals",         Items.PINK_PETALS),
    ZEBRA_LONGWING     ("zebra_longwing",      "Zebra Longwing",      24, null,     false, ModelShape.STANDARD,    Biomes.MANGROVE_SWAMP,   "Mangrove Propagules", Items.MANGROVE_PROPAGULE),
    RUSTYPAGE          ("rustypage",           "Rustypage",           24, null,     false, ModelShape.STANDARD,    Biomes.LUSH_CAVES,       "Glow Berries",        Items.GLOW_BERRIES),
    CHARAXES           ("charaxes",            "Charaxes",            28, null,     false, ModelShape.SWALLOWTAIL, Biomes.SAVANNA,          "Poppies",             Items.POPPY),
    BIRDWING           ("birdwing",            "Birdwing",            30, null,     false, ModelShape.BROAD,       Biomes.JUNGLE,           "Azure Bluet",         Items.AZURE_BLUET),
    BLUE_MONARCH       ("blue_monarch",        "Blue Monarch",        32, null,     false, ModelShape.BROAD,       Biomes.FLOWER_FOREST,    "Cornflowers",         Items.CORNFLOWER),
    EMERALD_SWALLOWTAIL("emerald_swallowtail", "Emerald Swallowtail", 34, null,     false, ModelShape.SWALLOWTAIL, Biomes.LUSH_CAVES,       "Rose Bushes",         Items.ROSE_BUSH),
    CRIMSON_MONARCH    ("crimson_monarch",     "Crimson Monarch",     40, "nether", true,  ModelShape.STANDARD,    Biomes.CRIMSON_FOREST,   "Crimson Fungus",      Items.CRIMSON_FUNGUS),
    GLOWSTONE_MORPHO   ("glowstone_morpho",    "Glowstone Morpho",    44, "nether", true,  ModelShape.BROAD,       Biomes.NETHER_WASTES,    "Glowstone",           Items.GLOWSTONE),
    CHORUS_MORPHO      ("chorus_morpho",       "Chorus Morpho",       46, "end",    true,  ModelShape.BROAD,       Biomes.END_HIGHLANDS,    "Chorus Flowers",      Items.CHORUS_FLOWER),
    ENDER_EYESPOT      ("ender_eyespot",       "Ender Eyespot",       48, "end",    true,  ModelShape.STANDARD,    Biomes.END_MIDLANDS,     "Chorus Fruit",        Items.CHORUS_FRUIT),
    SOUL_MONARCH       ("soul_monarch",        "Soul Monarch",        50, "soul",   true,  ModelShape.BROAD,       Biomes.SOUL_SAND_VALLEY, "Soul Torches",        Items.SOUL_TORCH);

    /** Encyclopedia rarity tier, derived from {@link #baitPower}. */
    public enum Rarity {
        COMMON("Common", 0xFF6E6E6E),
        RARE("Rare", 0xFF2C6FB0),
        EPIC("Epic", 0xFF8A33C0);

        private final String label;
        private final int color;

        Rarity(String label, int color) {
            this.label = label;
            this.color = color;
        }

        public String label() { return label; }

        /** ARGB colour for the rarity label in the encyclopedia. */
        public int color() { return color; }
    }

    public enum ModelShape {
        STANDARD("standard"),
        SWALLOWTAIL("swallowtail"),
        BROAD("broad"),
        SMALL("small");

        private final String id;

        ModelShape(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    private final String id;
    private final String displayName;
    private final int baitPower;
    private final String themeId; // nullable — no loot theme
    private final boolean glowing;
    private final ModelShape modelShape;
    private final ResourceKey<Biome> biome;
    private final String likes;
    private final Item likesItem;

    ButterflySpecies(String id, String displayName, int baitPower, String themeId, boolean glowing,
                     ModelShape modelShape, ResourceKey<Biome> biome, String likes, Item likesItem) {
        this.id = id;
        this.displayName = displayName;
        this.baitPower = baitPower;
        this.themeId = themeId;
        this.glowing = glowing;
        this.modelShape = modelShape;
        this.biome = biome;
        this.likes = likes;
        this.likesItem = likesItem;
    }

    public String id() { return id; }

    public String displayName() { return displayName; }

    public int baitPower() { return baitPower; }

    /** Nullable themed-loot key, or {@code null} for no theme. */
    public String themeId() { return themeId; }

    public boolean isGlowing() { return glowing; }

    public ModelShape modelShape() { return modelShape; }

    /** The vanilla biome this species naturally spawns in (drives both spawning and the book). */
    public ResourceKey<Biome> biome() { return biome; }

    /** Favourite flower/block this species is tempted by — shown in the encyclopedia. */
    public String likes() { return likes; }

    /** The item the live entity is tempted by — kept in lock-step with {@link #likes}. */
    public Item likesItem() { return likesItem; }

    /**
     * Display name of {@link #biome}, derived so the encyclopedia's "Location" line is
     * literally the biome the species spawns in (e.g. {@code end_highlands → "End Highlands"}).
     */
    public String location() {
        String path = biome.identifier().getPath();
        StringBuilder sb = new StringBuilder(path.length());
        boolean capNext = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '_') {
                sb.append(' ');
                capNext = true;
            } else if (capNext) {
                sb.append(Character.toUpperCase(c));
                capNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Encyclopedia rarity tier, derived from the fishing-bait ladder. */
    public Rarity rarity() {
        if (baitPower <= 18) return Rarity.COMMON;
        if (baitPower <= 34) return Rarity.RARE;
        return Rarity.EPIC;
    }

    public static ButterflySpecies byOrdinal(int ordinal) {
        ButterflySpecies[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }

    /** Lookup by string id; falls back to {@link #MONARCH} for unknown ids. */
    public static ButterflySpecies byId(String id) {
        for (ButterflySpecies s : values()) {
            if (s.id.equals(id)) return s;
        }
        return MONARCH;
    }
}
