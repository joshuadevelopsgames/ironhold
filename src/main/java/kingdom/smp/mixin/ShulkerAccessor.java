package kingdom.smp.mixin;

import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Shulker.class)
public interface ShulkerAccessor {
    @Invoker("setRawPeekAmount")
    void ironhold$setRawPeekAmount(int peekAmount);

    @Accessor("currentPeekAmount")
    void ironhold$setCurrentPeekAmount(float peekAmount);

    @Accessor("currentPeekAmountO")
    void ironhold$setCurrentPeekAmountO(float peekAmount);
}
