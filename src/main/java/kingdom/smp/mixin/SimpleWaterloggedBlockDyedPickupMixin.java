package kingdom.smp.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import kingdom.smp.dyewater.DyedWater;
import kingdom.smp.dyewater.DyedWaterlog;

/**
 * Closes the colour round-trip: scooping water out of a block that was waterlogged from dyed water returns
 * the matching coloured bucket (instead of a plain water bucket) and un-waterlogs the block. Clearing the
 * stored colour is handled for free by {@code LevelChunkWaterlogColorMixin} when the block updates to
 * {@code WATERLOGGED=false}.
 */
@Mixin(SimpleWaterloggedBlock.class)
public interface SimpleWaterloggedBlockDyedPickupMixin {

    @Inject(method = "pickupBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), cancellable = true)
    private void ironhold$dyedPickup(LivingEntity user, LevelAccessor level, BlockPos pos, BlockState state,
                                     CallbackInfoReturnable<ItemStack> cir) {
        if (!state.getValue(BlockStateProperties.WATERLOGGED)) return;
        if (!(level instanceof LevelReader reader)) return;
        DyeColor color = DyedWaterlog.get(reader, pos);
        if (color == null) return; // not a dyed-waterlogged block — let vanilla return the water bucket

        level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, false), 3);
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
        cir.setReturnValue(new ItemStack(DyedWater.BUCKET.get(color).get()));
    }
}
