package kingdom.smp.rpg;

import java.util.List;

import kingdom.smp.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Themed starter kits granted on promotion at major branch points (Phase 5 ⑭). Fits Ironhold's earned
 * progression — you're handed a class's signature gear the moment you become it. Minor in-line steps
 * grant nothing. Spec: {@code specs/fantasia-ports/09-class-promotion-kits.md}.
 */
public final class ClassKit {
    private ClassKit() {}

    /** Grant the class's kit (if any) into the player's inventory. */
    public static void grant(ServerPlayer player, PlayerClass clazz) {
        List<ItemStack> kit = kitFor(clazz);
        if (kit.isEmpty()) {
            return;
        }
        for (ItemStack stack : kit) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        player.sendSystemMessage(Component.literal("✦ You are equipped as a " + clazz.id() + ".")
            .withStyle(ChatFormatting.GOLD));
    }

    private static List<ItemStack> kitFor(PlayerClass clazz) {
        return switch (clazz) {
            // Tier 1 — first identity.
            case SQUIRE -> List.of(new ItemStack(Items.IRON_SWORD), new ItemStack(Items.SHIELD), new ItemStack(Items.BREAD, 4));
            case MAGE_APPRENTICE -> List.of(new ItemStack(ModItems.ARCANE_SCEPTER.get()), new ItemStack(Items.BREAD, 4));
            case ARCHER -> List.of(new ItemStack(Items.BOW), new ItemStack(Items.ARROW, 32), new ItemStack(Items.BREAD, 4));
            case MEDIC -> List.of(new ItemStack(ModItems.BANDAGE.get(), 4), new ItemStack(Items.BREAD, 4));
            // Tier 2 — branch lines.
            case KNIGHT -> List.of(new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_SWORD), new ItemStack(Items.SHIELD));
            case WIZARD -> List.of(new ItemStack(ModItems.ARCANE_SCEPTER.get()), new ItemStack(Items.GOLDEN_CARROT, 6));
            case RANGER -> List.of(new ItemStack(Items.BOW), new ItemStack(Items.ARROW, 64));
            case CLERIC -> List.of(new ItemStack(ModItems.BANDAGE.get(), 8), new ItemStack(Items.GOLDEN_CARROT, 4));
            default -> List.of(); // advanced/Divine kits = follow-up
        };
    }
}
