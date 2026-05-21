package kingdom.smp.seasons.network;

import kingdom.smp.Ironhold;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Server -> Client: snapshot of one dimension's season cycle counter. Sent every
 * {@link kingdom.smp.seasons.SeasonConfig#SYNC_INTERVAL_TICKS} ticks and on player join.
 */
public record SyncSeasonPayload(ResourceKey<Level> dimension, int cycleTicks) implements CustomPacketPayload {

    public static final Type<SyncSeasonPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "sync_season"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSeasonPayload> STREAM_CODEC =
        StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), SyncSeasonPayload::dimension,
            ByteBufCodecs.VAR_INT, SyncSeasonPayload::cycleTicks,
            SyncSeasonPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
