package kingdom.smp.mixin;

import kingdom.smp.ModItems;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes an invisible item frame drop our {@code invisible_item_frame} item when broken (and
 * supply it as the pick-block result), instead of a plain vanilla frame. The only frames that
 * are ever invisible are the ones we place, so {@code isInvisible()} is a safe marker. The
 * glow-frame subclass overrides {@code getFrameItemStack}, so it is unaffected by this injection.
 */
@Mixin(ItemFrame.class)
public abstract class ItemFrameInvisibleDropMixin {

    @Inject(method = "getFrameItemStack()Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), cancellable = true)
    private void ironhold$dropInvisibleVariant(CallbackInfoReturnable<ItemStack> cir) {
        ItemFrame self = (ItemFrame) (Object) this;
        if (self.isInvisible()) {
            cir.setReturnValue(new ItemStack(ModItems.INVISIBLE_ITEM_FRAME.get()));
        }
    }
}
