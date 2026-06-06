package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import kingdom.smp.Ironhold;
import kingdom.smp.ModItems;
import kingdom.smp.client.VanityAccessoryRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Generic back-mounted cosmetic layer — the body-bone counterpart to {@link
 * HeadAppendageLayer}. Capes ride the additive CHEST channel; tails ride the
 * additive LEGS channel (so a cape and a tail can be worn together), both
 * mounted on the body bone so they follow the torso.
 *
 * <p><b>Tails are animated segment chains.</b> The approach is mimicked
 * clean-room from the Ears mod (MIT, by Una/exaskye — studied for technique,
 * not copied): each tail is a chain of nested segments rooted at the lower back
 * that
 * <ul>
 *   <li>droops by a base angle (per tail),</li>
 *   <li><b>lifts and straightens as the player moves</b> ({@code walkAnimationSpeed}),</li>
 *   <li>idly sways with a phase-delayed sine wave running down the segments, and</li>
 *   <li>wags side-to-side in step with the walk cycle ({@code walkAnimationPos}).</li>
 * </ul>
 * This is what gives the tail life rather than the dead, stiff stack it was.
 *
 * <p>Geometry is authored in body-local space (y is down, the torso top is y=0,
 * the back surface z=+2). Back pieces are large, so the shared
 * {@code LayerDefinition} uses a 64x64 UV space.
 */
