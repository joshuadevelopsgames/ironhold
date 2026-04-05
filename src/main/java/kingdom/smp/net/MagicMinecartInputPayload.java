package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client -> Server key state while riding MagicMinecartEntity. */
public record MagicMinecartInputPayload(
    boolean forward,
    boolean backward,
    boolean left,
    boolean right,
    boolean jump,
    boolean sprint
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MagicMinecartInputPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "magic_minecart_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MagicMinecartInputPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.BOOL, MagicMinecartInputPayload::forward,
            ByteBufCodecs.BOOL, MagicMinecartInputPayload::backward,
            ByteBufCodecs.BOOL, MagicMinecartInputPayload::left,
            ByteBufCodecs.BOOL, MagicMinecartInputPayload::right,
            ByteBufCodecs.BOOL, MagicMinecartInputPayload::jump,
            ByteBufCodecs.BOOL, MagicMinecartInputPayload::sprint,
            MagicMinecartInputPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
