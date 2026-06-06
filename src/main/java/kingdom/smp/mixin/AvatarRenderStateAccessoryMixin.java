package kingdom.smp.mixin;

import kingdom.smp.client.VanityAccessoryRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries the worn additive vanity accessories (halo / wings) into the render layers. */
@Mixin(AvatarRenderState.class)
public class AvatarRenderStateAccessoryMixin implements VanityAccessoryRenderState {

    @Unique
    private ItemStack ironhold$headAccessory = ItemStack.EMPTY;
    @Unique
    private ItemStack ironhold$chestAccessory = ItemStack.EMPTY;
    @Unique
    private ItemStack ironhold$legsAccessory = ItemStack.EMPTY;
    @Unique
    private ItemStack ironhold$feetAccessory = ItemStack.EMPTY;

    @Override
    public void ironhold$setHeadAccessory(ItemStack stack) {
        this.ironhold$headAccessory = stack;
    }

    @Override
    public ItemStack ironhold$headAccessory() {
        return this.ironhold$headAccessory == null ? ItemStack.EMPTY : this.ironhold$headAccessory;
    }

    @Override
    public void ironhold$setChestAccessory(ItemStack stack) {
        this.ironhold$chestAccessory = stack;
    }

    @Override
    public ItemStack ironhold$chestAccessory() {
        return this.ironhold$chestAccessory == null ? ItemStack.EMPTY : this.ironhold$chestAccessory;
    }

    @Override
    public void ironhold$setLegsAccessory(ItemStack stack) {
        this.ironhold$legsAccessory = stack;
    }

    @Override
    public ItemStack ironhold$legsAccessory() {
        return this.ironhold$legsAccessory == null ? ItemStack.EMPTY : this.ironhold$legsAccessory;
    }

    @Override
    public void ironhold$setFeetAccessory(ItemStack stack) {
        this.ironhold$feetAccessory = stack;
    }

    @Override
    public ItemStack ironhold$feetAccessory() {
        return this.ironhold$feetAccessory == null ? ItemStack.EMPTY : this.ironhold$feetAccessory;
    }
}
