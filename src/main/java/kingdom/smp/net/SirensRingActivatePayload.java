package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → server: player pressed the Siren's Ring ability key. */
public record SirensRingActivatePayload() implements CustomPacketPayload {

    public static final Type<SirensRingActivatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "sirens_ring_activate"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SirensRingActivatePayload> STREAM_CODEC =
            StreamCodec.unit(new SirensRingActivatePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
