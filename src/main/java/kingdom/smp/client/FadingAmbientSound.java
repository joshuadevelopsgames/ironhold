package kingdom.smp.client;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

/**
 * A looping ambient sound that fades in when started and fades out when stopped.
 */
public class FadingAmbientSound extends AbstractSoundInstance implements TickableSoundInstance {

    private final float targetVolume;
    private final float fadeSpeed;
    private boolean stopping = false;
    private boolean done = false;

    public FadingAmbientSound(Identifier soundId, float targetVolume, float fadeSpeed) {
        super(soundId, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.targetVolume = targetVolume;
        this.fadeSpeed = fadeSpeed;
        this.volume = 0.0F; // start silent
        this.looping = true;
        this.attenuation = Attenuation.NONE;
        this.relative = true;
    }

    /** Signal this sound to fade out and stop. */
    public void fadeOut() {
        this.stopping = true;
    }

    public boolean isFadingOut() {
        return stopping;
    }

    @Override
    public void tick() {
        if (stopping) {
            volume -= fadeSpeed;
            if (volume <= 0.0F) {
                volume = 0.0F;
                done = true;
            }
        } else if (volume < targetVolume) {
            volume = Math.min(targetVolume, volume + fadeSpeed);
        }
    }

    @Override
    public boolean isStopped() {
        return done;
    }
}
