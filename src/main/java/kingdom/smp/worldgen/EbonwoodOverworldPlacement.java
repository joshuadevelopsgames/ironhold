package kingdom.smp.worldgen;

/**
 * EbonwoodOverworldPlacement — documents and centralises the climate parameters
 * for placing the Ebonwood Hollow biome in the overworld MultiNoise source.
 *
 * ════════════════════════════════════════════════════════════════════════════
 * CURRENT STATUS: ACTIVE — placed via OverworldBiomeBuilderMixin
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Ebonwood Hollow is injected into the overworld MultiNoiseBiomeSource by
 * {@code kingdom.smp.mixin.OverworldBiomeBuilderMixin}, which appends one
 * Climate.ParameterPoint at the tail of {@code OverworldBiomeBuilder.addBiomes()}.
 * This fires for every world that uses the {@code minecraft:overworld} preset.
 *
 * The three original implementation paths are documented below for reference:
 *
 * ────────────────────────────────────────────────────────────────────────────
 * PATH A — TerraBlender (preferred, Tectonic/Terralith compatible)
 * ────────────────────────────────────────────────────────────────────────────
 * Add TerraBlender as a dependency in build.gradle once a build for MC 26.x is
 * available from https://github.com/Glitchfiend/TerraBlender :
 *
 *   repositories {
 *       maven { url "https://maven.glitchfiend.io/public/" }
 *   }
 *   dependencies {
 *       implementation "com.github.glitchfiend:TerraBlender-neoforge:${mc_version}-${tb_version}"
 *   }
 *
 * Then un-comment the Region class below and register it:
 *
 *   // In Ironhold constructor:
 *   terrablender.api.RegionManager.register(new EbonwoodRegion());
 *
 * ────────────────────────────────────────────────────────────────────────────
 * PATH B — Vanilla dimension override (no extra deps, conflicts w/ terrain mods)
 * ────────────────────────────────────────────────────────────────────────────
 * Create data/minecraft/dimension/overworld.json that includes ALL vanilla biomes
 * plus ironhold:ebonwood_hollow with the parameters below.  This file will
 * conflict with Terralith/Tectonic datapacks if they also override it.
 *
 * The Ebonwood Hollow climate niche (mimics pale garden rarity):
 *   temperature  : [-0.45, -0.15]   — cool
 *   humidity     : [-0.35,  0.10]   — dryish
 *   continentalness: [0.30,  1.00]  — inland only
 *   erosion      : [-0.78, -0.375]  — hilly/upland
 *   weirdness    : [ 0.56,  1.00]   — high-weirdness band (rare)
 *   depth        : [0.0,    0.0]    — surface layer only
 *   offset       : 0.0
 *
 * ────────────────────────────────────────────────────────────────────────────
 * PATH C — Testing without overworld placement
 * ────────────────────────────────────────────────────────────────────────────
 * In a creative world use the single-biome preset:
 *   /gamemode creative
 *   Create a new world → More options → World Type: Single Biome
 *   → Choose "Ebonwood Hollow" (ironhold:ebonwood_hollow)
 *
 * Or place the biome manually in any world with:
 *   /place biome ironhold:ebonwood_hollow  (1.21+ command)
 */

/*
 * ─── TerraBlender Region (uncomment when TB is available for 26.x) ────────
 *
 * import terrablender.api.Region;
 * import terrablender.api.RegionType;
 * import com.mojang.datafixer.util.Pair;
 * import net.minecraft.core.Registry;
 * import net.minecraft.world.level.biome.Biome;
 * import net.minecraft.world.level.biome.Climate;
 * import net.minecraft.resources.Identifier;   // renamed from ResourceLocation in MC 26.x
 * import java.util.function.Consumer;
 * import kingdom.smp.Ironhold;
 *
 * public class EbonwoodRegion extends Region {
 *
 *     public EbonwoodRegion() {
 *         super(
 *             Identifier.fromNamespaceAndPath(Ironhold.MODID, "ebonwood_region"),
 *             RegionType.OVERWORLD,
 *             2   // weight = 2 → rare, roughly equivalent to pale garden frequency
 *         );
 *     }
 *
 *     @Override
 *     public void addBiomes(Registry<Biome> registry,
 *                           Consumer<Pair<Climate.ParameterPoint, net.minecraft.resources.ResourceKey<Biome>>> mapper) {
 *         addBiome(mapper,
 *             Climate.parameters(
 *                 Climate.Parameter.span(-0.45f, -0.15f),  // temperature
 *                 Climate.Parameter.span(-0.35f,  0.10f),  // humidity
 *                 Climate.Parameter.span( 0.30f,  1.00f),  // continentalness
 *                 Climate.Parameter.span(-0.78f, -0.375f), // erosion
 *                 Climate.Parameter.point(0.0f),            // depth (surface)
 *                 Climate.Parameter.span( 0.56f,  1.00f),  // weirdness
 *                 0.0f                                       // offset
 *             ),
 *             Ironhold.EBONWOOD_HOLLOW
 *         );
 *     }
 * }
 */
public final class EbonwoodOverworldPlacement {
    private EbonwoodOverworldPlacement() {}
}
