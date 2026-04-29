package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.PiglinVillagerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.PiglinRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for the Nether Villager ({@link PiglinVillagerEntity}).
 * <p>
 * Uses the custom {@link PiglinVillagerModel} — vanilla piglin head/ears +
 * standard humanoid body and legs, with villager-style folded "praying" arms
 * across the chest — so the silhouette reads as "trading villager" rather than
 * "swinging-armed piglin warrior".
 */
public class PiglinVillagerRenderer
    extends MobRenderer<PiglinVillagerEntity, PiglinRenderState, PiglinVillagerModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/piglin_villager.png");

    public PiglinVillagerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PiglinVillagerModel(ctx.bakeLayer(PiglinVillagerModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public Identifier getTextureLocation(PiglinRenderState state) {
        return TEXTURE;
    }

    @Override
    public PiglinRenderState createRenderState() {
        return new PiglinRenderState();
    }
}
