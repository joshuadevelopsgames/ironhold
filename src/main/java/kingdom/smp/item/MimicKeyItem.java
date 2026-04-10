package kingdom.smp.item;

import java.util.List;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.accessory.AccessoryInventory;
import kingdom.smp.accessory.AccessoryItem;
import kingdom.smp.entity.BabyMimicEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Mimic Key — an accessory that summons a baby mimic companion while equipped.
 * The baby mimic's name is stored on the key. Inventory is NOT stored on the
 * key — items drop when unequipped and the mimic starts empty on re-equip.
 * The key is the source of truth ONLY for the custom name.
 */
public class MimicKeyItem extends AccessoryItem {

    private static final String TAG_MIMIC_NAME = "MimicName";

    public MimicKeyItem(Properties props) {
        super(props);
    }

    @Override
    public void onAccessoryTick(Player player, ItemStack stack) {
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        // Only check every 20 ticks (1 second) instead of every tick
        if (player.tickCount % 20 != 0) return;
        ServerLevel level = (ServerLevel) serverPlayer.level();

        List<BabyMimicEntity> owned = level.getEntitiesOfClass(
            BabyMimicEntity.class,
            player.getBoundingBox().inflate(48),
            e -> e.isTame() && e.isOwnedBy(player)
        );

        if (owned.isEmpty()) {
            spawnCompanion(serverPlayer, level, stack);
        } else {
            // Periodically sync the mimic's custom name back to the key so it
            // survives unequip/re-equip and /kill without loss.
            BabyMimicEntity companion = owned.getFirst();
            if (companion.hasCustomName()) {
                String name = companion.getCustomName().getString();
                CustomData.update(DataComponents.CUSTOM_DATA, stack,
                    tag -> tag.putString(TAG_MIMIC_NAME, name));
            }
        }
    }

    public static boolean isEquipped(Player player) {
        AccessoryInventory inv = player.getData(ModAttachments.ACCESSORY_INV.get());
        for (int i = 0; i < AccessoryInventory.ACCESSORY_SLOTS; i++) {
            if (inv.getItem(i).getItem() instanceof MimicKeyItem) return true;
        }
        return false;
    }

    private static ItemStack findMimicKey(ServerPlayer player) {
        AccessoryInventory accInv = player.getData(ModAttachments.ACCESSORY_INV.get());
        for (int i = 0; i < AccessoryInventory.ACCESSORY_SLOTS; i++) {
            ItemStack stack = accInv.getItem(i);
            if (stack.getItem() instanceof MimicKeyItem) return stack;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof MimicKeyItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Remove all baby mimics owned by this player. Saves name, drops items.
     */
    public static void removeAllCompanions(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        List<BabyMimicEntity> companions = level.getEntitiesOfClass(
            BabyMimicEntity.class,
            player.getBoundingBox().inflate(64),
            e -> e.isTame() && e.isOwnedBy(player)
        );
        if (companions.isEmpty()) return;

        ItemStack keyStack = findMimicKey(player);

        for (BabyMimicEntity companion : companions) {
            // Save name to key
            if (!keyStack.isEmpty() && companion.hasCustomName()) {
                String name = companion.getCustomName().getString();
                CustomData.update(DataComponents.CUSTOM_DATA, keyStack,
                    tag -> tag.putString(TAG_MIMIC_NAME, name));
            }
            // Drop items then despawn
            companion.dropAllItems();
            companion.discard();
        }
    }

    private void spawnCompanion(ServerPlayer player, ServerLevel level, ItemStack keyStack) {
        BabyMimicEntity mimic = new BabyMimicEntity(Ironhold.BABY_MIMIC.get(), level);
        mimic.setPos(player.getX() + 1, player.getY(), player.getZ() + 1);
        mimic.tame(player);
        mimic.setOrderedToSit(false);

        // Restore custom name from key
        CustomData customData = keyStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains(TAG_MIMIC_NAME)) {
            String savedName = tag.getStringOr(TAG_MIMIC_NAME, "");
            if (!savedName.isEmpty()) {
                mimic.setCustomName(Component.literal(savedName));
                mimic.setCustomNameVisible(true);
            }
        }

        level.addFreshEntity(mimic);
    }

    @Override
    public List<Component> getAccessoryTooltip() {
        return List.of(
            Component.literal("  Summons a baby mimic companion").withStyle(ChatFormatting.GOLD),
            Component.literal("  that follows you around").withStyle(ChatFormatting.GRAY));
    }
}
