package kingdom.smp.mixin;

import kingdom.smp.moon.GravityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Carries the entity's hysteresis gravity direction on its render state (set during
 * extractRenderState) so the model renderer orients the body to the same face the camera and
 * physics use, instead of a raw per-frame position lookup that disagrees near cube edges.
 */
@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateGravityMixin implements GravityRenderState {

    @Unique private Direction ironhold$gravity = Direction.DOWN;

    @Override
    public Direction ironhold$gravity() {
        return ironhold$gravity;
    }

    @Override
    public void ironhold$setGravity(Direction gravity) {
        ironhold$gravity = gravity;
    }
}
