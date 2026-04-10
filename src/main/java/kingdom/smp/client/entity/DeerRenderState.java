package kingdom.smp.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Carries deer-specific render flags for {@link PinkDeerModel#setupAnim}. */
public class DeerRenderState extends LivingEntityRenderState {
    /** True while the deer is performing the grazing animation. */
    public boolean isGrazing;
}
