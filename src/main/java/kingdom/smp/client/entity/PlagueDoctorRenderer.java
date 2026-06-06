package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.PlagueDoctorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Renders Doctor Corvus using the custom {@link PlagueDoctorModel} (generated
 * from scripts/gen_plague_doctor.py) — a beaked, wide-brim-hatted humanoid in a
 * black robe. Replaces the earlier villager-rig placeholder.
 */
public class PlagueDoctorRenderer
    extends MobRenderer<PlagueDoctorEntity, PlagueDoctorRenderState, PlagueDoctorModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/plague_doctor.png");

    public PlagueDoctorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlagueDoctorModel(ctx.bakeLayer(PlagueDoctorModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public Identifier getTextureLocation(PlagueDoctorRenderState state) {
        return TEXTURE;
    }

    @Override
    public PlagueDoctorRenderState createRenderState() {
        return new PlagueDoctorRenderState();
    }
}
