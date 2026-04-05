package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client: tells the client to open the kingdom selection screen.
 */
public record OpenKingdomSelectionPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenKingdomSelectionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "open_kingdom_select"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenKingdomSelectionPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenKingdomSelectionPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
