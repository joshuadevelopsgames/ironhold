package kingdom.smp.quest;

import kingdom.smp.ModAttachments;
import kingdom.smp.game.RpgProgressionActions;
import kingdom.smp.net.ModNetworking;
import kingdom.smp.skill.PlayerSkillState;
import kingdom.smp.skill.SkillSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Server-side quest lifecycle: accept → track → (complete | expire) → redeem.
 *
 * <p>State lives in {@link QuestSavedData}; timer visuals in {@link QuestBossBars}; player
 * feedback in {@link QuestFeedback}. Skill-point rewards route through
 * {@link PlayerSkillState#withMilestone} (idempotent on quest id); class XP through
 * {@link RpgProgressionActions#grantClassXp}.
 */
public final class QuestService {
    private QuestService() {}

    /** Overworld game time — single clock for deadlines so cross-dimension play stays consistent. */
    private static long now(ServerPlayer player) {
        return player.level().getServer().overworld().getGameTime();
    }

    private static QuestSavedData data(ServerPlayer player) {
        return QuestSavedData.get((ServerLevel) player.level());
    }

    // ── Accept ──────────────────────────────────────────────────────────────

    /** Offer/accept a quest. Returns true if newly accepted; false if unknown or already in progress. */
    public static boolean accept(ServerPlayer player, String questId) {
        QuestDef def = Quests.byId(questId);
        if (def == null) return false;

        QuestSavedData data = data(player);
        QuestProgress existing = data.get(player.getUUID(), questId);
        // Re-accept only allowed if never taken or previously failed.
        if (existing != null && existing.status() != QuestStatus.FAILED) return false;

        long deadline = now(player) + def.durationTicks();
        QuestProgress progress = QuestProgress.start(def, deadline);
        data.put(player.getUUID(), progress);

        QuestBossBars.start(player, def, progress, now(player));
        QuestFeedback.accepted(player, def);
        return true;
    }

    // ── Kill tracking ─────────────────────────────────────────────────────────

    public static void recordKill(ServerPlayer player, Identifier entityId) {
        QuestSavedData data = data(player);
        for (QuestProgress prog : data.all(player.getUUID())) {
            if (prog.status() != QuestStatus.ACTIVE) continue;
            QuestDef def = Quests.byId(prog.questId());
            if (def == null) continue;

            boolean changed = false;
            for (int i = 0; i < def.objectives().size(); i++) {
                QuestObjective obj = def.objectives().get(i);
                if (obj.type() == QuestObjective.Type.SLAY
                        && obj.target().equals(entityId)
                        && prog.killCount(i) < obj.count()) {
                    prog = prog.incrementKill(i);
                    changed = true;
                }
            }
            if (changed) {
                data.put(player.getUUID(), prog);
                evaluate(player, def, prog);
            }
        }
    }

    // ── Per-tick maintenance: expiry + inventory completion + bar refresh ──────

    public static void tick(ServerPlayer player) {
        QuestSavedData data = data(player);
        long now = now(player);
        for (QuestProgress prog : data.all(player.getUUID())) {
            if (prog.status() == QuestStatus.ACTIVE && now >= prog.deadlineTick()) {
                fail(player, prog);
                continue;
            }
            if (prog.status() != QuestStatus.ACTIVE && prog.status() != QuestStatus.COMPLETE) continue;

            QuestDef def = Quests.byId(prog.questId());
            if (def == null) continue;
            if (prog.status() == QuestStatus.ACTIVE) {
                evaluate(player, def, prog); // inventory-backed objectives may have just completed
            }
            QuestBossBars.update(player, def, prog, now);
        }
    }

    /** Re-check completion for an ACTIVE quest; transition to COMPLETE with feedback if all met. */
    private static void evaluate(ServerPlayer player, QuestDef def, QuestProgress prog) {
        if (prog.status() != QuestStatus.ACTIVE) return;
        if (!isComplete(player, def, prog)) return;
        QuestProgress done = prog.withStatus(QuestStatus.COMPLETE);
        data(player).put(player.getUUID(), done);
        QuestBossBars.update(player, def, done, now(player));
        QuestFeedback.completed(player, def);
    }

    private static void fail(ServerPlayer player, QuestProgress prog) {
        QuestDef def = Quests.byId(prog.questId());
        QuestProgress failed = prog.withStatus(QuestStatus.FAILED);
        data(player).put(player.getUUID(), failed);
        QuestBossBars.stop(player, prog.questId());
        if (def != null) QuestFeedback.failed(player, def);
    }

    // ── Redeem ──────────────────────────────────────────────────────────────

    /** Claim rewards for a COMPLETE quest. Returns true on payout. */
    public static boolean redeem(ServerPlayer player, String questId) {
        QuestSavedData data = data(player);
        QuestProgress prog = data.get(player.getUUID(), questId);
        if (prog == null || prog.status() != QuestStatus.COMPLETE) return false;
        QuestDef def = Quests.byId(questId);
        if (def == null) return false;

        // Re-verify and consume DELIVER items so the player can't drop them after completing.
        if (!isComplete(player, def, prog)) {
            // Items were spent/lost since completion — knock it back to ACTIVE.
            data.put(player.getUUID(), prog.withStatus(QuestStatus.ACTIVE));
            player.sendSystemMessage(Component.literal("You no longer have the required items."));
            return false;
        }
        consumeDeliveries(player, def);
        grantReward(player, def);

        data.put(player.getUUID(), prog.withStatus(QuestStatus.CLAIMED));
        QuestBossBars.stop(player, questId);
        QuestFeedback.redeemed(player, def);
        return true;
    }

    private static void grantReward(ServerPlayer player, QuestDef def) {
        QuestReward reward = def.reward();
        if (reward.hasSkillPoints()) {
            SkillSavedData skills = SkillSavedData.get((ServerLevel) player.level());
            PlayerSkillState state = skills.stateFor(player.getUUID());
            PlayerSkillState updated = state.withMilestone(def.milestoneId(), reward.skillPoints());
            if (updated != state) {
                skills.setState(player.getUUID(), updated);
                ModNetworking.syncSkillsToClient(player);
            }
        }
        if (reward.hasClassXp()) {
            RpgProgressionActions.grantClassXp(player, reward.classXp());
        }
        Inventory inv = player.getInventory();
        for (ItemStack stack : reward.items()) {
            if (!stack.isEmpty()) inv.placeItemBackInInventory(stack.copy());
        }
    }

    // ── Objective evaluation ──────────────────────────────────────────────────

    public static boolean isComplete(ServerPlayer player, QuestDef def, QuestProgress prog) {
        for (int i = 0; i < def.objectives().size(); i++) {
            if (count(player, def, prog, i) < def.objectives().get(i).count()) return false;
        }
        return true;
    }

    /** Current progress toward objective {@code i}: live inventory count, or stored kill tally. */
    public static int count(ServerPlayer player, QuestDef def, QuestProgress prog, int i) {
        QuestObjective obj = def.objectives().get(i);
        if (obj.inventoryBacked()) {
            return Math.min(obj.count(), inventoryCount(player, obj.target()));
        }
        return Math.min(obj.count(), prog.killCount(i));
    }

    private static int inventoryCount(ServerPlayer player, Identifier itemId) {
        Inventory inv = player.getInventory();
        int total = 0;
        for (int s = 0; s < inv.getContainerSize(); s++) {
            ItemStack stack = inv.getItem(s);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void consumeDeliveries(ServerPlayer player, QuestDef def) {
        Inventory inv = player.getInventory();
        for (QuestObjective obj : def.objectives()) {
            if (obj.type() != QuestObjective.Type.DELIVER) continue;
            int remaining = obj.count();
            for (int s = 0; s < inv.getContainerSize() && remaining > 0; s++) {
                ItemStack stack = inv.getItem(s);
                if (stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(obj.target())) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }

    // ── Login/logout bar management ─────────────────────────────────────────────

    /** Recreate timer bars for all of a player's in-progress quests (call on login). */
    public static void rebuildBars(ServerPlayer player) {
        QuestSavedData data = data(player);
        long now = now(player);
        for (QuestProgress prog : data.all(player.getUUID())) {
            if (prog.status() != QuestStatus.ACTIVE && prog.status() != QuestStatus.COMPLETE) continue;
            QuestDef def = Quests.byId(prog.questId());
            if (def != null) QuestBossBars.start(player, def, prog, now);
        }
    }

    public static UUID idOf(ServerPlayer player) {
        return player.getUUID();
    }
}
