package kingdom.smp.mixin;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cleans up "spent" Trueflight arrows.
 *
 * <p>The Trueflight enchantment strips an arrow's gravity (see
 * {@link kingdom.smp.enchant.NoGravityProjectileEffect}) so it flies in a flat line. Air drag still
 * bleeds its speed, so once it has effectively stopped it would otherwise hang motionless in the air
 * forever. This removes such an arrow the moment it goes near-still in flight — but <em>only</em> in
 * flight: an arrow stuck in a block (or one that has already struck an entity and been discarded) is
 * left untouched, so normal "pick it back up" / decoration behaviour is unaffected.
 *
 * <p>The Trueflight marker is simply "weightless + still airborne": nothing else in the game makes an
 * arrow no-gravity, so this proxy is reliable without tagging each arrow individually. Because
 * Tempest Arrows extend {@link AbstractArrow} and call {@code super.tick()}, this covers them too.
 */
@Mixin(AbstractArrow.class)
public abstract class TrueflightArrowDespawnMixin {

    @Shadow
    protected abstract boolean isInGround();

    @Inject(method = "tick", at = @At("TAIL"))
    private void ironhold$despawnSpentTrueflight(CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        if (!self.isAlive()) return;                              // already removed (e.g. struck an entity)
        if (!(self.level() instanceof ServerLevel level)) return; // server-authoritative
        if (!self.isNoGravity() || this.isInGround()) return;     // only weightless arrows still airborne
        if (self.getDeltaMovement().lengthSqr() >= 0.01) return;  // still moving (>0.1 blocks/tick) — keep flying

        level.sendParticles(ParticleTypes.POOF, self.getX(), self.getY(), self.getZ(),
                3, 0.05, 0.05, 0.05, 0.0);
        self.discard();
    }
}
