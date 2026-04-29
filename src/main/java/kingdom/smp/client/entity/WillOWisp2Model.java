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

/**
 * Will-o'-the-Wisp model — a glowing orb head with a small flame plume on top,
 * two thin "arms" hanging at its sides like an Allay's, and a tapered flame
 * body trailing to a point. Anatomy follows vanilla character convention:
 * head + body + two side-mounted arms.
 *
 * <p>UV unwrap (64×64 texture) follows Minecraft's standard cube layout —
 * each cube reserves a [TOP][BOTTOM] strip on top and a
 * [WEST][NORTH][EAST][SOUTH] strip below. See the texture script for the
 * exact regions used.
 */
public class WillOWisp2Model extends EntityModel<WillOWisp2RenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(Ironhold.MODID, "will_o_wisp_2"), "main");

    private final ModelPart core;
    private final ModelPart headOrb;
    private final ModelPart topPlume;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart bodyMid;
    private final ModelPart bodyTaper;
    private final ModelPart bottomFlame;

    public WillOWisp2Model(ModelPart root) {
        super(root);
        this.core = root.getChild("core");
        this.headOrb = this.core.getChild("head_orb");
        this.topPlume = this.headOrb.getChild("top_plume");
        this.leftArm = this.core.getChild("left_arm");
        this.rightArm = this.core.getChild("right_arm");
        this.bodyMid = this.core.getChild("body_mid");
        this.bodyTaper = this.bodyMid.getChild("body_taper");
        this.bottomFlame = this.bodyTaper.getChild("bottom_flame");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Root pivot — wisps hover, so push the whole thing slightly above ground
        PartDefinition core = root.addOrReplaceChild("core",
            CubeListBuilder.create(),
            PartPose.offset(0.0F, 18.0F, 0.0F));

        // ── Head orb (the bright glowing bulb) ────────────────────────────
        PartDefinition headOrb = core.addOrReplaceChild("head_orb",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -6.0F, -3.0F, 6, 6, 6, new CubeDeformation(0.25F)),
            PartPose.offset(0.0F, -8.0F, 0.0F));

        // ── Top plume (a single thin flame licking upward, S-curved) ──────
        PartDefinition plume = headOrb.addOrReplaceChild("top_plume",
            CubeListBuilder.create()
                .texOffs(24, 0).addBox(-0.5F, -4.0F, -0.5F, 1, 4, 1, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.2F, 0.0F, -0.15F));
        plume.addOrReplaceChild("plume_tip",
            CubeListBuilder.create()
                .texOffs(24, 5).addBox(-0.5F, -3.0F, -0.5F, 1, 3, 1, new CubeDeformation(-0.15F)),
            PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, -0.4F, 0.0F, 0.35F));

        // ── Two arms, character-style (Allay-like 1×4×1) ─────────────────
        // Anchored at the shoulder line (just under the head orb) and offset
        // outward to body's side. Default pose hangs down with a slight
        // outward tilt — looks like a small humanoid character at rest.
        // zRot sign chosen so each arm swings AWAY from the body (not across it):
        //   left_arm at x=-2 → positive zRot rotates its bottom further -X (outward)
        //   right_arm at x=+2 → negative zRot rotates its bottom further +X (outward)
        addArm(core, "left_arm",  -2.0F,  0.7F);
        addArm(core, "right_arm",  2.0F, -0.7F);

        // ── Body mid (attaches directly under the head orb) ───────────────
        PartDefinition bodyMid = core.addOrReplaceChild("body_mid",
            CubeListBuilder.create()
                .texOffs(40, 0).addBox(-1.5F, 0.0F, -1.5F, 3, 4, 3, CubeDeformation.NONE),
            PartPose.offset(0.0F, -8.0F, 0.0F));

        // ── Body taper (narrower) ─────────────────────────────────────────
        PartDefinition bodyTaper = bodyMid.addOrReplaceChild("body_taper",
            CubeListBuilder.create()
                .texOffs(40, 8).addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2, CubeDeformation.NONE),
            PartPose.offset(0.0F, 4.0F, 0.0F));

        // ── Bottom flame tip (sharp 1-pixel-wide tail) ────────────────────
        bodyTaper.addOrReplaceChild("bottom_flame",
            CubeListBuilder.create()
                .texOffs(40, 14).addBox(-0.5F, 0.0F, -0.5F, 1, 4, 1, CubeDeformation.NONE),
            PartPose.offset(0.0F, 4.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * Build a single hanging arm: 1×4×1 column anchored at the shoulder,
     * default-tilted slightly outward via zRot so the arm doesn't clip into
     * the body. Mirroring is achieved by passing a positive/negative xOffset
     * and zRot — the same UV region is reused for both arms.
     */
    private static void addArm(PartDefinition parent, String name, float xOffset, float baseZRot) {
        parent.addOrReplaceChild(name,
            CubeListBuilder.create()
                .texOffs(0, 16).addBox(-0.5F, 0.0F, -0.5F, 1, 4, 1, CubeDeformation.NONE),
            PartPose.offsetAndRotation(xOffset, -7.0F, 0.0F, 0.0F, 0.0F, baseZRot));
    }

    @Override
    public void setupAnim(WillOWisp2RenderState state) {
        super.setupAnim(state);

        float t = state.ageInTicks;

        // Gentle bob — the whole core drifts up and down
        this.core.y = 18.0F + (float) Math.sin(t * 0.12F) * 0.6F;

        // Head orb slight wobble
        this.headOrb.zRot = (float) Math.sin(t * 0.08F) * 0.05F;
        this.headOrb.xRot = (float) Math.cos(t * 0.07F) * 0.04F;

        // Top plume sways like a candle flame
        this.topPlume.zRot = (float) Math.sin(t * 0.15F) * 0.3F;
        this.topPlume.xRot = (float) Math.cos(t * 0.12F) * 0.2F;

        // Arms held outward in a T-ish pose, gently swaying.
        // Sign matches the rest pose set in createBodyLayer (left=+, right=-).
        float armSwing = (float) Math.sin(t * 0.1F) * 0.12F;
        this.leftArm.zRot  =  0.7F + armSwing;
        this.rightArm.zRot = -0.7F - armSwing;
        this.leftArm.xRot  = (float) Math.sin(t * 0.09F) * 0.1F;
        this.rightArm.xRot = (float) Math.sin(t * 0.09F + Math.PI) * 0.1F;

        // Body taper sways the opposite way for an organic flame look
        this.bodyMid.zRot = (float) Math.sin(t * 0.1F + Math.PI) * 0.06F;
        this.bodyTaper.zRot = (float) Math.sin(t * 0.13F) * 0.1F;
        this.bottomFlame.zRot = (float) Math.sin(t * 0.16F) * 0.25F;
    }
}
