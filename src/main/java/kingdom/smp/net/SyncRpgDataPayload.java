package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

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
        int maxCarryWeight,
        List<Integer> completedClassOrdinals
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncRpgDataPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "sync_rpg"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRpgDataPayload> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public SyncRpgDataPayload decode(RegistryFriendlyByteBuf buf) {
                int kingdom = ByteBufCodecs.VAR_INT.decode(buf);
                int clazz = ByteBufCodecs.VAR_INT.decode(buf);
                int level = ByteBufCodecs.VAR_INT.decode(buf);
                int xp = ByteBufCodecs.VAR_INT.decode(buf);
                int toNext = ByteBufCodecs.VAR_INT.decode(buf);
                int weight = ByteBufCodecs.VAR_INT.decode(buf);
                int maxWeight = ByteBufCodecs.VAR_INT.decode(buf);
                int count = ByteBufCodecs.VAR_INT.decode(buf);
                List<Integer> completed = new java.util.ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    completed.add(ByteBufCodecs.VAR_INT.decode(buf));
                }
                return new SyncRpgDataPayload(kingdom, clazz, level, xp, toNext, weight, maxWeight, completed);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, SyncRpgDataPayload payload) {
                ByteBufCodecs.VAR_INT.encode(buf, payload.kingdomIndex);
                ByteBufCodecs.VAR_INT.encode(buf, payload.classIndex);
                ByteBufCodecs.VAR_INT.encode(buf, payload.classLevel);
                ByteBufCodecs.VAR_INT.encode(buf, payload.xpIntoLevel);
                ByteBufCodecs.VAR_INT.encode(buf, payload.xpToNext);
                ByteBufCodecs.VAR_INT.encode(buf, payload.carryWeight);
                ByteBufCodecs.VAR_INT.encode(buf, payload.maxCarryWeight);
                ByteBufCodecs.VAR_INT.encode(buf, payload.completedClassOrdinals.size());
                for (int ord : payload.completedClassOrdinals) {
                    ByteBufCodecs.VAR_INT.encode(buf, ord);
                }
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
