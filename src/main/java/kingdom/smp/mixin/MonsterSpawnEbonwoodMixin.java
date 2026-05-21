package kingdom.smp.mixin;

import kingdom.smp.Ironhold;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * In the Ebonwood Hollow, monsters can spawn regardless of light level —
 * the biome is treated as perpetual night for spawning purposes.
 */
@Mixin(Monster.class)
public abstract class MonsterSpawnEbonwoodMixin {

    @Inject(method = "isDarkEnoughToSpawn", at = @At("HEAD"), cancellable = true)
    private static void ironhold$alwaysDarkInEbonwood(
            ServerLevelAccessor level, BlockPos pos, RandomSource random,
            CallbackInfoReturnable<Boolean> cir) {
        if (level.getBiome(pos).is(kingdom.smp.ModWorldgen.EBONWOOD_HOLLOW)) {
            cir.setReturnValue(true);
        }
    }
}
