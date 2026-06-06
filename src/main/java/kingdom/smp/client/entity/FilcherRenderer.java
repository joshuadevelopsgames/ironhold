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
import net.minecraft.world.item.ItemStack;

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

    public FilcherRenderer(EntityRendererProvider.Context ctx) {
        super(
            ctx,
            new FilcherModel(ctx.bakeLayer(FilcherModel.LAYER_LOCATION)),
            new FilcherModel(ctx.bakeLayer(FilcherModel.LAYER_LOCATION)),
            ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR,      ctx.getModelSet(), ZombieModel::new),
            ArmorModelSet.bake(ModelLayers.ZOMBIE_BABY_ARMOR, ctx.getModelSet(), BabyZombieModel::new)
        );
        // Emissive glowing-yellow eyes over the dark face.
        addLayer(new FilcherEyesLayer(this));
    }

    @Override
    public Identifier getTextureLocation(FilcherRenderState state) {
        return TEXTURE;
    }

    @Override
    public void extractRenderState(FilcherEntity entity, FilcherRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isStalking   = entity.getTarget() instanceof Player
                             && entity.getMainHandItem().isEmpty()
                             && !entity.isShowingOff();
        state.isShowingOff = entity.isShowingOff();
        state.isKing = entity.isKing();
        // Suppress the equipped-helmet armor layer — the king filcher wears its crown in
        // the HEAD slot for game logic (drop chance, AI etc.), but visually the crown is
        // already drawn by the "crown" bone in FilcherModel. Clearing headEquipment here
        // stops vanilla's HumanoidArmorLayer from rendering a second floating crown item
        // on top of the bone-crown.
        state.headEquipment = ItemStack.EMPTY;
    }

    @Override
    public FilcherRenderState createRenderState() {
        return new FilcherRenderState();
    }
}
