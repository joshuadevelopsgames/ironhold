package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client: append the next NPC line to the already-open Warden screen.
 * {@code status} can be either {@code "reply"} (new line to typewriter-reveal)
 * or {@code "thinking"} (hint to show a typing indicator).
 */
public record UpdateWardenScreenPayload(
    int entityId,
    String status,
    String dialogue
) implements CustomPacketPayload {

    public static final String STATUS_REPLY    = "reply";
    public static final String STATUS_THINKING = "thinking";
    public static final String STATUS_HEARD    = "heard";
    /** Server tells the client to close the screen — used when the NPC entity is removed mid-conversation. */
    public static final String STATUS_CLOSE    = "close";

    public static final Type<UpdateWardenScreenPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "update_warden_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateWardenScreenPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,     UpdateWardenScreenPayload::entityId,
            ByteBufCodecs.STRING_UTF8, UpdateWardenScreenPayload::status,
            ByteBufCodecs.STRING_UTF8, UpdateWardenScreenPayload::dialogue,
            UpdateWardenScreenPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
