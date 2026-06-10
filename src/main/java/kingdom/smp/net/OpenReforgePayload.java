package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → client: open (or refresh) the blacksmith lock-and-reroll screen for the gear in the
 * player's main hand. The affix list itself is read client-side from the item's synced
 * {@code ironhold:affixes} component; this carries only what the client can't derive —
 * {@code locksAllowed} (Blacksmithing rank) and the current coin {@code cost}.
 */
public record OpenReforgePayload(int locksAllowed, int cost) implements CustomPacketPayload {

    public static final Type<OpenReforgePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "open_reforge"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenReforgePayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenReforgePayload::locksAllowed,
            ByteBufCodecs.VAR_INT, OpenReforgePayload::cost,
            OpenReforgePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
