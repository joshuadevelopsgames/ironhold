package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → server: player pressed the Kanga push-to-talk key (toggle). */
public record KangaPushToTalkPayload() implements CustomPacketPayload {

    public static final Type<KangaPushToTalkPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "kanga_ptt"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KangaPushToTalkPayload> STREAM_CODEC =
            StreamCodec.unit(new KangaPushToTalkPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
