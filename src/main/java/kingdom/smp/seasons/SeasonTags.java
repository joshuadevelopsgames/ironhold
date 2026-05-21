package kingdom.smp.seasons;

import kingdom.smp.Ironhold;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

/** Tag keys consumed by the seasons system. */
public final class SeasonTags {
    private SeasonTags() {}

    public static final TagKey<Block> SPRING_CROPS     = blockTag("spring_crops");
    public static final TagKey<Block> SUMMER_CROPS     = blockTag("summer_crops");
    public static final TagKey<Block> AUTUMN_CROPS     = blockTag("autumn_crops");
    public static final TagKey<Block> WINTER_CROPS     = blockTag("winter_crops");
    public static final TagKey<Block> YEAR_ROUND_CROPS = blockTag("year_round_crops");
    public static final TagKey<Block> GREENHOUSE_GLASS = blockTag("greenhouse_glass");
    public static final TagKey<Block> UNBREAKABLE_INFERTILE_CROPS = blockTag("unbreakable_infertile_crops");

    public static final TagKey<Biome> TROPICAL_BIOMES     = biomeTag("tropical_biomes");
    public static final TagKey<Biome> BLACKLISTED_BIOMES  = biomeTag("blacklisted_biomes");
    public static final TagKey<Biome> INFERTILE_BIOMES    = biomeTag("infertile_biomes");

    private static TagKey<Block> blockTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Ironhold.MODID, path));
    }

    private static TagKey<Biome> biomeTag(String path) {
        return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(Ironhold.MODID, path));
    }
}
