package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent client → server when the player clicks the ✦ tab on the inventory.
 * The server responds by opening the {@link kingdom.smp.accessory.AccessoryMenu}.
 */
public record OpenAccessoryPayload() implements CustomPacketPayload {

    public static final Type<OpenAccessoryPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "open_accessory"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAccessoryPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenAccessoryPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
