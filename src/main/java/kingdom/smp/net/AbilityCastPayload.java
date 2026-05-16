package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → server: cast the ability bound to {@code slot} (0..3 → Z/X/C/V) for the player's class. */
public record AbilityCastPayload(int slot) implements CustomPacketPayload {

    public static final Type<AbilityCastPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "ability_cast"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityCastPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AbilityCastPayload::slot,
            AbilityCastPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
