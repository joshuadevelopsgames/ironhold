package kingdom.smp.client.entity;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * Render state for stone statues. Deliberately a plain {@link HumanoidRenderState}
 * and NOT an {@code AvatarRenderState}: the entity render dispatcher routes any
 * {@code AvatarRenderState} to the vanilla player renderer at submission time
 * (EntityRenderDispatcher.getRenderer), which would bypass our custom
 * {@link StoneStatueRenderer#submit} (and thus the pedestal layer + lift) and
 * wrongly attach the player's cosmetic layers. Using a non-Avatar state keeps
 * submission on our own renderer.
 */
public class StatueRenderState extends HumanoidRenderState {
}
