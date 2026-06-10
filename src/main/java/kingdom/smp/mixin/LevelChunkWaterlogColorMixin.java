package kingdom.smp.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import kingdom.smp.dyewater.DyedWaterlog;

/**
 * Forgets a stored water colour (see {@link DyedWaterlog}) when a cell stops holding water — every block
 * change funnels through {@code LevelChunk.setBlockState}, so this one hook covers all removal paths
 * (drained flow, broken waterlogged block, replaced by stone/lava, frozen to ice, …). Colour is preserved
 * while the cell still holds water (a flowing-level change or a freshly waterlogged block both keep it).
 *
 * <p>Fast path: chunks with no stored colours return immediately, so the hot block-update path is barely
 * affected.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkWaterlogColorMixin {

    @Shadow public abstract Level getLevel();

    @Inject(method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"))
    private void ironhold$clearWaterColor(BlockPos pos, BlockState newState, int flags,
                                          CallbackInfoReturnable<BlockState> cir) {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) return; // server owns the data; client is synced
        // A waterlogged block's getFluidState() is WATER too, so this keeps colour through waterlogging.
        if (newState.getFluidState().getType() != Fluids.WATER) {
            DyedWaterlog.clearOnChunk((LevelChunk) (Object) this, pos);
        }
    }
}
