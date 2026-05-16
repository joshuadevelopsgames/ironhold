package kingdom.smp.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import kingdom.smp.rtf.GeneratorContext;
import kingdom.smp.rtf.RTFRandomState;
import kingdom.smp.rtf.WorldGenFlags;
import kingdom.smp.rtf.densityfunction.CellSampler;
import kingdom.smp.rtf.densityfunction.CellSampler.Cache2d;
import kingdom.smp.rtf.tile.Tile;
import kingdom.smp.rtf.tile.TileCache;

/**
 * Swap every {@link CellSampler} sampled by a {@link NoiseChunk} for a per-chunk {@link CellSampler.CacheChunk}
 * wrapper that reads the chunk's prebuilt {@link Tile.Chunk} cell grid at full per-block resolution.
 *
 * <p>Without this mixin, every {@code CellSampler.compute()} hits the {@link Cache2d} fallback which snaps to
 * {@code QuartPos} (4-block) coordinates, producing the visible 4×4×~4 voxel-block terraces in mountains. With
 * this mixin loaded, in-chunk samples read the tile cell directly so the surface follows the cell's continuous
 * height field per-block.</p>
 */
@Mixin(NoiseChunk.class)
public abstract class NoiseChunkRTFMixin {
    private RandomState ironhold$randomState;
    private int ironhold$chunkX;
    private int ironhold$chunkZ;
    private int ironhold$generationHeight;
    private Tile.Chunk ironhold$chunk;
    private Cache2d ironhold$cache2d;
    private NoiseGeneratorSettings ironhold$generatorSettings;

    @Mutable
    @Shadow
    @Final
    private int cellCountY;

    @Shadow
    @Final
    private int cellHeight;

    @Redirect(
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/RandomState;router()Lnet/minecraft/world/level/levelgen/NoiseRouter;"
        ),
        method = "<init>",
        require = 1
    )
    private NoiseRouter ironhold$captureRouter(RandomState routerOwner,
                                               int cellCountXZ,
                                               RandomState randomState,
                                               int chunkMinBlockX,
                                               int chunkMinBlockZ,
                                               NoiseSettings noiseSettings,
                                               DensityFunctions.BeardifierOrMarker beardifierOrMarker,
                                               NoiseGeneratorSettings noiseGeneratorSettings,
                                               Aquifer.FluidPicker globalFluidPicker,
                                               Blender blender) {
        this.ironhold$randomState = routerOwner;
        this.ironhold$chunkX = SectionPos.blockToSectionCoord(chunkMinBlockX);
        this.ironhold$chunkZ = SectionPos.blockToSectionCoord(chunkMinBlockZ);
        this.ironhold$generatorSettings = noiseGeneratorSettings;

        GeneratorContext generatorContext;
        if ((Object) this.ironhold$randomState instanceof RTFRandomState rtfRandomState
            && (generatorContext = rtfRandomState.generatorContext()) != null) {
            boolean cache = !WorldGenFlags.fastLookups() || CellSampler.isCachedNoiseChunk(cellCountXZ);

            // Pass load=false so getGenerationHeight uses provideAtChunkIfPresent (non-blocking).
            // If the tile isn't cached yet, we fall back to the full vertical generation height —
            // slightly less efficient, but never stalls NoiseChunk.<init> on tile generation.
            this.ironhold$generationHeight = generatorContext.lookup.getGenerationHeight(
                this.ironhold$chunkX, this.ironhold$chunkZ, noiseGeneratorSettings, false);

            if (this.cellHeight > 0) {
                this.cellCountY = Math.min(this.cellCountY, this.ironhold$generationHeight / this.cellHeight);
            }
            this.ironhold$cache2d = new CellSampler.Cache2d();

            if (cache) {
                TileCache tileCache = generatorContext.cache;
                if (tileCache != null && !WorldGenFlags.isAmbientNoiseColumnSampling()) {
                    Tile tile = tileCache.provideAtChunkIfPresent(this.ironhold$chunkX, this.ironhold$chunkZ);
                    if (tile != null) {
                        this.ironhold$chunk = tile.getChunkReader(this.ironhold$chunkX, this.ironhold$chunkZ);
                    } else {
                        // Pre-warm RTF tile on real multi-cell NoiseChunk paths only — not during
                        // iterateNoiseColumn ambient sampling (guarded by NoiseBasedChunkGeneratorRTFMixin).
                        tileCache.queueAtChunk(this.ironhold$chunkX, this.ironhold$chunkZ);
                    }
                }
            }
        } else {
            this.ironhold$generationHeight = noiseSettings.height();
        }

        return routerOwner.router();
    }

    @Inject(
        at = @At("HEAD"),
        method = "wrapNew",
        cancellable = true
    )
    private void ironhold$wrapNew(DensityFunction function, CallbackInfoReturnable<DensityFunction> callback) {
        if (this.ironhold$cache2d != null
            && (Object) this.ironhold$randomState instanceof RTFRandomState
            && function instanceof CellSampler mapped) {
            callback.setReturnValue(mapped.new CacheChunk(
                this.ironhold$chunk,
                this.ironhold$cache2d,
                this.ironhold$chunkX,
                this.ironhold$chunkZ));
        }
    }
}
