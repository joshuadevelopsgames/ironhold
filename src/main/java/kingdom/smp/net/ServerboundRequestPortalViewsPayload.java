package kingdom.smp.net;

import java.util.List;
import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → Server: the set of cross-dimensional views the client currently needs streamed — one
 * {@link Entry} per destination dimension, with the chunk-coordinate centre to stream around (the
 * portal's destination anchor). Sent by {@code PortalDetector} whenever the visible-portal set
 * changes; an empty list tells the server to close all of this player's views.
 */
public record ServerboundRequestPortalViewsPayload(List<Entry> views) implements CustomPacketPayload {

    /** One requested view: stream {@code dimId} around chunk ({@code centerChunkX}, {@code centerChunkZ}). */
    public record Entry(Identifier dimId, int centerChunkX, int centerChunkZ) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC =
            StreamCodec.composite(
                Identifier.STREAM_CODEC, Entry::dimId,
                ByteBufCodecs.VAR_INT, Entry::centerChunkX,
                ByteBufCodecs.VAR_INT, Entry::centerChunkZ,
                Entry::new);
    }

    public static final Type<ServerboundRequestPortalViewsPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "request_portal_views"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundRequestPortalViewsPayload> STREAM_CODEC =
        StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), ServerboundRequestPortalViewsPayload::views,
            ServerboundRequestPortalViewsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
