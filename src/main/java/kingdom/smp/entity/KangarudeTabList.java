package kingdom.smp.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import kingdom.smp.Ironhold;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adds and removes a fake "Kangarude" entry in the vanilla Tab player list so
 * he shows up alongside real players when {@code /k2 kanga join} runs.
 *
 * <p>Skin texture: we use the offline-derived UUID for our entry (so it lines
 * up with {@link kingdom.smp.client.entity.KangarudeSkinCache}'s in-world
 * lookup) but copy the {@code textures} property from the real Mojang
 * profile of the user "Kangarude". Tab-list head icons are rendered from
 * that property, not from the UUID — so the head matches the in-world model.
 */
public final class KangarudeTabList {

    /** Stable UUID derived from the username — matches KangarudeSkinCache. */
    public static final UUID PROFILE_ID =
        UUID.nameUUIDFromBytes(("OfflinePlayer:" + KangarudeEntity.SKIN_OWNER_NAME).getBytes());

    /** Bare profile (no textures) used until the async lookup finishes. */
    private static final GameProfile BARE_PROFILE =
        new GameProfile(PROFILE_ID, KangarudeEntity.SKIN_OWNER_NAME);

    /** Cached profile-with-textures, populated once per server start. */
    private static volatile GameProfile texturedProfile = null;
    private static volatile boolean lookupStarted = false;

    private KangarudeTabList() {}

    public static void add(MinecraftServer server) {
        if (server == null) return;

        GameProfile cached = texturedProfile;
        if (cached != null) {
            sendAddPacket(server, cached);
            return;
        }

        // First add this session: send bare profile immediately so Kanga
        // appears in the tab list without delay, then re-broadcast with the
        // real skin once Mojang's session service responds.
        sendAddPacket(server, BARE_PROFILE);
        ensureLookupStarted(server);
    }

    public static void remove(MinecraftServer server) {
        if (server == null) return;
        ClientboundPlayerInfoRemovePacket packet =
            new ClientboundPlayerInfoRemovePacket(List.of(PROFILE_ID));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(packet);
        }
    }

    private static void sendAddPacket(MinecraftServer server, GameProfile profile) {
        ClientboundPlayerInfoUpdatePacket.Entry entry =
            new ClientboundPlayerInfoUpdatePacket.Entry(
                PROFILE_ID,
                profile,
                true,                                // listed (visible in Tab)
                0,                                   // latency ms
                GameType.SURVIVAL,
                Component.literal(KangarudeEntity.SKIN_OWNER_NAME),
                true,                                // showHat
                0,                                   // listOrder
                null                                 // chatSession (no signed chat)
            );

        EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = EnumSet.of(
            ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME);

        // Construct with an empty real-player list, then drop our fake entry
        // into the (now-non-final, via AT) `entries` field.
        ClientboundPlayerInfoUpdatePacket packet =
            new ClientboundPlayerInfoUpdatePacket(actions, List.<ServerPlayer>of());
        packet.entries = List.of(entry);

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(packet);
        }
    }

    private static synchronized void ensureLookupStarted(MinecraftServer server) {
        if (lookupStarted) return;
        lookupStarted = true;

        Thread t = new Thread(() -> {
            try {
                Optional<GameProfile> resolved = server.services().profileResolver()
                    .fetchByName(KangarudeEntity.SKIN_OWNER_NAME);
                if (resolved.isEmpty()) {
                    Ironhold.LOGGER.warn(
                        "[Kangarude] Profile resolver returned no result for '{}'.",
                        KangarudeEntity.SKIN_OWNER_NAME);
                    return;
                }
                UUID realId = resolved.get().id();

                MinecraftSessionService sessions = server.services().sessionService();
                ProfileResult result = sessions.fetchProfile(realId, true);
                if (result == null) {
                    Ironhold.LOGGER.warn(
                        "[Kangarude] Session service returned null for {}.", realId);
                    return;
                }
                GameProfile real = result.profile();

                // Compose: keep our offline UUID, copy the textures property
                // from the real account so the tab head loads correctly.
                GameProfile composed =
                    new GameProfile(PROFILE_ID, KangarudeEntity.SKIN_OWNER_NAME);
                composed.properties().putAll(real.properties());
                texturedProfile = composed;

                Ironhold.LOGGER.info(
                    "[Kangarude] Tab-list skin resolved ({} texture properties).",
                    real.properties().size());

                // Re-broadcast: REMOVE then ADD so the client refreshes the
                // entry's textures (vanilla typically ignores ADD for an
                // already-known UUID).
                server.execute(() -> {
                    remove(server);
                    sendAddPacket(server, composed);
                });
            } catch (Throwable e) {
                Ironhold.LOGGER.warn("[Kangarude] Tab-list skin lookup failed: {}", e.toString());
            }
        }, "Kangarude-Tab-Skin");
        t.setDaemon(true);
        t.start();
    }
}
