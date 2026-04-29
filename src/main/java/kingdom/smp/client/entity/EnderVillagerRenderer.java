package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.EnderVillagerEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;

/**
 * Renderer for {@link EnderVillagerEntity}. Uses the vanilla {@link VillagerModel}
 * geometry so the texture's UVs line up perfectly, but skips the
 * {@code VillagerProfessionLayer} (which would draw biome+profession overlays on
 * top of our custom texture) and adds an emissive {@link EyesLayer} that lights
 * up the glowing eyes and chest gem from {@code ender_villager_glow.png}.
 */
public class EnderVillagerRenderer
    extends AgeableMobRenderer<EnderVillagerEntity, VillagerRenderState, VillagerModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/ender_villager.png");
    private static final Identifier GLOW_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/ender_villager_glow.png");
    private static final RenderType EYES = RenderTypes.eyes(GLOW_TEXTURE);

    public EnderVillagerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx,
            new VillagerModel(ctx.bakeLayer(ModelLayers.VILLAGER)),
            new VillagerModel(ctx.bakeLayer(ModelLayers.VILLAGER)),
            0.5F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    public Identifier getTextureLocation(VillagerRenderState state) {
        return TEXTURE;
    }

    @Override
    public VillagerRenderState createRenderState() {
        return new VillagerRenderState();
    }

    @Override
    public void extractRenderState(EnderVillagerEntity entity, VillagerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        // VillagerModel can read from villagerData during animation setup; initialize
        // it to a NONE-profession PLAINS villager so nothing tries to look up missing data.
        // Note: no profession-overlay layer is wired up, so the chosen values are inert.
        if (state.villagerData == null) {
            var registryAccess = entity.registryAccess();
            var typeRegistry = registryAccess.lookupOrThrow(Registries.VILLAGER_TYPE);
            var profRegistry = registryAccess.lookupOrThrow(Registries.VILLAGER_PROFESSION);
            state.villagerData = new VillagerData(
                typeRegistry.getOrThrow(VillagerType.PLAINS),
                profRegistry.getOrThrow(VillagerProfession.NONE),
                1);
        }
    }
}
