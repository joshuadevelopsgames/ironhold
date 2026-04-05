package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client: tells the client to open the class selection screen.
 */
public record OpenClassSelectionPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenClassSelectionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "open_class_select"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenClassSelectionPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenClassSelectionPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
