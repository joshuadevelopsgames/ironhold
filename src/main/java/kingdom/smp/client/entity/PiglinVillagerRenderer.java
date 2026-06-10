package kingdom.smp.client.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.PiglinVillagerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * GeckoLib renderer for {@link PiglinVillagerEntity}, using the converted
 * {@code piglin_villager.geo.json} model + animation file. The bbmodel's
 * bone hierarchy (head with ear+nose subparts, body with the 43°-tilted
 * "arms" praying-villager bone, separate legs) is preserved exactly.
 */
public class PiglinVillagerRenderer extends GeoEntityRenderer<PiglinVillagerEntity, LivingEntityRenderState> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/piglin_villager.png");

    public PiglinVillagerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        this.shadowRadius = 0.5F;
    }

    /** Geo model wrapper that points the texture path at our PNG. */
    private static class Model extends DefaultedEntityGeoModel<PiglinVillagerEntity> {
        Model() {
            super(Identifier.fromNamespaceAndPath(Ironhold.MODID, "piglin_villager"));
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return TEXTURE;
        }
    }
}
