package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * Bidirectional: client -> server when the local player triggers the point
 * emote (the carried UUID is ignored server-side; the server uses the sender),
 * then server -> nearby clients carrying the pointing player's UUID so they
 * play the same procedural pose.
 */
public record PointEmotePayload(UUID player) implements CustomPacketPayload {

    public static final Type<PointEmotePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "point_emote"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PointEmotePayload> STREAM_CODEC =
        StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, PointEmotePayload::player,
            PointEmotePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
