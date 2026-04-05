package kingdom.smp.game;

import kingdom.smp.ModAttachments;
import kingdom.smp.item.gear.ClassArmorItem;
import kingdom.smp.item.gear.ClassBowItem;
import kingdom.smp.item.gear.ClassWeaponItem;
import kingdom.smp.item.gear.GearTier;
import kingdom.smp.rpg.PlayerClass;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

/**
 * Enforces class + level requirements when a player equips class gear.
 *
 * <p>Because {@link LivingEquipmentChangeEvent} is not cancellable, we schedule
 * removal on the next server tick. There is a single-tick flicker but the item
 * is immediately returned to the player's inventory (or dropped if full).
 */
public final class EquipRestriction {
    private EquipRestriction() {}

    @SubscribeEvent
    public static void onEquipChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack incoming = event.getTo();
        if (incoming.isEmpty()) return;

        // Determine requirements from the item type
        PlayerClass requiredClass = null;
        GearTier requiredTier = null;

        if (incoming.getItem() instanceof ClassArmorItem a) {
            requiredClass = a.armorClass();
            requiredTier  = a.tier();
        } else if (incoming.getItem() instanceof ClassWeaponItem w) {
            requiredClass = w.weaponClass();
            requiredTier  = w.tier();
        } else if (incoming.getItem() instanceof ClassBowItem b) {
            requiredClass = b.weaponClass();
            requiredTier  = b.tier();
        } else {
            return; // not class gear
        }

        final PlayerKingdomRpgData rpg = player.getData(ModAttachments.PLAYER_RPG);
        final boolean wrongClass = rpg.playerClass() != requiredClass;
        final boolean lowLevel   = rpg.classLevel() < requiredTier.classLevelRequired;

        if (!wrongClass && !lowLevel) return; // all good

        // Schedule rejection on next tick — can't safely mutate inventory inside this event
        final ItemStack copy         = incoming.copy();
        final var       slot         = event.getSlot();
        final String    reqClassName = ClassArmorItem.capitalize(requiredClass.name());
        final int       reqLevel     = requiredTier.classLevelRequired;
        final boolean   isWrongClass = wrongClass;

        player.level().getServer().execute(() -> {
            // Clear the slot that was just (incorrectly) filled
            player.setItemSlot(slot, ItemStack.EMPTY);

            // Return the item to inventory; drop it if inventory is full
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }

            // Feedback message
            Component msg = isWrongClass
                ? Component.literal("Only a " + reqClassName + " can equip this.")
                    .withStyle(ChatFormatting.RED)
                : Component.literal("Requires " + reqClassName + " level " + reqLevel + ".")
                    .withStyle(ChatFormatting.RED);

            player.connection.send(new ClientboundSetActionBarTextPacket(msg)); // action bar (less intrusive)
        });
    }
}
