package kingdom.smp.client.entity;

import kingdom.smp.entity.KingEnderEyeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/** Renders as a billboarded ender_eye item, using the same generic renderer
 *  vanilla uses for thrown snowballs / pearls. The actual ender-eye visual
 *  comes from the entity's getItem() returning an ItemStack of Items.ENDER_EYE. */
public class KingEnderEyeRenderer extends ThrownItemRenderer<KingEnderEyeEntity> {
    public KingEnderEyeRenderer(EntityRendererProvider.Context ctx) {
        // scale 1.1× makes the eye read clearly at boss-fight distance,
        // fullBright = true so the eye's purple sclera glows in dark arenas.
        super(ctx, 1.1F, true);
    }
}
