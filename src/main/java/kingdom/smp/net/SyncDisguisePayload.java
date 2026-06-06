package kingdom.smp.net;

import java.util.Optional;
import java.util.UUID;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent server → all tracking clients (and self) whenever a player's disguise changes.
 * Each client caches the disguise type so the render mixin can substitute the player's
 * model with the disguised entity. An empty {@code entityTypeId} clears the disguise.
 */
public record SyncDisguisePayload(UUID playerUUID, Optional<Identifier> entityTypeId)
        implements CustomPacketPayload {

    public static final Type<SyncDisguisePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "sync_disguise"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDisguisePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
                    SyncDisguisePayload::playerUUID,
                    ByteBufCodecs.optional(Identifier.STREAM_CODEC),
                    SyncDisguisePayload::entityTypeId,
                    SyncDisguisePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
