package kingdom.smp.client.entity.dragon;

import kingdom.smp.entity.dragon.KingdomDragonEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Stub renderer for the (stubbed) {@link KingdomDragonEntity}. Renders nothing —
 * inherits {@link EntityRenderer}'s default render path which is a no-op when
 * we don't override anything visual. Re-implement when the dragon is rebuilt.
 */
public class KingdomDragonRenderer extends EntityRenderer<KingdomDragonEntity, EntityRenderState> {

    public KingdomDragonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
