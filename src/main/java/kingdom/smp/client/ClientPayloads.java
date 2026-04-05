package kingdom.smp.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sends payloads to the integrated/dedicated server from the client. */
public final class ClientPayloads {
    private ClientPayloads() {}

    public static void sendToServer(CustomPacketPayload payload) {
        var mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundCustomPayloadPacket(payload));
        }
    }
}
