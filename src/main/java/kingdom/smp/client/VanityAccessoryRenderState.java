package kingdom.smp.client;

import net.minecraft.world.item.ItemStack;

/**
 * Duck interface mixed into vanilla {@code AvatarRenderState} so additive vanity
 * accessories (the halo, the angel wings) can reach their dedicated render layers
 * <em>without</em> being substituted over the player's real armor.
 *
 * <p>Replacement-style vanity (cat ears, shroomcap) swaps the visible armor via
 * {@code LivingEntityVanityMixin} and is read off {@code headEquipment}. Additive
 * accessories instead leave the armor untouched and travel through this channel,
 * populated each frame in {@code AvatarRendererMixin} from the accessory inventory.
 * Stacks default to {@link ItemStack#EMPTY} when nothing is worn.
 */
public interface VanityAccessoryRenderState {
    void ironhold$setHeadAccessory(ItemStack stack);

    ItemStack ironhold$headAccessory();

    void ironhold$setChestAccessory(ItemStack stack);

    ItemStack ironhold$chestAccessory();

    void ironhold$setLegsAccessory(ItemStack stack);

    ItemStack ironhold$legsAccessory();

    void ironhold$setFeetAccessory(ItemStack stack);

    ItemStack ironhold$feetAccessory();
}
