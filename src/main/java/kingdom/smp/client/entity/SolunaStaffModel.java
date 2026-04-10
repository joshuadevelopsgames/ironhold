package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.item.SolunaStaffItem;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * GeckoLib model for the Soluna Staff that fades between sun and moon textures
 * based on time of day.
 *
 * <p>Day (ticks 0–12000): sun texture.
 * <p>Night (ticks 12000–24000): moon texture.
 * <p>Crossfade over 1000 ticks at each transition (dusk at 12000, dawn at 0/24000).
 */
public class SolunaStaffModel extends DefaultedItemGeoModel<SolunaStaffItem> {

    private static final Identifier SUN_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/item/soluna_staff_sun.png");
    private static final Identifier MOON_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/item/soluna_staff_moon.png");

    /** Half-width of the fade window in ticks (500 ticks = ~25 seconds each side). */
    private static final int FADE_HALF = 500;

    public SolunaStaffModel() {
        super(Identifier.fromNamespaceAndPath(Ironhold.MODID, "soluna_staff"));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return isNight() ? MOON_TEXTURE : SUN_TEXTURE;
    }

    /** Returns true when the moon texture should be the primary. */
    static boolean isNight() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        long t = mc.level.getOverworldClockTime() % 24000;
        return t >= 12000;
    }

    /**
     * Returns the current fade alpha (0.0 = fully transparent, 1.0 = fully opaque).
     * Dips toward 0 at each swap point so the texture change is hidden by the fade.
     */
    static float getFadeAlpha() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 1f;
        long t = mc.level.getOverworldClockTime() % 24000;

        // Distance to the dusk swap (tick 12000)
        float duskDist = Math.abs(t - 12000);

        // Distance to the dawn swap (tick 0 / 24000), wrapping
        float dawnDist = Math.min(t, 24000 - t);

        // Use whichever transition point is closer
        float nearest = Math.min(duskDist, dawnDist);

        if (nearest >= FADE_HALF) return 1f;

        // Smoothstep for a gentle fade
        float progress = nearest / FADE_HALF;
        return Mth.clamp(progress * progress * (3f - 2f * progress), 0.05f, 1f);
    }
}
