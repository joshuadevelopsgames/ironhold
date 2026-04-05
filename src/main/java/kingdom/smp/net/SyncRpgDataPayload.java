package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client: syncs the full RPG snapshot so the client can render HUD / screens.
 */
public record SyncRpgDataPayload(
        int kingdomIndex,
        int classIndex,
        int classLevel,
        int xpIntoLevel,
        int xpToNext,
        int carryWeight,
        int maxCarryWeight
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncRpgDataPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "sync_rpg"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRpgDataPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SyncRpgDataPayload::kingdomIndex,
                    ByteBufCodecs.VAR_INT, SyncRpgDataPayload::classIndex,
                    ByteBufCodecs.VAR_INT, SyncRpgDataPayload::classLevel,
                    ByteBufCodecs.VAR_INT, SyncRpgDataPayload::xpIntoLevel,
                    ByteBufCodecs.VAR_INT, SyncRpgDataPayload::xpToNext,
                    ByteBufCodecs.VAR_INT, SyncRpgDataPayload::carryWeight,
                    ByteBufCodecs.VAR_INT, SyncRpgDataPayload::maxCarryWeight,
                    SyncRpgDataPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
