package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.ModBlocks;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MushroomCowRenderer;
import net.minecraft.client.renderer.entity.state.MushroomCowRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.cow.MushroomCow;

/**
 * Renderer for the Moonshroom — reuses the vanilla mooshroom model but swaps in
 * the light-blue body texture and resolves the recoloured {@code moon_mushroom}
 * block model for the back-mushrooms, so no red ever shows.
 */
public class MoonshroomRenderer extends MushroomCowRenderer {

    private static final Identifier ADULT =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/moonshroom/moonshroom.png");
    private static final Identifier BABY =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/moonshroom/moonshroom_baby.png");

    private final BlockModelResolver blockModelResolver;

    public MoonshroomRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.blockModelResolver = ctx.getBlockModelResolver();
    }

    @Override
    public Identifier getTextureLocation(MushroomCowRenderState state) {
        return state.isBaby ? BABY : ADULT;
    }

    @Override
    public void extractRenderState(MushroomCow entity, MushroomCowRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        this.blockModelResolver.update(
            state.mushroomModel,
            ModBlocks.MOON_MUSHROOM.get().defaultBlockState(),
            MushroomCowRenderer.BLOCK_DISPLAY_CONTEXT);
    }
}
