package kingdom.smp.gear;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kingdom.smp.ModItems;
import kingdom.smp.net.OpenReforgePayload;
import kingdom.smp.skill.Profession;
import kingdom.smp.skill.ProfessionRank;
import kingdom.smp.skill.SkillEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Targeted lock-and-reroll of a gear item's affixes at the blacksmith, charged in gold coins
 * (spec {@code specs/fantasia-ports/07-gear-affixes.md} §5).
 *
 * <p>Sneak-right-clicking Tobias with affixable gear opens {@code ReforgeScreen} via
 * {@link OpenReforgePayload}. The screen lets the player lock affixes they like — lock count
 * gated by Blacksmithing rank (Novice 0 … Master all-but-one) — and reroll the rest via
 * {@code ReforgeActionPayload}. Cost escalates with the item's reforge count, capped at 4× base.
 */
public final class AffixReforge {
    private AffixReforge() {}

    private static final int COST_PER_AFFIX = 8;
    private static final int COST_MIN = 8;
    private static final int MAX_COST_MULTIPLIER = 4;

    /** True if this stack can be reforged at all (affixable gear with at least one slot). */
    public static boolean reforgeable(ItemStack gear) {
        return AffixRoller.gearClass(gear) != AffixRoller.GearClass.NONE
            && AffixData.capacity(gear) > 0;
    }

    /**
     * Locks allowed for this player on an item with {@code capacity} affix slots. Always leaves
     * at least one slot unlocked — reforging must reroll <i>something</i>.
     */
    public static int locksAllowed(ServerPlayer player, int capacity) {
        if (capacity <= 0) return 0;
        int byRank;
        if (SkillEffects.hasAtLeast(player, Profession.BLACKSMITHING, ProfessionRank.MASTER)) {
            byRank = capacity - 1; // all-but-one
        } else if (SkillEffects.hasAtLeast(player, Profession.BLACKSMITHING, ProfessionRank.EXPERT)) {
            byRank = 2;
        } else if (SkillEffects.hasAtLeast(player, Profession.BLACKSMITHING, ProfessionRank.APPRENTICE)) {
            byRank = 1;
        } else {
            byRank = 0;
        }
        return Math.max(0, Math.min(byRank, capacity - 1));
    }

    /** Coin cost for the next reroll of this item: base by slot count × escalation by reforge count. */
    public static int cost(ItemStack gear) {
        int base = Math.max(COST_MIN, AffixData.capacity(gear) * COST_PER_AFFIX);
        int mult = Math.min(MAX_COST_MULTIPLIER, 1 + GearComponents.reforgeCount(gear));
        return base * mult;
    }

    /** Gate + open the lock-and-reroll screen for the gear in the player's main hand. */
    public static void open(ServerPlayer player) {
        ItemStack gear = player.getMainHandItem();
        if (!reforgeable(gear)) return;
        // Rank-gated crafting (⑧): reforging requires Blacksmithing. This is the safe, in-our-control
        // instance of rank-gating; the full 5-station crafting gate is deferred to runtime verification.
        if (!SkillEffects.hasAtLeast(player, Profession.BLACKSMITHING, ProfessionRank.NOVICE)) {
            actionBar(player, Component.literal("Reforging requires Blacksmithing (Novice)."));
            return;
        }
        PacketDistributor.sendToPlayer(player,
            new OpenReforgePayload(locksAllowed(player, AffixData.capacity(gear)), cost(gear)));
    }

    /**
     * Apply a reroll request from the screen: validate rank/locks/coins against the item currently
     * in the main hand, keep locked indices, reroll the rest, and refresh the open screen.
     */
    public static void applyReroll(ServerPlayer player, int lockMask) {
        ItemStack gear = player.getMainHandItem();
        if (!reforgeable(gear)) return;
        if (!SkillEffects.hasAtLeast(player, Profession.BLACKSMITHING, ProfessionRank.NOVICE)) {
            return;
        }

        List<AffixInstance> current = AffixData.get(gear);
        Set<Integer> locked = new HashSet<>();
        for (int i = 0; i < current.size(); i++) {
            if ((lockMask & (1 << i)) != 0) locked.add(i);
        }
        if (locked.size() > locksAllowed(player, AffixData.capacity(gear))) {
            actionBar(player, Component.literal("Too many locks for your Blacksmithing rank."));
            return;
        }

        int cost = cost(gear);
        if (countCoins(player) < cost) {
            actionBar(player, Component.literal("Reforging costs " + cost + " coins."));
            return;
        }
        chargeCoins(player, cost);
        AffixRoller.rerollKeeping(gear, locked);
        GearComponents.bumpReforgeCount(gear);
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
        actionBar(player, Component.literal("Reforged for " + cost + " coins — "
            + AffixData.get(gear).size() + " affix(es)."));
        // Refresh the open screen so the new affixes + escalated cost show immediately.
        PacketDistributor.sendToPlayer(player,
            new OpenReforgePayload(locksAllowed(player, AffixData.capacity(gear)), cost(gear)));
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
