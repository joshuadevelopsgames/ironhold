package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.StoneStatueEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renders a {@link StoneStatueEntity} as a carved-stone humanoid standing on a
 * stone-brick plinth ({@link StatueBaseLayer}). One renderer class serves every
 * statue variant; the per-statue stone texture is supplied at registration.
 *
 * <p>Uses a plain {@link HumanoidModel} + {@link StatueRenderState} (NOT a player
 * model / AvatarRenderState) so the entity render dispatcher keeps submission on
 * THIS renderer — otherwise {@link #submit} (the pedestal + lift) is bypassed.
 * The skin's outer layers (jacket/sleeves/pants) are baked into the base texture
 * by the stonify script; the head/hat overlay is drawn by HumanoidModel's hat.
 */
public class StoneStatueRenderer
    extends HumanoidMobRenderer<StoneStatueEntity, StatueRenderState, HumanoidModel<StatueRenderState>> {

    public static final Identifier KANGARUDE_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/statue/kangarude.png");
    public static final Identifier HAALINA_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/statue/haalina.png");
    public static final Identifier FACELACES_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/statue/facelaces.png");
    public static final Identifier RED_RAICHU_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/statue/red_raichu.png");
    public static final Identifier TWOHRD_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/statue/twohrd.png");
    public static final Identifier ARCATHEONE_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/statue/arcatheone.png");
    public static final Identifier CHEAKIE_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/statue/cheakie.png");

    /** Pedestal height in model-space pixels (16 = 1 block); the figure is lifted by this. */
    static final float PEDESTAL_HEIGHT = 8.0F;

    private final Identifier texture;

    public StoneStatueRenderer(EntityRendererProvider.Context ctx, Identifier texture) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.texture = texture;
        this.addLayer(new StatueBaseLayer(this, ctx.bakeLayer(StatueBaseLayer.LAYER_LOCATION)));
    }

    @Override
    public StatueRenderState createRenderState() {
        return new StatueRenderState();
    }

    @Override
    public void extractRenderState(StoneStatueEntity entity, StatueRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // Freeze the rig: no walk sway, no idle limb/head bob — pure stone.
        state.walkAnimationPos = 0.0F;
        state.walkAnimationSpeed = 0.0F;
        state.ageInTicks = 0.0F;
        state.xRot = 0.0F;
        state.yRot = 0.0F;
    }

    @Override
    public void submit(StatueRenderState state, PoseStack pose,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        // Lift the whole figure (and its plinth layer) so the plinth rests on the
        // ground and the statue stands on top instead of sinking in.
        pose.pushPose();
        pose.translate(0.0F, PEDESTAL_HEIGHT / 16.0F, 0.0F);
        super.submit(state, pose, collector, camera);
        pose.popPose();
    }

    @Override
    public Identifier getTextureLocation(StatueRenderState state) {
        return texture;
    }
}
