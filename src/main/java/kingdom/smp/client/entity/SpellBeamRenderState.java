package kingdom.smp.client.entity;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class SpellBeamRenderState extends EntityRenderState {
    /** Beam origin (caster/staff tip) relative to the entity's world position (beam end). */
    public float originDX, originDY, originDZ;
    public float beamR, beamG, beamB;
    /** Fade: 1.0 = fully visible, 0.0 = gone. */
    public float alpha;
}
