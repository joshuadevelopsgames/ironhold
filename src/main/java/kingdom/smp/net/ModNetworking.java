package kingdom.smp.net;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.accessory.AccessoryInventory;
import kingdom.smp.accessory.AccessoryMenu;
import kingdom.smp.client.VanityCache;
import kingdom.smp.client.VillagerDialogueCache;
import kingdom.smp.entity.MagicMinecartEntity;
import kingdom.smp.game.CloudDoubleJumpHandler;
import kingdom.smp.game.EncumbranceHandler;
import kingdom.smp.rpg.PlayerClass;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import kingdom.smp.rpg.RpgProgression;
import net.minecraft.network.chat.Component;
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

        // ── Kingdom Villager dialogue & emotes ───────────────────────────────
        registrar.playToClient(VillagerDialoguePayload.TYPE, VillagerDialoguePayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                VillagerDialogueCache.receiveDialogue(payload)));

        registrar.playToClient(VillagerEmotePayload.TYPE, VillagerEmotePayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() ->
                VillagerDialogueCache.receiveEmote(payload)));
    }

    public static void syncToClient(ServerPlayer player) {
        PlayerKingdomRpgData rpg = player.getData(ModAttachments.PLAYER_RPG.get());
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
            maxWeight);
        PacketDistributor.sendToPlayer(player, payload);
    }

    private static void handleClassChoice(ServerPlayer player, ClassChoicePayload payload) {
        int idx = payload.classIndex();
        if (idx < 0 || idx >= PlayerClass.values().length) {
            return;
        }
        PlayerClass chosen = PlayerClass.fromIndex(idx);
        if (chosen == PlayerClass.PEASANT) {
            return;
        }
        PlayerKingdomRpgData cur = player.getData(ModAttachments.PLAYER_RPG.get());
        if (cur.playerClass() != PlayerClass.PEASANT) {
            return;
        }
        PlayerKingdomRpgData next = new PlayerKingdomRpgData(cur.kingdomIndex(), chosen.ordinal(), 1, 0);
        player.setData(ModAttachments.PLAYER_RPG.get(), next);
        syncToClient(player);

        var server = player.level().getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(
                Component.literal("\u00A76" + player.getName().getString() + " has answered the call of the " + chosen.id() + "."),
                false);
        }
        Ironhold.LOGGER.info("{} chose class {}", player.getName().getString(), chosen.id());
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
