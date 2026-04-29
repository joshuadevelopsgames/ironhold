package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server → Client: tells the client to open the King's Console screen. */
public record OpenConsolePayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenConsolePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "open_console"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenConsolePayload> STREAM_CODEC =
            StreamCodec.unit(new OpenConsolePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
