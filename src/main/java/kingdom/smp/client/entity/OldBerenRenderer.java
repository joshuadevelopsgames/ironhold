package kingdom.smp.client.entity;

import kingdom.smp.entity.OldBerenEntity;
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
 * Renderer for Old Beren — vanilla villager rig with the NITWIT profession
 * (no overlay, the plain green-tunic villager). Reads as "village drunk
 * without a trade", which fits.
 */
public class OldBerenRenderer
    extends AgeableMobRenderer<OldBerenEntity, VillagerRenderState, VillagerModel> {

    private static final Identifier VILLAGER_BASE_TEXTURE =
        Identifier.withDefaultNamespace("textures/entity/villager/villager.png");

    public OldBerenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx,
            new VillagerModel(ctx.bakeLayer(ModelLayers.VILLAGER)),
            new VillagerModel(ctx.bakeLayer(ModelLayers.VILLAGER)),
            0.5F);
        this.addLayer(new VillagerProfessionLayer<>(
            this, ctx.getResourceManager(), "villager",
            new VillagerModel(ctx.bakeLayer(ModelLayers.VILLAGER)),
            new VillagerModel(ctx.bakeLayer(ModelLayers.VILLAGER))));
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
    public void extractRenderState(OldBerenEntity entity, VillagerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        var registryAccess = entity.registryAccess();
        var typeRegistry = registryAccess.lookupOrThrow(Registries.VILLAGER_TYPE);
        var profRegistry = registryAccess.lookupOrThrow(Registries.VILLAGER_PROFESSION);
        state.villagerData = new VillagerData(
            typeRegistry.getOrThrow(VillagerType.PLAINS),
            profRegistry.getOrThrow(VillagerProfession.NITWIT),
            1);
    }
}
