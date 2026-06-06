package kingdom.smp.net;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.accessory.AccessoryInventory;
import kingdom.smp.accessory.AccessoryMenu;
import kingdom.smp.client.ClientSneakDetectionState;
import kingdom.smp.client.DisguiseCache;
import kingdom.smp.client.VanityCache;
import kingdom.smp.client.VillagerDialogueCache;
import kingdom.smp.entity.MagicMinecartEntity;
import kingdom.smp.game.ClassStatHandler;
import kingdom.smp.game.CloudDoubleJumpHandler;
import kingdom.smp.item.SirensRingItem;
import kingdom.smp.game.EncumbranceHandler;
import kingdom.smp.game.PromotionStoneSummoner;
import kingdom.smp.game.RpgXpBarSync;
import kingdom.smp.rpg.ability.AbilityDispatch;
import kingdom.smp.rpg.CompletedClasses;
import kingdom.smp.rpg.PlayerClass;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import kingdom.smp.rpg.RpgProgression;
import kingdom.smp.skill.ClientSkillData;
import kingdom.smp.skill.PlayerSkillState;
import kingdom.smp.skill.Profession;
import kingdom.smp.skill.SkillSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Ironhold.MODID).versioned("1");

        registrar.playToClient(SyncRpgDataPayload.TYPE, SyncRpgDataPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> ClientRpgData.receive(payload)));

        registrar.playToClient(OpenClassSelectionPayload.TYPE, OpenClassSelectionPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(ClientRpgData::openClassSelection));

        registrar.playToClient(OpenKingdomSelectionPayload.TYPE, OpenKingdomSelectionPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(ClientRpgData::openKingdomSelection));

        registrar.playToClient(OpenProfilePayload.TYPE, OpenProfilePayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(ClientRpgData::openProfile));

        registrar.playToClient(OpenMenuPayload.TYPE, OpenMenuPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(ClientRpgData::openMenu));

        registrar.playToClient(OpenConsolePayload.TYPE, OpenConsolePayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(ClientRpgData::openConsole));

        registrar.playToClient(WaxOverlayPayload.TYPE, WaxOverlayPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> kingdom.smp.client.WaxOverlayRenderer.add(payload.pos())));

        registrar.playToServer(ClassChoicePayload.TYPE, ClassChoicePayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    handleClassChoice(sp, payload);
                }
            }));

        registrar.playToServer(KingdomChoicePayload.TYPE, KingdomChoicePayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    handleKingdomChoice(sp, payload);
                }
            }));

        registrar.playToServer(MagicMinecartInputPayload.TYPE, MagicMinecartInputPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp && sp.getVehicle() instanceof MagicMinecartEntity cart) {
                    cart.updateServerInput(
                        payload.forward(),
                        payload.backward(),
                        payload.left(),
                        payload.right(),
                        payload.jump(),
                        payload.sprint()
                    );
                }
            }));

        registrar.playToServer(CloudDoubleJumpPayload.TYPE, CloudDoubleJumpPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    CloudDoubleJumpHandler.tryApplyDoubleJump(sp);
                }
            }));

        registrar.playToServer(SirensRingActivatePayload.TYPE, SirensRingActivatePayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp && SirensRingItem.isEquipped(sp)) {
                    SirensRingItem.tryActivate(sp);
                }
            }));

        registrar.playToServer(SeashellDashPayload.TYPE, SeashellDashPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    kingdom.smp.item.SeashellItem.tryDash(sp);
                }
            }));

        registrar.playToServer(AbilityCastPayload.TYPE, AbilityCastPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    AbilityDispatch.tryCast(sp, payload.slot());
                }
            }));

        registrar.playToServer(KangaPushToTalkPayload.TYPE, KangaPushToTalkPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    kingdom.smp.ai.KangaPttBridge.togglePtt(sp);
                }
            }));

        // ── Accessory / Vanity payloads ───────────────────────────────────────

        registrar.playToServer(OpenAccessoryPayload.TYPE, OpenAccessoryPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    AccessoryInventory inv = sp.getData(ModAttachments.ACCESSORY_INV.get());
                    sp.openMenu(new SimpleMenuProvider(
                        (id, playerInv, p) -> new AccessoryMenu(id, playerInv, inv),
                        Component.literal("Equipment")));
                }
            }));

        registrar.playToClient(SyncVanityPayload.TYPE, SyncVanityPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                VanityCache.update(
                    payload.playerUUID(),
                    payload.vanityHead(),
                    payload.vanityChest(),
                    payload.vanityLegs(),
                    payload.vanityFeet())));

        registrar.playToClient(SyncDisguisePayload.TYPE, SyncDisguisePayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                DisguiseCache.update(payload.playerUUID(), payload.entityTypeId())));

        // ── Kingdom Villager dialogue & emotes ───────────────────────────────
        registrar.playToClient(VillagerDialoguePayload.TYPE, VillagerDialoguePayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                VillagerDialogueCache.receiveDialogue(payload)));

        registrar.playToClient(VillagerEmotePayload.TYPE, VillagerEmotePayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                VillagerDialogueCache.receiveEmote(payload)));

        // ── Player "point" emote (clean-room procedural pose) ────────────────
        // NeoForge 26.1 requires explicit server + client handlers for a bidirectional
        // payload (the single-handler overload leaves the clientbound side unhandled).
        //  • Server handler: rebroadcast to nearby players, trusting the sender's UUID.
        //  • Client handler: play the pose for that player. (Its body only class-loads
        //    PointEmoteState on invocation, which never happens on a dedicated server.)
        registrar.playBidirectional(PointEmotePayload.TYPE, PointEmotePayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    PacketDistributor.sendToPlayersTrackingEntity(sp,
                        new PointEmotePayload(sp.getUUID()));
                }
            }),
            (payload, ctx) -> ctx.enqueueWork(() ->
                kingdom.smp.client.emote.PointEmoteState.receive(payload.player())));

        registrar.playToClient(OpenVillagerScreenPayload.TYPE, OpenVillagerScreenPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                VillagerDialogueCache.openDialogueScreen(payload)));

        // ── Warden Halric (interactive AI dialogue) ──────────────────────────
        registrar.playToClient(OpenWardenScreenPayload.TYPE, OpenWardenScreenPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                VillagerDialogueCache.openWardenScreen(payload)));

        registrar.playToClient(UpdateWardenScreenPayload.TYPE, UpdateWardenScreenPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                VillagerDialogueCache.updateWardenScreen(payload)));

        registrar.playToServer(WardenChatPayload.TYPE, WardenChatPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    handleWardenChat(sp, payload);
                }
            }));

        registrar.playToServer(WardenPttTogglePayload.TYPE, WardenPttTogglePayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    kingdom.smp.ai.KangaPttBridge.togglePtt(sp);
                }
            }));

        registrar.playToServer(NpcMutePayload.TYPE, NpcMutePayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    kingdom.smp.ai.NpcMuteRegistry.get((ServerLevel) sp.level())
                        .setMuted(sp.getUUID(), payload.npcTag(), payload.muted());
                }
            }));

        // ── Skill tree (profession-skill-system) ──────────────────────────────
        registrar.playToClient(SyncSkillStatePayload.TYPE, SyncSkillStatePayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> ClientSkillData.receive(payload)));

        registrar.playToClient(SyncUseSkillsPayload.TYPE, SyncUseSkillsPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> kingdom.smp.skill.ClientUseSkillData.receive(payload)));

        registrar.playToServer(SpendSkillPointPayload.TYPE, SpendSkillPointPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    handleSpendSkillPoint(sp, payload.profession());
                }
            }));

        registrar.playToServer(RespecSkillPayload.TYPE, RespecSkillPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    handleRespecAll(sp);
                }
            }));

        registrar.playToClient(SneakDetectionPayload.TYPE, SneakDetectionPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> ClientSneakDetectionState.receive(payload)));

        registrar.playToClient(SyncMentionNamesPayload.TYPE, SyncMentionNamesPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                kingdom.smp.client.ClientMentionNames.receive(payload)));

        // ── Immersive portals: cross-dimensional see-through streaming ────────
        registrar.playToClient(ClientboundOpenPortalViewPayload.TYPE, ClientboundOpenPortalViewPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                kingdom.smp.portal.client.ClientDimensionStack.open(payload)));

        registrar.playToClient(ClientboundChunkRedirectPayload.TYPE, ClientboundChunkRedirectPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                kingdom.smp.portal.client.ClientDimensionStack.applyChunk(payload)));

        registrar.playToClient(ClientboundClosePortalViewPayload.TYPE, ClientboundClosePortalViewPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                kingdom.smp.portal.client.ClientDimensionStack.close(payload.dimId())));

        registrar.playToServer(ServerboundRequestPortalViewsPayload.TYPE, ServerboundRequestPortalViewsPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    kingdom.smp.portal.server.PortalViewTracker.handleRequest(sp, payload.views());
                }
            }));

        // ── Fishing bite minigame ─────────────────────────────────────────────
        registrar.playToClient(FishingBiteStartPayload.TYPE, FishingBiteStartPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> ClientRpgData.openFishingMinigame(payload)));

        registrar.playToServer(FishingMinigameResultPayload.TYPE, FishingMinigameResultPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    kingdom.smp.fishing.FishingMinigameManager.resolve(sp, payload.won());
                }
            }));

        // ── Blacksmithing forge minigame ──────────────────────────────────────
        registrar.playToServer(ForgeHammerRequestPayload.TYPE, ForgeHammerRequestPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    kingdom.smp.blacksmithing.BlacksmithingMinigameManager.tryStartFromAnvil(sp);
                }
            }));

        registrar.playToClient(ForgeMinigameStartPayload.TYPE, ForgeMinigameStartPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> ClientRpgData.openForgeMinigame(payload)));

        registrar.playToServer(ForgeMinigameResultPayload.TYPE, ForgeMinigameResultPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) {
                    kingdom.smp.blacksmithing.BlacksmithingMinigameManager.resolve(
                        sp, payload.success(), payload.perfectStrikes(), payload.goodStrikes());
                }
            }));
    }

    /**
     * Server-side handler for the player typing a reply in the dialogue
     * screen. Validates the entity id, ensures the entity is an
     * {@link kingdom.smp.ai.NpcChatPartner}, and forwards the message.
     * Rejects out-of-range or wrong-type entities silently.
     */
    private static void handleWardenChat(ServerPlayer player, WardenChatPayload payload) {
        var entity = player.level().getEntity(payload.entityId());
        if (!(entity instanceof kingdom.smp.ai.NpcChatPartner partner)) return;
        if (!(entity instanceof net.minecraft.world.entity.Entity e)) return;
        if (e.distanceToSqr(player) > 16.0 * 16.0) return;
        String message = payload.message();
        if (message == null || message.isBlank()) return;
        if (message.length() > 500) message = message.substring(0, 500);
        partner.onPartnerChat(player, message);
    }

    private static void handleRespecAll(ServerPlayer player) {
        SkillSavedData data = SkillSavedData.get((ServerLevel) player.level());
        PlayerSkillState state = data.stateFor(player.getUUID());
        // No-op if nothing is spent.
        if (state.totalPointsSpent() <= 0) {
            syncSkillsToClient(player);
            return;
        }
        PlayerSkillState updated = state.respecAll();
        data.setState(player.getUUID(), updated);
        syncSkillsToClient(player);
    }

    /**
     * Server-side handler for a client's skill-point spend request.
     * Validates available points and rank progression, applies the spend, syncs back.
     */
    private static void handleSpendSkillPoint(ServerPlayer player, Profession profession) {
        SkillSavedData data = SkillSavedData.get((ServerLevel) player.level());
        PlayerSkillState state = data.stateFor(player.getUUID());
        PlayerSkillState updated = state.trySpendOn(profession);
        if (updated == null) {
            // Out of points or already at Master — silently ignore. Re-sync current state
            // so the client UI corrects itself if it was out of date.
            syncSkillsToClient(player);
            return;
        }
        data.setState(player.getUUID(), updated);
        syncSkillsToClient(player);
    }

    /** Push the player's current PlayerSkillState to their client. */
    public static void syncSkillsToClient(ServerPlayer player) {
        SkillSavedData data = SkillSavedData.get((ServerLevel) player.level());
        PlayerSkillState state = data.stateFor(player.getUUID());
        SyncSkillStatePayload payload = new SyncSkillStatePayload(
                state.unspentProfessionPoints(),
                state.currentRanks(),
                state.milestonesCompleted().size());
        PacketDistributor.sendToPlayer(player, payload);
    }

    /** Push the player's current use-to-level skill XP (Pickpocket/Sneak/Fishing) to their client. */
    public static void syncUseSkillsToClient(ServerPlayer player) {
        kingdom.smp.skill.useskill.PlayerUseSkills skills = player.getData(ModAttachments.USE_SKILLS.get());
        PacketDistributor.sendToPlayer(player, new SyncUseSkillsPayload(skills.xp()));
    }

    public static void syncToClient(ServerPlayer player) {
        PlayerKingdomRpgData rpg = player.getData(ModAttachments.PLAYER_RPG.get());
        CompletedClasses completed = player.getData(ModAttachments.COMPLETED_CLASSES.get());
        int weight = EncumbranceHandler.weightFor(player);
        int maxWeight = rpg.playerClass().maxCarryWeight();
        int xpToNext = RpgProgression.xpToReachNextLevel(rpg.classLevel());
        SyncRpgDataPayload payload = new SyncRpgDataPayload(
            rpg.kingdomIndex(),
            rpg.classIndex(),
            rpg.classLevel(),
            rpg.xpIntoLevel(),
            xpToNext,
            weight,
            maxWeight,
            completed.classOrdinals());
        PacketDistributor.sendToPlayer(player, payload);
    }

    private static void handleClassChoice(ServerPlayer player, ClassChoicePayload payload) {
        int idx = payload.classIndex();
        if (idx < 0 || idx >= PlayerClass.values().length) {
            Ironhold.LOGGER.warn("Class choice from {} rejected: invalid ordinal {}",
                player.getName().getString(), idx);
            return;
        }
        PlayerClass chosen = PlayerClass.fromIndex(idx);
        if (chosen == PlayerClass.PEASANT) {
            return;
        }
        PlayerKingdomRpgData cur = player.getData(ModAttachments.PLAYER_RPG.get());
        PlayerClass currentClass = cur.playerClass();
        CompletedClasses completed = player.getData(ModAttachments.COMPLETED_CLASSES.get());

        // Creative-mode players bypass the level gate so they can test freely.
        boolean bypass = player.isCreative();

        // ── Validate the promotion ───────────────────────────────────────────
        // 1) Player must have reached the promotion level for their current tier
        int promoLevel = RpgProgression.promotionLevelForTier(currentClass.tier());
        if (!bypass && promoLevel > 0 && cur.classLevel() < promoLevel) {
            player.sendSystemMessage(Component.literal(
                "§cYou must reach " + currentClass.id() + " Level " + promoLevel
                + " before promoting (currently Level " + cur.classLevel() + ")."));
            Ironhold.LOGGER.info("{} class choice rejected: {} L{} < required L{}",
                player.getName().getString(), currentClass.id(), cur.classLevel(), promoLevel);
            return;
        }

        // 2) Chosen class must be exactly one tier above current (or Tier 1 for Peasant)
        int expectedTier = currentClass.tier() + 1;
        if (chosen.tier() != expectedTier) {
            player.sendSystemMessage(Component.literal(
                "§cYou cannot promote directly to " + chosen.id()
                + " (Tier " + chosen.tier() + ") from " + currentClass.id()
                + " (Tier " + currentClass.tier() + ")."));
            return;
        }

        // 3) Prerequisites must be met: current class counts as completed + all prior completions
        CompletedClasses withCurrent = completed.withCompleted(currentClass);
        if (!chosen.canUnlock(withCurrent.asSet())) {
            player.sendSystemMessage(Component.literal(
                "§cYou have not completed the prerequisites for " + chosen.id() + "."));
            return;
        }

        // ── Apply the promotion ──────────────────────────────────────────────
        // Mark current class as completed
        player.setData(ModAttachments.COMPLETED_CLASSES.get(), withCurrent);

        // Switch to new class, reset to level 1
        PlayerKingdomRpgData next = new PlayerKingdomRpgData(
            cur.kingdomIndex(), chosen.ordinal(), 1, 0);
        player.setData(ModAttachments.PLAYER_RPG.get(), next);
        ClassStatHandler.apply(player, next);
        RpgXpBarSync.sync(player, next);
        syncToClient(player);

        // Consume the summoned Class Stone they promoted at (no-op if none).
        PromotionStoneSummoner.removeFor(player);

        // ── Broadcast ────────────────────────────────────────────────────────
        var server = player.level().getServer();
        if (server != null) {
            String verb = currentClass == PlayerClass.PEASANT
                ? " has answered the call of the "
                : " has ascended to ";
            server.getPlayerList().broadcastSystemMessage(
                Component.literal("\u00A76" + player.getName().getString() + verb + chosen.id() + "."),
                false);
        }
        Ironhold.LOGGER.info("{} promoted from {} to {}",
            player.getName().getString(), currentClass.id(), chosen.id());
    }

    private static void handleKingdomChoice(ServerPlayer player, KingdomChoicePayload payload) {
        int idx = payload.kingdomIndex();
        if (idx < 0 || idx > 3) {
            return;
        }
        PlayerKingdomRpgData cur = player.getData(ModAttachments.PLAYER_RPG.get());
        PlayerKingdomRpgData next = new PlayerKingdomRpgData(idx, cur.classIndex(), cur.classLevel(), cur.xpIntoLevel());
        player.setData(ModAttachments.PLAYER_RPG.get(), next);
        syncToClient(player);
        Ironhold.LOGGER.info("{} joined kingdom {}", player.getName().getString(), idx);
    }
}
