package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Server → Client: a fishing bobber owned by this player just had a bite.
 * Tells the client to open the bite minigame.
 *
 * @param hookEntityId    the FishingHook entity id (currently unused on the
 *                        client but kept for future per-hook visuals/debug)
 * @param hookZoneHeight  pixel height of the player's hook zone, scaled by
 *                        their Fishing profession rank + use-skill level
 * @param motionPattern   0 = calm sine, 1 = jumpy, 2 = thrashing
 * @param catchPreview    the pre-rolled item the player will receive on win
 *                        (the bite minigame displays this in the bar so the
 *                        player sees what they're fighting for)
 */
public record FishingBiteStartPayload(int hookEntityId, int hookZoneHeight, int motionPattern,
                                      ItemStack catchPreview)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FishingBiteStartPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "fishing_bite_start"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FishingBiteStartPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, FishingBiteStartPayload::hookEntityId,
                    ByteBufCodecs.VAR_INT, FishingBiteStartPayload::hookZoneHeight,
                    ByteBufCodecs.VAR_INT, FishingBiteStartPayload::motionPattern,
                    ItemStack.OPTIONAL_STREAM_CODEC, FishingBiteStartPayload::catchPreview,
                    FishingBiteStartPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
