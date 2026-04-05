package kingdom.smp.net;

import java.util.UUID;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Sent server → all tracking clients whenever a player's vanity slots change.
 * Each client caches the data so that other players' vanity armor is rendered.
 */
public record SyncVanityPayload(
        UUID playerUUID,
        ItemStack vanityHead,
        ItemStack vanityChest,
        ItemStack vanityLegs,
        ItemStack vanityFeet
) implements CustomPacketPayload {

    public static final Type<SyncVanityPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "sync_vanity"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncVanityPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString),
                    SyncVanityPayload::playerUUID,
                    ItemStack.OPTIONAL_STREAM_CODEC,
                    SyncVanityPayload::vanityHead,
                    ItemStack.OPTIONAL_STREAM_CODEC,
                    SyncVanityPayload::vanityChest,
                    ItemStack.OPTIONAL_STREAM_CODEC,
                    SyncVanityPayload::vanityLegs,
                    ItemStack.OPTIONAL_STREAM_CODEC,
                    SyncVanityPayload::vanityFeet,
                    SyncVanityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
