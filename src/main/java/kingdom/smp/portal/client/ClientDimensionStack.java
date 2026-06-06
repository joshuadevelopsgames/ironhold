package kingdom.smp.portal.client;

import java.util.HashMap;
import java.util.Map;
import kingdom.smp.Ironhold;
import kingdom.smp.net.ClientboundChunkRedirectPayload;
import kingdom.smp.net.ClientboundOpenPortalViewPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Owns the client's render-only secondary {@link ClientLevel}s — one per destination dimension that a
 * visible portal points into. Each carries its own dedicated {@link LevelRenderer} (the main
 * {@code mc.levelRenderer} is {@code final} and hardcoded into {@code GameRenderer.renderLevel}, so it
 * cannot be reused for a second level without a per-frame {@code setLevel}+rebuild thrash). The secondary
 * level reuses the real {@code mc.getConnection()} so it shares the connection's registries — the
 * {@code ClientLevel} ctor requires that.
 *
 * <p>All access is confined to the client (render) thread; network handlers hop on via
 * {@code ctx.enqueueWork(...)}.
 */
public final class ClientDimensionStack {
    private ClientDimensionStack() {}

    /** A secondary dimension: its level plus the dedicated renderer bound to it. */
    public record Secondary(ClientLevel level, LevelRenderer renderer) {}

    private static final Map<ResourceKey<Level>, Secondary> OPEN = new HashMap<>();

    public static Secondary get(ResourceKey<Level> dim) {
        return OPEN.get(dim);
    }

    public static boolean isOpen(ResourceKey<Level> dim) {
        return OPEN.containsKey(dim);
    }

    /** Build (or no-op if already open) the secondary level described by an open-view payload. */
    public static void open(ClientboundOpenPortalViewPayload p) {
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, p.dimId());
        if (OPEN.containsKey(dim)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener conn = mc.getConnection();
        if (conn == null) {
            return;
        }
        Holder<DimensionType> dimType = conn.registryAccess()
            .lookupOrThrow(Registries.DIMENSION_TYPE)
            .getOrThrow(ResourceKey.create(Registries.DIMENSION_TYPE, p.dimTypeId()));

        ClientLevel.ClientLevelData data = new ClientLevel.ClientLevelData(Difficulty.NORMAL, false, false);
        LevelRenderer renderer = new LevelRenderer(
            mc,
            mc.getEntityRenderDispatcher(),
            mc.getBlockEntityRenderDispatcher(),
            mc.renderBuffers(),
            mc.gameRenderer.getGameRenderState(),
            mc.gameRenderer.getFeatureRenderDispatcher());

        ClientLevel level = new ClientLevel(
            conn, data, dim, dimType,
            p.chunkRadius(), p.chunkRadius(),
            renderer, false, p.biomeZoomSeed(), p.seaLevel());
        renderer.setLevel(level); // builds the renderer's own section dispatcher / view area
        renderer.onResourceManagerReload(mc.getResourceManager()); // init sky/cloud/outline sub-renderers
        // Seed the secondary chunk cache's view so streamed chunks pass its in-range check.
        level.getChunkSource().updateViewRadius(p.chunkRadius());
        level.getChunkSource().updateViewCenter(p.centerChunkX(), p.centerChunkZ());

        OPEN.put(dim, new Secondary(level, renderer));
        Ironhold.LOGGER.info("[portal] opened secondary view of {} (radius {}, centre {},{})",
            p.dimId(), p.chunkRadius(), p.centerChunkX(), p.centerChunkZ());
    }

    /** Replay a redirected vanilla chunk packet against the matching secondary level. */
    public static void applyChunk(ClientboundChunkRedirectPayload p) {
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, p.dimId());
        Secondary sec = OPEN.get(dim);
        if (sec == null) {
            return;
        }
        ClientboundLevelChunkWithLightPacket pkt = p.chunk();
        ClientboundLevelChunkPacketData cdata = pkt.getChunkData();
        int x = pkt.getX();
        int z = pkt.getZ();
        // Same call ClientPacketListener.updateLevelChunk makes; light is applied in a later milestone.
        sec.level().getChunkSource().replaceWithPacketData(
            x, z, cdata.getReadBuffer(), cdata.getHeightmaps(), cdata.getBlockEntitiesTagsConsumer(x, z));

        // M1 verification handle: surface the running secondary chunk count in the log.
        int loaded = sec.level().getChunkSource().getLoadedChunksCount();
        if (loaded == 1 || loaded % 5 == 0) {
            Ironhold.LOGGER.info("[portal] {} secondary chunk(s) loaded for {}", loaded, p.dimId());
        }
    }

    /** Tear down a secondary view, freeing its renderer's GPU buffers. */
    public static void close(Identifier dimId) {
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, dimId);
        Secondary sec = OPEN.remove(dim);
        if (sec != null) {
            sec.renderer().close();
            Ironhold.LOGGER.info("[portal] closed secondary view of {}", dimId);
        }
    }

    /** Drop everything (e.g. on disconnect). */
    public static void clear() {
        for (Secondary sec : OPEN.values()) {
            sec.renderer().close();
        }
        OPEN.clear();
    }
}
