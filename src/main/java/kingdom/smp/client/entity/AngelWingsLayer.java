package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
 * Renders a pair of feathered angel wings on the player's back when the item is
 * worn in the CHEST vanity slot. Each wing is a fan of thin feather plates rooted
 * at the shoulder: a long primary row sweeping out along a rising arc, with a
 * shorter covert row layered in front to fill the upper wing. Left and right are
 * built as separate X-mirrored parts (rather than a {@code scale(-1)} flip) so the
 * face winding stays correct. The wings are always lit for a clean angelic white
 * and beat with a slow idle flap.
 *
 * <p>The whole rig is mounted on the body bone so it follows the torso, and the
 * pose constants below (sweep / roll / pitch / attach offsets) are deliberately
 * isolated for quick in-game tuning.
 */
public class AngelWingsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(Ironhold.MODID, "angel_wings"), "main");

    private static final Identifier TEXTURE_ID =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/cosmetic/angel_wings.png");
    private static final RenderType TEXTURE = RenderTypes.entityCutout(TEXTURE_ID);

    /** Always lit so the white feathers read as softly luminous rather than shaded grey. */
    private static final int FULL_BRIGHT = 0xF000F0;

    // ── Feather-fan geometry (model-space pixels / radians) ──────────────────────
    private static final float ARM_RADIUS = 8.5F;   // length of the leading-edge arc
    private static final float ARM_SWEEP = 1.40F;   // how far the arc curves outward (~80°)
    private static final float ARM_RISE = 0.60F;    // how much the arc lifts as it sweeps out
    private static final int PRIMARIES = 9;          // long outer flight feathers
    private static final float PRIMARY_LEN_MIN = 7.0F;
    private static final float PRIMARY_LEN_MAX = 14.0F;
    private static final float PRIMARY_SPLAY = 0.50F;  // outward tilt of the outer tips
    private static final int COVERTS = 6;            // short feathers filling the upper wing
    private static final float COVERT_LEN_MIN = 4.0F;
    private static final float COVERT_LEN_MAX = 7.0F;
    private static final float COVERT_SPLAY = 0.30F;
    private static final float FEATHER_WIDTH = 2.0F;
    // Depth layering — every feather gets a UNIQUE z so overlapping broad faces never
    // share a plane (no z-fighting). Primaries sit in a back band, coverts in a front
    // band; the whole span stays under the 1px feather thickness so no feather's front
    // face can ever coincide with another's back face either. Within a band the depths
    // are interleaved (see layerDepth) so fan-adjacent feathers — which overlap most —
    // land far apart in z.
    private static final float PRIMARY_BAND = 0.45F;  // primaries occupy z in [-0.45, 0]
    private static final float COVERT_FRONT = 0.15F;  // coverts begin here, in front of primaries
    private static final float COVERT_BAND = 0.30F;   // coverts occupy z in [0.15, 0.45]

    // ── Mount + pose (tune live) ─────────────────────────────────────────────────
    private static final float ATTACH_Y = 3.0F;     // down the back from the body pivot
    private static final float ATTACH_Z = 2.2F;     // behind the back surface
    private static final float SHOULDER_X = 1.0F;   // each wing rooted just off the spine
    private static final float BASE_SWEEP = -0.40F; // yaw: angle the wings backward
    private static final float BASE_ROLL = -0.30F;  // roll: lift the wing tips upward
    private static final float BASE_PITCH = 0.15F;  // pitch: tilt the whole span back
    private static final float WING_SCALE = 0.9F;

    // ── Idle flap ────────────────────────────────────────────────────────────────
    private static final float FLAP_SPEED = 0.08F;  // radians/tick (~5s cycle)
    private static final float FLAP_AMPLITUDE = 0.12F;

    private final ModelPart wingRight;
    private final ModelPart wingLeft;

    public AngelWingsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, ModelPart root) {
        super(parent);
        this.wingRight = root.getChild("wing_right");
        this.wingLeft = root.getChild("wing_left");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        addFeathers(root.addOrReplaceChild("wing_right", CubeListBuilder.create(), PartPose.ZERO), +1);
        addFeathers(root.addOrReplaceChild("wing_left", CubeListBuilder.create(), PartPose.ZERO), -1);
        return LayerDefinition.create(mesh, 16, 16);
    }

    /**
     * Build one wing's feathers into {@code wing}. {@code side} is +1 for the right
     * wing (feathers fan toward +X) and -1 for the left (mirrored to -X), so each
     * part is independently correct and needs no pose-space mirroring.
     */
    private static void addFeathers(PartDefinition wing, int side) {
        // Long primaries along the rising leading-edge arc.
        for (int i = 0; i < PRIMARIES; i++) {
            float t = i / (float) (PRIMARIES - 1);
            float beta = t * ARM_SWEEP;
            float ax = side * ARM_RADIUS * Mth.sin(beta);
            float ay = -ARM_RADIUS * (1.0F - Mth.cos(beta)) * ARM_RISE;
            float az = -PRIMARY_BAND + layerDepth(i, PRIMARIES, PRIMARY_BAND);
            float len = PRIMARY_LEN_MIN + (PRIMARY_LEN_MAX - PRIMARY_LEN_MIN) * Mth.sin(t * Mth.PI * 0.8F);
            float roll = -side * t * PRIMARY_SPLAY; // outer tips splay outward
            wing.addOrReplaceChild("p" + i, feather(len),
                PartPose.offsetAndRotation(ax, ay, az, 0.0F, 0.0F, roll));
        }
        // Short coverts over the inner wing, layered slightly in front of the primaries.
        for (int i = 0; i < COVERTS; i++) {
            float t = i / (float) (COVERTS - 1);
            float beta = t * ARM_SWEEP * 0.75F;
            float ax = side * ARM_RADIUS * Mth.sin(beta);
            float ay = -ARM_RADIUS * (1.0F - Mth.cos(beta)) * ARM_RISE;
            float az = COVERT_FRONT + layerDepth(i, COVERTS, COVERT_BAND);
            float len = COVERT_LEN_MIN + (COVERT_LEN_MAX - COVERT_LEN_MIN) * t;
            float roll = -side * t * COVERT_SPLAY;
            wing.addOrReplaceChild("c" + i, feather(len),
                PartPose.offsetAndRotation(ax, ay, az, 0.0F, 0.0F, roll));
        }
    }

    /**
     * A distinct depth in {@code [0, band)} for feather {@code i} of {@code count},
     * interleaved (evens then odds) so neighbouring feathers in the fan land roughly
     * half a band apart in z — the feathers that overlap most never sit at adjacent
     * depths, and no two ever share a depth.
     */
    private static float layerDepth(int i, int count, float band) {
        int half = (count + 1) / 2;
        int rank = (i % 2 == 0) ? i / 2 : half + i / 2;
        return band * rank / (count - 1);
    }

    /** A single thin feather plate, rooted at its top and hanging down (+Y). */
    private static CubeListBuilder feather(float length) {
        return CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-FEATHER_WIDTH / 2.0F, 0.0F, -0.5F, FEATHER_WIDTH, length, 1.0F);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                       AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) return;
        if (!((VanityAccessoryRenderState) state).ironhold$chestAccessory().is(ModItems.ANGEL_WINGS.get())) return;

        float flap = Mth.sin(state.ageInTicks * FLAP_SPEED) * FLAP_AMPLITUDE;

        pose.pushPose();
        this.getParentModel().body.translateAndRotate(pose);
        pose.translate(0.0F, ATTACH_Y / 16.0F, ATTACH_Z / 16.0F);
        submitWing(pose, collector, packedLight, this.wingRight, +1, flap);
        submitWing(pose, collector, packedLight, this.wingLeft, -1, flap);
        pose.popPose();
    }

    private void submitWing(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                            ModelPart wing, int side, float flap) {
        pose.pushPose();
        pose.translate(side * SHOULDER_X / 16.0F, 0.0F, 0.0F);
        // Mirror the sweep/roll per side; the flap adds onto the sweep so both wings
        // open and close together.
        pose.mulPose(Axis.YP.rotation(side * (BASE_SWEEP + flap)));
        pose.mulPose(Axis.ZP.rotation(side * BASE_ROLL));
        pose.mulPose(Axis.XP.rotation(BASE_PITCH));
        pose.scale(WING_SCALE, WING_SCALE, WING_SCALE);
        collector.submitModelPart(wing, pose, TEXTURE, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null);
        pose.popPose();
    }
}
