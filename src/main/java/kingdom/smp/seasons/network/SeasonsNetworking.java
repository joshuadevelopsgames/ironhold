package kingdom.smp.seasons.network;

import kingdom.smp.Ironhold;
import kingdom.smp.seasons.client.SeasonClientState;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SeasonsNetworking {
    private SeasonsNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Ironhold.MODID).versioned("1");
        registrar.playToClient(SyncSeasonPayload.TYPE, SyncSeasonPayload.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(() -> SeasonClientState.receive(payload)));
    }
}
