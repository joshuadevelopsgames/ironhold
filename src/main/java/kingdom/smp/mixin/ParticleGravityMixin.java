package kingdom.smp.mixin;

import kingdom.smp.moon.GravityHelper;
import kingdom.smp.moon.ModMoonDimensions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Particles have their own hard-coded world-down (-Y) gravity, independent of the
 * moon's directional gravity — so on a side face, break/step particles drift along
 * world -Y (sideways, "in front of you") instead of settling onto the wall-floor.
 * Here we redirect that gravity toward the current face: we neutralise vanilla's
 * upcoming {@code yd -= 0.04*gravity} and apply the same pull along the face axis.
 */
@Mixin(Particle.class)
public abstract class ParticleGravityMixin {

    @Shadow protected double xd;
    @Shadow protected double yd;
    @Shadow protected double zd;
    @Shadow protected double x;
    @Shadow protected double y;
    @Shadow protected double z;
    @Shadow protected float gravity;
    @Shadow @org.spongepowered.asm.mixin.Final protected ClientLevel level;

    @Inject(method = "tick", at = @At("HEAD"))
    private void ironhold$redirectGravity(CallbackInfo ci) {
        if (this.gravity == 0.0f) return;
        if (!this.level.dimension().equals(ModMoonDimensions.MOON_LEVEL)) return;

        Direction g = GravityHelper.getGravityDirectionAt(this.x, this.y, this.z);
        if (g == Direction.DOWN) return; // top face: vanilla -Y is already correct

        double pull = 0.04 * this.gravity;
        this.yd += pull;                       // cancel vanilla's upcoming yd -= pull
        this.xd += g.getStepX() * pull;        // ...and apply it toward the face instead
        this.yd += g.getStepY() * pull;
        this.zd += g.getStepZ() * pull;
    }
}
