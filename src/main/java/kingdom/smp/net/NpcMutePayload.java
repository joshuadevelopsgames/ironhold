package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Bidirectional payload for the per-NPC mute toggle.
 *
 * <ul>
 *   <li><strong>Client → Server</strong>: player tapped the Mute button on a
 *       dialogue screen — server updates {@link kingdom.smp.ai.NpcMuteRegistry}.</li>
 *   <li><strong>Server → Client</strong>: server tells the client the current
 *       muted state when a dialogue screen opens, so the button reflects the
 *       persisted setting from the start.</li>
 * </ul>
 *
 * {@code npcTag} matches {@link kingdom.smp.ai.NpcChatPartner#tag()}.
 */
public record NpcMutePayload(
    String npcTag,
    boolean muted
) implements CustomPacketPayload {

    public static final Type<NpcMutePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "npc_mute"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NpcMutePayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, NpcMutePayload::npcTag,
            ByteBufCodecs.BOOL,        NpcMutePayload::muted,
            NpcMutePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
