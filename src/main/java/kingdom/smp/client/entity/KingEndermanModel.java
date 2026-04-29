package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Mutant-enderman-shaped boss with a crown.
 *  Hierarchy: pelvis → abdomen → chest → (neck → head → jaw + crown) + 4 arms.
 *  Legs are digitigrade: legJoint → thigh → shin. */
public class KingEndermanModel extends EntityModel<KingEndermanRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(Ironhold.MODID, "king_enderman"), "main");

    private static final CubeDeformation NONE = CubeDeformation.NONE;

    private final ModelPart pelvis;
    private final ModelPart abdomen;
    private final ModelPart chest;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart crown;
    private final Arm rightArm;
    private final Arm leftArm;
    private final Arm lowerRightArm;
    private final Arm lowerLeftArm;
    private final ModelPart legJoint1;
    private final ModelPart legJoint2;
    private final ModelPart thigh1;
    private final ModelPart thigh2;
    private final ModelPart shin1;
    private final ModelPart shin2;

    public KingEndermanModel(ModelPart root) {
        super(root);
        this.pelvis    = root.getChild("pelvis");
        this.abdomen   = this.pelvis.getChild("abdomen");
        this.chest     = this.abdomen.getChild("chest");
        this.neck      = this.chest.getChild("neck");
        this.head      = this.neck.getChild("head");
        this.jaw       = this.head.getChild("jaw");
        this.crown     = this.head.getChild("crown");
        this.rightArm      = new Arm(this.chest, "right");
        this.leftArm       = new Arm(this.chest, "left");
        this.lowerRightArm = new Arm(this.chest, "lower_right");
        this.lowerLeftArm  = new Arm(this.chest, "lower_left");
        this.legJoint1 = this.abdomen.getChild("leg_joint1");
        this.legJoint2 = this.abdomen.getChild("leg_joint2");
        this.thigh1    = this.legJoint1.getChild("thigh1");
        this.thigh2    = this.legJoint2.getChild("thigh2");
        this.shin1     = this.thigh1.getChild("shin1");
        this.shin2     = this.thigh2.getChild("shin2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // pelvis: invisible anchor at hip height
        PartDefinition pelvis = root.addOrReplaceChild("pelvis",
            CubeListBuilder.create(),
            PartPose.offset(0F, -16F, 0F));

        // abdomen: 8 x 10 x 4
        PartDefinition abdomen = pelvis.addOrReplaceChild("abdomen",
            CubeListBuilder.create()
                .texOffs(50, 22).addBox(-4F, -10F, -2F, 8F, 10F, 4F, NONE),
            PartPose.ZERO);

        // chest: 10 x 16 x 6, sits atop abdomen
        PartDefinition chest = abdomen.addOrReplaceChild("chest",
            CubeListBuilder.create()
                .texOffs(76, 0).addBox(-5F, -16F, -3F, 10F, 16F, 6F, NONE),
            PartPose.offset(0F, -8F, 0F));

        // neck: 3 x 4 x 3
        PartDefinition neck = chest.addOrReplaceChild("neck",
            CubeListBuilder.create()
                .texOffs(32, 14).addBox(-1.5F, -4F, -1.5F, 3F, 4F, 3F, NONE),
            PartPose.offset(0F, -15F, 0F));

        // head: 8 x 6 x 8 (vanilla enderman head UV)
        PartDefinition head = neck.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4F, -4F, -8F, 8F, 6F, 8F, new CubeDeformation(0.5F)),
            PartPose.offset(0F, -5F, 3F));

        // jaw: 8 x 2 x 8, hangs below head
        head.addOrReplaceChild("jaw",
            CubeListBuilder.create()
                .texOffs(0, 14).addBox(-4F, 0F, -8F, 8F, 2F, 8F, NONE),
            PartPose.offset(0F, 2F, 0F));

        // crown: ring + 4 spikes, sits atop head
        // ring 10 x 2 x 10 hollow (front + back + sides as thin slabs)
        head.addOrReplaceChild("crown",
            CubeListBuilder.create()
                .texOffs(74, 36).addBox(-5F, -7F, -9F, 10F, 3F, 1F, NONE)   // front  unwrap 22x4
                .texOffs(74, 40).addBox(-5F, -7F, 1F,  10F, 3F, 1F, NONE)   // back   unwrap 22x4
                .texOffs(24, 34).addBox(-5F, -7F, -8F,  1F, 3F, 9F, NONE)   // right  unwrap 20x12
                .texOffs(24, 46).addBox( 4F, -7F, -8F,  1F, 3F, 9F, NONE)   // left   unwrap 20x12
                .texOffs(96, 36).addBox(-4F, -10F, -8F, 1F, 3F, 1F, NONE)   // spike FR  unwrap 4x4
                .texOffs(96, 36).addBox( 3F, -10F, -8F, 1F, 3F, 1F, NONE)   // spike FL  (shared UV)
                .texOffs(96, 36).addBox(-4F, -10F,  0F, 1F, 3F, 1F, NONE)   // spike BR  (shared UV)
                .texOffs(96, 36).addBox( 3F, -10F,  0F, 1F, 3F, 1F, NONE)   // spike BL  (shared UV)
                .texOffs(100, 36).addBox(-1F, -11F, -4F, 2F, 4F, 2F, NONE), // tall center spike  unwrap 8x6
            PartPose.ZERO);

        // 4 arms attached to chest
        Arm.create(chest, "right",       -4F, false);
        Arm.create(chest, "left",         4F, true);
        Arm.create(chest, "lower_right", -4F, false);
        Arm.create(chest, "lower_left",   4F, true);

        // 2 legs attached to abdomen
        PartDefinition legJoint1 = abdomen.addOrReplaceChild("leg_joint1",
            CubeListBuilder.create(),
            PartPose.offset(-2F, 0F, 0F));
        PartDefinition legJoint2 = abdomen.addOrReplaceChild("leg_joint2",
            CubeListBuilder.create(),
            PartPose.offset( 2F, 0F, 0F));

        PartDefinition thigh1 = legJoint1.addOrReplaceChild("thigh1",
            CubeListBuilder.create()
                .texOffs(0, 34).addBox(-1.5F, 0F, -1.5F, 3F, 24F, 3F, new CubeDeformation(0.5F)),
            PartPose.offset(0F, -2F, 0F));
        PartDefinition thigh2 = legJoint2.addOrReplaceChild("thigh2",
            CubeListBuilder.create()
                .texOffs(0, 34).mirror().addBox(-1.5F, 0F, -1.5F, 3F, 24F, 3F, new CubeDeformation(0.5F)),
            PartPose.offset(0F, -2F, 0F));

        thigh1.addOrReplaceChild("shin1",
            CubeListBuilder.create()
                .texOffs(12, 34).addBox(-1.5F, 0F, -1.5F, 3F, 24F, 3F, new CubeDeformation(0.5F)),
            PartPose.offset(0F, 23F, 0F));
        thigh2.addOrReplaceChild("shin2",
            CubeListBuilder.create()
                .texOffs(12, 34).mirror().addBox(-1.5F, 0F, -1.5F, 3F, 24F, 3F, new CubeDeformation(0.5F)),
            PartPose.offset(0F, 23F, 0F));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(KingEndermanRenderState state) {
        super.setupAnim(state);

        // Resting/initial pose: slight slouch, digitigrade leg bend.
        this.abdomen.xRot = 0.31415927F;
        this.chest.xRot   = 0.3926991F;
        this.neck.xRot    = 0.19634955F;
        this.head.xRot    = -0.7853982F;
        this.head.yRot    = 0F;
        this.head.zRot    = 0F;
        this.jaw.xRot     = 0F;
        this.crown.xRot   = 0F;
        this.thigh1.xRot = -0.8975979F; this.thigh1.zRot =  0.2617994F;
        this.thigh2.xRot = -0.8975979F; this.thigh2.zRot = -0.2617994F;
        this.shin1.xRot  =  0.7853982F; this.shin1.zRot  = -0.1308997F;
        this.shin2.xRot  =  0.7853982F; this.shin2.zRot  =  0.1308997F;
        this.pelvis.y = -16F;

        // Restore arm rest pose.
        this.rightArm.rest(true);
        this.leftArm.rest(false);
        this.lowerRightArm.rest(true);
        this.lowerLeftArm.rest(false);
        this.lowerRightArm.upper.xRot += 0.10F;
        this.lowerRightArm.upper.zRot -= 0.20F;
        this.lowerLeftArm.upper.xRot  += 0.10F;
        this.lowerLeftArm.upper.zRot  += 0.20F;

        // Head look.
        float yaw   = state.yRot * Mth.DEG_TO_RAD;
        float pitch = state.xRot * Mth.DEG_TO_RAD;
        this.head.yRot += yaw   * 0.7F;
        this.head.zRot -= yaw   * 0.7F;
        this.head.xRot += pitch * 0.5F;
        this.neck.xRot += pitch * 0.3F;
        this.chest.xRot += pitch * 0.2F;

        // Idle breathe — body uses one rhythm; each arm gets its own phase + frequency
        // so the four arms drift independently rather than mirroring in pairs.
        float t = state.ageInTicks;
        float breathe = Mth.sin(t * 0.15F);
        this.jaw.xRot  += breathe * 0.02F + 0.02F;
        this.neck.xRot -= breathe * 0.02F;
        float idleR  = Mth.sin(t * 0.13F);                              // upper right
        float idleL  = Mth.sin(t * 0.11F + 1.7F);                       // upper left
        float idleLR = Mth.sin(t * 0.09F + 3.3F);                       // lower right
        float idleLL = Mth.sin(t * 0.10F + 4.9F);                       // lower left
        this.rightArm.upper.zRot      += idleR  * 0.05F;
        this.rightArm.upper.xRot      += idleR  * 0.03F;
        this.leftArm.upper.zRot       -= idleL  * 0.05F;
        this.leftArm.upper.xRot       += idleL  * 0.03F;
        this.lowerRightArm.upper.zRot += idleLR * 0.04F;
        this.lowerRightArm.upper.xRot += idleLR * 0.025F;
        this.lowerLeftArm.upper.zRot  -= idleLL * 0.04F;
        this.lowerLeftArm.upper.xRot  += idleLL * 0.025F;

        // Walk cycle (digitigrade alternation).
        float swingSpeed  = 0.3F;
        float swingAmount = state.walkAnimationSpeed;
        float wp = state.walkAnimationPos;
        float legA = ( Mth.sin((wp - 0.8F) * swingSpeed) + 0.8F) * swingAmount;
        float legB = (-(Mth.sin((wp + 0.8F) * swingSpeed) - 0.8F)) * swingAmount;
        float shinA = ( Mth.sin((wp + 0.8F) * swingSpeed) - 0.8F) * swingAmount;
        float shinB = (-(Mth.sin((wp - 0.8F) * swingSpeed) + 0.8F)) * swingAmount;
        float bob   = Math.abs(Mth.sin(wp * swingSpeed) * swingAmount);
        this.pelvis.y -= bob;
        this.legJoint1.xRot += legA  * 0.6F;
        this.legJoint2.xRot += legB  * 0.6F;
        this.shin1.xRot     += shinA * 0.3F;
        this.shin2.xRot     += shinB * 0.3F;

        // Walk arm swing — 4 arms phase-shifted by π/2 around the walk cycle so each
        // peaks at a different stride moment (rolling-gait look, not paired mirror).
        float walkPhase = wp * swingSpeed;
        float swingR  = Mth.sin(walkPhase)               * swingAmount;
        float swingL  = Mth.sin(walkPhase + Mth.PI)      * swingAmount;
        float swingLR = Mth.sin(walkPhase + Mth.HALF_PI) * swingAmount;
        float swingLL = Mth.sin(walkPhase + 3F * Mth.HALF_PI) * swingAmount;
        this.rightArm.upper.xRot      -= swingR  * 0.6F;
        this.leftArm.upper.xRot       -= swingL  * 0.6F;
        this.lowerRightArm.upper.xRot -= swingLR * 0.4F;
        this.lowerLeftArm.upper.xRot  -= swingLL * 0.4F;

        // Enrage: sit up, mouth open, arms flailing — each arm with its own phase
        // and frequency so the motion reads as chaotic, not in lockstep.
        if (state.isEnraged) {
            float flailR  = Mth.sin(t * 0.30F);
            float flailL  = Mth.sin(t * 0.27F + 2.1F);
            float flailLR = Mth.sin(t * 0.33F + 3.7F);
            float flailLL = Mth.sin(t * 0.29F + 5.2F);
            this.abdomen.xRot -= 0.20F;
            this.chest.xRot   -= 0.25F;
            this.head.xRot    += 0.30F;
            this.jaw.xRot     += 0.45F + Mth.sin(t * 0.4F) * 0.05F;
            this.rightArm.upper.zRot      += 0.35F + flailR  * 0.20F;
            this.rightArm.upper.xRot      += flailR  * 0.15F;
            this.leftArm.upper.zRot       -= 0.35F + flailL  * 0.20F;
            this.leftArm.upper.xRot       += flailL  * 0.15F;
            this.lowerRightArm.upper.zRot += 0.25F + flailLR * 0.20F;
            this.lowerRightArm.upper.xRot += flailLR * 0.15F;
            this.lowerLeftArm.upper.zRot  -= 0.25F + flailLL * 0.20F;
            this.lowerLeftArm.upper.xRot  += flailLL * 0.15F;
        }
    }

    /** Three-segment arm: upper, forearm, hand. */
    static class Arm {
        final ModelPart upper;
        final ModelPart foreArm;
        final ModelPart hand;

        Arm(ModelPart chest, String prefix) {
            this.upper   = chest.getChild(prefix + "_arm");
            this.foreArm = this.upper.getChild("fore_arm");
            this.hand    = this.foreArm.getChild("hand");
        }

        void rest(boolean right) {
            this.upper.xRot   = -0.5235988F;
            this.upper.yRot   = 0F;
            this.upper.zRot   = right ?  0.5235988F : -0.5235988F;
            this.foreArm.xRot = -0.62831855F;
            this.foreArm.yRot = 0F;
            this.foreArm.zRot = 0F;
            this.hand.xRot    = 0F;
            this.hand.yRot    = right ? -0.3926991F : 0.3926991F;
            this.hand.zRot    = 0F;
        }

        static void create(PartDefinition chest, String prefix, float xOffset, boolean mirror) {
            boolean lower = prefix.startsWith("lower_");
            // Upper arm 3x22x3 unwrap = 12x25.  Forearm 3x18x3 unwrap = 12x21.  Hand 4x4x4 unwrap = 16x8.
            // All four arms share these texOffs (no per-arm asymmetry in UV).
            PartDefinition upper = chest.addOrReplaceChild(prefix + "_arm",
                CubeListBuilder.create()
                    .texOffs(108, 0)
                    .mirror(mirror)
                    .addBox(-1.5F, lower ? 6F : 0F, -1.5F, 3F, 22F, 3F, new CubeDeformation(0.1F)),
                PartPose.offset(xOffset, -14F, 0F));
            PartDefinition foreArm = upper.addOrReplaceChild("fore_arm",
                CubeListBuilder.create()
                    .texOffs(108, 25)
                    .mirror(mirror)
                    .addBox(-1.5F, 0F, -1.5F, 3F, 18F, 3F, NONE),
                PartPose.offset(0F, 21F, 1F));
            foreArm.addOrReplaceChild("hand",
                CubeListBuilder.create()
                    .texOffs(108, 46)
                    .mirror(mirror)
                    .addBox(-2F, 0F, -2F, 4F, 4F, 4F, NONE),
                PartPose.offset(0F, 17.5F, 0F));
        }
    }
}
