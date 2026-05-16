package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client: current sneak-detection state for the sneak-eye HUD.
 * State encoding matches client-side {@code SneakEyeHud.State} ordinals.
 */
public record SneakDetectionPayload(byte state) implements CustomPacketPayload {

    public static final Type<SneakDetectionPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "sneak_detection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SneakDetectionPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.BYTE, SneakDetectionPayload::state,
            SneakDetectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
