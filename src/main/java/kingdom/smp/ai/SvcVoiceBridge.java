package kingdom.smp.ai;

import kingdom.smp.Ironhold;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bridge between Kangarude's TTS pipeline and Simple Voice Chat.
 *
 * <p>SVC is a <b>soft dependency</b> — if it isn't on the runtime classpath
 * (e.g. there's no SVC build for the current Minecraft version yet), the
 * bridge logs once and silently drops audio. The dialogue text bubble still
 * shows up via {@code VillagerDialoguePayload}, so the NPC remains usable.
 *
 * <p>When SVC is present, the actual entity-channel + Opus-frame plumbing
 * lives in a separate impl class loaded reflectively to keep SVC types out
 * of the verifier when SVC isn't loaded.
 */
public final class SvcVoiceBridge {

    private static final String SVC_MOD_ID = "voicechat";
    private static final AtomicBoolean WARNED_MISSING = new AtomicBoolean(false);
    private static volatile Boolean svcPresent;

    private SvcVoiceBridge() {}

    public static boolean isAvailable() {
        Boolean cached = svcPresent;
        if (cached != null) return cached;
        boolean loaded = ModList.get().isLoaded(SVC_MOD_ID);
        svcPresent = loaded;
        return loaded;
    }

    /**
     * Play a clip of PCM-S16LE audio (mono, {@link ElevenLabsClient#SAMPLE_RATE_HZ})
     * as if the given entity were speaking it. Picks up SVC's spatial mixing
     * automatically — listeners hear the position, attenuation, occlusion.
     *
     * <p>Calls are queued per-entity so back-to-back lines don't talk over each
     * other.
     *
     * @return true if the audio was queued for playback; false if SVC is absent
     *         and the caller should fall back to a chat bubble only.
     */
    public static boolean speakAs(Entity entity, byte[] pcmS16le24kHz) {
        if (pcmS16le24kHz == null || pcmS16le24kHz.length == 0) return false;
        if (!isAvailable()) {
            if (WARNED_MISSING.compareAndSet(false, true)) {
                Ironhold.LOGGER.warn(
                    "[Kangarude] Simple Voice Chat is not loaded — voice output is disabled."
                  + " Install SVC for the current MC version to hear NPC dialogue.");
            }
            return false;
        }
        try {
            Class<?> impl = Class.forName("kingdom.smp.ai.svc.SvcVoiceBridgeImpl");
            Object result = impl.getMethod("speakAs", Entity.class, byte[].class)
                .invoke(null, entity, pcmS16le24kHz);
            return result instanceof Boolean b && b;
        } catch (Throwable t) {
            Ironhold.LOGGER.warn("[Kangarude] SVC bridge dispatch failed: {}", t.getMessage());
            return false;
        }
    }
}
