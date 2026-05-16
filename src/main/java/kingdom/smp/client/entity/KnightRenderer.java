package kingdom.smp.client.entity;

import kingdom.smp.entity.KnightEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

public class KnightRenderer extends HumanoidMobRenderer<KnightEntity, AvatarRenderState, PlayerModel> {

    private final Identifier texture;

    /**
     * @param ctx        Renderer context from NeoForge
     * @param texture    Base skin texture path (neutral skin-tone humanoid)
     * @param helmetDef  LayerDefinition for the custom 3D helmet geometry (from KnightArmorModelDefs)
     */
    public KnightRenderer(EntityRendererProvider.Context ctx, Identifier texture, LayerDefinition helmetDef) {
        super(ctx, new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.texture = texture;

        // Bake standard armor models for chest/legs/feet slots from the vanilla PLAYER_ARMOR set.
        // These provide correctly-UV-mapped body/arm/boot/legging geometry.
        ArmorModelSet<HumanoidModel<AvatarRenderState>> standardSet =
            ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, ctx.getModelSet(),
                p -> new HumanoidModel<>(p));

        // Bake the custom helmet model from the Epic Knights geometry definition.
        // The head bone is at PartPose.ZERO matching vanilla convention so setupAnim()
        // correctly positions it at the entity's head location.
        HumanoidModel<AvatarRenderState> helmetModel = new HumanoidModel<>(helmetDef.bakeRoot());

        // Combine: custom head + vanilla chest/legs/feet
        ArmorModelSet<HumanoidModel<AvatarRenderState>> customSet = new ArmorModelSet<>(
            helmetModel,
            standardSet.chest(),
            standardSet.legs(),
            standardSet.feet()
        );

        // HumanoidArmorLayer reads Equippable + EquipmentAsset from each armor slot and
        // renders the texture via EquipmentLayerRenderer.  Our custom helmet model supplies
        // the 3D shape while the equipment JSON supplies the Epic Knights texture.
        this.addLayer(new HumanoidArmorLayer<>(this, customSet, ctx.getEquipmentRenderer()));
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return texture;
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }
}
