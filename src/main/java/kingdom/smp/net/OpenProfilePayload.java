package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client: tells the client to open the profile screen.
 */
public record OpenProfilePayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenProfilePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "open_profile"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenProfilePayload> STREAM_CODEC =
            StreamCodec.unit(new OpenProfilePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
