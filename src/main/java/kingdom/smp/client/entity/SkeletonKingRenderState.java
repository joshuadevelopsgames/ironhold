package kingdom.smp.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Carries skeleton-king-specific flags for {@link SkeletonKingModel#setupAnim}. */
public class SkeletonKingRenderState extends LivingEntityRenderState {
    /** True while the king has a combat target — raises the sword arm. */
    public boolean isAggressive;
}
