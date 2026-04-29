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
 * Shipwreck Mimic model — chest body with teeth, tongue, and 6 crab legs
 * (3 per side) that scuttle when the mimic is awake.
 */
public class ShipwreckMimicModel extends EntityModel<MimicRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(Ironhold.MODID, "shipwreck_mimic"), "main");

    private final ModelPart bottom;
    private final ModelPart lid;
    private final ModelPart lock;
    private final ModelPart teeth;
    private final ModelPart tongue;

    // Crab legs — 3 per side, each with an upper + lower segment
    private final ModelPart legFL, legFLTip; // front left
    private final ModelPart legML, legMLTip; // middle left
    private final ModelPart legBL, legBLTip; // back left
    private final ModelPart legFR, legFRTip; // front right
    private final ModelPart legMR, legMRTip; // middle right
    private final ModelPart legBR, legBRTip; // back right

    public ShipwreckMimicModel(ModelPart root) {
        super(root);
        this.bottom = root.getChild("bottom");
        this.lid = root.getChild("lid");
        this.lock = root.getChild("lock");
        this.teeth = this.lid.getChild("teeth");
        this.tongue = this.bottom.getChild("tongue");

        // Left legs
        this.legFL = this.bottom.getChild("leg_fl");
        this.legFLTip = this.legFL.getChild("leg_fl_tip");
        this.legML = this.bottom.getChild("leg_ml");
        this.legMLTip = this.legML.getChild("leg_ml_tip");
        this.legBL = this.bottom.getChild("leg_bl");
        this.legBLTip = this.legBL.getChild("leg_bl_tip");

        // Right legs
        this.legFR = this.bottom.getChild("leg_fr");
        this.legFRTip = this.legFR.getChild("leg_fr_tip");
        this.legMR = this.bottom.getChild("leg_mr");
        this.legMRTip = this.legMR.getChild("leg_mr_tip");
        this.legBR = this.bottom.getChild("leg_br");
        this.legBRTip = this.legBR.getChild("leg_br_tip");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── Chest body (same as MimicModel) ─────────────────────────────
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

        // Teeth
        lidPart.addOrReplaceChild("teeth",
            CubeListBuilder.create()
                .texOffs(0, 48).addBox(2.0F, -3.0F, 12.0F, 1.0F, 3.0F, 1.0F)
                .texOffs(0, 48).addBox(4.5F, -2.0F, 12.0F, 1.0F, 2.0F, 1.0F)
                .texOffs(0, 48).addBox(7.5F, -4.0F, 12.0F, 1.0F, 4.0F, 1.0F)
                .texOffs(0, 48).addBox(10.5F, -2.0F, 12.0F, 1.0F, 2.0F, 1.0F)
                .texOffs(0, 48).addBox(13.0F, -3.0F, 12.0F, 1.0F, 3.0F, 1.0F),
            PartPose.ZERO);

        // Tongue
        bottomPart.addOrReplaceChild("tongue",
            CubeListBuilder.create()
                .texOffs(0, 54)
                .addBox(-3.0F, 0.0F, 0.0F, 6.0F, 1.0F, 9.0F),
            PartPose.offset(8.0F, 10.0F, 8.0F));

        // ── Crab legs — 3 per side ──────────────────────────────────────
        // Each leg: upper segment (2x2x6) angled outward, lower segment (2x2x5)
        // texOffs(42, 0) = leg texture area on the 64x64 sheet

        // Left legs — attach to left side of chest (x=1), angled outward
        addLeg(bottomPart, "leg_fl", 0.0F,  3.0F,  4.0F,  -0.4F, true);   // front
        addLeg(bottomPart, "leg_ml", 0.0F,  3.0F,  8.0F,   0.0F, true);   // middle
        addLeg(bottomPart, "leg_bl", 0.0F,  3.0F, 12.0F,   0.4F, true);   // back

        // Right legs — attach to right side (x=15), angled outward
        addLeg(bottomPart, "leg_fr", 16.0F, 3.0F,  4.0F,  -0.4F, false);  // front
        addLeg(bottomPart, "leg_mr", 16.0F, 3.0F,  8.0F,   0.0F, false);  // middle
        addLeg(bottomPart, "leg_br", 16.0F, 3.0F, 12.0F,   0.4F, false);  // back

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addLeg(PartDefinition parent, String name,
                                float x, float y, float z, float zRot, boolean left) {
        float sideAngle = left ? 0.5F : -0.5F; // Lean outward
        // Upper segment
        PartDefinition upper = parent.addOrReplaceChild(name,
            CubeListBuilder.create()
                .texOffs(42, 0)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
            PartPose.offsetAndRotation(x, y, z, 0.0F, 0.0F, sideAngle + zRot * 0.3F));
        // Lower segment (tip)
        upper.addOrReplaceChild(name + "_tip",
            CubeListBuilder.create()
                .texOffs(50, 0)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.3F, 0.0F, 0.0F));
    }

    @Override
    public void setupAnim(MimicRenderState state) {
        super.setupAnim(state);

        boolean awake = state.awakened;
        int awakeTicks = state.awakeTicks;

        // ── Legs — always visible, scuttle when awake ───────────────────
        if (awake && awakeTicks > 10) {
            float t = state.ageInTicks;
            // Each leg pair has a phase offset for a scuttling gait
            animLegPair(legFL, legFLTip, legFR, legFRTip, t, 0.0F);
            animLegPair(legML, legMLTip, legMR, legMRTip, t, 2.1F);
            animLegPair(legBL, legBLTip, legBR, legBRTip, t, 4.2F);
        } else {
            // Dormant — legs tucked under the chest (folded up)
            resetLeg(legFL, legFLTip, true);
            resetLeg(legML, legMLTip, true);
            resetLeg(legBL, legBLTip, true);
            resetLeg(legFR, legFRTip, false);
            resetLeg(legMR, legMRTip, false);
            resetLeg(legBR, legBRTip, false);
        }

        // ── Chest animations (same as MimicModel) ──────────────────────
        if (!awake) {
            this.lid.xRot = 0.0F;
            this.lock.xRot = 0.0F;
            this.teeth.visible = false;
            this.tongue.visible = false;
            return;
        }

        this.teeth.visible = true;

        float lidAngle;
        if (awakeTicks <= 15) {
            float progress = (float) awakeTicks / 15.0F;
            float smoothed = progress * progress * (3.0F - 2.0F * progress);
            lidAngle = -smoothed * 1.4F;
            this.lid.xRot = lidAngle;
            this.lock.xRot = lidAngle;
            this.tongue.visible = awakeTicks > 10;
        } else {
            float chompCycle = state.ageInTicks * 0.3F;
            lidAngle = -(0.52F + Mth.sin(chompCycle) * 0.35F);
            this.lid.xRot = lidAngle;
            this.lock.xRot = lidAngle;
            float openness = Mth.clamp((-lidAngle - 0.17F) / 0.7F, 0.0F, 1.0F);
            this.tongue.visible = openness > 0.3F;
        }

        float openFactor = Mth.clamp(-lidAngle / 1.4F, 0.0F, 1.0F);
        this.tongue.xRot = -0.3F * openFactor + Mth.sin(state.ageInTicks * 0.15F) * 0.06F;
    }

    private void animLegPair(ModelPart uL, ModelPart tL, ModelPart uR, ModelPart tR,
                              float time, float phase) {
        // Alternating gait — left moves forward while right moves back
        float swing = Mth.sin(time * 0.5F + phase) * 0.6F;
        uL.xRot = swing;
        uR.xRot = -swing;
        // Tips bend to look like crab joint
        tL.xRot = 0.3F + Mth.sin(time * 0.5F + phase + 1.0F) * 0.3F;
        tR.xRot = 0.3F + Mth.sin(time * 0.5F + phase + 1.0F + (float) Math.PI) * 0.3F;
    }

    private void resetLeg(ModelPart upper, ModelPart tip, boolean left) {
        // Legs tucked — folded flat against the bottom of the chest
        upper.xRot = 0.0F;
        upper.zRot = left ? 1.2F : -1.2F; // Folded tight against body
        tip.xRot = 0.8F; // Bent under
    }
}
