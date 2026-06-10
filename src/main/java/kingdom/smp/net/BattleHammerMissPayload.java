package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → server: the wielder whiffed a swing into the air while carrying a charged
 *  Battle Hammer (a "full miss"), so the server should break its forge-power combo.
 *  Misses are only detectable client-side (LeftClickEmpty), hence the round-trip. */
public record BattleHammerMissPayload() implements CustomPacketPayload {

    public static final Type<BattleHammerMissPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "battle_hammer_miss"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BattleHammerMissPayload> STREAM_CODEC =
            StreamCodec.unit(new BattleHammerMissPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
