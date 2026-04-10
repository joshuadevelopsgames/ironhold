package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import kingdom.smp.entity.LunarOrbEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.WindChargeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class LunarOrbRenderer extends EntityRenderer<LunarOrbEntity, EntityRenderState> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/projectiles/wind_charge.png");
    private final WindChargeModel model;

    public LunarOrbRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new WindChargeModel(ctx.bakeLayer(ModelLayers.WIND_CHARGE));
    }

    @Override
    public void submit(EntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        collector.submitModel(
            this.model,
            state,
            pose,
            RenderTypes.breezeWind(TEXTURE, state.ageInTicks * 0.03F % 1.0F, 0.0F),
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            state.outlineColor,
            null
        );
        super.submit(state, pose, collector, camera);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
