package kingdom.smp.mixin;

import kingdom.smp.game.LockProtectionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ContainerOrHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stops vanilla hoppers from inserting into or extracting from locked storage. */
@Mixin(HopperBlockEntity.class)
public abstract class HopperLockMixin {

    @Inject(
        method = "getContainerOrHandlerAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;DDDLnet/minecraft/core/Direction;)Lnet/neoforged/neoforge/transfer/item/ContainerOrHandler;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void ironhold$denyLockedStorage(
            Level level,
            BlockPos pos,
            BlockState state,
            double x,
            double y,
            double z,
            Direction side,
            CallbackInfoReturnable<ContainerOrHandler> cir) {
        if (LockProtectionHandler.isLocked(level.getBlockEntity(pos))) {
            cir.setReturnValue(ContainerOrHandler.EMPTY);
        }
    }
}
