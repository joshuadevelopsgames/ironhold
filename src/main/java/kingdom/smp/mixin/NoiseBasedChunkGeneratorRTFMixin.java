package kingdom.smp.mixin;

import java.util.OptionalInt;
import java.util.function.Predicate;

import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

import kingdom.smp.rtf.WorldGenFlags;

/**
 * Wraps {@code iterateNoiseColumn} only for {@code getBaseHeight} / {@code getBaseColumn} so {@link NoiseChunkRTFMixin}
 * skips tile pre-queue for structure height probes without relying on HEAD/RETURN (exceptions would leave depth stuck).
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorRTFMixin {

    @Invoker(value = "iterateNoiseColumn", remap = true)
    abstract OptionalInt ironhold$invokeIterateNoiseColumn(
            LevelHeightAccessor heightAccessor,
            RandomState randomState,
            int x,
            int z,
            MutableObject<NoiseColumn> column,
            Predicate<BlockState> stoppingPredicate);

    @Redirect(
            method = { "getBaseHeight", "getBaseColumn" },
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/levelgen/NoiseBasedChunkGenerator;iterateNoiseColumn(Lnet/minecraft/world/level/LevelHeightAccessor;Lnet/minecraft/world/level/levelgen/RandomState;IILorg/apache/commons/lang3/mutable/MutableObject;Ljava/util/function/Predicate;)Ljava/util/OptionalInt;",
                            remap = true))
    private OptionalInt ironhold$redirectIterateForAmbientProbes(
            NoiseBasedChunkGenerator self,
            LevelHeightAccessor heightAccessor,
            RandomState randomState,
            int x,
            int z,
            MutableObject<NoiseColumn> column,
            Predicate<BlockState> stoppingPredicate) {
        WorldGenFlags.pushAmbientNoiseColumnSampling();
        try {
            return ((NoiseBasedChunkGeneratorRTFMixin) (Object) self)
                    .ironhold$invokeIterateNoiseColumn(
                            heightAccessor, randomState, x, z, column, stoppingPredicate);
        } finally {
            WorldGenFlags.popAmbientNoiseColumnSampling();
        }
    }
}
