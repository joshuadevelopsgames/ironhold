package kingdom.smp.mixin;

import kingdom.smp.Ironhold;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SurfaceSystem.class)
public class SurfaceSystemMixin {

    private static final ResourceKey<Biome> EBONWOOD_HOLLOW = ResourceKey.create(
        Registries.BIOME,
        Identifier.fromNamespaceAndPath("ironhold", "ebonwood_hollow")
    );

    @Inject(method = "buildSurface", at = @At("RETURN"))
    private void ironhold$replaceGrassWithEbonwoodGrass(
        RandomState randomState,
        BiomeManager biomeManager,
        Registry<Biome> biomes,
        boolean useLegacyRandomSource,
        WorldGenerationContext context,
        ChunkAccess chunk,
        NoiseChunk noiseChunk,
        SurfaceRules.RuleSource ruleSource,
        CallbackInfo ci
    ) {
        net.minecraft.world.level.ChunkPos chunkPos = chunk.getPos();
        BlockState ebonwoodGrass = Ironhold.EBONWOOD_GRASS.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int absX = chunkPos.getMinBlockX() + localX;
                int absZ = chunkPos.getMinBlockZ() + localZ;

                Holder<Biome> biome = biomeManager.getBiome(pos.set(absX, 64, absZ));
                if (!biome.is(EBONWOOD_HOLLOW)) continue;

                // WORLD_SURFACE heightmap returns Y above the top non-empty block
                int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ) - 1;
                pos.set(absX, surfaceY, absZ);

                if (chunk.getBlockState(pos).is(Blocks.GRASS_BLOCK)) {
                    chunk.setBlockState(pos, ebonwoodGrass, 3);
                }
            }
        }
    }
}
