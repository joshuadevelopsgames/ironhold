package kingdom.smp.client.entity;

import kingdom.smp.entity.BramBardEntity;
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
 * Renderer for Bram the Bard — vanilla villager rig with the FLETCHER
 * profession overlay (feather-trimmed apron, reads as someone who works
 * with strings + craft). DESERT villager type for a warmer, more theatrical
 * palette than the rest of the village.
 */
public class BramBardRenderer
    extends AgeableMobRenderer<BramBardEntity, VillagerRenderState, VillagerModel> {

    private static final Identifier VILLAGER_BASE_TEXTURE =
        Identifier.withDefaultNamespace("textures/entity/villager/villager.png");

    public BramBardRenderer(EntityRendererProvider.Context ctx) {
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
    public void extractRenderState(BramBardEntity entity, VillagerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        var registryAccess = entity.registryAccess();
        var typeRegistry = registryAccess.lookupOrThrow(Registries.VILLAGER_TYPE);
        var profRegistry = registryAccess.lookupOrThrow(Registries.VILLAGER_PROFESSION);
        state.villagerData = new VillagerData(
            typeRegistry.getOrThrow(VillagerType.DESERT),
            profRegistry.getOrThrow(VillagerProfession.FLETCHER),
            1);
    }
}
