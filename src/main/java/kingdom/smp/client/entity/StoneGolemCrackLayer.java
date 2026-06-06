package kingdom.smp.client.entity;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.TextureLayerGeoLayer;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.StoneGolemEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Progressive damage cracks for the stone golem, mirroring vanilla iron-golem crackiness: as health
 * drops it overlays an increasingly cracked texture on the (animated) model. Re-renders the model with
 * a crack texture via {@link TextureLayerGeoLayer}; the texture is chosen by the health fraction captured
 * into the render state by {@link StoneGolemRenderer}. Above 75% health no overlay is drawn.
 */
public class StoneGolemCrackLayer extends TextureLayerGeoLayer<StoneGolemEntity, Void, LivingEntityRenderState> {

    /** Health fraction (0..1) stashed on the render state by the renderer each frame. */
    public static final DataTicket<Float> HEALTH_FRACTION = DataTicket.create("stone_golem_health", Float.class);

    private static final Identifier LOW = tex("low");
    private static final Identifier MEDIUM = tex("medium");
    private static final Identifier HIGH = tex("high");

    private static Identifier tex(String level) {
        return Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/stone_golem_cracks_" + level + ".png");
    }

    public StoneGolemCrackLayer(GeoRenderer<StoneGolemEntity, Void, LivingEntityRenderState> renderer) {
        super(renderer, HIGH, RenderTypes::entityTranslucent);
    }

    /** Crack texture for a health fraction, or null when undamaged (>75%). */
    private static @Nullable Identifier crackFor(float frac) {
        if (frac > 0.75f) {
            return null;
        }
        if (frac > 0.5f) {
            return LOW;
        }
        if (frac > 0.25f) {
            return MEDIUM;
        }
        return HIGH;
    }

    @Override
    protected Identifier getTextureResource(LivingEntityRenderState state) {
        Identifier t = crackFor(state.getOrDefaultGeckolibData(HEALTH_FRACTION, 1.0f));
        return t != null ? t : HIGH;
    }

    @Override
    protected RenderType getRenderType(LivingEntityRenderState state) {
        return RenderTypes.entityTranslucent(getTextureResource(state));
    }

    @Override
    public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> renderPass, SubmitNodeCollector collector) {
        if (crackFor(renderPass.renderState().getOrDefaultGeckolibData(HEALTH_FRACTION, 1.0f)) == null) {
            return; // undamaged — skip the overlay entirely
        }
        super.submitRenderTask(renderPass, collector);
    }
}
