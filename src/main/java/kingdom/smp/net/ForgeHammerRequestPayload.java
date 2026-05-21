package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → Server: the player clicked the hammer button on an open anvil.
 * Carries no data — the server reads the player's open {@code AnvilMenu} to
 * find the gear + repair material, then opens the forge minigame
 * ({@code BlacksmithingMinigameManager.tryStartFromAnvil}).
 */
public record ForgeHammerRequestPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ForgeHammerRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "forge_hammer_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ForgeHammerRequestPayload> STREAM_CODEC =
            StreamCodec.unit(new ForgeHammerRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
