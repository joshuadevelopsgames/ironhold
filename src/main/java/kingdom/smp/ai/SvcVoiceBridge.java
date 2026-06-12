package kingdom.smp.ai;

import kingdom.smp.Ironhold;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
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
            Method m = implMethod("speakAs", Entity.class, byte[].class);
            Object result = m == null ? null : m.invoke(null, entity, pcmS16le24kHz);
            return result instanceof Boolean b && b;
        } catch (Throwable t) {
            Ironhold.LOGGER.warn("[Kangarude] SVC bridge dispatch failed: {}", t.getMessage());
            return false;
        }
    }

    // ── Streaming playback ───────────────────────────────────────────────────
    // Mirrors SvcVoiceBridgeImpl.beginStream/feedStream/endStream. The handle is
    // an opaque Object so SVC types never leak through this facade.

    /** Open an incremental playback stream. Null when SVC is absent/not ready. */
    public static Object beginStreamAs(Entity entity) {
        if (!isAvailable()) {
            if (WARNED_MISSING.compareAndSet(false, true)) {
                Ironhold.LOGGER.warn(
                    "[Kangarude] Simple Voice Chat is not loaded — voice output is disabled."
                  + " Install SVC for the current MC version to hear NPC dialogue.");
            }
            return null;
        }
        try {
            Method m = implMethod("beginStream", Entity.class);
            return m == null ? null : m.invoke(null, entity);
        } catch (Throwable t) {
            Ironhold.LOGGER.warn("[Kangarude] SVC bridge beginStream failed: {}", t.getMessage());
            return null;
        }
    }

    /** Feed a PCM-S16LE @ 24kHz chunk into a stream from {@link #beginStreamAs}. */
    public static boolean feedStream(Object handle, byte[] pcmChunk) {
        if (handle == null) return false;
        try {
            Method m = implMethod("feedStream", Object.class, byte[].class);
            Object result = m == null ? null : m.invoke(null, handle, pcmChunk);
            return result instanceof Boolean b && b;
        } catch (Throwable t) {
            Ironhold.LOGGER.warn("[Kangarude] SVC bridge feedStream failed: {}", t.getMessage());
            return false;
        }
    }

    /** Close a stream; buffered audio drains normally. */
    public static void endStream(Object handle) {
        if (handle == null) return;
        try {
            Method m = implMethod("endStream", Object.class);
            if (m != null) m.invoke(null, handle);
        } catch (Throwable t) {
            Ironhold.LOGGER.warn("[Kangarude] SVC bridge endStream failed: {}", t.getMessage());
        }
    }

    // Cached per-signature Method handles — feedStream runs once per network
    // chunk, so repeating Class.forName/getMethod there would be pure waste.
    private static volatile Method speakAsMethod, beginStreamMethod, feedStreamMethod, endStreamMethod;

    private static Method implMethod(String name, Class<?>... params) throws Exception {
        Method cached = switch (name) {
            case "speakAs" -> speakAsMethod;
            case "beginStream" -> beginStreamMethod;
            case "feedStream" -> feedStreamMethod;
            case "endStream" -> endStreamMethod;
            default -> null;
        };
        if (cached != null) return cached;
        Class<?> impl = Class.forName("kingdom.smp.ai.svc.SvcVoiceBridgeImpl");
        Method m = impl.getMethod(name, params);
        switch (name) {
            case "speakAs" -> speakAsMethod = m;
            case "beginStream" -> beginStreamMethod = m;
            case "feedStream" -> feedStreamMethod = m;
            case "endStream" -> endStreamMethod = m;
        }
        return m;
    }
}
