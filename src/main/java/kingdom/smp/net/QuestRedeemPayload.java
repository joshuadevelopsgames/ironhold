package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → server: REDEEM clicked on the quest board for {@code questId}. The server re-validates
 * against the open {@code QuestBoardMenu} and {@code QuestService.redeem} (status, live items) —
 * the payload is a request, not a grant.
 */
public record QuestRedeemPayload(String questId) implements CustomPacketPayload {

    public static final Type<QuestRedeemPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "quest_redeem"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestRedeemPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, QuestRedeemPayload::questId,
            QuestRedeemPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
