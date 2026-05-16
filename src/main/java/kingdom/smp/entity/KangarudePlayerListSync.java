package kingdom.smp.entity;

import com.mojang.authlib.GameProfile;
import kingdom.smp.mixin.PlayerInfoUpdatePacketAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side bookkeeping + packet broadcasting for the synthetic player-list
 * entries that represent online {@link KangarudeEntity} instances.
 *
 * <p>Each Kangarude shows up in the vanilla tab list as if it were a real
 * player. When the entity transforms into Kangabrine the original "Kangarude"
 * entry is removed and a new "Kangabrine" entry is added — matching the
 * "left the game"/"joined the game" theatrics the player sees in chat.
 *
 * <p>Vanilla {@link ClientboundPlayerInfoUpdatePacket} has no public
 * constructor accepting arbitrary {@code Entry} lists, so we build it with
 * an empty {@code ServerPlayer} collection and overwrite the {@code entries}
 * + {@code actions} fields via {@link PlayerInfoUpdatePacketAccessor}.
 */
public final class KangarudePlayerListSync {
    private KangarudePlayerListSync() {}

    public enum Mode { KANGARUDE, KANGABRINE }

    /** All currently-listed synthetic profiles, keyed by their fake profile UUID. */
    private static final Map<UUID, Listing> LISTED = new ConcurrentHashMap<>();

    public record Listing(UUID profileId, String displayName, Mode mode) {}

    /** Client-visible profile name for a given mode — used by the client skin mixin. */
    public static String nameFor(Mode mode) {
        return mode == Mode.KANGABRINE ? "Kangabrine" : "Kangarude";
    }

    /** Derive a stable, deterministic profile UUID per (entity, mode). */
    public static UUID profileIdFor(UUID entityId, Mode mode) {
        String key = (mode == Mode.KANGABRINE ? "kangabrine:" : "kangarude:") + entityId;
        return UUID.nameUUIDFromBytes(key.getBytes());
    }

    /** Broadcast an ADD entry to every online player. Idempotent. */
    public static void broadcastAdd(MinecraftServer server, UUID profileId, Mode mode) {
        if (server == null) return;
        String name = nameFor(mode);
        LISTED.put(profileId, new Listing(profileId, name, mode));

        ClientboundPlayerInfoUpdatePacket pkt = buildAddPacket(profileId, name);
        server.getPlayerList().getPlayers().forEach(p -> p.connection.send(pkt));
    }

    /** Broadcast a REMOVE entry to every online player. */
    public static void broadcastRemove(MinecraftServer server, UUID profileId) {
        if (server == null) return;
        LISTED.remove(profileId);

        ClientboundPlayerInfoRemovePacket pkt =
            new ClientboundPlayerInfoRemovePacket(List.of(profileId));
        server.getPlayerList().getPlayers().forEach(p -> p.connection.send(pkt));
    }

    /** Re-send every currently-listed entry to a player who just logged in. */
    public static void resendAllTo(ServerPlayer player) {
        if (player == null) return;
        for (Listing listing : LISTED.values()) {
            player.connection.send(buildAddPacket(listing.profileId, listing.displayName));
        }
    }

    private static ClientboundPlayerInfoUpdatePacket buildAddPacket(UUID profileId, String name) {
        GameProfile profile = new GameProfile(profileId, name);
        ClientboundPlayerInfoUpdatePacket.Entry entry = new ClientboundPlayerInfoUpdatePacket.Entry(
            profileId,
            profile,
            true,                 // listed
            0,                    // latency (ms) — 0 hides the ping bars
            GameType.SURVIVAL,
            Component.literal(name),
            false,                // showHat — irrelevant for our use
            0,                    // listOrder
            null);                // chatSession

        // Vanilla constructors require a ServerPlayer collection; pass an empty
        // list so the stream/map produces no entries, then overwrite via accessor.
        ClientboundPlayerInfoUpdatePacket pkt = new ClientboundPlayerInfoUpdatePacket(
            EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER),
            List.of());
        PlayerInfoUpdatePacketAccessor acc = (PlayerInfoUpdatePacketAccessor) pkt;
        acc.ironhold$setActions(EnumSet.of(
            ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY));
        acc.ironhold$setEntries(List.of(entry));
        return pkt;
    }
}
