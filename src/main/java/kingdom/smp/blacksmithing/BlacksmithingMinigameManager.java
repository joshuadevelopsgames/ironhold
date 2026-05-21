package kingdom.smp.blacksmithing;

import kingdom.smp.gear.ItemCondition;
import kingdom.smp.net.ForgeMinigameStartPayload;
import kingdom.smp.skill.Profession;
import kingdom.smp.skill.ProfessionRank;
import kingdom.smp.skill.SkillEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side state for active blacksmithing forge-minigame sessions. There
 * is at most one session per player at a time.
 *
 * <p><b>Flow:</b> the player loads a vanilla anvil normally (gear + repair
 * material) and clicks the hammer button on the anvil screen
 * ({@link kingdom.smp.mixin.AnvilHammerButtonMixin}). That fires a
 * {@code ForgeHammerRequestPayload}; {@link #tryStartFromAnvil} validates the
 * setup, lifts the gear + material out of the anvil into the session, and opens
 * the forge minigame. How well the player plays decides how much durability
 * (condition) is restored when they {@link #resolve}.
 *
 * <p>Because the minigame screen replaces the anvil screen (closing the
 * container), the inputs are stashed in the session up-front rather than left
 * in the live menu — on resolve / abandon / logout we hand them back.
 *
 * <p>This forge <i>repairs condition</i>; it no longer changes item quality.
 */
public final class BlacksmithingMinigameManager {

    private BlacksmithingMinigameManager() {}

    /** Successful strikes needed to complete a forge; mirrors the client constant. */
    public static final int TARGET_STRIKES = 5;

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    /** Stashed inputs lifted from the anvil for the duration of the minigame. */
    private record Session(ItemStack gear, ItemStack material) {}

    /**
     * Begin a forge session from the player's open anvil. No-op (returns
     * silently) if a session is already active or the anvil isn't set up with
     * damaged reforgeable gear + a valid repair material.
     */
    public static void tryStartFromAnvil(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (SESSIONS.containsKey(uuid)) return;
        if (!(player.containerMenu instanceof AnvilMenu am)) return;

        ItemStack gear = am.getSlot(0).getItem();
        ItemStack material = am.getSlot(1).getItem();
        if (!ForgeEligibility.isForgeRepair(gear, material)) return;

        // Lift the inputs out of the anvil so the imminent container-close
        // doesn't return them — we hand them back ourselves on resolve.
        am.getSlot(0).set(ItemStack.EMPTY);
        am.getSlot(1).set(ItemStack.EMPTY);
        am.broadcastChanges();

        SESSIONS.put(uuid, new Session(gear, material));

        ProfessionRank rank = SkillEffects.rankFor(player, Profession.BLACKSMITHING);
        int rankOrdinal = rank == null ? -1 : rank.ordinal();
        PacketDistributor.sendToPlayer(player,
                new ForgeMinigameStartPayload(gear.copy(), rankOrdinal));
    }

    /**
     * Called by the network handler when the client returns a result. Applies
     * a performance-scaled durability repair to the stashed gear, consumes the
     * material units used, and hands everything back to the player.
     */
    public static void resolve(ServerPlayer player, boolean success, int perfectStrikes, int goodStrikes) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) return;

        ItemStack gear = session.gear();
        ItemStack material = session.material();

        // Sentinel: a negative perfect count means the player abandoned the
        // forge (closed the screen) — return the inputs untouched.
        if (perfectStrikes < 0) {
            giveBack(player, gear);
            giveBack(player, material);
            return;
        }

        int maxDmg = gear.getMaxDamage();
        int curDmg = gear.getDamageValue();
        if (maxDmg <= 0 || curDmg <= 0) {
            // Nothing to repair — return inputs as-is.
            giveBack(player, gear);
            giveBack(player, material);
            return;
        }

        // Total botch (zero perfect strikes → 0% efficiency): no repair, and the
        // bungled hammering actually warps the metal — chip off ~10% durability
        // (never enough to outright break it). Materials are kept.
        if (perfectStrikes <= 0) {
            int penalty = Math.max(1, maxDmg / 10);
            int newDmg = Math.min(maxDmg - 1, curDmg + penalty);
            gear.setDamageValue(Math.max(0, newDmg));
            giveBack(player, gear);
            giveBack(player, material);
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.ANVIL_DESTROY, SoundSource.PLAYERS, 0.6f, 0.9f);
            player.sendSystemMessage(Component.literal(
                    "You botched the forge — the metal warped and lost some durability.")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
            return;
        }

        // Repair efficiency is driven purely by perfect strikes: each one is worth
        // 20% — so 1→20%, 2→40%, 3→60%, 4→80%, 5→100% (a masterful forge).
        float perf = clamp(perfectStrikes * 0.20f, 0.0f, 1.0f);

        int unitRepair = Math.max(1, maxDmg / 4);          // vanilla parity: 1 unit ≈ 25%
        int maxUnits = Math.min(material.getCount(), 4);    // anvil caps at 4 units
        int possible = maxUnits * unitRepair;
        int desired = Math.min(curDmg, Math.round(possible * perf));

        if (desired <= 0) {
            // Too sloppy to make progress — keep the materials, no repair.
            giveBack(player, gear);
            giveBack(player, material);
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 0.7f);
            player.sendSystemMessage(Component.literal(
                    "The heat slipped away — no progress, but your materials are intact.")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
            return;
        }

        int unitsUsed = Math.min(maxUnits, Math.max(1, (int) Math.ceil(desired / (double) unitRepair)));
        int repaired = Math.min(curDmg, Math.min(desired, unitsUsed * unitRepair));

        gear.setDamageValue(curDmg - repaired);
        material.shrink(unitsUsed);

        giveBack(player, gear);
        giveBack(player, material);

        ItemCondition cond = ItemCondition.fromStack(gear);
        int pct = Math.round(100f * (1f - (float) gear.getDamageValue() / (float) maxDmg));
        boolean great = perfectStrikes >= 5;
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, great ? 1.3f : 1.0f);
        player.sendSystemMessage(Component.literal(
                (great ? "A masterful forge! " : "Repaired ") + "— "
                        + cond.displayName() + " (" + pct + "%)")
                .withStyle(Style.EMPTY.withColor(cond.tooltipColor())));
    }

    /** Return any stashed inputs on logout so they don't vanish. */
    public static void clear(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null) {
            giveBack(player, session.gear());
            giveBack(player, session.material());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static void giveBack(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }
}
