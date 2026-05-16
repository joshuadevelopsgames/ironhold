package kingdom.smp.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Carries shulker-herder-specific flags for {@link ShulkerHerderModel#setupAnim}. */
public class ShulkerHerderRenderState extends LivingEntityRenderState {
    /** True while the herder is in combat (target acquired). */
    public boolean isAggressive;
}
