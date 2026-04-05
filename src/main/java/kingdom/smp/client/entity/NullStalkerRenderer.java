package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.NullStalkerEntity;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renders the Null Stalker using the standard Enderman model with a custom
 * purple-tinted texture. Particle aura is handled server-side in the entity.
 */
public class NullStalkerRenderer extends EndermanRenderer {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/null_stalker.png");

    // Fallback to vanilla enderman texture until custom art is created
    private static final Identifier TEXTURE_FALLBACK =
        Identifier.withDefaultNamespace("textures/entity/enderman/enderman.png");

    public NullStalkerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTextureLocation(EndermanRenderState state) {
        // Use custom texture if it exists, otherwise vanilla enderman
        return TEXTURE_FALLBACK; // swap to TEXTURE once null_stalker.png is created
    }
}
