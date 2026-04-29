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
import net.minecraft.client.renderer.entity.state.PiglinRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Hybrid model for the Nether Villager (Villager × Piglin crossbreed).
 *
 * Geometry: vanilla piglin head + ears + standard humanoid body and legs,
 * with villager-style folded "praying" arms in place of the dangling
 * humanoid arms. The arm pose is the iconic villager/wandering-trader
 * silhouette — both forearms held horizontally across the chest, hands
 * meeting at the front.
 *
 * UV layout (matches what {@code piglin_villager.png} expects, modulo the
 * arm region which moves from piglin's (40,16) to villager's folded-arm
 * pair (44,22) + (40,38)):
 *
 *   head       8x8x8  @ texOffs(0,0)        — piglin head
 *   right_ear  1x5x4  @ texOffs(39,6)       — piglin ear
 *   left_ear   1x5x4  @ texOffs(51,6)       — piglin ear
 *   body       8x12x4 @ texOffs(16,16)      — humanoid body
 *   right_leg  4x12x4 @ texOffs(0,16)       — humanoid leg
 *   left_leg   4x12x4 @ texOffs(0,16) mirrored — humanoid leg (shared UV)
 *   arms vertical L+R: 4x8x4 @ texOffs(44,22) (mirrored for the second cube)
 *   arms cross-piece:  8x4x4 @ texOffs(40,38) — chest band joining both arms
 */
public class PiglinVillagerModel extends EntityModel<PiglinRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "piglin_villager"), "main");

    private static final float EAR_BASE_TILT = 0.5235988F; // 30°

    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart body;
    private final ModelPart arms;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public PiglinVillagerModel(ModelPart root) {
        super(root);
        this.head     = root.getChild("head");
        this.leftEar  = head.getChild("left_ear");
        this.rightEar = head.getChild("right_ear");
        this.body     = root.getChild("body");
        this.arms     = root.getChild("arms");
        this.leftLeg  = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F),
            PartPose.offset(0F, 0F, 0F));

        // Piglin ears — thin vertical flaps tilted outward at 30°.
        head.addOrReplaceChild("left_ear",
            CubeListBuilder.create()
                .texOffs(51, 6).addBox(0F, 0F, -2F, 1F, 5F, 4F),
            PartPose.offsetAndRotation(4.5F, -6F, 0F, 0F, 0F, -EAR_BASE_TILT));
        head.addOrReplaceChild("right_ear",
            CubeListBuilder.create().mirror()
                .texOffs(39, 6).addBox(-1F, 0F, -2F, 1F, 5F, 4F),
            PartPose.offsetAndRotation(-4.5F, -6F, 0F, 0F, 0F, EAR_BASE_TILT));

        root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4F, 0F, -2F, 8F, 12F, 4F),
            PartPose.offset(0F, 0F, 0F));

        // Folded "praying" arms — exact same cube structure as VillagerModel.
        // Two vertical 4x8x4 cubes (one mirrored) form the forearms held horizontal,
        // and an 8x4x4 cross-piece spans the chest joining them.
        // The whole thing is tilted ~-43° on X so the forearms point forward + down.
        root.addOrReplaceChild("arms",
            CubeListBuilder.create()
                .texOffs(44, 22).addBox(-8F, -2F, -2F, 4F, 8F, 4F)
                .texOffs(44, 22).mirror().addBox( 4F, -2F, -2F, 4F, 8F, 4F).mirror(false)
                .texOffs(40, 38).addBox(-4F,  2F, -2F, 8F, 4F, 4F),
            PartPose.offsetAndRotation(0F, 3F, -1F, -0.75F, 0F, 0F));

        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 12F, 4F),
            PartPose.offset(-2F, 12F, 0F));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().mirror()
                .texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 12F, 4F),
            PartPose.offset(2F, 12F, 0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(PiglinRenderState state) {
        super.setupAnim(state);

        // Head look (yaw/pitch).
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        // Ear flap when walking — small extra tilt on top of the resting 30°.
        float earFlap = Mth.cos(state.walkAnimationPos * 0.6662F)
            * state.walkAnimationSpeed * 0.4F;
        leftEar.zRot  = -EAR_BASE_TILT - earFlap;
        rightEar.zRot =  EAR_BASE_TILT + earFlap;

        // Legs swing alternately while walking (vanilla humanoid stride).
        float legSwing = Mth.cos(state.walkAnimationPos * 0.6662F)
            * 1.4F * state.walkAnimationSpeed;
        rightLeg.xRot =  legSwing;
        leftLeg.xRot  = -legSwing;
        rightLeg.yRot = 0F;
        leftLeg.yRot  = 0F;

        // Arms stay folded across the chest. Tiny vertical bob keeps it lively
        // without breaking the silhouette.
        arms.y = 3F + Mth.sin(state.walkAnimationPos * 0.3F)
            * 0.5F * state.walkAnimationSpeed;
        arms.xRot = -0.75F;
    }
}
