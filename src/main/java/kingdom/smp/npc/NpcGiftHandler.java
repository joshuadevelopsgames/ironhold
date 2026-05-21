package kingdom.smp.npc;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.ai.NpcChatPartner;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Sneak-right-click an NPC with an item in hand → give it as a gift.
 *
 * <p>Reads the NPC's manifest, applies the matching {@code rapport_delta}, sends
 * the manifest's reaction line to the player as chat, plays a small chime, and
 * persists the new rapport on the player's {@link PlayerNpcBonds} attachment.
 *
 * <p>Daily cap: one gift per NPC per server day. Past the cap, the NPC tells
 * the player they've already received enough today and the item is not consumed.
 *
 * <p>Only triggers on {@link NpcChatPartner}s with a non-empty manifest, so it
 * doesn't collide with vanilla villager trading or PickpocketHandler (which
 * targets {@link net.minecraft.world.entity.npc.Villager}, not the named
 * Ironhold NPCs that extend {@code PathfinderMob}).
 */
public final class NpcGiftHandler {
    private NpcGiftHandler() {}

    /** One gift per NPC per server-day, per player. */
    private static final int DAILY_GIFT_CAP = 1;

    /** Ticks-per-day in vanilla; used to bucket gifts into "days". */
    private static final long TICKS_PER_DAY = 24000L;

    /** Stardew-style: a gift given on the NPC's birthday counts for far more. */
    private static final int BIRTHDAY_GIFT_MULTIPLIER = 5;
    private static final long DAYS_PER_SEASON  = 30L;
    private static final long SEASONS_PER_YEAR = 4L;

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Entity target = event.getTarget();
        if (!(target instanceof NpcChatPartner partner)) return;

        // Crouch-right-click is the dedicated gift gesture for our NPCs.
        // A normal (standing) right-click falls through to open the dialogue.
        if (!player.isCrouching()) {
            Ironhold.LOGGER.debug("[NpcGift] {} right-clicked {} but not sneaking — passing to dialogue",
                player.getName().getString(), partner.tag());
            return;
        }

