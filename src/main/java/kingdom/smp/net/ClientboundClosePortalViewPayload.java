package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client: tear down the secondary view of dimension {@code dimId} (no player still needs to
 * see through a portal into it), freeing the secondary {@code ClientLevel} and its renderer.
 */
public record ClientboundClosePortalViewPayload(Identifier dimId) implements CustomPacketPayload {

    public static final Type<ClientboundClosePortalViewPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "close_portal_view"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundClosePortalViewPayload> STREAM_CODEC =
        StreamCodec.composite(
            Identifier.STREAM_CODEC, ClientboundClosePortalViewPayload::dimId,
            ClientboundClosePortalViewPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
