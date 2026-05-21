package milkucha.trmt.client;

import milkucha.trmt.TRMT;
import milkucha.trmt.TRMTBlocks;
import milkucha.trmt.client.debug.ErosionDebugHud;
import milkucha.trmt.client.network.ClientErosionCache;
import milkucha.trmt.network.VersionCheckPayload;
import milkucha.trmt.network.VersionResponsePayload;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.List;

/**
 * Client-side mod-bus listeners for the TRMT NeoForge port. Equivalent of
 * upstream's {@code TRMTClient.onInitializeClient()}.
 *
 * <p>Server-side block-placement rejection (the upstream {@code UseBlockCallback}
 * for sunken eroded sand) is already handled on the game bus by
 * {@link milkucha.trmt.handler.ErosionEvents#onRightClickBlock}, and NeoForge
 * fires that event on both client and server — so the client-side prediction
 * handler from upstream isn't needed here.
 *
 * <p>Payload receive handlers (server → client packets) are registered in
 * {@link milkucha.trmt.network.TRMTPayloads} but their bodies live here:
 * see {@link #onSyncChunk}, {@link #onUpdateStage}, {@link #onVersionCheck}.
 */
@EventBusSubscriber(modid = TRMT.MOD_ID, value = Dist.CLIENT)
public final class TRMTClient {
    private TRMTClient() {}

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        TRMTClientConfig.load();
    }

    /**
     * Grass-colour tint for eroded grass — biome grass colour in-world, vanilla green in inventory.
     *
     * <p>Uses {@code TRMTBlocks.H_GRASS.get()} directly rather than the resolved
     * {@code ERODED_GRASS_BLOCK} field because this event can fire before
     * {@code FMLCommonSetupEvent}'s enqueueWork runs. With the field still null,
     * the registration silently failed and the grass-block top texture rendered
     * untinted (vanilla grass texture is gray; the green comes from the tint).
     */
    @SubscribeEvent
    static void onRegisterBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(
                List.of(new BlockTintSource() {
                    @Override
                    public int color(BlockState state) {
                        return 0x79C05A;
                    }

                    @Override
                    public int colorInWorld(BlockState state, BlockAndTintGetter world, BlockPos pos) {
                        return BiomeColors.getAverageGrassColor(world, pos);
                    }
                }),
                TRMTBlocks.H_GRASS.get());
    }

    /** Erosion debug HUD (compass-cross overlay). Disabled by default; toggle via {@code trmt-client.json}. */
    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // Equivalent of upstream Fabric HudElementRegistry.addLast — draws on top of everything.
        event.registerAboveAll(ErosionDebugHud.LAYER_ID, ErosionDebugHud::render);
    }

    /** Drop the client-side cache on disconnect so stale data can't leak into the next session. */
    @SubscribeEvent
    static void onClientPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientErosionCache.getInstance().clear();
    }

    // ── payload handlers (referenced from TRMTPayloads) ────────────────────

    public static void onSyncChunk(milkucha.trmt.network.SyncChunkPayload payload,
                                   net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        java.util.Map<BlockPos, ClientErosionCache.Entry> chunkEntries =
                new java.util.HashMap<>(payload.entries().size());
        for (var e : payload.entries()) {
            chunkEntries.put(e.pos(),
                    new ClientErosionCache.Entry(e.stage(), e.walkedOnCount(), e.threshold(), e.lastTouchedGameTime()));
        }
        net.minecraft.world.level.ChunkPos chunkPos =
                new net.minecraft.world.level.ChunkPos(payload.chunkX(), payload.chunkZ());
        ctx.enqueueWork(() -> ClientErosionCache.getInstance().setChunk(chunkPos, chunkEntries));
    }

    public static void onUpdateStage(milkucha.trmt.network.UpdateStagePayload payload,
                                     net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientErosionCache.getInstance().setEntry(
                payload.pos(),
                payload.stage(),
                payload.walkedOnCount(),
                payload.threshold(),
                payload.lastTouchedGameTime()));
    }

    /** Configuration-phase: server tells us its version, we reply with our own. */
    public static void onVersionCheck(VersionCheckPayload payload,
                                      net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        String myVersion = ModList.get().getModContainerById(TRMT.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("0.0.0");
        ctx.reply(new VersionResponsePayload(myVersion));
    }
}
