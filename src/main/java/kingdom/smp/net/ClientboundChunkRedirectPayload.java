package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.Identifier;

/**
 * Server → Client: a vanilla {@link ClientboundLevelChunkWithLightPacket} re-addressed to the secondary
 * dimension {@code dimId} instead of the player's current level. By composing the vanilla packet's own
 * {@code STREAM_CODEC} we reuse all of Mojang's chunk + light (de)serialisation; the client replays it
 * against the matching secondary {@code ClientLevel} (see {@code ClientDimensionStack}).
 */
public record ClientboundChunkRedirectPayload(Identifier dimId, ClientboundLevelChunkWithLightPacket chunk)
        implements CustomPacketPayload {

    public static final Type<ClientboundChunkRedirectPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "chunk_redirect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundChunkRedirectPayload> STREAM_CODEC =
        StreamCodec.composite(
            Identifier.STREAM_CODEC, ClientboundChunkRedirectPayload::dimId,
            ClientboundLevelChunkWithLightPacket.STREAM_CODEC, ClientboundChunkRedirectPayload::chunk,
            ClientboundChunkRedirectPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
