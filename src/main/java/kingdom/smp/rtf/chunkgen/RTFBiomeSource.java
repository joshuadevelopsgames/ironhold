package kingdom.smp.rtf.chunkgen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import kingdom.smp.rtf.GeneratorContext;
import kingdom.smp.rtf.cell.Cell;
import kingdom.smp.rtf.data.preset.settings.BuiltinPresets;
import kingdom.smp.rtf.data.preset.settings.Preset;
import kingdom.smp.rtf.heightmap.WorldLookup;
import kingdom.smp.rtf.terrain.Terrain;
import kingdom.smp.rtf.terrain.TerrainType;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * RTF-flavored biome source: routes biome selection through RTF's cell climate when an
 * active GeneratorContext is available, falling back to a wrapped vanilla biome source
 * (typically {@code minecraft:multi_noise} with the overworld preset).
 *
 * Biome candidates are partitioned by tag (ocean / river / beach / mountain / hot / cold / wet / dry)
 * the first time they're needed, so any biome registry the fallback contributes is automatically
 * categorized — including modded biomes that opt into the relevant tags.
 */
public final class RTFBiomeSource extends BiomeSource {
    public static final MapCodec<RTFBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BiomeSource.CODEC.fieldOf("fallback").forGetter(s -> s.fallback)
    ).apply(instance, RTFBiomeSource::new));

    private final BiomeSource fallback;

    private volatile WorldLookup lookup;
    private volatile Long lookupSeed;
    private volatile boolean categorized;

    private final List<Holder<Biome>> deepOcean = new ArrayList<>();
    private final List<Holder<Biome>> ocean = new ArrayList<>();
    private final List<Holder<Biome>> frozenOcean = new ArrayList<>();
    private final List<Holder<Biome>> river = new ArrayList<>();
    private final List<Holder<Biome>> frozenRiver = new ArrayList<>();
    private final List<Holder<Biome>> beach = new ArrayList<>();
    private final List<Holder<Biome>> snowyBeach = new ArrayList<>();
    private final List<Holder<Biome>> stonyShore = new ArrayList<>();
    private final List<Holder<Biome>> mountainPeaks = new ArrayList<>();
    private final List<Holder<Biome>> mountainSlopes = new ArrayList<>();
    private final List<Holder<Biome>> coldDry = new ArrayList<>();
    private final List<Holder<Biome>> coldWet = new ArrayList<>();
    private final List<Holder<Biome>> temperateDry = new ArrayList<>();
    private final List<Holder<Biome>> temperateWet = new ArrayList<>();
    private final List<Holder<Biome>> hotDry = new ArrayList<>();
    private final List<Holder<Biome>> hotWet = new ArrayList<>();
    private final List<Holder<Biome>> badlands = new ArrayList<>();

    public RTFBiomeSource(BiomeSource fallback) {
        this.fallback = fallback;
    }

    public BiomeSource fallback() {
        return fallback;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return fallback.possibleBiomes().stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        ensureLookup();
        WorldLookup lookup = this.lookup;
        if (lookup == null) {
            return fallback.getNoiseBiome(quartX, quartY, quartZ, sampler);
        }
        ensureCategorized();
        int worldX = quartX << 2;
        int worldZ = quartZ << 2;
        Cell cell = new Cell();
        lookup.apply(cell, worldX, worldZ);
        Holder<Biome> picked = pickBiome(cell, quartY << 2);
        return picked != null ? picked : fallback.getNoiseBiome(quartX, quartY, quartZ, sampler);
    }

    private void ensureLookup() {
        Long hint = RTFChunkGenerator.getGlobalSeedHint();
        if (hint == null) {
            return;
        }
        if (this.lookupSeed != null && this.lookupSeed.equals(hint) && this.lookup != null) {
            return;
        }
        synchronized (this) {
            if (this.lookupSeed != null && this.lookupSeed.equals(hint) && this.lookup != null) {
                return;
            }
            Preset preset = BuiltinPresets.makeDefault();
            GeneratorContext ctx = GeneratorContext.makeCached(preset, hint.intValue(), 16, 2, false);
            this.lookup = ctx.lookup;
            this.lookupSeed = hint;
        }
    }

    private void ensureCategorized() {
        if (categorized) {
            return;
        }
        synchronized (this) {
            if (categorized) {
                return;
            }
            for (Holder<Biome> b : fallback.possibleBiomes()) {
                if (b.is(BiomeTags.IS_DEEP_OCEAN)) deepOcean.add(b);
                else if (b.is(BiomeTags.IS_OCEAN) && b.is(BiomeTags.HAS_VILLAGE_SNOWY)) frozenOcean.add(b);
                else if (b.is(BiomeTags.IS_OCEAN)) ocean.add(b);
                if (b.is(BiomeTags.IS_RIVER) && b.is(BiomeTags.HAS_VILLAGE_SNOWY)) frozenRiver.add(b);
                else if (b.is(BiomeTags.IS_RIVER)) river.add(b);
                if (b.is(BiomeTags.IS_BEACH) && b.is(BiomeTags.HAS_VILLAGE_SNOWY)) snowyBeach.add(b);
                else if (b.is(BiomeTags.IS_BEACH)) beach.add(b);
                if (b.is(BiomeTags.IS_BADLANDS)) badlands.add(b);
                if (b.is(BiomeTags.IS_MOUNTAIN)) {
                    mountainPeaks.add(b);
                    mountainSlopes.add(b);
                }
                boolean isCold = b.is(BiomeTags.IS_TAIGA) || b.is(BiomeTags.HAS_VILLAGE_SNOWY);
                boolean isHot = b.is(BiomeTags.IS_JUNGLE) || b.is(BiomeTags.IS_SAVANNA) || b.is(BiomeTags.HAS_DESERT_PYRAMID);
                boolean isWet = b.is(BiomeTags.IS_FOREST) || b.is(BiomeTags.IS_JUNGLE) || b.is(BiomeTags.IS_TAIGA);
                if (b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_RIVER) || b.is(BiomeTags.IS_BEACH) || b.is(BiomeTags.IS_MOUNTAIN)) {
                    continue;
                }
                if (isCold && isWet) coldWet.add(b);
                else if (isCold) coldDry.add(b);
                else if (isHot && isWet) hotWet.add(b);
                else if (isHot) hotDry.add(b);
                else if (isWet) temperateWet.add(b);
                else temperateDry.add(b);
            }
            // Fallback any empty bucket onto a sane neighbor so picks never fail.
            ensureNonEmpty(coldWet, coldDry, temperateWet, temperateDry);
            ensureNonEmpty(coldDry, coldWet, temperateDry, temperateWet);
            ensureNonEmpty(temperateWet, temperateDry, coldWet);
            ensureNonEmpty(temperateDry, temperateWet, coldDry);
            ensureNonEmpty(hotWet, hotDry, temperateWet);
            ensureNonEmpty(hotDry, hotWet, temperateDry);
            ensureNonEmpty(badlands, hotDry, temperateDry);
            ensureNonEmpty(beach, temperateDry);
            ensureNonEmpty(snowyBeach, beach, coldDry);
            ensureNonEmpty(stonyShore, mountainSlopes, beach, temperateDry);
            ensureNonEmpty(river, temperateWet, temperateDry);
            ensureNonEmpty(frozenRiver, river, coldDry);
            ensureNonEmpty(ocean, deepOcean, temperateWet);
            ensureNonEmpty(deepOcean, ocean);
            ensureNonEmpty(frozenOcean, ocean, coldWet);
            ensureNonEmpty(mountainPeaks, mountainSlopes, coldDry);
            ensureNonEmpty(mountainSlopes, mountainPeaks, coldWet);
            categorized = true;
        }
    }

    @SafeVarargs
    private static void ensureNonEmpty(List<Holder<Biome>> target, List<Holder<Biome>>... fallbacks) {
        if (!target.isEmpty()) return;
        for (List<Holder<Biome>> f : fallbacks) {
            if (!f.isEmpty()) {
                target.addAll(f);
                return;
            }
        }
    }

    private Holder<Biome> pickBiome(Cell cell, int worldY) {
        Terrain terrain = cell.terrain != null ? cell.terrain : TerrainType.NONE;
        float temperature = cell.temperature;
        float moisture = cell.moisture;
        boolean isCold = temperature < 0.3F;
        boolean isHot = temperature > 0.7F;
        boolean isWet = moisture > 0.55F;
        long seedTag = (long) cell.continentX * 73856093L ^ (long) cell.continentZ * 19349663L;

        if (terrain == TerrainType.DEEP_OCEAN) {
            if (isCold && !frozenOcean.isEmpty()) return pickFrom(frozenOcean, seedTag);
            return pickFrom(deepOcean, seedTag);
        }
        if (terrain == TerrainType.SHALLOW_OCEAN) {
            if (isCold && !frozenOcean.isEmpty()) return pickFrom(frozenOcean, seedTag);
            return pickFrom(ocean, seedTag);
        }
        if (terrain == TerrainType.RIVER) {
            return isCold ? pickFrom(frozenRiver, seedTag) : pickFrom(river, seedTag);
        }
        if (terrain == TerrainType.LAKE) {
            return isCold ? pickFrom(frozenRiver, seedTag) : pickFrom(river, seedTag);
        }
        if (terrain == TerrainType.BEACH) {
            return isCold ? pickFrom(snowyBeach, seedTag) : pickFrom(beach, seedTag);
        }
        if (terrain == TerrainType.COAST) {
            return pickFrom(stonyShore, seedTag);
        }
        if (terrain == TerrainType.BADLANDS) {
            return pickFrom(badlands, seedTag);
        }
        if (terrain == TerrainType.MOUNTAINS_1 || terrain == TerrainType.MOUNTAINS_2 || terrain == TerrainType.MOUNTAINS_3 || terrain == TerrainType.MOUNTAIN_CHAIN || terrain == TerrainType.MOUNTAIN_CLIFFS) {
            return pickFrom(mountainPeaks, seedTag);
        }
        if (isHot && isWet) return pickFrom(hotWet, seedTag);
        if (isHot) return pickFrom(hotDry, seedTag);
        if (isCold && isWet) return pickFrom(coldWet, seedTag);
        if (isCold) return pickFrom(coldDry, seedTag);
        return pickFrom(isWet ? temperateWet : temperateDry, seedTag);
    }

    private static Holder<Biome> pickFrom(List<Holder<Biome>> bucket, long tag) {
        if (bucket.isEmpty()) {
            return null;
        }
        int idx = (int) Math.floorMod(tag, (long) bucket.size());
        return bucket.get(idx);
    }
}
