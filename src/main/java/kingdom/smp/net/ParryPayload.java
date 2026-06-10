package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → server: player tapped the Parry key. Server validates cooldown + opens the parry window. */
public record ParryPayload() implements CustomPacketPayload {

    public static final Type<ParryPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "parry"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ParryPayload> STREAM_CODEC =
        StreamCodec.unit(new ParryPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
