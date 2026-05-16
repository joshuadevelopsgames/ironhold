package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → Server: the player tapped the "Talk" button in the Warden dialogue screen. */
public record WardenPttTogglePayload(
    int entityId
) implements CustomPacketPayload {

    public static final Type<WardenPttTogglePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "warden_ptt_toggle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WardenPttTogglePayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WardenPttTogglePayload::entityId,
            WardenPttTogglePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
