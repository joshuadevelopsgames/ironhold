package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> Client: a Kingdom Villager emits a thought bubble (emote icon).
 * Client displays this as an icon above the villager's head.
 */
public record VillagerEmotePayload(
    int entityId,
    int emoteOrdinal
) implements CustomPacketPayload {

    public static final Type<VillagerEmotePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "villager_emote"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VillagerEmotePayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VillagerEmotePayload::entityId,
            ByteBufCodecs.VAR_INT, VillagerEmotePayload::emoteOrdinal,
            VillagerEmotePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
