package kingdom.smp.client.entity;

import kingdom.smp.entity.PippaUrchinEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;

/**
 * Renderer for Pippa the Urchin — vanilla villager rig, NITWIT profession
 * (no overlay, plain tunic, fits a street kid without a trade). She uses
 * the baby villager model — combined with her SCALE=0.7 attribute and
 * isBaby() override, she reads unambiguously as a child in the alleys.
 *
 * <p>SWAMP villager type for a duller, dustier palette than the well-kept
 * villagers — fits a kid who sleeps under awnings.
 */
public class PippaUrchinRenderer
    extends AgeableMobRenderer<PippaUrchinEntity, VillagerRenderState, VillagerModel> {

    private static final Identifier VILLAGER_BASE_TEXTURE =
        Identifier.withDefaultNamespace("textures/entity/villager/villager.png");

    public PippaUrchinRenderer(EntityRendererProvider.Context ctx) {
        super(ctx,
            new VillagerModel(ctx.bakeLayer(ModelLayers.VILLAGER)),
            new VillagerModel(ctx.bakeLayer(ModelLayers.VILLAGER_BABY)),
            0.4F);
        this.addLayer(new VillagerProfessionLayer<>(
            this, ctx.getResourceManager(), "villager",
            new VillagerModel(ctx.bakeLayer(ModelLayers.VILLAGER)),
            new VillagerModel(ctx.bakeLayer(ModelLayers.VILLAGER_BABY))));
    }

    @Override
    public Identifier getTextureLocation(VillagerRenderState state) {
        return VILLAGER_BASE_TEXTURE;
    }

    @Override
    public VillagerRenderState createRenderState() {
        return new VillagerRenderState();
    }

    @Override
    public void extractRenderState(PippaUrchinEntity entity, VillagerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        var registryAccess = entity.registryAccess();
        var typeRegistry = registryAccess.lookupOrThrow(Registries.VILLAGER_TYPE);
        var profRegistry = registryAccess.lookupOrThrow(Registries.VILLAGER_PROFESSION);
        state.villagerData = new VillagerData(
            typeRegistry.getOrThrow(VillagerType.SWAMP),
            profRegistry.getOrThrow(VillagerProfession.NITWIT),
            1);
    }
}