        // From here we always suppress the dialogue: a crouch-click should
        // never open a conversation, so the gesture is reserved for gifting
        // and the NPC reliably takes whatever the player offers.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            // Empty-handed crouch on a recruitable, well-bonded NPC toggles their
            // companion state (follow → station at base → dismiss).
            tryToggleCompanion(player, partner, target);
            return;
        }

        // Resolve the manifest by NPC tag — tag().toLowerCase() maps to the
        // datapack file at data/ironhold/npc_manifests/npc/<tag>.json.
        NpcManifest manifest = manifestFor(partner);
        if (manifest == null || manifest == NpcManifest.EMPTY) {
            Ironhold.LOGGER.warn("[NpcGift] no manifest for npc tag '{}' (expected ironhold:npc/{})",
                partner.tag(), partner.tag().toLowerCase());
            return;
        }

        String itemId = NpcItemReaction.itemId(held);
        NpcItemReaction.Reaction reaction = NpcItemReaction.resolve(manifest, held);
        if (!reaction.isPresent()) {
            // The NPC has no opinion on this item — don't consume it (avoid
            // accidental loss of valuables), but give the player feedback so
            // the silent crouch-click isn't confusing.
            Ironhold.LOGGER.info("[NpcGift] {} offered {} to {} — no preference match (manifest has {} tastes, {} disdains)",
                player.getName().getString(), itemId, partner.tag(),
                manifest.tastes().size(), manifest.disdains().size());
            player.sendSystemMessage(
                Component.literal("[" + partner.tag() + "] ")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true))
                    .copy()
                    .append(Component.literal("(They have no particular use for that.)")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true))));
            return;
        }

        Ironhold.LOGGER.info("[NpcGift] {} gifting {} to {} — matched {} ({} rapport)",
            player.getName().getString(), itemId, partner.tag(), reaction.kind(), reaction.rapportDelta());

        PlayerNpcBonds bonds = player.getData(ModAttachments.NPC_BONDS);
        String npcKey = partner.tag();
        long today = player.level().getGameTime() / TICKS_PER_DAY;

        if (bonds.giftCapReachedToday(npcKey, today, DAILY_GIFT_CAP)) {
            // Over-the-cap notice stays in chat — the NPC shouldn't waste a
            // voiced line just to say "no thanks, had enough today".
            player.sendSystemMessage(
                Component.literal("[" + partner.tag() + "] ")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true))
                    .copy()
                    .append(Component.literal("(They've had enough gifts today. Try again tomorrow.)")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true))));
            return;
        }

        // A gift on the NPC's birthday lands far harder (positive gifts only).
        boolean birthday = isBirthdayToday(manifest, today);
        int effectiveDelta = reaction.rapportDelta();
        if (birthday && effectiveDelta > 0) {
            effectiveDelta *= BIRTHDAY_GIFT_MULTIPLIER;
        }

        // Apply gift: consume one item, shift rapport, persist.
        if (!player.isCreative()) {
            held.shrink(1);
        }
        PlayerNpcBonds updated = bonds.applyGift(npcKey, effectiveDelta, today);
        player.setData(ModAttachments.NPC_BONDS, updated);

        if (birthday && reaction.rapportDelta() > 0) {
            player.sendSystemMessage(
                Component.literal("  ")
                    .append(Component.literal("(It's their birthday — your gift means all the more.)")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE).withItalic(true))));
        }

        // Route the reaction into the voiced dialogue UI (opens screen if not
        // already open, otherwise pushes the line into the existing one) and
        // speak it aloud via the NPC's ElevenLabs voice.
        partner.giftReaction(player, reaction.line());

        // Quiet "+N rapport" / "-N rapport" line in chat so the player still
        // sees the numeric shift — the dialogue screen's hearts row picks it
        // up automatically on the next render once the synced bonds attachment
        // refreshes.
        ChatFormatting deltaColor = effectiveDelta >= 0
            ? ChatFormatting.GREEN : ChatFormatting.RED;
        String deltaSign = effectiveDelta >= 0 ? "+" : "";
        player.sendSystemMessage(
            Component.literal("  ")
                .append(Component.literal(deltaSign + effectiveDelta + " rapport with " + partner.tag())
                    .withStyle(Style.EMPTY.withColor(deltaColor).withItalic(true))));

        // Small chime — positive gifts get a brighter pitch.
        if (player.level() instanceof ServerLevel sl) {
            float pitch = effectiveDelta >= 0 ? 1.2f : 0.7f;
            sl.playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, player.getSoundSource(), 0.6f, pitch);
        }

        // If this gift pushed the player across a bond milestone, deliver the
        // reward + voiced line now (one milestone per interaction).
        NpcMilestones.tryDeliver(player, partner, manifest);
    }

    /**
     * Empty-handed crouch interaction: if the target is a recruitable companion
     * NPC the player has bonded with, advance its follow/station/dismiss state.
     * Otherwise it's a harmless no-op (the dialogue stays suppressed).
     */
    private static void tryToggleCompanion(ServerPlayer player, NpcChatPartner partner, Entity target) {
        if (!(target instanceof NpcCompanion companion)) {
            Ironhold.LOGGER.debug("[NpcGift] {} crouch-clicked {} empty-handed — not a companion NPC",
                player.getName().getString(), partner.tag());
            return;
        }
        NpcManifest manifest = manifestFor(partner);
        if (manifest == null || !manifest.companion()) {
            player.sendSystemMessage(npcNote(partner, "(They're not the sort to leave their post.)"));
            return;
        }
        // The trust gate only blocks the initial recruit; an NPC already
        // travelling with you can always be re-stationed or sent home.
        int rapport = player.getData(ModAttachments.NPC_BONDS).rapportFor(partner.tag());
        if (companion.disposition() == NpcDisposition.FREE && rapport < NpcRapport.RAPPORT_TRUSTED) {
            player.sendSystemMessage(npcNote(partner,
                "(They're fond of you — but not enough to follow you anywhere yet. Earn their trust first.)"));
            return;
        }
        companion.cycleCompanionState(player);
    }

    private static Component npcNote(NpcChatPartner partner, String text) {
        return Component.literal("[" + partner.tag() + "] ")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true))
            .copy()
            .append(Component.literal(text)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true)));
    }

    /**
     * Is {@code today} (server day = gameTime / 24000) the NPC's birthday?
     * The manifest stores season 0-3 and day 1-30; the year is treated as
     * 4 × 30 = 120 days, with day 0 = the first day of spring.
     */
    private static boolean isBirthdayToday(NpcManifest manifest, long today) {
        NpcManifest.Birthday bd = manifest.birthday();
        if (bd == null) return false;
        long dayOfYear = Math.floorMod(today, DAYS_PER_SEASON * SEASONS_PER_YEAR);
        int season = (int) (dayOfYear / DAYS_PER_SEASON);
        int dayOfSeason = (int) (dayOfYear % DAYS_PER_SEASON) + 1;
        return bd.season() == season && bd.day() == dayOfSeason;
    }

    private static NpcManifest manifestFor(NpcChatPartner partner) {
        // Convention: data/ironhold/npc_manifests/npc/<tag-lowercase>.json
        // matches partner.tag() — the same name string the dialogue subtitle uses.
        String key = partner.tag().toLowerCase();
        net.minecraft.resources.Identifier id =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("ironhold", "npc/" + key);
        return NpcManifests.resolve(id, null);
    }
}
