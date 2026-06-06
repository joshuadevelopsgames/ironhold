package kingdom.smp.mixin;

import kingdom.smp.client.VanityCache;
import kingdom.smp.item.VanityCosmeticItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side mixin that replaces the visible equipment of players
 * with their vanity armor, if any. Because {@code getItemBySlot} is called
 * during render-state extraction (before layers run), this single hook
 * covers the entire visual pipeline. Inventory slots read directly from
 * the container, so the vanilla inventory still shows real armor.
 * <p>
 * Uses {@link VanityCache} (from {@code SyncVanityPayload}) and falls back to the
 * synced {@link AccessoryInventory} attachment so the local player updates without
 * relying only on the next vanity packet.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityVanityMixin {

    @Inject(method = "getItemBySlot", at = @At("RETURN"), cancellable = true)
    private void ironhold$injectVanityArmor(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        //noinspection ConstantValue — mixin casts
        if (!((Object) this instanceof Player player)) return;
        if (!player.level().isClientSide()) return;

        // Only substitute for humanoid armor slots
        if (slot != EquipmentSlot.HEAD && slot != EquipmentSlot.CHEST
                && slot != EquipmentSlot.LEGS && slot != EquipmentSlot.FEET) {
            return;
        }

        // Prefer attachment (updated immediately by container slot sync on this client);
        // VanityCache covers other players via SyncVanityPayload.
        ItemStack vanity = VanityCache.resolve(player, slot);
        if (vanity.isEmpty()) {
            return;
        }
        // Additive accessories (halo, wings) overlay the armor instead of replacing it,
        // so leave the real equipment visible — their render layer reads them off
        // VanityAccessoryRenderState instead.
        if (vanity.getItem() instanceof VanityCosmeticItem cosmetic && cosmetic.overlaysArmor()) {
            return;
        }
        cir.setReturnValue(vanity);
    }
}
