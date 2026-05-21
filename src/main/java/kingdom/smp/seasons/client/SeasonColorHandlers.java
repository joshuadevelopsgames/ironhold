package kingdom.smp.seasons.client;

import com.mojang.logging.LogUtils;
import kingdom.smp.seasons.Season;
import kingdom.smp.seasons.SeasonState;
import kingdom.smp.seasons.Seasons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

import java.lang.reflect.Field;

/**
 * Swaps {@code BiomeColors.GRASS_COLOR_RESOLVER} and {@code FOLIAGE_COLOR_RESOLVER} with seasonal
 * wrappers at client startup. Each wrapper blends the current sub-season's overlay on top of the
 * vanilla biome color.
 *
 * <p>Resolver is called per-block during chunk mesh rebuilds, so the blend must stay cheap —
 * no registry lookups or tag tests here. Biome opt-outs can be added later via a cached
 * Set&lt;Biome&gt; built on world load.
 *
 * <p>Forces a chunk rebuild on sub-season change so visible color refreshes immediately.
 */
public final class SeasonColorHandlers {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Season.SubSeason lastClientSubSeason = null;

    private SeasonColorHandlers() {}

    public static void install() {
        ColorResolver vanillaGrass    = BiomeColors.GRASS_COLOR_RESOLVER;
        ColorResolver vanillaFoliage  = BiomeColors.FOLIAGE_COLOR_RESOLVER;

        ColorResolver seasonalGrass = (biome, x, z) -> blend(vanillaGrass.getColor(biome, x, z), true);
        ColorResolver seasonalFoliage = (biome, x, z) -> blend(vanillaFoliage.getColor(biome, x, z), false);

        if (!setStatic(BiomeColors.class, "GRASS_COLOR_RESOLVER", seasonalGrass)) {
            LOGGER.warn("[seasons] could not swap GRASS_COLOR_RESOLVER — grass tint will not change with season");
        }
        if (!setStatic(BiomeColors.class, "FOLIAGE_COLOR_RESOLVER", seasonalFoliage)) {
            LOGGER.warn("[seasons] could not swap FOLIAGE_COLOR_RESOLVER — leaf tint will not change with season");
        }
    }

    private static int blend(int vanilla, boolean grass) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return vanilla;
        if (!Seasons.isEnabled(mc.level)) return vanilla;

        SeasonState state = Seasons.current(mc.level);
        Season.SubSeason sub = state.subSeason();
        if (!sub.hasOverlay()) return vanilla;

        int overlay = grass ? sub.grassOverlay() : sub.foliageOverlay();
        return mix(vanilla, overlay, sub.saturation());
    }

    private static int mix(int vanilla, int overlay, float strength) {
        float s = Mth.clamp(strength, 0f, 1f);
        float inv = 1f - s;
        int vr = (vanilla >> 16) & 0xFF, vg = (vanilla >> 8) & 0xFF, vb = vanilla & 0xFF;
        int or = (overlay >> 16) & 0xFF, og = (overlay >> 8) & 0xFF, ob = overlay & 0xFF;
        int r = Math.round(vr * inv + or * s);
        int g = Math.round(vg * inv + og * s);
        int b = Math.round(vb * inv + ob * s);
        return (r << 16) | (g << 8) | b;
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        SeasonClientState.onClientTick();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Season.SubSeason now = Seasons.current(mc.level).subSeason();
        if (lastClientSubSeason != now) {
            lastClientSubSeason = now;
            if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SeasonClientState.reset();
        lastClientSubSeason = null;
    }

    private static boolean setStatic(Class<?> owner, String fieldName, Object value) {
        try {
            Field f = owner.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(null, value);
            return true;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return false;
        }
    }
}
