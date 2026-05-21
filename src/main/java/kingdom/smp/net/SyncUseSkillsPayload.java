package kingdom.smp.net;

import kingdom.smp.Ironhold;
import kingdom.smp.skill.useskill.UseSkill;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.Map;

/**
 * Server → Client: snapshot of the player's use-to-level skill XP totals
 * (Pickpocket / Sneak / Fishing). Sent on login and periodically while playing.
 * The client derives level + progress from the XP via {@link kingdom.smp.skill.useskill.UseSkillCurve}.
 */
public record SyncUseSkillsPayload(Map<UseSkill, Float> xp) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncUseSkillsPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "sync_use_skills"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncUseSkillsPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SyncUseSkillsPayload decode(RegistryFriendlyByteBuf buf) {
                    int count = ByteBufCodecs.VAR_INT.decode(buf);
                    Map<UseSkill, Float> xp = new EnumMap<>(UseSkill.class);
                    for (int i = 0; i < count; i++) {
                        UseSkill skill = UseSkill.STREAM_CODEC.decode(buf);
                        float amount = ByteBufCodecs.FLOAT.decode(buf);
                        xp.put(skill, amount);
                    }
                    return new SyncUseSkillsPayload(xp);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, SyncUseSkillsPayload payload) {
                    ByteBufCodecs.VAR_INT.encode(buf, payload.xp.size());
                    for (var entry : payload.xp.entrySet()) {
                        UseSkill.STREAM_CODEC.encode(buf, entry.getKey());
                        ByteBufCodecs.FLOAT.encode(buf, entry.getValue());
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
