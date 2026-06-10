package kingdom.smp.gear;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

/**
 * Non-combat affix hooks. Prospector boosts dig speed via {@link PlayerEvent.BreakSpeed} (an event,
 * not an attribute, to respect the ore-quality firewall — see spec §4); Scholar boosts XP gains via
 * {@link PlayerXpEvent.XpChange}. Enduring lives in {@code ItemStackMaxDamageMixin} alongside the
 * quality durability multiplier. Registered to the game bus in {@code Ironhold}.
 * Spec: {@code specs/fantasia-ports/07-gear-affixes.md}.
 */
public final class AffixUtilityHandler {
    private AffixUtilityHandler() {}

    private static final EquipmentSlot[] ARMOR_SLOTS =
        { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    /** Scholar stacks across held item + armor; cap so a full Mint set can't go infinite. */
    private static final float SCHOLAR_CAP = 0.60f;

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack tool = event.getEntity().getMainHandItem();
        float roll = AffixData.rollOf(tool, Affix.PROSPECTOR);
        if (roll > 0f) {
            event.setNewSpeed(event.getNewSpeed() * (1f + roll));
        }
    }

    @SubscribeEvent
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        // Only amplify gains — never deepen XP costs (anvil/enchanting charge negative amounts).
        if (event.getAmount() <= 0 || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        float total = AffixData.rollOf(player.getMainHandItem(), Affix.SCHOLAR);
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            total += AffixData.rollOf(player.getItemBySlot(slot), Affix.SCHOLAR);
        }
        if (total > 0f) {
            total = Math.min(total, SCHOLAR_CAP);
            event.setAmount(Math.max(1, Math.round(event.getAmount() * (1f + total))));
        }
    }
}
