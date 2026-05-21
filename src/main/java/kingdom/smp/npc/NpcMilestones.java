package kingdom.smp.npc;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.ai.NpcChatPartner;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Delivers Stardew-style one-time "bond rewards" when a player's rapport with a
 * named NPC first crosses a milestone defined in that NPC's manifest.
 *
 * <p>Each call grants at most one milestone — the lowest threshold the player
 * now qualifies for but hasn't yet claimed — so successive interactions walk
 * through the rewards one ceremonial moment at a time. The crossed threshold is
 * stamped onto {@link PlayerNpcBonds} so it never repeats.
 */
public final class NpcMilestones {

    private NpcMilestones() {}

    /**
     * Deliver the next unclaimed bond milestone the player qualifies for, if any.
     *
     * @return true if a milestone was delivered this call.
     */
    public static boolean tryDeliver(ServerPlayer player, NpcChatPartner partner, @Nullable NpcManifest manifest) {
        if (manifest == null || manifest.milestones().isEmpty()) return false;

        PlayerNpcBonds bonds = player.getData(ModAttachments.NPC_BONDS);
        String npcKey = partner.tag();
        PlayerNpcBonds.Entry entry = bonds.get(npcKey);
        int rapport = entry.rapport();
        int claimed = entry.claimedMilestone();

        NpcManifest.Milestone next = null;
        for (NpcManifest.Milestone m : manifest.milestones()) {
            if (m.rapport() > claimed && m.rapport() <= rapport
                && (next == null || m.rapport() < next.rapport())) {
                next = m;
            }
        }
        if (next == null) return false;

        // Persist the claim first so a delivery failure can't re-grant on a retry.
        player.setData(ModAttachments.NPC_BONDS, bonds.withClaimedMilestone(npcKey, next.rapport()));

        for (NpcManifest.ItemGrant g : next.grants()) {
            grantItem(player, g);
        }
        for (NpcManifest.EffectGrant e : next.effects()) {
            grantEffect(player, e);
        }

        if (player.level() instanceof ServerLevel sl) {
            sl.playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, player.getSoundSource(), 0.7f, 1.4f);
        }

        Ironhold.LOGGER.info("[NpcMilestone] {} reached bond {} with {} — granted {} item(s), {} effect(s)",
            player.getName().getString(), next.rapport(), npcKey, next.grants().size(), next.effects().size());

        if (next.line() != null && !next.line().isBlank()) {
            partner.giftReaction(player, next.line());
        }
        return true;
    }

    private static void grantItem(ServerPlayer player, NpcManifest.ItemGrant g) {
        Identifier id = Identifier.parse(g.item());
        Item item = BuiltInRegistries.ITEM.get(id).map(h -> h.value()).orElse(null);
        if (item == null) {
            Ironhold.LOGGER.warn("[NpcMilestone] unknown item in milestone grant: {}", g.item());
            return;
        }
        int remaining = Math.max(1, g.count());
        int max = new ItemStack(item).getMaxStackSize();
        while (remaining > 0) {
            int n = Math.min(remaining, max);
            // Adds to inventory; any overflow is dropped at the player's feet.
            player.getInventory().placeItemBackInInventory(new ItemStack(item, n));
            remaining -= n;
        }
    }

    private static void grantEffect(ServerPlayer player, NpcManifest.EffectGrant e) {
        Identifier id = Identifier.parse(e.effect());
        BuiltInRegistries.MOB_EFFECT.get(id).ifPresentOrElse(
            holder -> player.addEffect(new MobEffectInstance(holder, e.durationTicks(), e.amplifier())),
            () -> Ironhold.LOGGER.warn("[NpcMilestone] unknown effect in milestone grant: {}", e.effect()));
    }
}
