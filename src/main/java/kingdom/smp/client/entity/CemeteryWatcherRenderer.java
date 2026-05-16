package kingdom.smp.client.entity;

import kingdom.smp.entity.CemeteryWatcherEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for Vesper — uses the vanilla wither-skeleton model + texture so
 * the silhouette is instantly recognizable, but the entity itself is one of
 * our peaceful NPC types (see {@link CemeteryWatcherEntity}).
 */
public class CemeteryWatcherRenderer
    extends HumanoidMobRenderer<CemeteryWatcherEntity, SkeletonRenderState, SkeletonModel<SkeletonRenderState>> {

    private static final Identifier WITHER_SKELETON_TEXTURE =
        Identifier.withDefaultNamespace("textures/entity/skeleton/wither_skeleton.png");

    public CemeteryWatcherRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SkeletonModel<>(ctx.bakeLayer(ModelLayers.WITHER_SKELETON)), 0.5F);
    }

    @Override
    public SkeletonRenderState createRenderState() {
        return new SkeletonRenderState();
    }

    @Override
    public Identifier getTextureLocation(SkeletonRenderState state) {
        return WITHER_SKELETON_TEXTURE;
    }
}
