package kingdom.smp.client.entity;

import kingdom.smp.entity.WardenHalricEntity;
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
import net.minecraft.world.phys.Vec3;

/**
 * Renderer for Warden Halric — reuses the vanilla villager rig (CLERIC robe)
 * so the silhouette reads as an NPC. Scale is driven by Attributes.SCALE on the
 * entity, which the render state picks up automatically. Texture / beard pass
 * will replace this later.
 */
public class WardenHalricRenderer
    extends AgeableMobRenderer<WardenHalricEntity, VillagerRenderState, VillagerModel> {

    private static final Identifier VILLAGER_BASE_TEXTURE =
        Identifier.withDefaultNamespace("textures/entity/villager/villager.png");

    public WardenHalricRenderer(EntityRendererProvider.Context ctx) {
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
    public void extractRenderState(WardenHalricEntity entity, VillagerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        var registryAccess = entity.registryAccess();
        var typeRegistry = registryAccess.lookupOrThrow(Registries.VILLAGER_TYPE);
        var profRegistry = registryAccess.lookupOrThrow(Registries.VILLAGER_PROFESSION);
        state.villagerData = new VillagerData(
            typeRegistry.getOrThrow(VillagerType.PLAINS),
            profRegistry.getOrThrow(VillagerProfession.CLERIC),
            1);

        // Scale=1.3 makes the model taller than the base BB attachment; nudge tag up
        if (state.nameTagAttachment != null) {
            state.nameTagAttachment = state.nameTagAttachment.add(0.0, 0.45, 0.0);
        }
    }
}
