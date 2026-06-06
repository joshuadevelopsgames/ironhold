package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import kingdom.smp.Ironhold;
import kingdom.smp.ModItems;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Renders a true-3D Shroomcap atop a player's head when they wear one in the
 * vanity HEAD slot. Reuses the Shroomling's own cap geometry (the three stacked
 * tiers — crown, mid, rim), baked as its own layer and mounted on the head bone
 * so the cap tilts with the head.
 *
 * <p>The shroomcap items carry an {@code Equippable} with no equipment asset, so
 * vanilla's armor layer draws nothing on the head — this layer supplies the
 * visible geometry instead. Blue and orange caps share the geometry and differ
 * only by texture, matched to the worn item.
 */
public class ShroomcapLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(Ironhold.MODID, "shroomcap"), "main");

    private static final RenderType BLUE = RenderTypes.entityCutout(
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/shroomling/shroomling.png"));
    private static final RenderType ORANGE = RenderTypes.entityCutout(
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/shroomling/shroomling_orange.png"));
    // Additive emissive pass for the orange cap, mirroring the orange Shroomling's
    // ShroomlingGlowLayer. The glow sheet shares the cap's box UVs, so the same
    // geometry lit through the eyes RenderType makes the orange cap glow.
    private static final RenderType GLOW = RenderTypes.eyes(
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/shroomling/shroomling_orange_glow.png"));

    private final ModelPart cap;

    public ShroomcapLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, ModelPart root) {
        super(parent);
        this.cap = root.getChild("cap");
    }

    /**
     * Cap geometry rooted at the origin — identical box UVs to
     * {@link ShroomlingModel}'s cap, but with the part pivot at (0,0,0) so the
     * rim's underside sits at y=0 and the crown rises to y=-4 (model space is
     * y-down). Texture sheet is the 64x64 Shroomling skin.
     */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "cap",
            CubeListBuilder.create()
                .texOffs(8, 7).addBox(-4.0F, -3.0F, -4.0F, 8.0F, 2.0F, 8.0F)
                .texOffs(4, 2).addBox(-5.0F, -1.0F, -5.0F, 10.0F, 1.0F, 10.0F)
                .texOffs(12, 6).addBox(-3.0F, -4.0F, -3.0F, 6.0F, 1.0F, 6.0F),
            PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                       AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) return;

        ItemStack head = state.headEquipment;
        RenderType texture;
        boolean orange;
        if (head.is(ModItems.SHROOMCAP_ORANGE.get())) {
            texture = ORANGE;
            orange = true;
        } else if (head.is(ModItems.SHROOMCAP.get())) {
            texture = BLUE;
            orange = false;
        } else {
            return;
        }

        pose.pushPose();
        // Mount on the head bone so the cap inherits head pitch/yaw, then apply the
        // baked-in transform tuned live via /shroomcapdebug. Translation is in pixels of
        // model space (÷16): (0, -4.8, 0) lifts the cap's rim onto the crown of the head
        // cube — which spans head-local y = -8..0 (px) — sitting just as it does atop the
        // Shroomling's own stem. Scale 1.25 is uniform about the rim anchor.
        this.getParentModel().head.translateAndRotate(pose);
        pose.translate(0.0F, -4.8F / 16.0F, 0.0F);
        pose.scale(1.25F, 1.25F, 1.25F);
        collector.submitModelPart(this.cap, pose, texture, packedLight, OverlayTexture.NO_OVERLAY, null);
        // Emissive overlay for the orange cap: re-submit the same geometry through the
        // eyes RenderType so the glow sheet ignores world light and stays lit, exactly
        // as ShroomlingGlowLayer does for the orange Shroomling itself.
        if (orange) {
            collector.submitModelPart(this.cap, pose, GLOW, packedLight, OverlayTexture.NO_OVERLAY, null);
        }
        pose.popPose();
    }
}
