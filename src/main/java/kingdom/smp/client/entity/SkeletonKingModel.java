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

/**
 * Skeleton King — an armoured undead monarch (the soul-fire lich from the
 * design render): bare skull face with glowing eyes, a jagged golden crown,
 * dark plate armour over a deep-red robe, angular pauldrons, a long tattered
 * cape, and a floor-length robe skirt. The sword is a held item supplied by
 * the renderer, not part of this mesh.
 *
 * <p>Silhouette breakdown (matches the reference render):
 * <ul>
 *   <li>Standard 8×8×8 skull head; the glowing eyes / soul-fire are an
 *       emissive texture concern, not geometry.</li>
 *   <li>Jagged golden crown — a rim ring topped with uneven spikes —
 *       parented to the head so it tracks head rotation.</li>
 *   <li>Plate cuirass torso with a red robe showing through; a front and
 *       back robe skirt hangs from the waist to the ankles (parented to the
 *       body like a vanilla evoker robe).</li>
 *   <li>Chunky angular pauldrons capping each shoulder (parented to the arms
 *       so they ride with the shoulder joint).</li>
 *   <li>Long tattered cape down the back, parented to the body and swaying
 *       with gait.</li>
 *   <li>Standard humanoid arms and legs.</li>
 * </ul>
 *
 * <p>Coordinate convention follows vanilla {@code HumanoidModel}: head and
 * body share pivot (0,0,0) at upper-chest level; head extends up (−Y), body
 * extends down (+Y). Arms and legs hang from their own pivots. Texture map is
 * 64×64.
 */
