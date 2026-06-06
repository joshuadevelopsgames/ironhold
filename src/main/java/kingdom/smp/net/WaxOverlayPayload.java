package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server → client: play a brief orange "just waxed" overlay on the given block. */
public record WaxOverlayPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<WaxOverlayPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "wax_overlay"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WaxOverlayPayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, WaxOverlayPayload::pos,
            WaxOverlayPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
