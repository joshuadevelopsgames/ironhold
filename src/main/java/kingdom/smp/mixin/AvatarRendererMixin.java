package kingdom.smp.mixin;

import kingdom.smp.client.VanityAccessoryRenderState;
import kingdom.smp.client.VanityCache;
import kingdom.smp.client.emote.PointEmoteState;
import kingdom.smp.client.emote.PointableRenderState;
import kingdom.smp.item.VanityCosmeticItem;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Populates the point-emote blend on the render state from the client registry. */
@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL"))
    private void ironhold$extractPoint(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        float amount = PointEmoteState.amount(avatar.getUUID(), avatar.level().getGameTime(), partialTick);
        ((PointableRenderState) state).ironhold$setPoint(amount);
    }

    /**
     * Route worn vanity cosmetics into the render state so their dedicated 3D layers can draw them.
     *
     * <p>{@code getItemBySlot(HEAD)} is vanity-substituted on the client (see
     * {@code LivingEntityVanityMixin}). A {@link VanityCosmeticItem} has no Equippable asset, so
     * vanilla skipped {@code headEquipment} ({@code ShroomcapLayer} reads it) and instead populated
     * {@code headItem}, which {@code CustomHeadLayer} would draw as a flat 2D sprite. Fix both:
     * publish the stack via {@code headEquipment} and clear the 2D {@code headItem}.
     */
    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL"))
    private void ironhold$extractVanityCosmetic(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        ItemStack head = avatar.getItemBySlot(EquipmentSlot.HEAD);
        if (head.getItem() instanceof VanityCosmeticItem) {
            state.headEquipment = head;
            state.headItem.clear();
        }
        ItemStack chest = avatar.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof VanityCosmeticItem) {
            state.chestEquipment = chest;
        }
    }

    /**
     * Publish additive vanity accessories (halo, wings) onto the render state through a
     * side channel, leaving the real armor in {@code headEquipment}/{@code chestEquipment}
     * untouched. These cosmetics are deliberately <em>not</em> substituted into
     * {@code getItemBySlot} (see {@code LivingEntityVanityMixin}), so they must be read
     * straight from the accessory inventory here.
     */
    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL"))
    private void ironhold$extractAccessoryOverlays(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        VanityAccessoryRenderState acc = (VanityAccessoryRenderState) state;
        acc.ironhold$setHeadAccessory(ironhold$overlay(avatar, EquipmentSlot.HEAD));
        acc.ironhold$setChestAccessory(ironhold$overlay(avatar, EquipmentSlot.CHEST));
        acc.ironhold$setLegsAccessory(ironhold$overlay(avatar, EquipmentSlot.LEGS));
        acc.ironhold$setFeetAccessory(ironhold$overlay(avatar, EquipmentSlot.FEET));
    }

    @Unique
    private static ItemStack ironhold$overlay(Avatar avatar, EquipmentSlot slot) {
        ItemStack worn = VanityCache.resolve(avatar, slot);
        return worn.getItem() instanceof VanityCosmeticItem cosmetic && cosmetic.overlaysArmor()
            ? worn : ItemStack.EMPTY;
    }
}
