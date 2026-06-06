package kingdom.smp.honeycomb;

import milkucha.trmt.TRMTBlocks;
import milkucha.trmt.erosion.BlockThresholds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * <b>Waxed terrain</b> — right-clicking an erodable block (grass / dirt / sand / their eroded
 * variants / leaves / configured vegetation) with honeycomb seals it against the TRMT walk-erosion
 * system. The position is recorded in {@link WaxedBlockData} and consulted by
 * {@link milkucha.trmt.erosion.ErosionMapManager#onStep} via {@link #isErosionProtected}.
 *
 * Registered on the game event bus from {@code Ironhold}.
 */
public final class HoneycombWax {
    private HoneycombWax() {}

    /** @return true if {@code pos} has been waxed and must not erode. */
    public static boolean isErosionProtected(MinecraftServer server, BlockPos pos) {
        if (server == null) return false;
        return WaxedBlockData.get(server).isWaxed(pos.asLong());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.HONEYCOMB)) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!isErodable(state)) return; // let vanilla handle copper waxing / no-ops

        // Swallow the interaction on both sides so the client doesn't mispredict; mutate on server.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level.isClientSide()) return;

        MinecraftServer server = level.getServer();
        if (server == null) return;
        if (!WaxedBlockData.get(server).wax(pos.asLong())) {
            // Already waxed — do nothing and don't consume another honeycomb.
            return;
        }

        Player player = event.getEntity();
        level.levelEvent(player, LevelEvent.PARTICLES_AND_SOUND_WAX_ON, pos, 0);
        // Brief orange overlay on the block for nearby clients (fades over a few seconds).
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(
                serverLevel, net.minecraft.world.level.ChunkPos.containing(pos), new kingdom.smp.net.WaxOverlayPayload(pos));
        }
        if (player == null || !player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
    }

    /** Drop the wax flag when a waxed block is broken, so a future block at that spot isn't protected. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.isCanceled()) return;
        MinecraftServer server = event.getLevel().getServer();
        if (server != null) {
            WaxedBlockData.get(server).unwax(event.getPos().asLong());
        }
    }

    private static boolean isErodable(BlockState state) {
        Block block = state.getBlock();
        return state.is(Blocks.GRASS_BLOCK)
            || state.is(Blocks.DIRT)
            || state.is(Blocks.COARSE_DIRT)
            || state.is(Blocks.SAND)
            || state.is(Blocks.RED_SAND)
            || state.is(TRMTBlocks.ERODED_GRASS_BLOCK)
            || state.is(TRMTBlocks.ERODED_DIRT)
            || state.is(TRMTBlocks.ERODED_COARSE_DIRT)
            || state.is(TRMTBlocks.ERODED_SAND)
            || BlockThresholds.isLeaves(block)
            || BlockThresholds.isVegetation(block);
    }
}
