package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

public class KnightArmorOverlayLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private final Identifier armorTexture;

    public KnightArmorOverlayLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, Identifier armorTexture) {
        super(parent);
        this.armorTexture = armorTexture;
    }

    @Override
    public void submit(
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int packedLight,
        AvatarRenderState state,
        float yRot,
        float xRot
    ) {
        // Draw a separate cutout pass using only custom armor overlay pixels.
        renderColoredCutoutModel(this.getParentModel(), armorTexture, poseStack, submitNodeCollector, packedLight, state, -1, 1);
    }
}
