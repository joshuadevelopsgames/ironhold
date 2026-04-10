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
 * King Filcher model — converted from "Flitcher model (1).json" (Bedrock geo).
 * Same base geometry as {@link FilcherModel} but includes a crown hat overlay
 * as a built-in part of the head, with the filcher_with_crown.png texture.
 */
public class FilcherKingModel extends ZombieModel<FilcherRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath("ironhold", "filcher_king"), "main");

    public FilcherKingModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head: pivot(0, 9, 0) → entity offset(0, 15, 0)
        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                // Main head: 6x6x6 uv(0,13)
                .texOffs(0, 13)
                .addBox(-3.0F, -6.0F, -3.0F, 6, 6, 6, CubeDeformation.NONE)
                // Right ear nub: 1x6x1 uv(16,25)
                .texOffs(16, 25)
                .addBox(2.0F, -6.0F, -4.0F, 1, 6, 1, CubeDeformation.NONE)
                // Left ear nub: 1x6x1 uv(20,25)
                .texOffs(20, 25)
                .addBox(-3.0F, -6.0F, -4.0F, 1, 6, 1, CubeDeformation.NONE),
            PartPose.offset(0.0F, 15.0F, 0.0F));

        // Horn: 1x4x1 rotated 90deg Z — uv(30,30)
        head.addOrReplaceChild("horn",
            CubeListBuilder.create()
                .texOffs(30, 30)
                .addBox(-6.0F, -2.0F, -4.0F, 1, 4, 1, CubeDeformation.NONE),
            PartPose.rotation(0.0F, 0.0F, (float)(Math.PI / 2)));

        // Empty hat — ZombieModel expects this child to exist
        head.addOrReplaceChild("hat",
            CubeListBuilder.create(),
            PartPose.ZERO);

        // Body: pivot(0, 9, 0), rot(-4.58deg → 0.08 rad)
        root.addOrReplaceChild("body",
            CubeListBuilder.create()
                // Main body: 8x9x4 uv(0,0)
                .texOffs(0, 0)
                .addBox(-4.0F, 0.0F, -2.0F, 8, 9, 4, CubeDeformation.NONE)
                // Left shoulder: 1x1x1 uv(32,8)
                .texOffs(32, 8)
                .addBox(-3.0F, 0.0F, -3.0F, 1, 1, 1, CubeDeformation.NONE)
                // Right shoulder: 1x1x1 uv(32,10)
                .texOffs(32, 10)
                .addBox(2.0F, 0.0F, -3.0F, 1, 1, 1, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 15.0F, 0.0F, 0.08F, 0.0F, 0.0F));

        // Right arm: pivot(4, 8, 0), rot(-8.59deg, 0, 2.86deg)
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
                .texOffs(24, 0)
                .addBox(-2.0F, -1.0F, -1.0F, 2, 7, 2, CubeDeformation.NONE),
            PartPose.offsetAndRotation(-4.0F, 16.0F, 0.0F, 0.15F, 0.0F, 0.05F));

        // Left arm: pivot(-4, 8, 0), rot(-8.59deg, 0, -2.86deg)
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
                .texOffs(24, 9)
                .addBox(0.0F, -1.0F, -1.0F, 2, 7, 2, CubeDeformation.NONE),
            PartPose.offsetAndRotation(4.0F, 16.0F, 0.0F, 0.15F, 0.0F, -0.05F));

        // Right leg
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
                .texOffs(0, 25)
                .addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2, CubeDeformation.NONE),
            PartPose.offset(-0.5F, 20.0F, 0.0F));

        // Left leg
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
                .texOffs(8, 25)
                .addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2, CubeDeformation.NONE),
            PartPose.offset(0.5F, 20.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(FilcherRenderState state) {
        super.setupAnim(state);

        float t         = state.ageInTicks;
        float walkSpeed = state.walkAnimationSpeed;
        boolean moving  = walkSpeed > 0.05F;

        if (state.isStalking) {
            body.xRot += 0.14F;
            head.xRot -= 0.10F;
            head.zRot += Mth.sin(t * 0.12F) * 0.04F;
            rightArm.xRot -= 0.12F;
            leftArm.xRot  -= 0.12F;
            if (moving) {
                rightLeg.xRot *= 0.35F;
                leftLeg.xRot  *= 0.35F;
            }
        } else if (state.isShowingOff) {
            head.y     += Mth.sin(t * 0.25F) * 0.6F;
            head.zRot  += Mth.sin(t * 0.20F) * 0.18F;
            rightArm.xRot -= 0.35F;
            leftArm.xRot  -= 0.35F;
            rightArm.zRot -= 0.20F;
            leftArm.zRot  += 0.20F;
            body.zRot += Mth.sin(t * 0.18F) * 0.06F;
        } else {
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
    }
}
