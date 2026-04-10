package kingdom.smp.block;

import kingdom.smp.Ironhold;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class GuillotineBlockEntity extends BlockEntity implements GeoBlockEntity {

    @Override
    public AABB getRenderBoundingBox() {
        // The guillotine model extends ~4.5 blocks tall and ~2 blocks wide from the base block.
        // Expand the render bounding box so MC doesn't cull it when the base goes off-screen.
        BlockPos pos = getBlockPos();
        return new AABB(pos.getX() - 1, pos.getY(), pos.getZ() - 1,
                        pos.getX() + 2, pos.getY() + 5, pos.getZ() + 2);
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GuillotineBlockEntity(BlockPos pos, BlockState state) {
        super(Ironhold.GUILLOTINE_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
