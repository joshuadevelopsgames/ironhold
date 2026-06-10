package kingdom.smp.gear;

import kingdom.smp.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Reroll of a gear item's affixes at the blacksmith, charged in gold coins.
 *
 * <p>v1: rerolls ALL affixes for a coin cost. The targeted lock-and-reroll GUI (keep the affixes you
 * like, reroll the rest, Blacksmithing-rank-gated lock count) is the planned refinement — see
 * {@code specs/fantasia-ports/07-gear-affixes.md} §5.
 */
public final class AffixReforge {
    private AffixReforge() {}

    private static final int COST_PER_AFFIX = 8;
    private static final int COST_MIN = 8;

    public static void tryReroll(ServerPlayer player, ItemStack gear) {
        // Rank-gated crafting (⑧): reforging requires Blacksmithing. This is the safe, in-our-control
        // instance of rank-gating; the full 5-station crafting gate is deferred to runtime verification.
        if (!kingdom.smp.skill.SkillEffects.hasAtLeast(
                player, kingdom.smp.skill.Profession.BLACKSMITHING, kingdom.smp.skill.ProfessionRank.NOVICE)) {
            actionBar(player, Component.literal("Reforging requires Blacksmithing (Novice)."));
            return;
        }
        int cost = Math.max(COST_MIN, AffixData.capacity(gear) * COST_PER_AFFIX);
        if (countCoins(player) < cost) {
            actionBar(player, Component.literal("Reforging costs " + cost + " coins."));
            return;
        }
        chargeCoins(player, cost);
        AffixRoller.roll(gear);
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
        actionBar(player, Component.literal("Reforged for " + cost + " coins — "
            + AffixData.get(gear).size() + " new affix(es)."));
    }

    private static int countCoins(ServerPlayer player) {
        int total = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems.GOLD_COIN.get())) {
                total += s.getCount();
            }
        }
        return total;
    }

    private static void chargeCoins(ServerPlayer player, int cost) {
        int remaining = cost;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems.GOLD_COIN.get())) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
    }

    private static void actionBar(ServerPlayer player, Component msg) {
        player.connection.send(new ClientboundSetActionBarTextPacket(msg));
    }
}
