package kingdom.smp.mixin;

import kingdom.smp.Ironhold;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Allows bats to spawn in the Ebonwood Hollow regardless of light level,
 * since the biome is meant to be perpetually dark and bat-heavy.
 */
@Mixin(Bat.class)
public abstract class BatSpawnMixin {

    @Inject(method = "checkBatSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void ironhold$allowBatsInEbonwood(
            EntityType<Bat> type, LevelAccessor level, EntitySpawnReason spawnType,
            BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
        if (level.getBiome(pos).is(kingdom.smp.ModWorldgen.EBONWOOD_HOLLOW)) {
            cir.setReturnValue(true);
        }
    }
}
