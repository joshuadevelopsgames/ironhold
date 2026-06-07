package kingdom.smp.mixin;

import kingdom.smp.game.LockProtectionHandler;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.material.PushReaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes a <em>locked</em> armor stand immovable by pistons, so it can't be shoved off its perch
 * to grief the owner. Unlocked stands keep vanilla behavior. Forcing {@code IGNORE} is honored by
 * both push paths in {@code PistonMovingBlockEntity} — {@code moveCollidedEntities} skips it
 * outright, and {@code matchesStickyCritera} only matches {@code NORMAL} — so a locked stand
 * resists regular and sticky/honey-drag piston motion alike.
 */
@Mixin(ArmorStand.class)
public abstract class ArmorStandPistonMixin {

    @Inject(method = "getPistonPushReaction", at = @At("HEAD"), cancellable = true)
    private void ironhold$ignorePistonsWhenLocked(CallbackInfoReturnable<PushReaction> cir) {
        if (LockProtectionHandler.isLocked((ArmorStand) (Object) this)) {
            cir.setReturnValue(PushReaction.IGNORE);
        }
    }
}
