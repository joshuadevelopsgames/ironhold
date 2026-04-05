package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → Server: player has chosen a kingdom from the selection screen.
 */
public record KingdomChoicePayload(int kingdomIndex) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<KingdomChoicePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "kingdom_choice"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KingdomChoicePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, KingdomChoicePayload::kingdomIndex,
                    KingdomChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
