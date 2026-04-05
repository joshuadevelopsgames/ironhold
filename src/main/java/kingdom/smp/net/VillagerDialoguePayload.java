package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> Client: a Kingdom Villager speaks (AI dialogue or fallback).
 * Client displays this as a floating speech bubble above the villager.
 */
public record VillagerDialoguePayload(
    int entityId,
    String villagerName,
    String profession,
    String dialogue
) implements CustomPacketPayload {

    public static final Type<VillagerDialoguePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "villager_dialogue"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VillagerDialoguePayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,      VillagerDialoguePayload::entityId,
            ByteBufCodecs.STRING_UTF8,  VillagerDialoguePayload::villagerName,
            ByteBufCodecs.STRING_UTF8,  VillagerDialoguePayload::profession,
            ByteBufCodecs.STRING_UTF8,  VillagerDialoguePayload::dialogue,
            VillagerDialoguePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
