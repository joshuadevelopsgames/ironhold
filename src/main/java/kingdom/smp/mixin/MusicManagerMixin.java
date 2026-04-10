package kingdom.smp.mixin;

import kingdom.smp.Ironhold;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses vanilla music in the Ebonwood Hollow biome.
 * Stops the current music track and prevents new ones from starting.
 * Does NOT affect the ambient sound category (where our custom loop plays).
 */
@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow private SoundInstance currentMusic;
    @Shadow private float currentGain;

    @Unique private int ironhold$fadeTimer = 0;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void ironhold$suppressMusicInEbonwood(CallbackInfo ci) {
        if (minecraft.level == null || minecraft.player == null) return;

        boolean inEbonwood = minecraft.level.getBiome(minecraft.player.blockPosition())
                .is(Ironhold.EBONWOOD_HOLLOW);
        if (!inEbonwood) {
            ironhold$fadeTimer = 0;
            return;
        }

        // Gradually fade out any playing vanilla music track
        if (currentMusic != null) {
            ironhold$fadeTimer++;
            // After ~8 seconds (160 ticks), stop the track
            if (ironhold$fadeTimer > 160) {
                minecraft.getSoundManager().stop(currentMusic);
                currentMusic = null;
                currentGain = 1.0F;
            }
        }

        // Skip the rest of MusicManager.tick() so no new music starts
        ci.cancel();
    }
}
