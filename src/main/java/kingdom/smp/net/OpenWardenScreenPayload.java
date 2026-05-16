package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client: open the interactive Warden dialogue screen with the
 * initial NPC line. The screen stays open after the line finishes so the
 * player can type or speak a reply; replies arrive via
 * {@link UpdateWardenScreenPayload}.
 */
public record OpenWardenScreenPayload(
    int entityId,
    String npcName,
    String npcTag,
    String subtitle,
    String dialogue,
    boolean muted
) implements CustomPacketPayload {

    public static final Type<OpenWardenScreenPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "open_warden_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenWardenScreenPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,     OpenWardenScreenPayload::entityId,
            ByteBufCodecs.STRING_UTF8, OpenWardenScreenPayload::npcName,
            ByteBufCodecs.STRING_UTF8, OpenWardenScreenPayload::npcTag,
            ByteBufCodecs.STRING_UTF8, OpenWardenScreenPayload::subtitle,
            ByteBufCodecs.STRING_UTF8, OpenWardenScreenPayload::dialogue,
            ByteBufCodecs.BOOL,        OpenWardenScreenPayload::muted,
            OpenWardenScreenPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
