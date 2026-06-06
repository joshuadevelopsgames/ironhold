package kingdom.smp.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class ShroomlingRenderState extends LivingEntityRenderState {
    public float hopBob;
    public boolean orange;
    /** True after the cap was pickpocketed — the model hides the cap tier. */
    public boolean capless;
}
