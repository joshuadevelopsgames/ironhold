package milkucha.trmt.network;

import milkucha.trmt.TRMT;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge payload registration for the TRMT port.
 *
 * <p>Equivalent of upstream's {@code PayloadTypeRegistry} block in
 * {@code TRMT.onInitialize()} plus the client-side
 * {@code ClientPlayNetworking.registerGlobalReceiver(...)} calls in
 * {@code TRMTClient.onInitializeClient()} — both directions live here.
 *
 * <p>The client handler bodies live in {@link milkucha.trmt.client.TRMTClient}.
 * They are referenced as static method references; the {@code milkucha.trmt.client}
 * package is never loaded on dedicated servers because the method-reference
 * resolution is lazy.
 */
public final class TRMTPayloads {
    private TRMTPayloads() {}

    /** Bumped whenever the wire format changes. */
    private static final String VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(TRMT.MOD_ID).versioned(VERSION);

        registrar.playToClient(
                SyncChunkPayload.ID,
                SyncChunkPayload.CODEC,
                milkucha.trmt.client.TRMTClient::onSyncChunk);

        registrar.playToClient(
                UpdateStagePayload.ID,
                UpdateStagePayload.CODEC,
                milkucha.trmt.client.TRMTClient::onUpdateStage);

        registrar.configurationToClient(
                VersionCheckPayload.ID,
                VersionCheckPayload.CODEC,
                milkucha.trmt.client.TRMTClient::onVersionCheck);

        registrar.configurationToServer(
                VersionResponsePayload.ID,
                VersionResponsePayload.CODEC,
                TRMTPayloads::onVersionResponse);

        TRMT.LOGGER.debug("TRMT payloads registered");
    }

    /**
     * Server-side: client replied with its version. Upstream disconnects on mismatch;
     * we currently just log because the configuration-phase send (server → client)
     * isn't wired yet — see TODO in {@link TRMT}.
     */
    private static void onVersionResponse(VersionResponsePayload payload, IPayloadContext ctx) {
        TRMT.LOGGER.debug("TRMT client version: {}", payload.version());
    }
}
