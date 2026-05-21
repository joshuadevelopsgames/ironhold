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
 * Slime Pet model — just a player head (base layer + hat overlay), UV-mapped to a standard
 * 64x64 player skin. The cube is centred at (8,8,8) in pixel space; the renderer scales it
 * down and parks it just above the ground so it reads as a tiny floating head.
 */
public class SlimePetModel extends EntityModel<SlimePetRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(Ironhold.MODID, "slime_pet"), "main");

    private final ModelPart head;

    public SlimePetModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Box centred on its own pivot (8,8,8) so it occupies the same 4..12 volume
        // as before but can be flipped in place. The head ships upside-down, so we
        // rotate it 180° about Z in setupAnim to stand it back upright.
        root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                .texOffs(32, 0)
                .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)),
            PartPose.offset(8.0F, 4.0F, 8.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(SlimePetRenderState state) {
        super.setupAnim(state);
        // Flip upright (Z-180) plus a slow wobble so it feels alive. No vertical bob —
        // the head sits on the ground and hops via the entity's slime move control.
        this.head.zRot = (float) Math.PI + Mth.sin(state.ageInTicks * 0.08F) * 0.05F;
    }
}
