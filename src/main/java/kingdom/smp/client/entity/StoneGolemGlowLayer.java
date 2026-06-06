package kingdom.smp.client.entity;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.StoneGolemEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The "borrowed life within" — an emissive amber heartbeat in the golem's eyes. Subclasses
 * {@link AutoGlowingGeoLayer} so it reuses GeckoLib's aligned whole-model re-render (staying locked
 * to the animated rig), and renders additively via {@link RenderTypes#eyes} so the glow reads as
 * emitted light in the dark rather than a flat decal.
 *
 * <p>The pulse intensity (0..1) is computed each frame by {@link StoneGolemRenderer} from health and
 * time — a slow calm beat at full health that quickens and flickers as the golem is broken down, and
 * fades to nothing on death (the light leaving the stone). Intensity selects one of four brightness
 * frames ({@code stone_golem_glow_0..3.png}); additive blending means a brighter frame = a stronger glow.
 */
public class StoneGolemGlowLayer
        extends AutoGlowingGeoLayer<StoneGolemEntity, Void, LivingEntityRenderState> {

    /** Heartbeat intensity 0..1 stashed by the renderer; ≤0 means the life has gone out (death). */
    public static final DataTicket<Float> GLOW = DataTicket.create("stone_golem_glow", Float.class);

    private static final int FRAMES = 4;

    public StoneGolemGlowLayer(GeoRenderer<StoneGolemEntity, Void, LivingEntityRenderState> renderer) {
        super(renderer);
    }

    @Override
    public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
        if (info.renderState().getOrDefaultGeckolibData(GLOW, 0.6f) <= 0.03f) {
            return; // life extinguished — no glow at all
        }
        super.submitRenderTask(info, collector);
    }

    @Override
    protected RenderType getRenderType(LivingEntityRenderState state) {
        // Additive "eyes" blend: the amber ADDS to whatever is behind it, so it reads as emitted light.
        return RenderTypes.eyes(getTextureResource(state));
    }

    @Override
    protected Identifier getTextureResource(LivingEntityRenderState state) {
        float glow = Mth.clamp(state.getOrDefaultGeckolibData(GLOW, 0.6f), 0f, 1f);
        int frame = Mth.clamp(Math.round(glow * (FRAMES - 1)), 0, FRAMES - 1);
        return Identifier.fromNamespaceAndPath(Ironhold.MODID,
                "textures/entity/stone_golem_glow_" + frame + ".png");
    }
}
