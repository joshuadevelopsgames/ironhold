package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → server: withdraw {@code amount} coins from the Coin Purse in menu slot {@code slotIndex}. */
public record CoinPurseWithdrawPayload(int slotIndex, int amount) implements CustomPacketPayload {

    public static final Type<CoinPurseWithdrawPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "coin_purse_withdraw"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CoinPurseWithdrawPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CoinPurseWithdrawPayload::slotIndex,
            ByteBufCodecs.VAR_INT, CoinPurseWithdrawPayload::amount,
            CoinPurseWithdrawPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
