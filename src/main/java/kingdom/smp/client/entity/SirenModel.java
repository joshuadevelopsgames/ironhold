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
import net.minecraft.util.Mth;
import net.minecraft.resources.Identifier;
import kingdom.smp.Ironhold;

/**
 * Custom model for the Siren — humanoid upper body with a fish tail.
 * The tail sways side-to-side when swimming and has a horizontal dolphin-style fin.
 */
public class SirenModel extends EntityModel<SirenRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(Ironhold.MODID, "siren"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart tailBase;
    private final ModelPart tailMid;
    private final ModelPart tailEnd;
    private final ModelPart fin;

    public SirenModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.tailBase = root.getChild("tail_base");
        this.tailMid = this.tailBase.getChild("tail_mid");
        this.tailEnd = this.tailMid.getChild("tail_end");
        this.fin = this.tailEnd.getChild("fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head — standard 8x8x8, positioned at top of body
        root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, CubeDeformation.NONE),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // Body — standard 8x12x4
        root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, CubeDeformation.NONE),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // Right arm
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
                .texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, CubeDeformation.NONE),
            PartPose.offset(-5.0F, 2.0F, 0.0F));

        // Left arm
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
                .texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, CubeDeformation.NONE),
            PartPose.offset(5.0F, 2.0F, 0.0F));

        // Tail base — rotated ~80° to extend horizontally out the back
        PartDefinition tailBasePart = root.addOrReplaceChild("tail_base",
            CubeListBuilder.create()
                .texOffs(0, 32).addBox(-3.0F, 0.0F, -3.0F, 6, 6, 6, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 12.0F, 2.0F, 1.4F, 0.0F, 0.0F));

        // Tail mid — narrower
        PartDefinition tailMidPart = tailBasePart.addOrReplaceChild("tail_mid",
            CubeListBuilder.create()
                .texOffs(0, 44).addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4, CubeDeformation.NONE),
            PartPose.offset(0.0F, 6.0F, 0.0F));

        // Tail end — thin tip
        PartDefinition tailEndPart = tailMidPart.addOrReplaceChild("tail_end",
            CubeListBuilder.create()
                .texOffs(24, 32).addBox(-1.5F, 0.0F, -1.5F, 3, 4, 3, CubeDeformation.NONE),
            PartPose.offset(0.0F, 6.0F, 0.0F));

        // Fin — horizontal dolphin-style fluke
        tailEndPart.addOrReplaceChild("fin",
            CubeListBuilder.create()
                .texOffs(24, 44).addBox(-5.0F, 0.0F, -1.0F, 10, 1, 4, CubeDeformation.NONE),
            PartPose.offset(0.0F, 4.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(SirenRenderState state) {
        super.setupAnim(state);

        float t = state.ageInTicks;
        float swimSpeed = state.walkAnimationSpeed;

        // Head look
        this.head.yRot = state.yRot * ((float) Math.PI / 180F);
        this.head.xRot = state.xRot * ((float) Math.PI / 180F);

        if (state.isAggressive) {
            // Zombie-style arms — straight out in front, slight sway
            float sway = (float) Math.sin(t * 0.1F) * 0.1F;
            this.rightArm.xRot = -1.5F + sway;
            this.leftArm.xRot = -1.5F - sway;
            this.rightArm.zRot = 0.0F;
            this.leftArm.zRot = 0.0F;
            this.rightArm.yRot = -0.1F;
            this.leftArm.yRot = 0.1F;
        } else {
            // Flowing idle arms
            float armSwing = 0.15F + swimSpeed * 0.4F;
            float armSpeed = 0.08F + swimSpeed * 0.06F;
            this.rightArm.xRot = (float) Math.sin(t * armSpeed) * armSwing - 0.2F;
            this.leftArm.xRot = (float) Math.sin(t * armSpeed + 2.0F) * armSwing - 0.2F;
            this.rightArm.zRot = (float) Math.sin(t * 0.06F) * 0.12F - 0.15F;
            this.leftArm.zRot = (float) Math.sin(t * 0.06F + Math.PI) * 0.12F + 0.15F;
            this.rightArm.yRot = (float) Math.cos(t * 0.07F) * 0.1F;
            this.leftArm.yRot = (float) Math.cos(t * 0.07F + Math.PI) * 0.1F;
        }

        // Tail undulation
        float swayAmount = 0.12F + swimSpeed * 0.5F;
        float tailSpeed = 0.1F + swimSpeed * 0.12F;
        this.tailBase.xRot = 1.4F + (float) Math.sin(t * tailSpeed) * swayAmount * 0.5F;
        this.tailMid.xRot = (float) Math.sin(t * tailSpeed + 1.2F) * swayAmount;
        this.tailEnd.xRot = (float) Math.sin(t * tailSpeed + 2.4F) * swayAmount * 1.3F;
    }
}
