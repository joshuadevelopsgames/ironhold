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
 * Shulker Herder — robed humanoid with a twin-horned cloth hood.
 *
 * <p>Silhouette breakdown (matches the End Village design sheet):
 * <ul>
 *   <li>Cloth hood wrapping the head, with a brass forehead band and a
 *       dark veil covering the face (two glowing eye slits)</li>
 *   <li>Two angled horn-like spikes rising from the front-top corners
 *       of the hood — the trademark "shulker antenna" silhouette</li>
 *   <li>Long cream tunic that hangs all the way to the boots</li>
 *   <li>Open purple cloak draped over the shoulders + back; tunic shows
 *       through the front gap</li>
 *   <li>Standard humanoid arms with brass cuffs</li>
 *   <li>Cream leggings visible below the cloak hem; dark boots at the bottom</li>
 * </ul>
 *
 * <p>Coordinate convention follows vanilla {@code IllagerModel}: head and
 * body share pivot (0,0,0) at upper-chest level; head extends up (-Y),
 * body and cloak extend down (+Y). Arms and legs hang from their own pivots.
 */
public class ShulkerHerderModel extends EntityModel<ShulkerHerderRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "shulker_herder"), "main");

    private final ModelPart head;
    private final ModelPart hood;
    private final ModelPart body;
    private final ModelPart cloak;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public ShulkerHerderModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.hood = this.head.getChild("hood");
        this.body = root.getChild("body");
        this.cloak = this.body.getChild("cloak");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── Head ─────────────────────────────────────────────────────────────
        // Standard 8x8x8 head — UV (0,0).
        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, CubeDeformation.NONE),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // Cloth hood wrap covering the head — slight inflation (0.5).
        // UV (32,0) — same layout as vanilla illager's hat overlay.
        PartDefinition hood = head.addOrReplaceChild("hood",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, new CubeDeformation(0.5F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // ── Twin front horns ─────────────────────────────────────────────────
        // Two thin spikes rising from the front-top corners of the hood,
        // angled slightly outward (~12°). Together they form the twin-peak
        // silhouette from the reference. Each is 2x5x2.
        // Right horn — UV (32,32). Origin x=-4.5 places it flush with the
        // right-front corner of the hood; rotation tilts the tip outward.
        hood.addOrReplaceChild("horn_right",
            CubeListBuilder.create()
                .texOffs(32, 32)
                .addBox(-1.0F, -5.0F, -1.0F, 2, 5, 2, CubeDeformation.NONE),
            PartPose.offsetAndRotation(-3.0F, -8.0F, -3.0F, 0.05F, 0.0F, -0.22F));

        // Left horn — mirrored to the left-front corner. UV (44,32).
        hood.addOrReplaceChild("horn_left",
            CubeListBuilder.create()
                .texOffs(44, 32)
                .mirror()
                .addBox(-1.0F, -5.0F, -1.0F, 2, 5, 2, CubeDeformation.NONE),
            PartPose.offsetAndRotation(3.0F, -8.0F, -3.0F, 0.05F, 0.0F, 0.22F));

        // Forehead band — thin 8x1x8 hoop wrapping the head at brow level.
        // Brass strap from the reference. UV (24,40), inflated 0.6 so it
        // sits proud of the hood.
        hood.addOrReplaceChild("hood_band",
            CubeListBuilder.create()
                .texOffs(24, 40)
                .addBox(-4.0F, -2.0F, -4.0F, 8, 1, 8, new CubeDeformation(0.6F)),
            PartPose.ZERO);

        // Veil — flat purple sheet covering the face, two glowing eye slits
        // painted on. Pinned just in front of the bare head's front face.
        // 7x5 thin plane. UV (52,32).
        hood.addOrReplaceChild("veil",
            CubeListBuilder.create()
                .texOffs(52, 32)
                .addBox(-3.5F, -3.0F, -4.6F, 7, 5, 0, CubeDeformation.NONE),
            PartPose.ZERO);

        // ── Body ─────────────────────────────────────────────────────────────
        // Torso — 8x12x4. Texture front face shows the cream tunic; back
        // face shows the purple cloak's interior. UV (16,16).
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(16, 16)
                .addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, CubeDeformation.NONE),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // ── Cloak ────────────────────────────────────────────────────────────
        // Purple cloak draped over the shoulders and hanging down the back.
        // Open in front so the cream tunic shows through.
        //   - shoulder yoke: 10x3x6 — wraps the upper chest/back/sides
        //   - back panel:    8x9x1 — flat sheet hanging behind the torso
        // Front of torso intentionally uncovered.
        PartDefinition cloak = body.addOrReplaceChild("cloak",
            CubeListBuilder.create()
                // Shoulder yoke. UV (16,52).
                .texOffs(16, 52)
                .addBox(-5.0F, 0.0F, -3.0F, 10, 3, 6, new CubeDeformation(0.25F))
                // Back panel — sits just behind the torso, drops to the hem.
                // UV (0,52).
                .texOffs(0, 52)
                .addBox(-4.0F, 3.0F, 1.8F, 8, 9, 1, CubeDeformation.NONE),
            PartPose.ZERO);

        // ── Arms ─────────────────────────────────────────────────────────────
        // Each arm is a stack of three pieces:
        //   1. Upper sleeve   — 4x9x4  (purple cloth)
        //   2. Sleeve flare   — 5x2x5  (drapes wider just above the cuff)
        //   3. Brass cuff     — 4x2x4 inflated 0.5 (chunky 3D band, ~5x3x5
        //                       visually — proud of the sleeve like a vambrace)
        // The cuff is its own cube, NOT a painted stripe — the silhouette
        // reads as bracelet-thickness even from a distance.
        //
        // Right arm — UV (40,16) for the sleeve.
        PartDefinition rightArm = root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
                .texOffs(40, 16)
                .addBox(-3.0F, -2.0F, -2.0F, 4, 9, 4, CubeDeformation.NONE),
            PartPose.offset(-5.0F, 2.0F, 0.0F));

        // Right sleeve flare — UV (16,38).
        rightArm.addOrReplaceChild("right_sleeve_flare",
            CubeListBuilder.create()
                .texOffs(16, 38)
                .addBox(-3.5F, 7.0F, -2.5F, 5, 2, 5, CubeDeformation.NONE),
            PartPose.ZERO);

        // Right cuff — UV (48,52). Inflated 0.5 → ~5x3x5 visual.
        rightArm.addOrReplaceChild("right_cuff",
            CubeListBuilder.create()
                .texOffs(48, 52)
                .addBox(-3.0F, 9.0F, -2.0F, 4, 2, 4, new CubeDeformation(0.5F)),
            PartPose.ZERO);

        // Left arm — UV (40,32) mirrored.
        PartDefinition leftArm = root.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
                .texOffs(40, 32)
                .mirror()
                .addBox(-1.0F, -2.0F, -2.0F, 4, 9, 4, CubeDeformation.NONE),
            PartPose.offset(5.0F, 2.0F, 0.0F));

        // Left sleeve flare — UV (36,38) mirrored.
        leftArm.addOrReplaceChild("left_sleeve_flare",
            CubeListBuilder.create()
                .texOffs(36, 38)
                .mirror()
                .addBox(-1.5F, 7.0F, -2.5F, 5, 2, 5, CubeDeformation.NONE),
            PartPose.ZERO);

        // Left cuff — UV (48,58) mirrored.
        leftArm.addOrReplaceChild("left_cuff",
            CubeListBuilder.create()
                .texOffs(48, 58)
                .mirror()
                .addBox(-1.0F, 9.0F, -2.0F, 4, 2, 4, new CubeDeformation(0.5F)),
            PartPose.ZERO);

        // ── Legs ─────────────────────────────────────────────────────────────
        // Cream tunic + leggings, dark boots at the bottom. Painted onto a
        // standard 4x12x4 leg cube.
        // Right leg — UV (0,16).
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, CubeDeformation.NONE),
            PartPose.offset(-2.0F, 12.0F, 0.0F));

        // Left leg — UV (0,32) mirrored.
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
                .texOffs(0, 32)
                .mirror()
                .addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, CubeDeformation.NONE),
            PartPose.offset(2.0F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(ShulkerHerderRenderState state) {
        super.setupAnim(state);

        float t = state.ageInTicks;
        float walkSpeed = state.walkAnimationSpeed;
        float walkPos = state.walkAnimationPos;

        // Head tracks the look target.
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;

        // Walking arm/leg swing — standard humanoid four-beat gait.
        float swing = Mth.cos(walkPos * 0.6662F) * 1.4F * walkSpeed;
        this.rightLeg.xRot = swing;
        this.leftLeg.xRot = -swing;

        if (state.isAggressive) {
            // Defending: arms raised, ready to strike with the staff.
            this.rightArm.xRot = -0.9F + Mth.cos(t * 0.12F) * 0.1F;
            this.leftArm.xRot  = -0.4F;
            this.rightArm.zRot = -0.15F;
            this.leftArm.zRot  =  0.15F;
        } else {
            // Calm idle — gentle arm sway, slight hand-on-staff pose on right.
            float idleSway = Mth.sin(t * 0.05F) * 0.08F;
            this.rightArm.xRot = -0.55F + idleSway;
            this.leftArm.xRot  = -0.05F + idleSway * 0.5F;
            this.rightArm.zRot = -0.08F;
            this.leftArm.zRot  =  0.08F + Mth.cos(t * 0.05F) * 0.04F;
        }

        // Subtle body tilt + cloak sway as the herder walks.
        this.body.zRot = Mth.sin(walkPos * 0.6662F) * 0.04F * walkSpeed;
        this.cloak.xRot = -Mth.abs(Mth.cos(walkPos * 0.6662F)) * 0.05F * walkSpeed;
    }
}
