package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → server: request a mid-air boost from Breeze in a Bottle (validated on server). */
public record CloudDoubleJumpPayload() implements CustomPacketPayload {

    public static final Type<CloudDoubleJumpPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "cloud_double_jump"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CloudDoubleJumpPayload> STREAM_CODEC =
            StreamCodec.unit(new CloudDoubleJumpPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
