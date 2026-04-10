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
 * Mimic model — vanilla single-chest geometry with teeth and a tongue
 * that appear when the mimic awakens.
 */
public class MimicModel extends EntityModel<MimicRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(Ironhold.MODID, "mimic"), "main");

    private final ModelPart bottom;
    private final ModelPart lid;
    private final ModelPart lock;
    private final ModelPart teeth;
    private final ModelPart tongue;

    public MimicModel(ModelPart root) {
        super(root);
        this.bottom = root.getChild("bottom");
        this.lid = root.getChild("lid");
        this.lock = root.getChild("lock");
        this.teeth = this.lid.getChild("teeth");
        this.tongue = this.bottom.getChild("tongue");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── Vanilla chest geometry (identical to ChestModel) ─────────────────

        PartDefinition bottomPart = root.addOrReplaceChild("bottom",
            CubeListBuilder.create()
                .texOffs(0, 19)
                .addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F),
            PartPose.ZERO);

        PartDefinition lidPart = root.addOrReplaceChild("lid",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(1.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F),
            PartPose.offset(0.0F, 9.0F, 1.0F));

        root.addOrReplaceChild("lock",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(7.0F, -2.0F, 14.0F, 2.0F, 4.0F, 1.0F),
            PartPose.offset(0.0F, 9.0F, 1.0F));

        // ── Teeth — hang DOWN from the inside of the lid at the front edge ───
        // y=0 is the inside (bottom) surface of the lid.
        // Negative y = hanging down into the chest cavity.
        // z=12..14 = the front edge of the lid (mouth side).
        // texOffs(0, 48) = white/ivory area on mimic texture.
        lidPart.addOrReplaceChild("teeth",
            CubeListBuilder.create()
                // Tooth 1 (far left, tall)
                .texOffs(0, 48)
                .addBox(2.0F, -3.0F, 12.0F, 1.0F, 3.0F, 1.0F)
                // Tooth 2
                .texOffs(0, 48)
                .addBox(4.5F, -2.0F, 12.0F, 1.0F, 2.0F, 1.0F)
                // Tooth 3 (center, tallest)
                .texOffs(0, 48)
                .addBox(7.5F, -4.0F, 12.0F, 1.0F, 4.0F, 1.0F)
                // Tooth 4
                .texOffs(0, 48)
                .addBox(10.5F, -2.0F, 12.0F, 1.0F, 2.0F, 1.0F)
                // Tooth 5 (far right, tall)
                .texOffs(0, 48)
                .addBox(13.0F, -3.0F, 12.0F, 1.0F, 3.0F, 1.0F),
            PartPose.ZERO);

        // ── Tongue — pivots from inside the mouth, extends out the front ──────
        // texOffs(0, 54) = pink/red area on mimic texture.
        bottomPart.addOrReplaceChild("tongue",
            CubeListBuilder.create()
                .texOffs(0, 54)
                .addBox(-3.0F, 0.0F, 0.0F, 6.0F, 1.0F, 9.0F),
            PartPose.offset(8.0F, 10.0F, 8.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MimicRenderState state) {
        super.setupAnim(state);

        if (!state.awakened) {
            // Dormant — lid & lock shut, teeth/tongue hidden.
            this.lid.xRot = 0.0F;
            this.lock.xRot = 0.0F;
            this.teeth.visible = false;
            this.tongue.visible = false;
            return;
        }

        this.teeth.visible = true;

        int awake = state.awakeTicks;

        float lidAngle;
        if (awake <= 15) {
            // Wake-up: lid swings open over 15 ticks (0 → 80°)
            float progress = (float) awake / 15.0F;
            float smoothed = progress * progress * (3.0F - 2.0F * progress);
            lidAngle = -smoothed * 1.4F;
            this.lid.xRot = lidAngle;
            this.lock.xRot = lidAngle;
            // Tongue appears once the mouth is mostly open
            this.tongue.visible = awake > 10;
        } else {
            // Active: lid "chomps" — oscillates between partly open and wide open
            float chompCycle = state.ageInTicks * 0.3F;
            lidAngle = -(0.52F + Mth.sin(chompCycle) * 0.35F);
            this.lid.xRot = lidAngle;
            this.lock.xRot = lidAngle;

            // Tongue retracts when mouth is closing, extends when opening
            // lidAngle ranges from -0.17 (nearly closed) to -0.87 (wide open)
            float openness = Mth.clamp((-lidAngle - 0.17F) / 0.7F, 0.0F, 1.0F);
            this.tongue.visible = openness > 0.3F;
        }

        // Tongue tilts upward, amount tied to how open the mouth is
        float openFactor = Mth.clamp(-lidAngle / 1.4F, 0.0F, 1.0F);
        this.tongue.xRot = -0.3F * openFactor + Mth.sin(state.ageInTicks * 0.15F) * 0.06F;
    }
}