public class SkeletonKingModel extends EntityModel<SkeletonKingRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "skeleton_king"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart cape;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public SkeletonKingModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.cape = this.body.getChild("cape");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── Head ─────────────────────────────────────────────────────────────
        // Standard 8×8×8 skull. UV (0,0). Face / glowing eyes painted on; the
        // soul-fire wisps are an emissive texture layer, not geometry.
        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, CubeDeformation.NONE),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // ── Crown (child of head; jagged golden circlet) ─────────────────────
        // A rim ring resting on the crown of the head (top edge at y=−8), with
        // uneven spikes rising from it. All cubes inherit head rotation.
        PartDefinition crown = head.addOrReplaceChild("crown",
            CubeListBuilder.create()
                // Rim ring — four bars boxing the top of the head.
                .texOffs(32, 0).addBox(-4.0F, -9.0F, -4.5F, 8, 2, 1, CubeDeformation.NONE) // front
                .texOffs(32, 4).addBox(-4.0F, -9.0F,  3.5F, 8, 2, 1, CubeDeformation.NONE) // back
                .texOffs(32, 8).addBox(-4.5F, -9.0F, -4.0F, 1, 2, 8, CubeDeformation.NONE) // right
                .texOffs(52, 8).addBox( 3.5F, -9.0F, -4.0F, 1, 2, 8, CubeDeformation.NONE) // left
                // Front spikes — tall centre flanked by shorter points.
                .texOffs(52, 0).addBox(-1.0F, -13.0F, -4.5F, 2, 4, 1, CubeDeformation.NONE)
                .texOffs(60, 0).addBox(-3.5F, -12.0F, -4.5F, 1, 3, 1, CubeDeformation.NONE)
                .texOffs(66, 0).addBox( 2.5F, -12.0F, -4.5F, 1, 3, 1, CubeDeformation.NONE)
                // Side spikes — tall points at the left/right.
                .texOffs(72, 0).addBox( 3.5F, -13.0F, -1.0F, 1, 4, 2, CubeDeformation.NONE)
                .texOffs(80, 0).addBox(-4.5F, -13.0F, -1.0F, 1, 4, 2, CubeDeformation.NONE)
                // Back spikes — centre point flanked by shorter points.
                .texOffs(72, 8).addBox(-1.0F, -12.5F, 3.5F, 2, 3, 1, CubeDeformation.NONE)
                .texOffs(80, 8).addBox(-3.5F, -12.0F, 3.5F, 1, 3, 1, CubeDeformation.NONE)
                .texOffs(86, 8).addBox( 2.5F, -12.0F, 3.5F, 1, 3, 1, CubeDeformation.NONE),
            PartPose.ZERO);

        // ── Body ─────────────────────────────────────────────────────────────
        // Plate cuirass over a red robe — standard 8×12×4 torso. UV (16,16).
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(0, 20)
                .addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, CubeDeformation.NONE),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // Robe skirt — front and back panels hanging from the waist to the
        // ankles, parented to the body (evoker-style). Legs swing within.
        body.addOrReplaceChild("robe_front",
            CubeListBuilder.create()
                .texOffs(0, 54).addBox(-4.0F, 12.0F, -2.6F, 8, 11, 1, CubeDeformation.NONE),
            PartPose.ZERO);
        body.addOrReplaceChild("robe_back",
            CubeListBuilder.create()
                .texOffs(20, 54).addBox(-4.0F, 12.0F, 1.6F, 8, 11, 1, CubeDeformation.NONE),
            PartPose.ZERO);

        // ── Cape (child of body; long tattered drape) ────────────────────────
        // Pivots at the shoulders (top of body, behind the torso) and hangs to
        // the feet. Swings on xRot with gait. Tattered hem is a texture detail.
        PartDefinition cape = body.addOrReplaceChild("cape",
            CubeListBuilder.create()
                .texOffs(0, 70).addBox(-5.0F, 0.0F, 0.0F, 10, 24, 1, CubeDeformation.NONE),
            PartPose.offset(0.0F, 1.0F, 2.6F));

        // ── Arms ─────────────────────────────────────────────────────────────
        // Standard 4×12×4 armoured arms. Right UV (24,20); left UV (40,20).
        PartDefinition rightArm = root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
                .texOffs(24, 20)
                .addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, CubeDeformation.NONE),
            PartPose.offset(-5.0F, 2.0F, 0.0F));

        // Right pauldron — chunky angular shoulder cap, inflated so it reads
        // proud of the arm. UV (0,40).
        rightArm.addOrReplaceChild("right_pauldron",
            CubeListBuilder.create()
                .texOffs(0, 40)
                .addBox(-4.0F, -3.5F, -3.5F, 5, 4, 7, new CubeDeformation(0.25F)),
            PartPose.ZERO);

        PartDefinition leftArm = root.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
                .texOffs(40, 20)
                .addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, CubeDeformation.NONE),
            PartPose.offset(5.0F, 2.0F, 0.0F));

        // Left pauldron — UV (24,40).
        leftArm.addOrReplaceChild("left_pauldron",
            CubeListBuilder.create()
                .texOffs(24, 40)
                .addBox(-1.0F, -3.5F, -3.5F, 5, 4, 7, new CubeDeformation(0.25F)),
            PartPose.ZERO);

        // ── Legs ─────────────────────────────────────────────────────────────
        // Standard 4×12×4 armoured legs / boots. Right UV (56,20); left UV (72,20).
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
                .texOffs(56, 20)
                .addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, CubeDeformation.NONE),
            PartPose.offset(-2.0F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
                .texOffs(72, 20)
                .addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, CubeDeformation.NONE),
            PartPose.offset(2.0F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(SkeletonKingRenderState state) {
        super.setupAnim(state);

        float t = state.ageInTicks;
        float walkSpeed = state.walkAnimationSpeed;
        float walkPos = state.walkAnimationPos;

        // Head tracks the look target.
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;

        // Walking gait — standard humanoid four-beat swing.
        float swing = Mth.cos(walkPos * 0.6662F) * 1.4F * walkSpeed;
        this.rightLeg.xRot = swing;
        this.leftLeg.xRot = -swing;
        this.leftArm.xRot = swing;

        if (state.isAggressive) {
            // Combat — right arm hoists the sword forward and slightly up.
            this.rightArm.xRot = -1.4F + Mth.cos(t * 0.12F) * 0.08F;
            this.rightArm.zRot = -0.1F;
        } else {
            // Idle — sword arm rests low with a faint sway.
            this.rightArm.xRot = -swing + Mth.sin(t * 0.045F) * 0.05F;
            this.rightArm.zRot = -0.05F;
        }

        // Cape sway — leans back as the king strides, with a slow idle drift.
        this.cape.xRot = -0.06F
            - Mth.abs(Mth.cos(walkPos * 0.6662F)) * 0.18F * walkSpeed
            + Mth.sin(t * 0.04F) * 0.03F;

        // Subtle body tilt while walking.
        this.body.zRot = Mth.sin(walkPos * 0.6662F) * 0.03F * walkSpeed;
    }
}
