package kingdom.smp.mixin;

import kingdom.smp.gear.GearComponents;
import kingdom.smp.gear.ItemQuality;
import kingdom.smp.gear.QualityScope;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Apply the {@link ItemQuality} durability multiplier to every damageable item. Hooks the
 * universal {@link ItemStack#getMaxDamage()} call site so vanilla and modded items both pick
 * up the modifier without per-item registration.
 *
 * Items at default (Good) quality produce a 1.0 multiplier and the original vanilla durability
 * is returned unchanged.
 *
 * @see <a href="../../../../specs/ore-quality-system.md">ore-quality-system.md §2</a>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMaxDamageMixin {

    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void ironhold$applyQualityAndFatigue(CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValueI();
        if (original <= 0) return; // not damageable — leave alone

        ItemStack self = (ItemStack) (Object) this;
        // Utility gear (shield/elytra/fishing rod/shears/flint and steel/brush/etc.) is excluded.
        if (!QualityScope.isEligible(self)) return;

        ItemQuality quality = GearComponents.getQuality(self);

        // Fast path: default (Good) quality → 1.0 multiplier, return as-is.
        if (quality == ItemQuality.defaultQuality()) return;

        float modified = original * quality.durabilityMultiplier();
        // Floor of 1 so quality nerfs never produce a 0-durability "instant break" item.
        cir.setReturnValue(Math.max(1, Math.round(modified)));
    }
}
