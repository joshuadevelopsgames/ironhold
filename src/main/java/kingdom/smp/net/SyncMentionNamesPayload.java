package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Server → Client: the set of voiced-NPC names that chat {@code @mentions} can
 * resolve to, so the client chat box can tab-complete them. Online player names
 * already come from the vanilla tab list, so only NPC names are synced here.
 */
public record SyncMentionNamesPayload(List<String> names) implements CustomPacketPayload {

    public static final Type<SyncMentionNamesPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "sync_mention_names"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMentionNamesPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncMentionNamesPayload::names,
            SyncMentionNamesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
