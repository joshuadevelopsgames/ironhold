package kingdom.smp.client.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Filcher geometry — transcribed from scratch/blockbench/filcher.geo.bbmodel
 * (Bedrock identifier: geometry.ironhold:filcher; texture 64×64).
 *
 * <p>Crown is a child of head, toggled visible only when the filcher is pack king.
 * The crown is a detailed 15-cube model (rim + spikes + gem accents); the two rotated
 * rim cubes are implemented as sub-bones with PartPose rotation so the UV faces stay
 * correct after the -90° Y rotation around the entity origin.
 */
public class FilcherModel extends ZombieModel<FilcherRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath("ironhold", "filcher"), "main");

    /** Field is named "crown" but the underlying bone keeps vanilla's name "hat" so that
     * HumanoidModel.<init> (zombie super) can find it via root.getChild("head").getChild("hat"). */
    private final ModelPart crown;

    public FilcherModel(ModelPart root) {
        super(root);
        this.crown = root.getChild("head").getChild("hat");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── Head (pivot Bedrock [0, 9, 0] → Java offset (0, 15, 0)) ──────────
        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 13).addBox(-3.0F, -6.0F, -3.0F, 6, 6, 6, CubeDeformation.NONE)
                .texOffs(16, 25).addBox(2.0F, -6.0F, -4.0F, 1, 6, 1, CubeDeformation.NONE)
                .texOffs(20, 25).addBox(-3.0F, -6.0F, -4.0F, 1, 6, 1, CubeDeformation.NONE),
            PartPose.offset(0.0F, 15.0F, 0.0F));

        // Horn — 1×4×1 with 90° Z rotation around head pivot.
        head.addOrReplaceChild("horn",
            CubeListBuilder.create()
                .texOffs(30, 30).addBox(-6.0F, -2.0F, -4.0F, 1, 4, 1, CubeDeformation.NONE),
            PartPose.rotation(0.0F, 0.0F, (float)(Math.PI / 2)));

        // ── Crown (15 cubes; child of head, same pivot) ──────────────────────
        // Bone name must remain "hat" so vanilla HumanoidModel.<init> (zombie super)
        // finds it. We just put the new crown geometry there instead of an inflated overlay.
        // y_java = 9 - (bedrock_y + size_y); x_java/z_java = bedrock_x/z (head pivot at 0,_,0).
        PartDefinition crown = head.addOrReplaceChild("hat",
            CubeListBuilder.create()
                // Front + back rims (non-rotated).
                .texOffs(24, 18).addBox(-3.0F, -7.0F, -4.0F, 6, 1, 1, CubeDeformation.NONE)
                .texOffs(24, 20).addBox(-3.0F, -7.0F,  3.0F, 6, 1, 1, CubeDeformation.NONE)
                // Front-center tall spike.
                .texOffs(24, 26).addBox(-1.0F, -10.0F, -4.0F, 2, 3, 1, CubeDeformation.NONE)
                // Side spikes (right at +X, left at -X).
                .texOffs(24, 30).addBox( 3.0F, -9.0F, -1.0F, 1, 2, 2, CubeDeformation.NONE)
                .texOffs(30, 26).addBox(-4.0F, -9.0F, -1.0F, 1, 2, 2, CubeDeformation.NONE)
                // Front-flanking small spikes.
                .texOffs(0, 31).addBox(-2.0F, -9.0F, -3.0F, 1, 2, 1, CubeDeformation.NONE)
                .texOffs(4, 31).addBox( 1.0F, -9.0F, -3.0F, 1, 2, 1, CubeDeformation.NONE)
                // Corner gems (4 around the rim + 4 at the corners).
                .texOffs(8, 31).addBox( 2.0F, -8.0F, -3.0F, 1, 1, 1, CubeDeformation.NONE)
                .texOffs(32, 0).addBox( 3.0F, -8.0F, -2.0F, 1, 1, 1, CubeDeformation.NONE)
                .texOffs(12, 31).addBox(-3.0F, -8.0F, -3.0F, 1, 1, 1, CubeDeformation.NONE)
                .texOffs(32, 2).addBox(-4.0F, -8.0F, -2.0F, 1, 1, 1, CubeDeformation.NONE)
                .texOffs(32, 4).addBox(-4.0F, -8.0F,  1.0F, 1, 1, 1, CubeDeformation.NONE)
                .texOffs(32, 6).addBox( 3.0F, -8.0F,  1.0F, 1, 1, 1, CubeDeformation.NONE),
            PartPose.ZERO);

        // Rotated rim cubes — Bedrock specifies them with rotation Y=-90° around
        // pivot (0, 0, 0) (entity origin). Express as sub-bones whose pivot is at
        // the entity origin (offset (0, 9, 0) from crown's pivot in Java terms),
        // then add the cubes in pre-rotation orientation.
        crown.addOrReplaceChild("crown_rim_right",
            CubeListBuilder.create()
                .texOffs(24, 22).addBox(-3.0F, -16.0F,  3.0F, 6, 1, 1, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 9.0F, 0.0F, 0.0F, (float)(-Math.PI / 2), 0.0F));
        crown.addOrReplaceChild("crown_rim_left",
            CubeListBuilder.create()
                .texOffs(24, 24).addBox(-3.0F, -16.0F, -4.0F, 6, 1, 1, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 9.0F, 0.0F, 0.0F, (float)(-Math.PI / 2), 0.0F));

        // ── Body (3 cubes: main + 2 tiny decoration nubs) ───────────────────
        root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8, 9, 4, CubeDeformation.NONE)
                .texOffs(17, 5).addBox(-3.0F, 0.0F, -3.0F, 1, 1, 1, CubeDeformation.NONE)
                .texOffs(14, 1).addBox( 2.0F, 0.0F, -3.0F, 1, 1, 1, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 15.0F, 0.0F, 0.08F, 0.0F, 0.0F));

        // ── Arms (both use shared UV (16, 40)) ──────────────────────────────
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
                .texOffs(16, 40).addBox(0.0F, -1.0F, -1.0F, 2, 7, 2, CubeDeformation.NONE),
            PartPose.offsetAndRotation(4.0F, 16.0F, 0.0F, 0.15F, 0.0F, 0.05F));

        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
                .texOffs(16, 40).addBox(-2.0F, -1.0F, -1.0F, 2, 7, 2, CubeDeformation.NONE),
            PartPose.offsetAndRotation(-4.0F, 16.0F, 0.0F, 0.15F, 0.0F, -0.05F));

        // ── Legs ────────────────────────────────────────────────────────────
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
                .texOffs(0, 25).addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2, CubeDeformation.NONE),
            PartPose.offset(0.5F, 20.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
                .texOffs(8, 25).addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2, CubeDeformation.NONE),
            PartPose.offset(-0.5F, 20.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    // ── Animation ─────────────────────────────────────────────────────────────

    @Override
    public void setupAnim(FilcherRenderState state) {
        super.setupAnim(state);

        // Crown bone visible only for king filchers — all 15 cubes (incl. rotated sub-bone
        // rims) inherit visibility from the crown ModelPart.
        crown.visible = state.isKing;

        float t         = state.ageInTicks;
        float walkSpeed = state.walkAnimationSpeed;
        boolean moving  = walkSpeed > 0.05F;

        if (state.isStalking) {
            animateStalking(t, moving);
        } else if (state.isShowingOff) {
            animateShowOff(t);
        } else {
            animateIdle(t, moving);
        }
    }

    private void animateIdle(float t, boolean moving) {
        head.zRot += Mth.sin(t * 0.04F) * 0.12F;
        head.y += Mth.sin(t * 0.07F) * 0.25F;
        rightArm.zRot -= Mth.sin(t * 0.05F + 0.8F) * 0.06F;
        leftArm.zRot  += Mth.sin(t * 0.05F) * 0.06F;

        if (moving) {
            rightLeg.xRot *= 0.55F;
            leftLeg.xRot  *= 0.55F;
            body.y += Math.abs(Mth.sin(t * 0.3F)) * 0.4F;
        }
    }

    private void animateStalking(float t, boolean moving) {
        body.xRot += 0.14F;
        head.xRot -= 0.10F;
        head.zRot += Mth.sin(t * 0.12F) * 0.04F;
        rightArm.xRot -= 0.12F;
        leftArm.xRot  -= 0.12F;

        if (moving) {
            rightLeg.xRot *= 0.35F;
            leftLeg.xRot  *= 0.35F;
        }
    }

    private void animateShowOff(float t) {
        head.y     += Mth.sin(t * 0.25F) * 0.6F;
        head.zRot  += Mth.sin(t * 0.20F) * 0.18F;
        rightArm.xRot -= 0.35F;
        leftArm.xRot  -= 0.35F;
        rightArm.zRot -= 0.20F;
        leftArm.zRot  += 0.20F;
        body.zRot += Mth.sin(t * 0.18F) * 0.06F;
    }
}
