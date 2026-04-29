package kingdom.smp.net;

import kingdom.smp.Ironhold;
import kingdom.smp.skill.Profession;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → Server: request to spend a profession point in the named tree. The server
 * validates available points and applies the spend, then echoes the updated state back
 * via {@link SyncSkillStatePayload}.
 */
public record SpendSkillPointPayload(Profession profession) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SpendSkillPointPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "spend_skill_point"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpendSkillPointPayload> STREAM_CODEC =
            StreamCodec.composite(
                    Profession.STREAM_CODEC,
                    SpendSkillPointPayload::profession,
                    SpendSkillPointPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
