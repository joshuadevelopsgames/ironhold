package milkucha.trmt.handler;

import milkucha.trmt.TRMT;
import milkucha.trmt.TRMTBlocks;
import milkucha.trmt.block.ErodedSandBlock;
import milkucha.trmt.erosion.ErosionMapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Game-bus event handlers for the TRMT NeoForge port.
 *
 * <p>Maps the upstream Fabric callbacks 1:1 to NeoForge events:
 * <ul>
 *   <li>{@code ServerLifecycleEvents.SERVER_STARTED} → {@link ServerStartedEvent}</li>
 *   <li>{@code ServerLifecycleEvents.SERVER_STOPPED} → {@link ServerStoppedEvent}</li>
 *   <li>{@code ServerPlayConnectionEvents.JOIN} → {@link PlayerEvent.PlayerLoggedInEvent}</li>
 *   <li>{@code PlayerBlockBreakEvents.AFTER} → {@link BreakBlockEvent} @ LOWEST priority,
 *       checking {@code !isCanceled()} so other mods can still veto the break</li>
 *   <li>{@code UseBlockCallback} → {@link PlayerInteractEvent.RightClickBlock}</li>
 * </ul>
 */
@EventBusSubscriber(modid = TRMT.MOD_ID)
public final class ErosionEvents {
    private ErosionEvents() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ErosionMapManager manager = ErosionMapManager.getInstance();
        manager.loadState(event.getServer());
        manager.migrateGrassEntries(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ErosionMapManager.reset();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ErosionMapManager.getInstance().sendFullSyncToPlayer(player);
        }
    }

    /**
     * Run at LOWEST so any mod that wants to cancel the break has already done so.
     * If {@code isCanceled()} is true at this point the block never breaks and the
     * erosion entry must be kept.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.isCanceled()) return;
        ErosionMapManager.getInstance().removeEntry(event.getPos());
    }

    /**
     * Block placement above sunken eroded sand (stages 1–4) is rejected because the
     * resulting AO darkening looks wrong. Upstream returns {@code InteractionResult.FAIL}
     * from a {@code UseBlockCallback}; on NeoForge we cancel the event and set the
     * cancellation result.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level world = event.getLevel();
        BlockHitResult hit = event.getHitVec();
        BlockPos placePos = hit.getBlockPos().relative(hit.getDirection());
        BlockState below = world.getBlockState(placePos.below());
        if (below.is(TRMTBlocks.ERODED_SAND)
                && below.getValue(ErodedSandBlock.STAGE) > 0
                && player.getItemInHand(event.getHand()).getItem() instanceof BlockItem) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }
}
