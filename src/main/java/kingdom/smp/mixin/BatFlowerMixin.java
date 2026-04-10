package kingdom.smp.mixin;

import kingdom.smp.Ironhold;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes bats occasionally fly toward and linger near bat flowers
 * in the Ebonwood Hollow biome.
 */
@Mixin(Bat.class)
public abstract class BatFlowerMixin {

    @Shadow
    private BlockPos targetPosition;

    /** The flower the bat is currently attracted to (null = none). */
    @Unique
    private BlockPos ironhold$flowerTarget = null;

    /** Ticks remaining to linger near the current flower. */
    @Unique
    private int ironhold$lingerTicks = 0;

    /** Cooldown before searching for another flower. */
    @Unique
    private int ironhold$searchCooldown = 0;

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void ironhold$attractToBatFlowers(CallbackInfo ci) {
        Bat self = (Bat) (Object) this;
        Level level = self.level();

        // Currently lingering near a flower — keep overriding the target
        if (ironhold$lingerTicks > 0 && ironhold$flowerTarget != null) {
            ironhold$lingerTicks--;
            // Check the flower still exists
            if (!level.getBlockState(ironhold$flowerTarget).is(Ironhold.BAT_FLOWER.get())) {
                ironhold$flowerTarget = null;
                ironhold$lingerTicks = 0;
                return;
            }
            // If close to the flower top, sit still (landed); otherwise flutter toward it
            boolean nearFlower = self.blockPosition().distSqr(ironhold$flowerTarget) <= 2;
            if (nearFlower) {
                this.targetPosition = ironhold$flowerTarget;
            } else {
                int wobbleX = self.getRandom().nextInt(3) - 1;
                int wobbleZ = self.getRandom().nextInt(3) - 1;
                this.targetPosition = ironhold$flowerTarget.offset(wobbleX, 0, wobbleZ);
            }
            return;
        }

        // Flower visit ended — set cooldown before next search
        if (ironhold$flowerTarget != null) {
            ironhold$flowerTarget = null;
            ironhold$searchCooldown = 200 + self.getRandom().nextInt(400); // 10-30 sec
            return;
        }

        if (ironhold$searchCooldown > 0) {
            ironhold$searchCooldown--;
            return;
        }

        // ~1 in 60 ticks (~3 seconds) try to find a nearby bat flower
        if (self.getRandom().nextInt(60) != 0) {
            return;
        }

        BlockPos batPos = self.blockPosition();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;

        for (int dx = -12; dx <= 12; dx += 2) {
            for (int dz = -12; dz <= 12; dz += 2) {
                for (int dy = -6; dy <= 6; dy++) {
                    mutable.set(batPos.getX() + dx, batPos.getY() + dy, batPos.getZ() + dz);
                    if (level.getBlockState(mutable).is(Ironhold.BAT_FLOWER.get())) {
                        double dist = mutable.distSqr(batPos);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = mutable.immutable();
                        }
                    }
                }
            }
        }

        if (closest != null) {
            // Resolve to the top of the flower
            var state = level.getBlockState(closest);
            int topY = closest.getY();
            if (state.hasProperty(net.minecraft.world.level.block.DoublePlantBlock.HALF)
                    && state.getValue(net.minecraft.world.level.block.DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER) {
                topY += 1;
            }
            // 70% chance to land on the flower, 30% hover nearby
            int yOffset = self.getRandom().nextFloat() < 0.7f ? 0 : 1 + self.getRandom().nextInt(2);
            ironhold$flowerTarget = new BlockPos(closest.getX(), topY + 1 + yOffset, closest.getZ());
            // Linger 6-16 seconds near the flower
            ironhold$lingerTicks = 120 + self.getRandom().nextInt(200);
            this.targetPosition = ironhold$flowerTarget;
        } else {
            // No flower nearby, wait before searching again
            ironhold$searchCooldown = 100 + self.getRandom().nextInt(100);
        }
    }
}
