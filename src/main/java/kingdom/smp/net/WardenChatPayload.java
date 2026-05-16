package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → Server: the player typed a reply in the Warden dialogue screen. */
public record WardenChatPayload(
    int entityId,
    String message
) implements CustomPacketPayload {

    public static final Type<WardenChatPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "warden_chat"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WardenChatPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,     WardenChatPayload::entityId,
            ByteBufCodecs.STRING_UTF8, WardenChatPayload::message,
            WardenChatPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
