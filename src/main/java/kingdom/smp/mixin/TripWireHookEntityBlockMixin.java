package kingdom.smp.mixin;

import kingdom.smp.block.TripwireRackBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Grafts the {@link EntityBlock} interface onto the vanilla tripwire hook so it
 * can carry a {@link TripwireRackBlockEntity} — the storage + render anchor that
 * lets a hook display a hung tool/item (see {@code TripwireRackHandler} for the
 * interaction and {@code TripwireRackRenderer} for the visual).
 *
 * <p>The block entity is lightweight and non-ticking; it stays empty until a
 * player hangs something, and the hook's normal redstone behaviour is untouched.
 * The hook keeps its default {@code RenderShape.MODEL}, so its own model still
 * renders with the item layered on top.
 */
@Mixin(TripWireHookBlock.class)
public abstract class TripWireHookEntityBlockMixin implements EntityBlock {

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TripwireRackBlockEntity(pos, state);
    }
}
