package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import kingdom.smp.Ironhold;
import kingdom.smp.ModItems;
import kingdom.smp.client.VanityAccessoryRenderState;
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
import net.minecraft.util.Mth;

/**
 * Renders a small glowing halo above a player's head when they wear the halo in
 * the vanity HEAD slot. The ring is mounted to the head bone so it follows head
 * pitch/yaw while floating above the crown.
 *
 * <p>The ring is a true closed circle: {@link #SEGMENTS} short cuboid segments
 * arranged radially, each rotated to sit tangent to the circle so the corners
 * meet seamlessly (an N-gon that reads as round). It always renders full-bright
 * with an additive {@linkplain RenderTypes#eyes eyes} pass on top, so the halo
 * glows like emitted light even in shade, and both passes are tinted by a slow
 * sine so the emissive gold gently pulsates.
 */
public class HaloLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(Ironhold.MODID, "halo"), "main");

    /** Number of cuboid segments forming the ring; 12 reads as a smooth circle. */
    private static final int SEGMENTS = 12;
    /** Ring radius in model-space pixels (diameter ~10px, floating above the crown). */
    private static final float RADIUS = 5.0F;
    /** Always lit — the halo emits its own light rather than sampling world light. */
    private static final int FULL_BRIGHT = 0xF000F0;
    /** Radians per tick for the glow pulse; ~0.09 gives a gentle ~3.5s breathe. */
    private static final float PULSE_SPEED = 0.09F;
    /** Peak vertical bob in model-space pixels — kept small for a subtle float. */
    private static final float BOB_AMPLITUDE = 0.5F;
    /** Radians per tick for the bob; offset from the pulse so they don't lock-step. */
    private static final float BOB_SPEED = 0.07F;
    /** Height the ring floats above the head pivot, in model-space pixels (y-down, so up). */
    private static final float FLOAT_HEIGHT = -11.5F;
    /** Uniform scale of the ring; below 1 shrinks it about its own centre. */
    private static final float SCALE = 0.85F;

    private static final Identifier TEXTURE_ID =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/halo.png");
    private static final RenderType TEXTURE = RenderTypes.entityCutout(TEXTURE_ID);
    private static final RenderType GLOW = RenderTypes.eyes(TEXTURE_ID);

    private final ModelPart halo;

    public HaloLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, ModelPart root) {
        super(parent);
        this.halo = root.getChild("halo");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Empty parent that just holds the ring segments; submitting it renders them all.
        PartDefinition ring = root.addOrReplaceChild(
            "halo", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        // Each segment is a thin gold bar centred on its own origin (3 long x 1 tall x
        // 2 deep). All segments share texOffs(0,0): the box unwraps to a 10x2px footprint
        // that fits the 16x16 gold sheet, so every segment samples the same solid gold.
        // Placed at RADIUS and rotated by its angle, each bar sits tangent to the circle;
        // the 3px length slightly exceeds the 2.59px chord, so neighbours overlap and
        // close the ring with no gaps.
        for (int i = 0; i < SEGMENTS; i++) {
            float angle = (float) (i * 2.0 * Math.PI / SEGMENTS);
            float x = (float) (Math.sin(angle) * RADIUS);
            float z = (float) (Math.cos(angle) * RADIUS);
            ring.addOrReplaceChild(
                "seg" + i,
                CubeListBuilder.create()
                    .texOffs(0, 0).addBox(-1.5F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(x, 0.0F, z, 0.0F, angle, 0.0F));
        }
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                       AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) return;
        if (!((VanityAccessoryRenderState) state).ironhold$headAccessory().is(ModItems.HALO.get())) return;

        // Slow emissive breathe: a sine on the render state's age tints both passes
        // between half and full brightness (alpha left opaque), so the gold halo
        // gently pulsates its glow.
        float brightness = 0.75F + 0.25F * Mth.sin(state.ageInTicks * PULSE_SPEED);
        int v = Math.round(brightness * 255.0F);
        int pulse = 0xFF000000 | (v << 16) | (v << 8) | v;

        // Slight slow bob: a sine offsets the float height a fraction of a pixel
        // (model space is y-down, so the sign just shifts which way it drifts).
        float bob = Mth.sin(state.ageInTicks * BOB_SPEED) * BOB_AMPLITUDE;

        pose.pushPose();
        this.getParentModel().head.translateAndRotate(pose);
        pose.translate(0.0F, (FLOAT_HEIGHT + bob) / 16.0F, 0.0F);
        pose.scale(SCALE, SCALE, SCALE);
        // Base pass full-bright so the gold reads as glowing, then an additive eyes pass
        // for the emissive bloom on top; both carry the pulse tint.
        collector.submitModelPart(this.halo, pose, TEXTURE, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null, pulse, null);
        collector.submitModelPart(this.halo, pose, GLOW, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null, pulse, null);
        pose.popPose();
    }
}
