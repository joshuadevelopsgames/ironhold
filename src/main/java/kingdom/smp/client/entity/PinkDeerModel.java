package kingdom.smp.client.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

// Note: model is typed on LivingEntityRenderState so it can be used by both
// DeerRenderState (which extends it) and the base type directly.

/**
 * Custom deer model — slender body, thin legs, small head with large ears,
 * short neck, tiny tail. Proportioned like a real fawn/doe.
 *
 * <pre>
 * Texture: 64x64
 *
 *   Body:    6×5×10  — torso, slightly elongated
 *   Neck:    3×5×3   — short upright neck
 *   Head:    4×4×5   — small delicate head
 *   Ears:    1×3×2   — large pointed ears (×2)
 *   Nose:    2×2×3   — small snout
 *   Legs:    2×8×2   — thin and long (×4)
 *   Tail:    1×3×1   — tiny upward tail
 * </pre>
 */
public class PinkDeerModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath("ironhold", "pink_deer"), "main");

    public static final ModelLayerLocation BABY_LAYER = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath("ironhold", "pink_deer"), "baby");

    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightBackLeg;
    private final ModelPart leftBackLeg;
    private final ModelPart tail;
    private final ModelPart rightEar;
    private final ModelPart leftEar;

    public PinkDeerModel(ModelPart root) {
        super(root);
        this.body         = root.getChild("body");
        this.neck         = body.getChild("neck");
        this.head         = neck.getChild("head");
        this.rightEar     = head.getChild("right_ear");
        this.leftEar      = head.getChild("left_ear");
        this.tail         = body.getChild("tail");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg  = root.getChild("left_front_leg");
        this.rightBackLeg  = root.getChild("right_back_leg");
        this.leftBackLeg   = root.getChild("left_back_leg");
    }

    public static LayerDefinition createBodyLayer() {
        return createDeerLayer(1.0F);
    }

    public static LayerDefinition createBabyLayer() {
        return createDeerLayer(0.7F);
    }

    private static LayerDefinition createDeerLayer(float scale) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Body: 6 wide × 5 tall × 10 long, centered at y=13 (legs are 8 tall)
        // Pivot at center of body
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-3.0F, -2.5F, -5.0F, 6, 5, 10, CubeDeformation.NONE),
            PartPose.offset(0.0F, 13.5F, 0.0F));

        // Neck: 3×5×3, angled forward and up from front of body
        PartDefinition neck = body.addOrReplaceChild("neck",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-1.5F, -5.0F, -1.5F, 3, 5, 3, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, -1.5F, -4.5F, -0.2F, 0.0F, 0.0F));

        // Head: 4×4×5, at top of neck
        PartDefinition head = neck.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(-2.0F, -3.0F, -4.0F, 4, 4, 5, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, -4.5F, 0.0F, 0.15F, 0.0F, 0.0F));

        // Nose/snout: 2×2×3, extends forward from head
        head.addOrReplaceChild("nose",
            CubeListBuilder.create()
                .texOffs(18, 16)
                .addBox(-1.0F, -1.0F, -3.0F, 2, 2, 3, CubeDeformation.NONE),
            PartPose.offset(0.0F, 0.0F, -4.0F));

        // Right ear: 1×3×2, angled outward
        head.addOrReplaceChild("right_ear",
            CubeListBuilder.create()
                .texOffs(0, 26)
                .addBox(-1.0F, -3.0F, 0.0F, 1, 3, 2, CubeDeformation.NONE),
            PartPose.offsetAndRotation(-1.5F, -3.0F, -1.0F, 0.0F, 0.0F, -0.35F));

        // Left ear: mirrored
        head.addOrReplaceChild("left_ear",
            CubeListBuilder.create()
                .texOffs(6, 26)
                .addBox(0.0F, -3.0F, 0.0F, 1, 3, 2, CubeDeformation.NONE),
            PartPose.offsetAndRotation(1.5F, -3.0F, -1.0F, 0.0F, 0.0F, 0.35F));

        // Tail: 1×3×1, small and upward
        body.addOrReplaceChild("tail",
            CubeListBuilder.create()
                .texOffs(44, 0)
                .addBox(-0.5F, -1.0F, 0.0F, 1, 3, 1, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, -2.0F, 5.0F, 0.6F, 0.0F, 0.0F));

        // Legs: 2×8×2 each, thin deer legs
        // Front right
        root.addOrReplaceChild("right_front_leg",
            CubeListBuilder.create()
                .texOffs(32, 8)
                .addBox(-1.0F, 0.0F, -1.0F, 2, 8, 2, CubeDeformation.NONE),
            PartPose.offset(-2.0F, 16.0F, -3.5F));

        // Front left
        root.addOrReplaceChild("left_front_leg",
            CubeListBuilder.create()
                .texOffs(40, 8)
                .addBox(-1.0F, 0.0F, -1.0F, 2, 8, 2, CubeDeformation.NONE),
            PartPose.offset(2.0F, 16.0F, -3.5F));

        // Back right
        root.addOrReplaceChild("right_back_leg",
            CubeListBuilder.create()
                .texOffs(48, 8)
                .addBox(-1.0F, 0.0F, -1.0F, 2, 8, 2, CubeDeformation.NONE),
            PartPose.offset(-2.0F, 16.0F, 3.5F));

        // Back left
        root.addOrReplaceChild("left_back_leg",
            CubeListBuilder.create()
                .texOffs(56, 8)
                .addBox(-1.0F, 0.0F, -1.0F, 2, 8, 2, CubeDeformation.NONE),
            PartPose.offset(2.0F, 16.0F, 3.5F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);

        float walkSpeed = state.walkAnimationSpeed;
        float walkPos   = state.walkAnimationPos;
        float t         = state.ageInTicks;

        // ── Grazing override ─────────────────────────────────────────────────
        // When DeerRenderState.isGrazing is true, dip the neck/head down as if
        // eating grass. A slow nibble oscillation on the head prevents it from
        // looking frozen. All other parts animate normally (or nearly so).
        boolean grazing = state instanceof DeerRenderState drs && drs.isGrazing;
        if (grazing) {
            // Graceful deer grazing: one slow, fluid arc instead of repeated bobbing.
            //
            // The ~5-second cycle means during any 3–6 s graze window the deer
            // performs at most one smooth dip — head lowers, lingers near the ground
            // with gentle nibbling, then lifts. Smoothstep easing gives the motion
            // natural acceleration/deceleration at the peaks, like a real deer.
            //
            // super.setupAnim() calls root().resetPose() each frame (neck.xRot resets
            // to its baked -0.2F), so every value here is a pure function of time.
            float raw    = (Mth.sin(t * 0.065F) + 1.0F) * 0.5F;  // 0..1, ~4.8 s period
            float phase  = raw * raw * (3.0F - 2.0F * raw);       // smoothstep easing

            // Neck does all the dipping — graceful arc from resting tilt (-0.2) down toward ground
            neck.xRot = -0.2F + phase * 1.35F;
            // Head stays at its resting angle; only subtle nibble when nose is near ground
            float nibble = phase > 0.7F ? Mth.sin(t * 0.45F) * 0.04F : 0F;
            head.xRot = 0.15F + nibble;
            // Legs stay planted while grazing
            rightFrontLeg.xRot = 0F;
            leftFrontLeg.xRot  = 0F;
            rightBackLeg.xRot  = 0F;
            leftBackLeg.xRot   = 0F;
        } else {
            // ── Legs ────────────────────────────────────────────────────────
            float legSwing = Mth.cos(walkPos * 0.6662F) * 1.2F * walkSpeed;
            rightFrontLeg.xRot =  legSwing;
            leftFrontLeg.xRot  = -legSwing;
            rightBackLeg.xRot  = -legSwing;
            leftBackLeg.xRot   =  legSwing;

            // ── Head bob while walking ───────────────────────────────────────
            head.xRot = 0.15F + Mth.sin(walkPos * 0.6662F) * 0.08F * walkSpeed;
        }

        // ── Ears ─────────────────────────────────────────────────────────────
        // Real deer ears: large, mobile, scan independently. At rest they angle
        // outward; when scanning they rotate forward on yRot; when alert they
        // perk upright (zRot → 0); occasional sharp twitch from beat oscillators.
        // While grazing, ears perk forward (alert while vulnerable).
        if (grazing) {
            // Grazing ears: perk slightly forward and outward — alert posture
            float gPerk = Mth.sin(t * 0.025F) * 0.12F;       // gentle drift
            rightEar.zRot = -0.15F + gPerk;
            leftEar.zRot  =  0.15F - gPerk;
            rightEar.yRot = -0.2F;   // pointed slightly forward
            leftEar.yRot  =  0.2F;
        } else if (walkSpeed < 0.1F) {
            // Beat-frequency oscillators: two irrationally-related sines multiply
            // to produce spikes above threshold only rarely — no state needed.

            // RIGHT ear — slow yaw scan + rare alert perk + sharp twitch
            float rScan   = Mth.sin(t * 0.025F) * 0.45F;                       // ~4s yaw sweep
            float rBeat   = Mth.sin(t * 0.31F) * Mth.sin(t * 0.17F);           // beats ~every 8s
            float rTwitch = rBeat > 0.65F ? (rBeat - 0.65F) * 2.2F : 0F;       // sharp spike
            float rPerk   = Math.max(0F, Mth.sin(t * 0.051F) * Mth.sin(t * 0.023F)); // rare upright perk
            rightEar.yRot = rScan + rTwitch * 0.3F;
            rightEar.zRot = -0.35F + rPerk * 0.32F;                             // toward vertical when perked

            // LEFT ear — different phase → truly independent from right
            float lScan   = Mth.sin(t * 0.028F + 1.8F) * 0.45F;
            float lBeat   = Mth.sin(t * 0.27F) * Mth.sin(t * 0.19F);
            float lTwitch = lBeat > 0.65F ? (lBeat - 0.65F) * 2.2F : 0F;
            float lPerk   = Math.max(0F, Mth.sin(t * 0.047F + 0.9F) * Mth.sin(t * 0.021F + 0.5F));
            leftEar.yRot = -(lScan + lTwitch * 0.3F);
            leftEar.zRot =  0.35F - lPerk * 0.32F;

        } else {
            // Moving: ears trail back slightly with speed, yaw centred
            float trail = Mth.clamp(walkSpeed * 0.13F, 0F, 0.18F);
            rightEar.zRot = -0.35F - trail;
            leftEar.zRot  =  0.35F + trail;
            rightEar.yRot = 0F;
            leftEar.yRot  = 0F;
        }

        // ── Tail ─────────────────────────────────────────────────────────────
        // Beat-frequency trigger for deer "flagging" — tail snaps up sharply to
        // show the white underside, then settles. Happens rarely (~every 12s).
        float tailBeat = Mth.sin(t * 0.041F) * Mth.sin(t * 0.113F);
        if (tailBeat > 0.72F) {
            // Flag: tail flips up toward -0.3 (perpendicular/past-vertical)
            float flip = (tailBeat - 0.72F) / 0.28F;          // 0 → 1
            tail.xRot = 0.6F - flip * 0.95F;                  // snaps up to ~-0.35
            tail.yRot = Mth.sin(t * 0.38F) * 0.3F;            // faster side-waggle while flagging
        } else {
            tail.xRot = 0.6F + Mth.sin(t * 0.1F) * 0.06F;    // gentle resting bob
            tail.yRot = Mth.sin(t * 0.1F) * 0.15F;            // slow idle sway
        }
    }
}
