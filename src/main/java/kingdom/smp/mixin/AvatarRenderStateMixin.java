package kingdom.smp.mixin;

import kingdom.smp.client.emote.PointableRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries the per-player "point" blend amount from extraction into setupAnim. */
@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements PointableRenderState {

    @Unique
    private float ironhold$point;

    @Override
    public void ironhold$setPoint(float amount) {
        this.ironhold$point = amount;
    }

    @Override
    public float ironhold$point() {
        return this.ironhold$point;
    }
}
