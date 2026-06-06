package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import kingdom.smp.Ironhold;
import kingdom.smp.item.DiamondScepterItem;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renders the light-blue charge force-field as a sphere centred on the player,
 * large enough to enclose the whole body, while they charge the Diamond Scepter.
 * (Player-body layer — renders in third person / on other players.)
 */
public class DiamondScepterFieldLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    /** Matches DiamondScepterItem.FULL_CHARGE_TICKS. */
    private static final float FULL_CHARGE = 40f;

    private static final Identifier FIELD_TEX = ForcefieldDome.FIELD_TEXTURE;

    public DiamondScepterFieldLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                       AvatarRenderState state, float yRot, float xRot) {
        if (!state.isUsingItem || state.isInvisible) return;
        boolean holdingScepter =
            state.rightHandItemStack.getItem() instanceof DiamondScepterItem
            || state.leftHandItemStack.getItem() instanceof DiamondScepterItem;
        if (!holdingScepter) return;

        float frac = Math.min(state.ticksUsingItem / FULL_CHARGE, 1f);
        // Big enough to enclose the whole player even at low charge, growing with charge.
        float radius = 1.3f + 0.6f * frac;
        float pulse = 0.85f + 0.15f * (float) Math.sin(state.ageInTicks * 0.3f);
        float alpha = (0.30f + 0.16f * frac) * pulse;
        float phase = state.ageInTicks * 0.012f;

        // Model space here is y-down with the origin near the head top; translate
        // down (+y) to seat the sphere at the player's midsection.
        pose.pushPose();
        pose.translate(0.0, 0.55, 0.0);
        ForcefieldDome.drawGlowing(pose, collector, FIELD_TEX, radius, alpha, phase);
        pose.popPose();
    }
}
