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
 * Baby mimic model — vanilla chest geometry with a tongue but no teeth.
 * The lid bobs and chomps gently.
 */
public class BabyMimicModel extends EntityModel<BabyMimicRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(Ironhold.MODID, "baby_mimic"), "main");

    private final ModelPart bottom;
    private final ModelPart lid;
    private final ModelPart lock;
    private final ModelPart tongue;

    public BabyMimicModel(ModelPart root) {
        super(root);
        this.bottom = root.getChild("bottom");
        this.lid = root.getChild("lid");
        this.lock = root.getChild("lock");
        this.tongue = this.bottom.getChild("tongue");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bottomPart = root.addOrReplaceChild("bottom",
            CubeListBuilder.create()
                .texOffs(0, 19)
                .addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F),
            PartPose.ZERO);

        root.addOrReplaceChild("lid",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(1.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F),
            PartPose.offset(0.0F, 9.0F, 1.0F));

        root.addOrReplaceChild("lock",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(7.0F, -2.0F, 14.0F, 2.0F, 4.0F, 1.0F),
            PartPose.offset(0.0F, 9.0F, 1.0F));

        // Tongue — same as MimicModel but smaller (baby-sized)
        bottomPart.addOrReplaceChild("tongue",
            CubeListBuilder.create()
                .texOffs(0, 54)
                .addBox(-3.0F, 0.0F, 0.0F, 6.0F, 1.0F, 9.0F),
            PartPose.offset(8.0F, 10.0F, 8.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(BabyMimicRenderState state) {
        super.setupAnim(state);

        // Gentle breathing chomp
        float chompCycle = state.ageInTicks * 0.15F;
        float lidAngle = -(0.3F + Mth.sin(chompCycle) * 0.2F);
        this.lid.xRot = lidAngle;
        this.lock.xRot = lidAngle;

        // Tongue — retracts when mouth closes, extends when open
        float openness = Mth.clamp((-lidAngle - 0.1F) / 0.4F, 0.0F, 1.0F);
        this.tongue.visible = openness > 0.3F;
        float openFactor = Mth.clamp(-lidAngle / 0.5F, 0.0F, 1.0F);
        this.tongue.xRot = -0.3F * openFactor + Mth.sin(state.ageInTicks * 0.15F) * 0.06F;
    }
}
