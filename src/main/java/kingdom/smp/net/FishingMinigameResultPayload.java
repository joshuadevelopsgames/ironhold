package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → Server: result of the fishing bite minigame. Server resolves the
 * pending session by either calling vanilla {@code retrieve()} (won) or
 * {@code discard()} (lost / closed / timed out).
 */
public record FishingMinigameResultPayload(boolean won) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FishingMinigameResultPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "fishing_minigame_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FishingMinigameResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, FishingMinigameResultPayload::won,
                    FishingMinigameResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
