package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Hopling model — small cube body, three chorus-tip horns, four wiggly legs.
 * Texture: 64x32. Entity bbox is ~0.9 blocks; visual fits inside that.
 */
public class HoplingModel extends EntityModel<HoplingRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Ironhold.MODID, "hopling"), "main");

    private final ModelPart body;
    private final ModelPart hornCenter;
    private final ModelPart hornLeft;
    private final ModelPart hornRight;
    private final ModelPart legFL;
    private final ModelPart legFR;
    private final ModelPart legBL;
    private final ModelPart legBR;

    public HoplingModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.hornCenter = this.body.getChild("horn_center");
        this.hornLeft = this.body.getChild("horn_left");
        this.hornRight = this.body.getChild("horn_right");
        this.legFL = root.getChild("leg_fl");
        this.legFR = root.getChild("leg_fr");
        this.legBL = root.getChild("leg_bl");
        this.legBR = root.getChild("leg_br");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-5.0F, -8.0F, -5.0F, 10.0F, 8.0F, 10.0F),
                PartPose.offset(0.0F, 21.0F, 0.0F));

        body.addOrReplaceChild(
                "horn_center",
                CubeListBuilder.create()
                        .texOffs(0, 18)
                        .addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, -8.0F, 0.0F));

        body.addOrReplaceChild(
                "horn_left",
                CubeListBuilder.create()
                        .texOffs(8, 18)
                        .addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(-2.5F, -8.0F, 1.0F, 0.0F, 0.0F, 0.35F));

        body.addOrReplaceChild(
                "horn_right",
                CubeListBuilder.create()
                        .texOffs(12, 18)
                        .addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(2.5F, -8.0F, -1.0F, 0.0F, 0.0F, -0.35F));

        CubeListBuilder leg = CubeListBuilder.create()
                .texOffs(16, 18)
                .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F);

        root.addOrReplaceChild("leg_fl", leg, PartPose.offset(-3.5F, 21.0F, -3.5F));
        root.addOrReplaceChild("leg_fr", leg, PartPose.offset(3.5F, 21.0F, -3.5F));
        root.addOrReplaceChild("leg_bl", leg, PartPose.offset(-3.5F, 21.0F, 3.5F));
        root.addOrReplaceChild("leg_br", leg, PartPose.offset(3.5F, 21.0F, 3.5F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(HoplingRenderState state) {
        super.setupAnim(state);

        float pos = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);

        float swing = Mth.cos(pos * 0.6F) * 0.6F * speed;
        this.legFL.xRot = swing;
        this.legBR.xRot = swing;
        this.legFR.xRot = -swing;
        this.legBL.xRot = -swing;

        float bob = Mth.cos(pos * 0.6F + (float) Math.PI) * 0.04F * speed + state.hopBob;
        this.body.y = 21.0F - bob * 6.0F;

        float idle = Mth.sin(state.ageInTicks * 0.05F) * 0.04F;
        this.hornCenter.zRot = idle;
        this.hornLeft.zRot = 0.35F + idle;
        this.hornRight.zRot = -0.35F - idle;

        if (state.teleportTicks > 0) {
            float t = state.teleportTicks / (float) kingdom.smp.entity.HoplingEntity.TELEPORT_TOTAL_TICKS;
            float pinch = (float) Math.sin(t * Math.PI);
            this.body.xScale = 1.0F - pinch * 0.7F;
            this.body.zScale = 1.0F - pinch * 0.7F;
            this.body.yScale = 1.0F + pinch * 0.4F;
        } else {
            this.body.xScale = 1.0F;
            this.body.yScale = 1.0F;
            this.body.zScale = 1.0F;
        }
    }
}
