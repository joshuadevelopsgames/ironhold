package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → Server: request to respec ALL profession progress at once.
 * Server applies {@link kingdom.smp.skill.PlayerSkillState#respecAll} which clears every
 * profession's rank and refunds {@code (totalSpent - 1)} points — the lossy respec cost
 * from spec §3.5, applied once globally rather than once per profession.
 */
public record RespecSkillPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RespecSkillPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "respec_skill"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RespecSkillPayload> STREAM_CODEC =
            StreamCodec.unit(new RespecSkillPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
