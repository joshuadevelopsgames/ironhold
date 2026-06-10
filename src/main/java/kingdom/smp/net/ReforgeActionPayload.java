package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → server: reroll the main-hand gear's unlocked affixes. Bit {@code i} of
 * {@code lockMask} set = keep the affix at index {@code i}. The server re-validates rank,
 * lock count, and coins — the payload is a request, not a grant.
 */
public record ReforgeActionPayload(int lockMask) implements CustomPacketPayload {

    public static final Type<ReforgeActionPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "reforge_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReforgeActionPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ReforgeActionPayload::lockMask,
            ReforgeActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
