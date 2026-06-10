package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → server: player tapped the Accessory Active key (e.g. Ender Regalia blink). */
public record AccessoryActivatePayload() implements CustomPacketPayload {

    public static final Type<AccessoryActivatePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "accessory_activate"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AccessoryActivatePayload> STREAM_CODEC =
        StreamCodec.unit(new AccessoryActivatePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
