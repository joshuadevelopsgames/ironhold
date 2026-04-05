package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → Server: player has chosen a class from the selection screen.
 */
public record ClassChoicePayload(int classIndex) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClassChoicePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "class_choice"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClassChoicePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ClassChoicePayload::classIndex,
                    ClassChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
