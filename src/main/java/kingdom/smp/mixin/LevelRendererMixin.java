package kingdom.smp.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overrides sky rendering in the Ebonwood Hollow biome to simulate perpetual night.
 *
 * SkyRenderState angles are in RADIANS (converted from degrees by SkyRenderer.extractRenderState).
 * Sun and moon are always 180° (PI radians) apart. Swapping their angles puts the moon
 * exactly where the sun was. Stars don't need modification — they're naturally correct.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    private static final ResourceKey<Biome> EBONWOOD_HOLLOW = ResourceKey.create(
        Registries.BIOME, Identifier.fromNamespaceAndPath("ironhold", "ebonwood_hollow"));

    private static final ResourceKey<Level> MOON_DIMENSION = ResourceKey.create(
        Registries.DIMENSION, Identifier.fromNamespaceAndPath("ironhold", "moon_dimension"));

    @Shadow
    @Final
    private LevelRenderState levelRenderState;

    private float skyBlend = 0.0f;
    private static final float SKY_BLEND_SPEED = 0.02f;

    @Inject(method = "extractLevel", at = @At("RETURN"))
    private void ironhold$overrideSkyForEbonwood(DeltaTracker deltaTracker, Camera camera, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // Moon dimension: pure space — no sky, no sun, no moon, only stars everywhere.
        if (mc.level.dimension().equals(MOON_DIMENSION)) {
            SkyRenderState sky = levelRenderState.skyRenderState;
            sky.skybox = DimensionType.Skybox.OVERWORLD; // overworld path is the one that draws stars
            sky.rainBrightness = 0.0f;                   // sun & moon alpha -> fully hidden
            sky.starBrightness = 1.0f;                   // stars at full brightness, regardless of time
            sky.skyColor = 0xFF000000;                   // black sky disc, no blue gradient
            sky.sunriseAndSunsetColor = 0;               // no horizon glow
            sky.shouldRenderDarkDisc = false;            // don't occlude stars below the horizon
            return;
        }

        boolean inEbonwood = mc.level.getBiome(mc.player.blockPosition()).is(EBONWOOD_HOLLOW);

        if (inEbonwood && skyBlend < 1.0f) {
            skyBlend = Math.min(1.0f, skyBlend + SKY_BLEND_SPEED);
        } else if (!inEbonwood && skyBlend > 0.0f) {
            skyBlend = Math.max(0.0f, skyBlend - SKY_BLEND_SPEED);
        }

        if (skyBlend <= 0.0f) return;

        SkyRenderState sky = levelRenderState.skyRenderState;
        float t = skyBlend;

        // Determine if it's currently daytime by checking sun position.
        // Sun angle 0 = overhead, PI = underground. Sun is "up" when angle < PI/2 or > 3PI/2.
        float origSun = sky.sunAngle;
        boolean isDaytime = origSun < (float)(Math.PI / 2.0) || origSun > (float)(3.0 * Math.PI / 2.0);

        if (isDaytime) {
            // Daytime: swap sun and moon so moon appears where sun is
            float origMoon = sky.moonAngle;
            sky.sunAngle = origMoon;
            sky.moonAngle = origSun;

            // Crossfade: fade out sun, fade in moon
            if (t < 0.5f) {
                // First half: dim everything to hide the sun
                sky.rainBrightness = lerp(sky.rainBrightness, 1.0f, t * 2.0f);
                sky.starBrightness = 0.0f;
            } else {
                // Second half: reveal the moon and stars
                sky.rainBrightness = 0.0f;  // fully bright celestial bodies
                sky.starBrightness = lerp(0.0f, 0.5f, (t - 0.5f) * 2.0f);
            }
        } else {
            // Nighttime: sky already looks like night — just ensure stars visible
            sky.starBrightness = lerp(sky.starBrightness, 0.5f, t);
            sky.rainBrightness = 0.0f;
        }

        // Always: dark sky, no sunrise/sunset glow
        sky.skyColor = lerpColor(sky.skyColor, 0xFF050010, t);
        sky.sunriseAndSunsetColor = 0;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static int lerpColor(int from, int to, float t) {
        int fR = (from >> 16) & 0xFF, fG = (from >> 8) & 0xFF, fB = from & 0xFF;
        int tR = (to >> 16) & 0xFF, tG = (to >> 8) & 0xFF, tB = to & 0xFF;
        int r = (int) lerp(fR, tR, t);
        int g = (int) lerp(fG, tG, t);
        int b = (int) lerp(fB, tB, t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
