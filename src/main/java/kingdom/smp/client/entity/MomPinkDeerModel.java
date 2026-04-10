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

/**
 * Dedicated model for the Mother Deer, exported from mom_pink_deer.bbmodel.
 *
 * <pre>
 * Texture: 64x64, box_uv
 *
 *   Body:    9×7.5×15   uv(0,0)   — large torso
 *   Neck:    4.5×7.5×4.5 uv(32,0) — thick upright neck
 *   Head:    6×6×7.5    uv(0,16)  — broad head
 *   Nose:    3×3×4.5    uv(18,16) — snout
 *   Ears:    1.5×4.5×3  uv(0,26)/(6,26) — tall pointed ears (×2)
 *   Legs:    3×12×3     uv(32-56,8)     — sturdy legs (×4)
 *   Tail:    1.5×4.5×1.5 uv(44,0) — short upward tail
 * </pre>
 */
public class MomPinkDeerModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath("ironhold", "mom_pink_deer"), "main");

    public static final ModelLayerLocation BABY_LAYER = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath("ironhold", "mom_pink_deer"), "baby");

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

    public MomPinkDeerModel(ModelPart root) {
        super(root);
        this.body          = root.getChild("body");
        this.neck          = body.getChild("neck");
        this.head          = neck.getChild("head");
        this.rightEar      = head.getChild("right_ear");
        this.leftEar       = head.getChild("left_ear");
        this.tail          = body.getChild("tail");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg  = root.getChild("left_front_leg");
        this.rightBackLeg  = root.getChild("right_back_leg");
        this.leftBackLeg   = root.getChild("left_back_leg");
    }

    public static LayerDefinition createBodyLayer() {
        return createLayer(1.0F);
    }

    public static LayerDefinition createBabyLayer() {
        return createLayer(0.7F);
    }

    /**
     * Geometry converted from mom_pink_deer.bbmodel (Blockbench Java Block, box_uv).
     * Coordinate conversion: entity pivot Y = 24 - bb origin Y;
     * cube offset Y = -(bb_to_y - bb_origin_y); rotation degrees → radians.
     */
    private static LayerDefinition createLayer(float scale) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Geometry from "Mother Deer COMPLETED - Converted.geo.json"
        // UV offsets from the geo.json. Non-integer dimensions FLOOR'd to match
        // Blockbench's UV pixel allocation (floor'd integers = zero UV overlaps,
        // zero transparent pixel sampling). 3D shape differs by at most 0.5px.

        // Body: 9×7×15 uv(0,0). Pivot at y=8.5 so bottom (y=12) meets leg tops.
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.5F, -3.5F, -7.5F, 9, 7, 15, CubeDeformation.NONE),
            PartPose.offset(0.0F, 8.5F, 0.0F));

        // Neck: 4×7×4 uv(26,22)
        PartDefinition neck = body.addOrReplaceChild("neck",
            CubeListBuilder.create()
                .texOffs(26, 22)
                .addBox(-2.0F, -7.0F, -2.0F, 4, 7, 4, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, -2.0F, -7.0F,
                -0.2F, 0.0F, 0.0F));

        // Head: 6×6×7 uv(0,22)
        PartDefinition head = neck.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 22)
                .addBox(-3.0F, -4.0F, -6.0F, 6, 6, 7, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F,
                0.15F, 0.0F, 0.0F));

        // Nose: 3×3×4 uv(42,22)
        head.addOrReplaceChild("nose",
            CubeListBuilder.create()
                .texOffs(42, 22)
                .addBox(-1.5F, -1.5F, -4.0F, 3, 3, 4, CubeDeformation.NONE),
            PartPose.offset(0.0F, -1.0F, -6.0F));

        // Right ear: 1×4×3 uv(48,0)
        head.addOrReplaceChild("right_ear",
            CubeListBuilder.create()
                .texOffs(48, 0)
                .addBox(-2.0F, -4.0F, 0.0F, 1, 4, 3, CubeDeformation.NONE),
            PartPose.offsetAndRotation(-2.0F, -4.0F, -1.5F,
                0.0F, 0.0F, -0.35F));

        // Left ear: 1×4×3 uv(48,7)
        head.addOrReplaceChild("left_ear",
            CubeListBuilder.create()
                .texOffs(48, 7)
                .addBox(1.0F, -4.0F, 0.0F, 1, 4, 3, CubeDeformation.NONE),
            PartPose.offsetAndRotation(2.0F, -4.0F, -1.5F,
                0.0F, 0.0F, 0.35F));

        // Tail: 1×4×1 uv(48,14)
        body.addOrReplaceChild("tail",
            CubeListBuilder.create()
                .texOffs(48, 14)
                .addBox(-0.5F, -4.0F, 0.0F, 1, 4, 1, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, -1.0F, 7.5F,
                0.6F, 0.0F, 0.0F));

        // Legs: 3×12×3 (already integers)
        root.addOrReplaceChild("right_front_leg",
            CubeListBuilder.create()
                .texOffs(26, 33)
                .addBox(-1.5F, 0.0F, -1.5F, 3, 12, 3, CubeDeformation.NONE),
            PartPose.offset(-3.0F, 12.0F, -5.0F));

        root.addOrReplaceChild("left_front_leg",
            CubeListBuilder.create()
                .texOffs(0, 35)
                .addBox(-1.5F, 0.0F, -1.5F, 3, 12, 3, CubeDeformation.NONE),
            PartPose.offset(3.0F, 12.0F, -5.0F));

        root.addOrReplaceChild("right_back_leg",
            CubeListBuilder.create()
                .texOffs(12, 35)
                .addBox(-1.5F, 0.0F, -1.5F, 3, 12, 3, CubeDeformation.NONE),
            PartPose.offset(-3.0F, 12.0F, 5.0F));

        root.addOrReplaceChild("left_back_leg",
            CubeListBuilder.create()
                .texOffs(38, 33)
                .addBox(-1.5F, 0.0F, -1.5F, 3, 12, 3, CubeDeformation.NONE),
            PartPose.offset(3.0F, 12.0F, 5.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);

        float walkSpeed = state.walkAnimationSpeed;
        float walkPos   = state.walkAnimationPos;
        float t         = state.ageInTicks;

        boolean grazing = state instanceof DeerRenderState drs && drs.isGrazing;
        if (grazing) {
            float raw   = (Mth.sin(t * 0.065F) + 1.0F) * 0.5F;
            float phase = raw * raw * (3.0F - 2.0F * raw);

            neck.xRot = -0.2F + phase * 1.35F;
            float nibble = phase > 0.7F ? Mth.sin(t * 0.45F) * 0.04F : 0F;
            head.xRot = 0.15F + nibble;
            rightFrontLeg.xRot = 0F;
            leftFrontLeg.xRot  = 0F;
            rightBackLeg.xRot  = 0F;
            leftBackLeg.xRot   = 0F;
        } else {
            float legSwing = Mth.cos(walkPos * 0.6662F) * 1.2F * walkSpeed;
            rightFrontLeg.xRot =  legSwing;
            leftFrontLeg.xRot  = -legSwing;
            rightBackLeg.xRot  = -legSwing;
            leftBackLeg.xRot   =  legSwing;

            head.xRot = 0.15F + Mth.sin(walkPos * 0.6662F) * 0.08F * walkSpeed;
        }

        // Ears
        if (grazing) {
            float gPerk = Mth.sin(t * 0.025F) * 0.12F;
            rightEar.zRot = -0.15F + gPerk;
            leftEar.zRot  =  0.15F - gPerk;
            rightEar.yRot = -0.2F;
            leftEar.yRot  =  0.2F;
        } else if (walkSpeed < 0.1F) {
            float rScan   = Mth.sin(t * 0.025F) * 0.45F;
            float rBeat   = Mth.sin(t * 0.31F) * Mth.sin(t * 0.17F);
            float rTwitch = rBeat > 0.65F ? (rBeat - 0.65F) * 2.2F : 0F;
            float rPerk   = Math.max(0F, Mth.sin(t * 0.051F) * Mth.sin(t * 0.023F));
            rightEar.yRot = rScan + rTwitch * 0.3F;
            rightEar.zRot = -0.35F + rPerk * 0.32F;

            float lScan   = Mth.sin(t * 0.028F + 1.8F) * 0.45F;
            float lBeat   = Mth.sin(t * 0.27F) * Mth.sin(t * 0.19F);
            float lTwitch = lBeat > 0.65F ? (lBeat - 0.65F) * 2.2F : 0F;
            float lPerk   = Math.max(0F, Mth.sin(t * 0.047F + 0.9F) * Mth.sin(t * 0.021F + 0.5F));
            leftEar.yRot = -(lScan + lTwitch * 0.3F);
            leftEar.zRot =  0.35F - lPerk * 0.32F;
        } else {
            float trail = Mth.clamp(walkSpeed * 0.13F, 0F, 0.18F);
            rightEar.zRot = -0.35F - trail;
            leftEar.zRot  =  0.35F + trail;
            rightEar.yRot = 0F;
            leftEar.yRot  = 0F;
        }

        // Tail
        float tailBeat = Mth.sin(t * 0.041F) * Mth.sin(t * 0.113F);
        if (tailBeat > 0.72F) {
            float flip = (tailBeat - 0.72F) / 0.28F;
            tail.xRot = 0.6F - flip * 0.95F;
            tail.yRot = Mth.sin(t * 0.38F) * 0.3F;
        } else {
            tail.xRot = 0.6F + Mth.sin(t * 0.1F) * 0.06F;
            tail.yRot = Mth.sin(t * 0.1F) * 0.15F;
        }
    }
}
