package kingdom.smp.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import kingdom.smp.dyewater.DyedWater;
import kingdom.smp.dyewater.DyedWaterlog;

/**
 * Scooping a coloured water source with an empty bucket returns the matching coloured bucket instead of a
 * plain water bucket. Mirrors vanilla {@link LiquidBlock#pickupBlock} (full sources only); the colour is
 * cleared automatically when the block becomes air (see {@code LevelChunkWaterlogColorMixin}).
 */
@Mixin(LiquidBlock.class)
public abstract class LiquidBlockDyedPickupMixin {

    @Inject(method = "pickupBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), cancellable = true)
    private void ironhold$dyedPickup(LivingEntity user, LevelAccessor level, BlockPos pos, BlockState state,
                                     CallbackInfoReturnable<ItemStack> cir) {
        if (state.getValue(LiquidBlock.LEVEL) != 0) return; // only full sources, like vanilla
        if (!(level instanceof LevelReader reader)) return;
        DyeColor color = DyedWaterlog.get(reader, pos);
        if (color == null) return; // uncoloured (or lava) — let vanilla return the normal bucket
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
        cir.setReturnValue(new ItemStack(DyedWater.BUCKET.get(color).get()));
    }
}
