package kingdom.smp.moon;

import net.minecraft.core.Direction;

/**
 * Duck interface mixed onto {@code LivingEntityRenderState} so a render state can carry the
 * entity's hysteresis gravity direction. The renderer reads this instead of recomputing a raw
 * position-based face, so the model's "up" matches the SAME face the camera and physics use —
 * otherwise the body could flip to a different face than you're standing on near a cube edge.
 */
public interface GravityRenderState {
    Direction ironhold$gravity();
    void ironhold$setGravity(Direction gravity);
}
