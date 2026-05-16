package kingdom.smp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceSystem;

@Mixin(SurfaceSystem.class)
public interface SurfaceSystemAccessor {
    @Accessor("defaultBlock")
    BlockState ironhold$getDefaultBlock();

    @Accessor("seaLevel")
    int ironhold$getSeaLevel();
}
