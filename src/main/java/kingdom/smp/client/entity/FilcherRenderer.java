package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.FilcherEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.BabyZombieModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Renders the {@link FilcherEntity} using the custom shadow-sprite model
 * ({@link FilcherModel}) and a hand-painted dark texture with glowing eyes.
 *
 * Uses {@link FilcherRenderState} to pass stalking/show-off flags to the model
 * so animations can vary based on what the filcher is doing.
 */
public class FilcherRenderer extends AbstractZombieRenderer<FilcherEntity, FilcherRenderState, ZombieModel<FilcherRenderState>> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/filcher.png");
    private static final Identifier KING_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/filcher_king.png");

    private final ZombieModel<FilcherRenderState> normalModel;
    private final ZombieModel<FilcherRenderState> kingModel;

    public FilcherRenderer(EntityRendererProvider.Context ctx) {
        super(
            ctx,
            new FilcherModel(ctx.bakeLayer(FilcherModel.LAYER_LOCATION)),
            new FilcherModel(ctx.bakeLayer(FilcherModel.LAYER_LOCATION)),
            ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR,      ctx.getModelSet(), ZombieModel::new),
            ArmorModelSet.bake(ModelLayers.ZOMBIE_BABY_ARMOR, ctx.getModelSet(), BabyZombieModel::new)
        );
        this.normalModel = this.model;
        this.kingModel = new FilcherKingModel(ctx.bakeLayer(FilcherKingModel.LAYER_LOCATION));
    }

    @Override
    public Identifier getTextureLocation(FilcherRenderState state) {
        return state.isKing ? KING_TEXTURE : TEXTURE;
    }

    @Override
    public void extractRenderState(FilcherEntity entity, FilcherRenderState state, float partialTick) {
        // Swap model BEFORE super so the correct model is used for rendering
        this.model = entity.isKing() ? kingModel : normalModel;
        super.extractRenderState(entity, state, partialTick);
        state.isStalking   = entity.getTarget() instanceof Player
                             && entity.getMainHandItem().isEmpty()
                             && !entity.isShowingOff();
        state.isShowingOff = entity.isShowingOff();
        state.isKing = entity.isKing();
    }

    @Override
    public FilcherRenderState createRenderState() {
        return new FilcherRenderState();
    }
}
