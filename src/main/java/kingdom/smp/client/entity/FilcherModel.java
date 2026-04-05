package kingdom.smp.client.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Custom geometry for the Filcher — a hunched shadow-sprite silhouette.
 *
 * Shape changes vs. vanilla zombie:
 *   Head  → 6×6×6 (compact, oversized-baby-head proportion)
 *   Body  → 8×9×4 (floor-length robe, slight forward lean)
 *   Arms  → 2×7×2 (long thin tentacle-arms draped at sides)
 *   Legs  → 2×4×2 (stubby, hidden under robe)
 *
 * <h3>Animations</h3>
 * <ul>
 *   <li><b>Idle</b> — head sways side to side (curious tilt) with a subtle
 *       breathing bob; arms drift with an independent lazy sway.</li>
 *   <li><b>Stalk</b> — body hunches sharply forward, head cranes up to peek;
 *       walk cycle is shortened to a mincing tiptoe shuffle.</li>
 *   <li><b>Show-off</b> — head bobs eagerly and tilts toward the viewer;
 *       arms raise slightly as if presenting the loot.</li>
 *   <li><b>Walk</b> — leg swing is dampened so the filcher always looks like
 *       it's sneaking even at full speed.</li>
 * </ul>
 */
public class FilcherModel extends ZombieModel<FilcherRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath("ironhold", "filcher"), "main");

    private final ModelPart crown;

    public FilcherModel(ModelPart root) {
        super(root);
        this.crown = root.getChild("head").getChild("crown");
    }

    // ── Geometry ──────────────────────────────────────────────────────────────

    /**
     * Part layout (all Y values in model-space units, 16 units = 1 block):
     * <pre>
     *   Head   6×6×6  — offset(0, 15, 0), box Y: 9–15   UV(0,0)
     *   Hat    6×6×6  — same pivot, CubeDeformation(1.5) UV(32,0)
     *   Body   8×9×4  — offset(0, 15, 0), box Y: 15–24  UV(16,16)
     *   R.arm  2×7×2  — offset(-5, 16, 0), box Y: 15–22 UV(0,36)
     *   L.arm  2×7×2  — offset( 5, 16, 0), box Y: 15–22 UV(8,36)
     *   R.leg  2×4×2  — offset(-0.5, 20, 0), Y: 20–24   UV(0,48)
     *   L.leg  2×4×2  — offset( 0.5, 20, 0), Y: 20–24   UV(12,48)
     * </pre>
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-3.0F, -6.0F, -3.0F, 6, 6, 6, CubeDeformation.NONE),
            PartPose.offset(0.0F, 15.0F, 0.0F));

        head.addOrReplaceChild("hat",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-3.0F, -6.0F, -3.0F, 6, 6, 6, new CubeDeformation(1.5F)),
            PartPose.ZERO);

        // Crown — only rendered for king filchers (crown.visible = state.isKing)
        head.addOrReplaceChild("crown",
            CubeListBuilder.create()
                .texOffs(0, 55)
                .addBox(-4.0F, -7.0F, -4.0F, 8, 1, 8, CubeDeformation.NONE)   // band
                .texOffs(32, 55)
                .addBox(-1.0F, -10.0F, -3.0F, 2, 3, 2, CubeDeformation.NONE)  // front prong
                .texOffs(40, 55)
                .addBox(-4.0F, -9.0F, -1.0F, 2, 2, 2, CubeDeformation.NONE)   // left prong
                .texOffs(48, 55)
                .addBox(2.0F, -9.0F, -1.0F, 2, 2, 2, CubeDeformation.NONE),   // right prong
            PartPose.ZERO);

        root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(16, 16)
                .addBox(-4.0F, 0.0F, -2.0F, 8, 9, 4, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 15.0F, 0.0F, 0.08F, 0.0F, 0.0F));

        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
                .texOffs(0, 36)
                .addBox(-2.0F, -1.0F, -1.0F, 2, 7, 2, CubeDeformation.NONE),
            PartPose.offsetAndRotation(-4.0F, 16.0F, 0.0F, 0.15F, 0.0F, 0.05F));

        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
                .texOffs(8, 36)
                .addBox(0.0F, -1.0F, -1.0F, 2, 7, 2, CubeDeformation.NONE),
            PartPose.offsetAndRotation(4.0F, 16.0F, 0.0F, 0.15F, 0.0F, -0.05F));

        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
                .texOffs(0, 48)
                .addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2, CubeDeformation.NONE),
            PartPose.offset(-0.5F, 20.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
                .texOffs(12, 48)
                .addBox(-1.0F, 0.0F, -1.0F, 2, 4, 2, CubeDeformation.NONE),
            PartPose.offset(0.5F, 20.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    // ── Animation ─────────────────────────────────────────────────────────────

    @Override
    public void setupAnim(FilcherRenderState state) {
        super.setupAnim(state);

        // Crown visibility: only render if king
        crown.visible = state.isKing;

        float t         = state.ageInTicks;
        float walkSpeed = state.walkAnimationSpeed;
        boolean moving  = walkSpeed > 0.05F;

        if (state.isStalking) {
            animateStalking(t, moving);
        } else if (state.isShowingOff) {
            animateShowOff(t);
        } else {
            animateIdle(t, moving);
        }
    }

    /**
     * Idle animation — gentle breathing, curious side-tilt, lazy arm sway.
     * Movement: dampened leg swing so walking always looks sneaky.
     */
    private void animateIdle(float t, boolean moving) {
        // Curious head side-tilt — slow oscillation left and right
        head.zRot += Mth.sin(t * 0.04F) * 0.12F;

        // Subtle breathing bob on the head
        head.y += Mth.sin(t * 0.07F) * 0.25F;

        // Independent lazy sway per arm — they don't move in sync
        rightArm.zRot -= Mth.sin(t * 0.05F + 0.8F) * 0.06F;
        leftArm.zRot  += Mth.sin(t * 0.05F) * 0.06F;

        if (moving) {
            // Dampened leg swing — mincing tiptoe shuffle
            rightLeg.xRot *= 0.55F;
            leftLeg.xRot  *= 0.55F;
            // Slight body bob with each step
            body.y += Math.abs(Mth.sin(t * 0.3F)) * 0.4F;
        }
    }

    /**
     * Stalk animation — sharp body hunch, head cranes up to peek forward,
     * arms creep ahead, walk reduced to an exaggerated tiptoe.
     */
    private void animateStalking(float t, boolean moving) {
        // Subtle forward lean — just enough to look purposeful, not dramatic
        body.xRot += 0.14F;

        // Head levels out so it peeks forward without an obvious silhouette change
        head.xRot -= 0.10F;

        // Very slight nervous micro-twitch
        head.zRot += Mth.sin(t * 0.12F) * 0.04F;

        // Arms stay mostly down, just creeping slightly forward
        rightArm.xRot -= 0.12F;
        leftArm.xRot  -= 0.12F;

        if (moving) {
            // Mincing steps — as little leg movement as possible
            rightLeg.xRot *= 0.35F;
            leftLeg.xRot  *= 0.35F;
        }
    }

    /**
     * Show-off animation — eager head bob, arms raised to present the loot.
     */
    private void animateShowOff(float t) {
        // Excited bobbing head
        head.y     += Mth.sin(t * 0.25F) * 0.6F;
        head.zRot  += Mth.sin(t * 0.20F) * 0.18F;

        // Arms raise outward to display the treasure
        rightArm.xRot -= 0.35F;
        leftArm.xRot  -= 0.35F;
        rightArm.zRot -= 0.20F;
        leftArm.zRot  += 0.20F;

        // Whole body sways proudly
        body.zRot += Mth.sin(t * 0.18F) * 0.06F;
    }
}
