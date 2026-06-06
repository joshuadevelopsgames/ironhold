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
 * Shroomling model — rounded three-tier mushroom cap atop a short stem with two
 * stubby legs. Texture 64x64. Box-UV offsets: stem(0,18) 6x4x6, leg(28,18) 2x2x2,
 * cap rim(4,2) 10x1x10, cap mid(8,7) 8x2x8, cap crown(12,6) 6x1x6.
 */
public class ShroomlingModel extends EntityModel<ShroomlingRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Ironhold.MODID, "shroomling"), "main");

    private final ModelPart body;
    private final ModelPart cap;
    private final ModelPart legLeft;
    private final ModelPart legRight;

    public ShroomlingModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.cap = this.body.getChild("cap");
        this.legLeft = root.getChild("leg_left");
        this.legRight = root.getChild("leg_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(0, 18)
                        .addBox(-3.0F, -4.0F, -3.0F, 6.0F, 4.0F, 6.0F),
                PartPose.offset(0.0F, 22.0F, 0.0F));

        body.addOrReplaceChild(
                "cap",
                CubeListBuilder.create()
                        .texOffs(8, 7).addBox(-4.0F, -3.0F, -4.0F, 8.0F, 2.0F, 8.0F)
                        .texOffs(4, 2).addBox(-5.0F, -1.0F, -5.0F, 10.0F, 1.0F, 10.0F)
                        .texOffs(12, 6).addBox(-3.0F, -4.0F, -3.0F, 6.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, -4.0F, 0.0F));

        CubeListBuilder leg = CubeListBuilder.create()
                .texOffs(28, 18)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F);

        root.addOrReplaceChild("leg_left", leg, PartPose.offset(-2.0F, 22.0F, 0.0F));
        root.addOrReplaceChild("leg_right", leg, PartPose.offset(2.0F, 22.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(ShroomlingRenderState state) {
        super.setupAnim(state);

        // Pickpocketed Shroomlings go bare-headed — hide the whole cap tier.
        this.cap.visible = !state.capless;

        float pos = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float age = state.ageInTicks;

        // --- Walk cycle ---
        // A quicker, springier stride than a stiff pendulum: the legs swing
        // through a wider arc and each foot lifts on its forward step so they
        // scamper instead of scraping the ground.
        float phase = pos * 1.1F;
        float stride = Mth.cos(phase) * 1.0F * speed;
        this.legLeft.xRot = stride;
        this.legRight.xRot = -stride;

        // Per-foot lift: left foot rises on the half-cycle it swings forward,
        // right foot on the opposite half. Clamp to the positive lobe of sine.
        float liftLeft = Math.max(0.0F, Mth.sin(phase)) * 0.6F * speed;
        float liftRight = Math.max(0.0F, -Mth.sin(phase)) * 0.6F * speed;

        // Side-to-side sway of the whole body, locked to the gait: sin(phase) is
        // the very signal that drives the per-foot lift (positive lobe lifts the
        // left foot, negative lobe the right), so the body rocks in time with
        // whichever foot is moving — the same look as the idle sway, but driven
        // by the walk and a touch more pronounced.
        this.body.zRot = Mth.sin(phase) * 0.20F * speed;

        // --- Body bob + hop rise ---
        float bob = Mth.cos(phase + (float) Math.PI) * 0.04F * speed + state.hopBob;
        float bodyY = 22.0F - bob * 6.0F;
        this.body.y = bodyY;

        // Legs track the body's vertical motion so they dangle with it during a
        // hop, then subtract the per-foot walk lift.
        this.legLeft.y = bodyY - liftLeft;
        this.legRight.y = bodyY - liftRight;

        // --- Jump foot-wiggle ---
        // While airborne (hopBob > 0) the feet flutter quickly and splay out,
        // like the Shroomling is paddling the air mid-hop.
        if (state.hopBob > 0.0F) {
            float flutter = Mth.sin(age * 1.6F) * 0.9F;
            this.legLeft.xRot += flutter;
            this.legRight.xRot -= flutter;
            this.legLeft.zRot = 0.35F;
            this.legRight.zRot = -0.35F;
        } else {
            this.legLeft.zRot = 0.0F;
            this.legRight.zRot = 0.0F;
        }

        // --- Idle cap sway --- fades out as the gait sway takes over, so the
        // two don't fight while walking; full strength when standing still.
        float idle = Mth.sin(age * 0.05F) * 0.05F * (1.0F - speed);
        this.cap.zRot = idle;
        this.cap.xRot = idle * 0.5F;
    }
}
