package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Generic head-appendage cosmetic layer. One baked mesh holds a named part per
 * appendage (bunny ears, fox ears, devil horns, unicorn horn, …); the worn
 * vanity HEAD item selects which part + texture to draw, mounted on the head
 * bone so it inherits head pitch/yaw. Adding a new head appendage is just:
 * a {@code createLayer} child part, a texture, and a map entry — no new class.
 *
 * <p>All appendage geometry is authored directly in head-local space (y is
 * down, the crown is at y=-8, the head cube spans x,z in [-4,4]) so {@link
 * #submit} only mounts on the head and draws the selected part. Each appendage
 * has its own 16x16 texture; the shared {@code LayerDefinition} uses a 16x16 UV
 * space, and each part's box texOffs are laid out to match its texture's
 * uniform-colour zones (see {@code scripts/gen_head_appendages.py}).
 *
 * <p>These are replace-path cosmetics (no {@code overlaysArmor}): they reach the
 * render via {@code getItemBySlot} substitution like {@link CatEarsLayer}, so
 * the worn appendage shows in place of a helmet's look.
 */
public class HeadAppendageLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(Ironhold.MODID, "head_appendages"), "main");

    private static RenderType tex(String name) {
        return RenderTypes.entityCutout(
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/cosmetic/" + name + ".png"));
    }

    /** One appendage: the baked geometry part and the texture to draw it with. */
    private record Piece(ModelPart part, RenderType texture) {}

    private final ModelPart bunny;
    private final ModelPart fox;
    private final ModelPart devil;
    private final ModelPart unicorn;
    private final ModelPart ram;
    private final ModelPart antennae;
    private final ModelPart elf;

    private java.util.Map<Item, Piece> pieces;

    public HeadAppendageLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, ModelPart root) {
        super(parent);
        this.bunny = root.getChild("bunny_ears");
        this.fox = root.getChild("fox_ears");
        this.devil = root.getChild("devil_horns");
        this.unicorn = root.getChild("unicorn_horn");
        this.ram = root.getChild("ram_horns");
        this.antennae = root.getChild("antennae");
        this.elf = root.getChild("elf_ears");
    }

    /** Lazily map each appendage item to its baked part + texture (needs registered items). */
    private Piece pieceFor(ItemStack head) {
        if (this.pieces == null) {
            java.util.Map<Item, Piece> m = new java.util.IdentityHashMap<>();
            m.put(ModItems.BUNNY_EARS.get(),   new Piece(this.bunny,    tex("bunny_ears")));
            m.put(ModItems.FOX_EARS.get(),     new Piece(this.fox,      tex("fox_ears")));
            m.put(ModItems.DEVIL_HORNS.get(),  new Piece(this.devil,    tex("devil_horns")));
            m.put(ModItems.UNICORN_HORN.get(), new Piece(this.unicorn,  tex("unicorn_horn")));
            m.put(ModItems.RAM_HORNS.get(),    new Piece(this.ram,      tex("ram_horns")));
            m.put(ModItems.ANTENNAE.get(),     new Piece(this.antennae, tex("antennae")));
            m.put(ModItems.ELF_EARS.get(),     new Piece(this.elf,      tex("elf_ears")));
            this.pieces = m;
        }
        return this.pieces.get(head.getItem());
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Bunny ears: tall, narrow, vertical; white shell + pink inner (pink zone v=10).
        root.addOrReplaceChild("bunny_ears",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -15.0F, -0.5F, 2.0F, 7.0F, 1.0F)
                .texOffs(0, 10).addBox(-2.5F, -14.0F, -1.0F, 1.0F, 5.0F, 1.0F)
                .texOffs(0, 0).addBox(1.0F, -15.0F, -0.5F, 2.0F, 7.0F, 1.0F)
                .texOffs(0, 10).addBox(1.5F, -14.0F, -1.0F, 1.0F, 5.0F, 1.0F),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // Fox ears: pointed stepped triangles at the top corners; orange + white inner (white zone v=9).
        root.addOrReplaceChild("fox_ears",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -11.0F, -0.5F, 3.0F, 3.0F, 1.0F)
                .texOffs(0, 4).addBox(-3.5F, -13.5F, -0.5F, 2.0F, 2.5F, 1.0F)
                .texOffs(0, 9).addBox(-3.25F, -12.5F, -1.0F, 1.5F, 3.0F, 1.0F)
                .texOffs(0, 0).addBox(1.0F, -11.0F, -0.5F, 3.0F, 3.0F, 1.0F)
                .texOffs(0, 4).addBox(1.5F, -13.5F, -0.5F, 2.0F, 2.5F, 1.0F)
                .texOffs(0, 9).addBox(1.75F, -12.5F, -1.0F, 1.5F, 3.0F, 1.0F),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // Devil horns: stepped boxes curving up-and-outward from the top corners (uniform red).
        root.addOrReplaceChild("devil_horns",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -10.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                .texOffs(0, 0).addBox(-5.0F, -12.0F, -0.5F, 1.5F, 2.0F, 1.5F)
                .texOffs(0, 0).addBox(-5.5F, -13.5F, -0.5F, 1.0F, 1.5F, 1.0F)
                .texOffs(0, 0).addBox(2.0F, -10.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                .texOffs(0, 0).addBox(3.5F, -12.0F, -0.5F, 1.5F, 2.0F, 1.5F)
                .texOffs(0, 0).addBox(4.5F, -13.5F, -0.5F, 1.0F, 1.5F, 1.0F),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // Unicorn horn: single centred vertical taper poking up from the front of the crown (uniform gold).
        root.addOrReplaceChild("unicorn_horn",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.25F, -11.0F, -3.0F, 2.5F, 3.0F, 2.5F)
                .texOffs(0, 0).addBox(-0.9F, -13.0F, -2.6F, 1.8F, 2.0F, 1.8F)
                .texOffs(0, 0).addBox(-0.5F, -14.5F, -2.2F, 1.0F, 1.5F, 1.0F),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // Ram horns: stepped boxes curling out, back, then down beside each side of the head (uniform tan).
        root.addOrReplaceChild("ram_horns",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.5F, -9.0F, -1.0F, 1.5F, 1.5F, 2.0F)
                .texOffs(0, 0).addBox(-6.5F, -8.0F, 0.0F, 1.5F, 1.5F, 1.5F)
                .texOffs(0, 0).addBox(-6.5F, -6.0F, 0.5F, 1.5F, 2.0F, 1.5F)
                .texOffs(0, 0).addBox(-6.0F, -4.5F, -0.5F, 1.5F, 1.5F, 1.5F)
                .texOffs(0, 0).addBox(4.0F, -9.0F, -1.0F, 1.5F, 1.5F, 2.0F)
                .texOffs(0, 0).addBox(5.0F, -8.0F, 0.0F, 1.5F, 1.5F, 1.5F)
                .texOffs(0, 0).addBox(5.0F, -6.0F, 0.5F, 1.5F, 2.0F, 1.5F)
                .texOffs(0, 0).addBox(4.5F, -4.5F, -0.5F, 1.5F, 1.5F, 1.5F),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // Antennae: two thin stalks (dark zone) each topped with a bobble (bright zone v=8).
        root.addOrReplaceChild("antennae",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, -13.0F, -0.5F, 1.0F, 5.0F, 1.0F)
                .texOffs(0, 8).addBox(-2.5F, -14.5F, -1.0F, 2.0F, 2.0F, 2.0F)
                .texOffs(0, 0).addBox(1.0F, -13.0F, -0.5F, 1.0F, 5.0F, 1.0F)
                .texOffs(0, 8).addBox(0.5F, -14.5F, -1.0F, 2.0F, 2.0F, 2.0F),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        // Elf ears: pointed stepped ears on the sides of the head, angled up-and-back (uniform skin).
        root.addOrReplaceChild("elf_ears",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, -6.0F, -1.0F, 1.0F, 3.0F, 2.0F)
                .texOffs(0, 0).addBox(-6.0F, -7.0F, -0.5F, 1.5F, 2.0F, 1.5F)
                .texOffs(0, 0).addBox(-7.0F, -8.0F, 0.0F, 1.5F, 1.5F, 1.0F)
                .texOffs(0, 0).addBox(4.0F, -6.0F, -1.0F, 1.0F, 3.0F, 2.0F)
                .texOffs(0, 0).addBox(4.5F, -7.0F, -0.5F, 1.5F, 2.0F, 1.5F)
                .texOffs(0, 0).addBox(5.5F, -8.0F, 0.0F, 1.5F, 1.5F, 1.0F),
            PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                       AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) return;
        Piece piece = pieceFor(state.headEquipment);
        if (piece == null) return;

        pose.pushPose();
        this.getParentModel().head.translateAndRotate(pose);
        collector.submitModelPart(piece.part(), pose, piece.texture(), packedLight,
            OverlayTexture.NO_OVERLAY, null);
        pose.popPose();
    }
}
