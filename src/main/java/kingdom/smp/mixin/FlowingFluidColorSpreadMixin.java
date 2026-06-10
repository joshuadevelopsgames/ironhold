package kingdom.smp.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import kingdom.smp.dyewater.DyedWaterlog;

/**
 * Carries a stamped water colour (see {@link DyedWaterlog}) along the flow. When water spreads into a new
 * cell, it inherits the colour of the cell it spread from — so a poured pool of coloured water keeps its
 * colour as it fills outward. Uncoloured water (oceans, rivers) and lava hit the fast path (the source cell
 * has no stored colour) and do nothing.
 */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidColorSpreadMixin {

    @Inject(method = "spreadTo(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/material/FluidState;)V",
            at = @At("TAIL"))
    private void ironhold$propagateColor(LevelAccessor level, BlockPos pos, BlockState blockState,
                                         Direction direction, FluidState fluidState, CallbackInfo ci) {
        if (!(level instanceof LevelReader reader) || reader.isClientSide()) return;
        if (!fluidState.is(FluidTags.WATER)) return; // water only — skip lava/other fluids
        DyeColor color = DyedWaterlog.get(reader, pos.relative(direction.getOpposite()));
        if (color != null && level.getChunk(pos) instanceof LevelChunk chunk) {
            DyedWaterlog.setOnChunk(chunk, pos, color);
        }
    }
}
