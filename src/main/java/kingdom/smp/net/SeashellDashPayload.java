package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → server: player tapped the Seashell dash key while in the water. */
public record SeashellDashPayload() implements CustomPacketPayload {

    public static final Type<SeashellDashPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "seashell_dash"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SeashellDashPayload> STREAM_CODEC =
        StreamCodec.unit(new SeashellDashPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
