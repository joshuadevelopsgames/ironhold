package kingdom.smp.client.entity;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/**
 * Custom worn model for the Magma Boots (feet slot). Rendered via
 * {@link MagmaBootsClientExtensions} through NeoForge's
 * {@code getHumanoidArmorModel} hook for {@code LayerType.HUMANOID} — same
 * mechanism the knight helmets use (see {@link KnightArmorModelDefs}).
 *
 * Geometry only lives on the leg parts; head/body/arms are present-but-empty
 * because the {@link net.minecraft.client.model.HumanoidModel} constructor
 * still calls {@code root.getChild(...)} on each. The boot cubes are children
 * of {@code right_leg}/{@code left_leg} so they inherit the player's leg pose.
 *
 * Two cubes per leg, leg-local coords (y=0 hip .. y=12 foot, -z = front):
 *   shaft (obsidian): box(-2.5, 3, -2.5, 5,8,5) at texOffs(0,0)
 *   sole  (lava)    : box(-2.5, 9, -4,   5,3,6) at texOffs(0,14) — toe juts forward
 *
 * Editable Blockbench source: {@code art/blockbench/magma_boots/magma_boots.bbmodel}.
 */
public final class MagmaBootsModelDef {

    private MagmaBootsModelDef() {}

    public static LayerDefinition createBootsLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);

        CubeListBuilder boot = CubeListBuilder.create()
            .texOffs(0, 0).addBox(-2.5f, 3.0f, -2.5f, 5.0f, 8.0f, 5.0f, new CubeDeformation(0.0f))
            .texOffs(0, 14).addBox(-2.5f, 9.0f, -4.0f, 5.0f, 3.0f, 6.0f, new CubeDeformation(0.0f));

        root.addOrReplaceChild("right_leg", boot, PartPose.offset(-1.9f, 12.0f, 0.0f));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(-2.5f, 3.0f, -2.5f, 5.0f, 8.0f, 5.0f, new CubeDeformation(0.0f))
                .texOffs(0, 14).addBox(-2.5f, 9.0f, -4.0f, 5.0f, 3.0f, 6.0f, new CubeDeformation(0.0f)),
            PartPose.offset(1.9f, 12.0f, 0.0f));

        return LayerDefinition.create(mesh, 64, 32);
    }
}
