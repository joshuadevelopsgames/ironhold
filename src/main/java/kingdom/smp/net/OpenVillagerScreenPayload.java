package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server → Client (interacting player only): opens the villager dialogue screen. */
public record OpenVillagerScreenPayload(
    String villagerName,
    String profession,
    String dialogue,
    int moodMillis,
    int entityId
) implements CustomPacketPayload {

    public static final Type<OpenVillagerScreenPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "open_villager_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenVillagerScreenPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenVillagerScreenPayload::villagerName,
            ByteBufCodecs.STRING_UTF8, OpenVillagerScreenPayload::profession,
            ByteBufCodecs.STRING_UTF8, OpenVillagerScreenPayload::dialogue,
            ByteBufCodecs.VAR_INT,     OpenVillagerScreenPayload::moodMillis,
            ByteBufCodecs.VAR_INT,     OpenVillagerScreenPayload::entityId,
            OpenVillagerScreenPayload::new);

    /** Encode mood float (-1..1) as millis integer. */
    public static int encodeMood(float mood) {
        return Math.round(mood * 1000f);
    }

    public float decodeMood() {
        return moodMillis / 1000f;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