public class BackCosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(Ironhold.MODID, "back_cosmetics"), "main");

    private static final float ATTACH_Y = 11.0F;   // lower back, in body-local pixels
    private static final float ATTACH_Z = 2.0F;    // at the back surface

    private static RenderType tex(String name) {
        return RenderTypes.entityCutout(
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/cosmetic/" + name + ".png"));
    }

    // ── Cape (static chest overlay) ──────────────────────────────────────────────
    private final ModelPart cape;
    private RenderType capeTex;

    // ── Tails (animated legs overlay) ────────────────────────────────────────────
    /** A tail: its segment chain + the animation shape (base droop, per-segment bend, sway sizes). */
    private static final class TailRig {
        final ModelPart root;
        final ModelPart[] segs;
        final float droop;       // base downward angle of the first segment (radians)
        final float[] bend;      // resting bend of each segment (radians); relaxes as you run
        final float idleAmp;     // idle sine sway amplitude (radians)
        final float wagAmp;      // walk-driven side wag amplitude (radians)
        final RenderType texture;
        TailRig(ModelPart root, ModelPart[] segs, float droop, float[] bend,
                float idleAmp, float wagAmp, RenderType texture) {
            this.root = root; this.segs = segs; this.droop = droop; this.bend = bend;
            this.idleAmp = idleAmp; this.wagAmp = wagAmp; this.texture = texture;
        }
    }

    private java.util.Map<Item, TailRig> tails;

    public BackCosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, ModelPart root) {
        super(parent);
        this.cape = root.getChild("cape");
        this.capeTex = tex("cape_red");
        java.util.Map<Item, TailRig> t = new java.util.IdentityHashMap<>();
        t.put(ModItems.CAT_TAIL.get(), new TailRig(root.getChild("cat_tail"), chain(root, "cat_tail", 5),
            0.50F, new float[]{0F, 0.14F, 0.14F, 0.12F, 0.12F}, 0.13F, 0.22F, tex("cat_tail")));
        t.put(ModItems.FOX_TAIL.get(), new TailRig(root.getChild("fox_tail"), chain(root, "fox_tail", 4),
            0.62F, new float[]{0F, 0.12F, 0.14F, 0.16F}, 0.10F, 0.17F, tex("fox_tail")));
        t.put(ModItems.DRAGON_TAIL.get(), new TailRig(root.getChild("dragon_tail"), chain(root, "dragon_tail", 5),
            0.42F, new float[]{0F, 0.05F, 0.08F, 0.10F, 0.12F}, 0.07F, 0.12F, tex("dragon_tail")));
        t.put(ModItems.DEVIL_TAIL.get(), new TailRig(root.getChild("devil_tail"), chain(root, "devil_tail", 4),
            0.50F, new float[]{0F, 0.12F, 0.16F, 0.20F}, 0.15F, 0.22F, tex("devil_tail")));
        this.tails = t;
    }

    /** Walk a named tail's nested "seg" chain and collect the {@code n} segment parts in order. */
    private static ModelPart[] chain(ModelPart root, String tailName, int n) {
        ModelPart[] segs = new ModelPart[n];
        ModelPart cur = root.getChild(tailName);
        for (int i = 0; i < n; i++) {
            cur = cur.getChild("seg");
            segs[i] = cur;
        }
        return segs;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Cape: a collar across the shoulders and a broad drape hanging down the back.
        root.addOrReplaceChild("cape",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 0.0F, 1.6F, 8.0F, 2.0F, 1.5F)
                .texOffs(0, 4).addBox(-4.5F, 1.5F, 2.3F, 9.0F, 18.0F, 1.0F),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // Tail chains (width/length/depth per segment, tapering to the tip). Built as nested
        // "seg" children so animating each segment's rotation curves and connects the whole tail.
        float[] catW = {2.6F, 2.2F, 1.8F, 1.4F, 1.0F};
        addTailChain(root, "cat_tail", catW, segLens(5, 3.0F), catW, uniform(5, 0, 0));

        float[] foxW = {3.0F, 3.4F, 2.8F, 2.1F};
        int[][] foxUV = {{0, 0}, {0, 0}, {0, 0}, {0, 40}};  // last segment samples the white-tip zone
        addTailChain(root, "fox_tail", foxW, segLens(4, 3.4F), foxW, foxUV);

        float[] drgW = {2.3F, 2.1F, 1.7F, 1.3F, 0.9F};
        addTailChain(root, "dragon_tail", drgW, segLens(5, 3.0F), drgW, uniform(5, 0, 0));

        float[] dvlW = {0.9F, 0.85F, 0.8F, 0.7F};
        PartDefinition devilLast = addTailChain(root, "devil_tail", dvlW,
            new float[]{3.4F, 3.4F, 3.0F, 2.0F}, dvlW, uniform(4, 0, 0));
        // Devil spade fin on the tip segment (rides the last segment's motion).
        devilLast.addOrReplaceChild("spade",
            CubeListBuilder.create()
                .texOffs(20, 0).addBox(-1.8F, 0.0F, -0.35F, 3.6F, 2.2F, 0.7F)
                .texOffs(20, 0).addBox(-0.9F, 2.0F, -0.35F, 1.8F, 1.4F, 0.7F),
            PartPose.offset(0.0F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static float[] segLens(int n, float len) {
        float[] a = new float[n];
        java.util.Arrays.fill(a, len);
        return a;
    }

    private static int[][] uniform(int n, int u, int v) {
        int[][] a = new int[n][];
        for (int i = 0; i < n; i++) a[i] = new int[]{u, v};
        return a;
    }

    /**
     * Build a tail as a nested chain of segments rooted at the lower back. Each segment is a
     * tapering box pivoted at its top; the next segment hangs from the previous segment's end
     * (offset by the parent's length), so rotating each segment at render time bends the whole
     * tail smoothly. Returns the last segment's {@link PartDefinition} for attaching tip decor.
     */
    private static PartDefinition addTailChain(PartDefinition root, String name,
                                               float[] width, float[] length, float[] depth, int[][] uv) {
        PartDefinition tail = root.addOrReplaceChild(name, CubeListBuilder.create(),
            PartPose.offset(0.0F, ATTACH_Y, ATTACH_Z));
        PartDefinition cur = tail;
        float prevLen = 0.0F;
        for (int i = 0; i < width.length; i++) {
            cur = cur.addOrReplaceChild("seg",
                CubeListBuilder.create()
                    .texOffs(uv[i][0], uv[i][1])
                    .addBox(-width[i] / 2.0F, 0.0F, -depth[i] / 2.0F, width[i], length[i], depth[i]),
                PartPose.offset(0.0F, prevLen, 0.0F));
            prevLen = length[i];
        }
        return cur;
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                       AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) return;
        VanityAccessoryRenderState acc = (VanityAccessoryRenderState) state;
        boolean cape = acc.ironhold$chestAccessory().is(ModItems.CAPE.get());
        TailRig tail = this.tails.get(acc.ironhold$legsAccessory().getItem());
        if (!cape && tail == null) return;

        pose.pushPose();
        this.getParentModel().body.translateAndRotate(pose);
        if (cape) {
            // Sway the drape around the shoulder pivot: it lifts back as you run and drifts
            // gently in the breeze when idle. Pivot is the body origin (shoulders/neck).
            float speed = Math.min(state.walkAnimationSpeed, 1.0F);
            this.cape.xRot = speed * 0.35F + Mth.sin(state.ageInTicks * 0.05F) * 0.04F;
            this.cape.zRot = Mth.sin(state.ageInTicks * 0.04F + 1.0F) * 0.03F;
            collector.submitModelPart(this.cape, pose, this.capeTex, packedLight,
                OverlayTexture.NO_OVERLAY, null);
        }
        if (tail != null) {
            animateTail(tail, state);
            collector.submitModelPart(tail.root, pose, tail.texture, packedLight,
                OverlayTexture.NO_OVERLAY, null);
        }
        pose.popPose();
    }

    /**
     * Pose a tail's segment chain for this frame: the first segment carries the base droop, a
     * movement lift (the tail raises as you pick up speed) and the idle sway; later segments
     * carry their resting bend (relaxing as you run) plus the phase-delayed idle wave; and every
     * segment wags side-to-side in step with the walk cycle.
     */
    private static void animateTail(TailRig t, AvatarRenderState state) {
        float time = state.ageInTicks;
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float walk = state.walkAnimationPos;
        for (int i = 0; i < t.segs.length; i++) {
            ModelPart seg = t.segs[i];
            float idle = Mth.sin(time * 0.08F - i * 0.7F) * t.idleAmp;
            if (i == 0) {
                seg.xRot = t.droop - speed * 0.45F + idle;   // lifts as you move
            } else {
                seg.xRot = t.bend[i] * (1.0F - speed * 0.5F) + idle;  // relaxes as you run
            }
            seg.yRot = Mth.sin(walk * 0.5F - i * 0.5F) * speed * t.wagAmp;  // walk wag
            seg.zRot = 0.0F;
        }
    }
}
