package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import kingdom.smp.Ironhold;
import kingdom.smp.ModItems;
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

/**
 * Renders a pair of true-3D cat ears atop a player's head when they wear the
 * {@code cat_ears} cosmetic in the vanity HEAD slot. Mirrors the
 * {@link ShroomcapLayer} pattern: a small baked mesh mounted on the head bone
 * so the ears inherit head pitch/yaw.
 *
 * <p>One symmetric ear mesh is drawn twice — splayed outward into a V and
 * tilted slightly forward — so the silhouette reads as a cat (short, wide,
 * triangular) rather than a rabbit (tall, narrow, vertical). Each ear is a
 * stepped triangle: a wide charcoal base box, a narrower charcoal tip box, and
 * a pink inner box poking forward. All boxes sample uniform-colour regions of
 * {@code textures/entity/cosmetic/cat_ears.png} (charcoal top half, pink bottom
 * half), so box-UV precision is irrelevant.
 *
 * <p>{@code cat_ears} is a {@link kingdom.smp.item.VanityCosmeticItem} with no
 * equipment asset, so vanilla's armor layer draws nothing on the head — this
 * layer supplies the visible geometry instead.
 */
public class CatEarsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(Ironhold.MODID, "cat_ears"), "main");

    private static RenderType texture(String name) {
        return RenderTypes.entityCutout(
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/cosmetic/" + name + ".png"));
    }

    // Lazily-built map from each cat-ear variant item to its texture. Lazy because it
    // dereferences registered items (DeferredItem.get()), which must run after registration.
    private static java.util.Map<net.minecraft.world.item.Item, RenderType> TEXTURES;

    private static RenderType textureFor(net.minecraft.world.item.ItemStack head) {
        if (TEXTURES == null) {
            java.util.Map<net.minecraft.world.item.Item, RenderType> m = new java.util.IdentityHashMap<>();
            m.put(ModItems.CAT_EARS.get(),         texture("cat_ears"));
            m.put(ModItems.CAT_EARS_BLACK.get(),   texture("cat_ears_black"));
            m.put(ModItems.CAT_EARS_WHITE.get(),   texture("cat_ears_white"));
            m.put(ModItems.CAT_EARS_CALICO.get(),  texture("cat_ears_calico"));
            m.put(ModItems.CAT_EARS_SIAMESE.get(), texture("cat_ears_siamese"));
            TEXTURES = m;
        }
        return TEXTURES.get(head.getItem());
    }

    // Placement of each ear's base centre, in head-local pixels (head crown is at
    // y=-8; the head cube spans x,z in [-4,4]). The ears sit at the top corners.
    private static final float EAR_X = 2.5F;     // horizontal offset from head centre
    private static final float EAR_Y = -8.0F;    // on the crown
    private static final float SPLAY = 0.35F;    // outward lean (~20°), into a V
    private static final float FORWARD_TILT = 0.10F; // small forward pitch (alert cat)

    private final ModelPart ear;

    public CatEarsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, ModelPart root) {
        super(parent);
        this.ear = root.getChild("ear");
    }

    /**
     * One symmetric ear, authored around its base centre (pivot at the origin,
     * the ear rising in -y). Short and wide so it reads as a cat: a 5px-wide
     * base box tapering to a 3px-wide tip (total height 3px), with a pink inner
     * box poking ~0.5px forward (-z is forward). Drawn twice in {@link #submit}.
     */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "ear",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, -1.5F, -0.5F, 5.0F, 1.5F, 1.0F)   // wide base (charcoal)
                .texOffs(0, 4).addBox(-1.5F, -3.0F, -0.5F, 3.0F, 1.5F, 1.0F)   // narrow tip (charcoal)
                .texOffs(0, 9).addBox(-1.25F, -2.6F, -1.0F, 2.5F, 2.0F, 1.0F), // pink inner, forward
            PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                       AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) return;
        RenderType tex = textureFor(state.headEquipment);
        if (tex == null) return;

        pose.pushPose();
        // Mount on the head bone so the ears inherit head pitch/yaw.
        this.getParentModel().head.translateAndRotate(pose);
        // Two ears, splayed outward into a V, each pitched slightly forward, with a small
        // idle life: a gentle continuous sway plus an occasional quick flick (cats twitch
        // their ears). The flick is a short sine pulse fired roughly every 6 seconds.
        float t = state.ageInTicks;
        float sway = Mth.sin(t * 0.03F) * 0.02F;
        float phase = (t % 130.0F) / 130.0F;
        float flick = phase < 0.06F ? Mth.sin(phase / 0.06F * Mth.PI) * 0.16F : 0.0F;
        float twitch = sway + flick;
        submitEar(pose, collector, packedLight, tex, -EAR_X, -(SPLAY + twitch));
        submitEar(pose, collector, packedLight, tex, EAR_X, SPLAY + twitch);
        pose.popPose();
    }

    private void submitEar(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                           RenderType tex, float earX, float splay) {
        pose.pushPose();
        pose.translate(earX / 16.0F, EAR_Y / 16.0F, 0.0F);
        pose.mulPose(Axis.ZP.rotation(splay));          // splay outward into the V
        pose.mulPose(Axis.XP.rotation(FORWARD_TILT));   // tilt forward
        collector.submitModelPart(this.ear, pose, tex, packedLight, OverlayTexture.NO_OVERLAY, null);
        pose.popPose();
    }
}
