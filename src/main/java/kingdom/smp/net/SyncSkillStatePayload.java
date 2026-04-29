package kingdom.smp.net;

import kingdom.smp.Ironhold;
import kingdom.smp.skill.Profession;
import kingdom.smp.skill.ProfessionRank;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Server → Client: full snapshot of the player's profession-skill state. Sent on login and
 * after any change (spend, respec, milestone award).
 *
 * Encodes ranks as a flat list of (profession ordinal, rank ordinal) pairs since codec maps
 * over RegistryFriendlyByteBuf are slightly clumsier than primitive lists for small payloads.
 */
public record SyncSkillStatePayload(
        int unspentPoints,
        Map<Profession, ProfessionRank> ranks,
        int milestoneCount) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncSkillStatePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "sync_skill_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSkillStatePayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SyncSkillStatePayload decode(RegistryFriendlyByteBuf buf) {
                    int unspent = ByteBufCodecs.VAR_INT.decode(buf);
                    int rankCount = ByteBufCodecs.VAR_INT.decode(buf);
                    Map<Profession, ProfessionRank> ranks = new HashMap<>(rankCount);
                    for (int i = 0; i < rankCount; i++) {
                        Profession p = Profession.STREAM_CODEC.decode(buf);
                        ProfessionRank r = ProfessionRank.STREAM_CODEC.decode(buf);
                        ranks.put(p, r);
                    }
                    int milestones = ByteBufCodecs.VAR_INT.decode(buf);
                    return new SyncSkillStatePayload(unspent, ranks, milestones);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, SyncSkillStatePayload payload) {
                    ByteBufCodecs.VAR_INT.encode(buf, payload.unspentPoints);
                    ByteBufCodecs.VAR_INT.encode(buf, payload.ranks.size());
                    for (var entry : payload.ranks.entrySet()) {
                        Profession.STREAM_CODEC.encode(buf, entry.getKey());
                        ProfessionRank.STREAM_CODEC.encode(buf, entry.getValue());
                    }
                    ByteBufCodecs.VAR_INT.encode(buf, payload.milestoneCount);
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
