package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → server: player tapped the Dodge key. Carries the current movement-input state so the
 * server can hop in the intended direction (neutral input = backhop). Server validates the cooldown.
 */
public record DodgePayload(boolean forward, boolean back, boolean left, boolean right)
    implements CustomPacketPayload {

    public static final Type<DodgePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "dodge"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DodgePayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.BOOL, DodgePayload::forward,
            ByteBufCodecs.BOOL, DodgePayload::back,
            ByteBufCodecs.BOOL, DodgePayload::left,
            ByteBufCodecs.BOOL, DodgePayload::right,
            DodgePayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
