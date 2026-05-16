package kingdom.smp.ai.svc;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import kingdom.smp.Ironhold;

/**
 * SVC plugin entry point. Discovered by SVC's NeoForge mod scan via the
 * {@link ForgeVoicechatPlugin} annotation. Captures the server-side API
 * so {@link SvcVoiceBridgeImpl} can play TTS audio through entity channels.
 *
 * <p>This class is only loaded when the {@code voicechat} mod is present,
 * since {@link kingdom.smp.ai.SvcVoiceBridge} dispatches into it reflectively.
 */
@ForgeVoicechatPlugin
public final class IronholdVoicechatPlugin implements VoicechatPlugin {

    /**
     * SVC VolumeCategory id used for every voiced NPC's audio channel. The
     * client sees one slider labeled "Voiced NPCs" in the SVC volume menu;
     * muting or attenuating it controls the entire NPC voice channel.
     *
     * <p>SVC enforces the regex {@code ^[a-z_]{1,16}$} on category ids — only
     * lowercase letters and underscores, up to 16 characters. Anything else
     * makes {@code VolumeCategory.Builder.build()} throw silently inside the
     * server-started handler and the category never registers, even though
     * {@code AudioChannel.setCategory(...)} still happily stamps the id onto
     * outgoing packets (audio plays, slider missing).
     */
    public static final String NPC_VOICE_CATEGORY_ID = "ironhold_npcs";

    private static volatile VoicechatServerApi serverApi;

    public static VoicechatServerApi getApi() {
        return serverApi;
    }

    @Override
    public String getPluginId() {
        return "ironhold";
    }

    @Override
    public void initialize(VoicechatApi api) {
        Ironhold.LOGGER.info("[Kangarude] SVC plugin initialized");
    }

    @Override
    public void registerEvents(EventRegistration r) {
        r.registerEvent(VoicechatServerStartedEvent.class, e -> {
            serverApi = e.getVoicechat();
            // Register the "Voiced NPCs" category so the client sees a dedicated
            // slider in SVC's volume menu that controls all NPC audio at once.
            // Wrap the builder call so a future regex/length violation logs loudly
            // instead of silently aborting the rest of this handler.
            try {
                VolumeCategory category = serverApi.volumeCategoryBuilder()
                    .setId(NPC_VOICE_CATEGORY_ID)
                    .setName("Voiced NPCs")
                    .setDescription("Audio from Ironhold NPCs (Kangarude, Warden Halric, Vesper)")
                    .build();
                serverApi.registerVolumeCategory(category);
                Ironhold.LOGGER.info("[Ironhold] SVC server API ready — NPC voice category '{}' registered",
                    NPC_VOICE_CATEGORY_ID);
            } catch (Throwable t) {
                Ironhold.LOGGER.error("[Ironhold] Failed to register SVC volume category — slider won't appear", t);
            }
        });
        r.registerEvent(VoicechatServerStoppedEvent.class, e -> {
            serverApi = null;
            SvcVoiceBridgeImpl.shutdown();
        });
        // Stream incoming mic audio for the active Kangarude partner (if any) into Whisper STT.
        r.registerEvent(MicrophonePacketEvent.class, MicrophoneListener::onMicrophone);
    }
}
