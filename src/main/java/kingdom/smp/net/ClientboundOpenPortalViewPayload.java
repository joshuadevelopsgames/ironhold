package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client: open (or refresh) a render-only secondary view of dimension {@code dimId}, carrying
 * the metadata the client needs to construct its {@code ClientLevel} (the {@code ClientLevel} ctor
 * requires these; registries come from the shared real connection). {@code centerChunkX/Z} seed the
 * secondary chunk cache's view centre so streamed chunks pass its in-range check. Sent once when a
 * player first needs to see into this dimension; chunks then follow via {@link ClientboundChunkRedirectPayload}.
 */
public record ClientboundOpenPortalViewPayload(Identifier dimId, Identifier dimTypeId,
                                               int chunkRadius, long biomeZoomSeed, int seaLevel,
                                               int centerChunkX, int centerChunkZ)
        implements CustomPacketPayload {

    public static final Type<ClientboundOpenPortalViewPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "open_portal_view"));

    // Hand-written codec: 7 fields exceeds StreamCodec.composite's arity, so encode explicitly.
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenPortalViewPayload> STREAM_CODEC =
        StreamCodec.of(
            (buf, p) -> {
                Identifier.STREAM_CODEC.encode(buf, p.dimId());
                Identifier.STREAM_CODEC.encode(buf, p.dimTypeId());
                buf.writeVarInt(p.chunkRadius());
                buf.writeVarLong(p.biomeZoomSeed());
                buf.writeVarInt(p.seaLevel());
                buf.writeVarInt(p.centerChunkX());
                buf.writeVarInt(p.centerChunkZ());
            },
            buf -> new ClientboundOpenPortalViewPayload(
                Identifier.STREAM_CODEC.decode(buf),
                Identifier.STREAM_CODEC.decode(buf),
                buf.readVarInt(),
                buf.readVarLong(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
