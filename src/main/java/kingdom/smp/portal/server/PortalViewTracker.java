package kingdom.smp.portal.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kingdom.smp.net.ClientboundChunkRedirectPayload;
import kingdom.smp.net.ClientboundClosePortalViewPayload;
import kingdom.smp.net.ClientboundOpenPortalViewPayload;
import kingdom.smp.net.ServerboundRequestPortalViewsPayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server side of the immersive-portal streaming bridge. For each player, mirrors a small area of every
 * destination dimension a visible portal points into (as requested by the client's
 * {@code PortalDetector}) down to that player's client, where it becomes a render-only secondary
 * {@code ClientLevel}. Chunks are sent as re-addressed vanilla packets ({@link ClientboundChunkRedirectPayload}).
 *
 * <p>M1 streams a static snapshot once per chunk; live block/entity updates come in M3. Force-generated
 * destination chunks are sent on the server thread, so the radius is kept small.
 */
public final class PortalViewTracker {
    private PortalViewTracker() {}

    /** Chunk radius streamed around each view centre (5×5 = 25 chunks). */
    private static final int STREAM_RADIUS = 2;
    /** Max simultaneous destination views per player. */
    private static final int MAX_VIEWS = 4;

    private static final Map<UUID, PlayerViews> STATE = new HashMap<>();

    private static final class PlayerViews {
        final Set<ResourceKey<Level>> openDims = new HashSet<>();
        final Map<ResourceKey<Level>, Set<Long>> sentChunks = new HashMap<>();
    }

    public static void handleRequest(ServerPlayer player, List<ServerboundRequestPortalViewsPayload.Entry> entries) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        PlayerViews st = STATE.computeIfAbsent(player.getUUID(), u -> new PlayerViews());

        Set<ResourceKey<Level>> requested = new HashSet<>();
        int count = 0;
        for (ServerboundRequestPortalViewsPayload.Entry e : entries) {
            if (count++ >= MAX_VIEWS) {
                break;
            }
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, e.dimId());
            ServerLevel dest = server.getLevel(dim);
            if (dest == null) {
                continue;
            }
            requested.add(dim);

            if (st.openDims.add(dim)) {
                Identifier dimTypeId = dest.dimensionTypeRegistration().unwrapKey().orElseThrow().identifier();
                long biomeZoomSeed = BiomeManager.obfuscateSeed(dest.getSeed());
                PacketDistributor.sendToPlayer(player, new ClientboundOpenPortalViewPayload(
                    e.dimId(), dimTypeId, STREAM_RADIUS + 1, biomeZoomSeed, dest.getSeaLevel(),
                    e.centerChunkX(), e.centerChunkZ()));
                st.sentChunks.put(dim, new HashSet<>());
            }

            Set<Long> sent = st.sentChunks.computeIfAbsent(dim, k -> new HashSet<>());
            for (int dx = -STREAM_RADIUS; dx <= STREAM_RADIUS; dx++) {
                for (int dz = -STREAM_RADIUS; dz <= STREAM_RADIUS; dz++) {
                    int cx = e.centerChunkX() + dx;
                    int cz = e.centerChunkZ() + dz;
                    long chunkKey = ((long) cx & 0xFFFFFFFFL) | (((long) cz & 0xFFFFFFFFL) << 32);
                    if (!sent.add(chunkKey)) {
                        continue;
                    }
                    LevelChunk chunk = dest.getChunk(cx, cz);
                    ClientboundLevelChunkWithLightPacket pkt =
                        new ClientboundLevelChunkWithLightPacket(chunk, dest.getLightEngine(), null, null);
                    PacketDistributor.sendToPlayer(player, new ClientboundChunkRedirectPayload(e.dimId(), pkt));
                }
            }
        }

        // Close any views the client no longer wants.
        List<ResourceKey<Level>> stale = new ArrayList<>();
        for (ResourceKey<Level> dim : st.openDims) {
            if (!requested.contains(dim)) {
                stale.add(dim);
            }
        }
        for (ResourceKey<Level> dim : stale) {
            st.openDims.remove(dim);
            st.sentChunks.remove(dim);
            PacketDistributor.sendToPlayer(player, new ClientboundClosePortalViewPayload(dim.identifier()));
        }
    }

    /** Drop a player's view state on logout so a rejoin re-opens cleanly. */
    public static void forget(ServerPlayer player) {
        STATE.remove(player.getUUID());
    }
}
