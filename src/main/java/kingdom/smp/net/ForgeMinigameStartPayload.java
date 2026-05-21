package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Server → Client: the player sneak-right-clicked an anvil while holding a
 * reforgeable gear item. Tells the client to open the blacksmithing forge
 * minigame.
 *
 * <p>Difficulty is derived entirely on the client from {@code rankOrdinal}
 * via {@code ForgeMinigameScreen}'s shared difficulty table, so both sides
 * agree on the challenge without shipping every tuning value over the wire.
 *
 * @param itemPreview  the gear item being forged (shown in the screen)
 * @param rankOrdinal  the player's Blacksmithing {@code ProfessionRank}
 *                     ordinal, or {@code -1} if they have no rank yet
 */
public record ForgeMinigameStartPayload(ItemStack itemPreview, int rankOrdinal)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ForgeMinigameStartPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "forge_minigame_start"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ForgeMinigameStartPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.OPTIONAL_STREAM_CODEC, ForgeMinigameStartPayload::itemPreview,
                    ByteBufCodecs.VAR_INT, ForgeMinigameStartPayload::rankOrdinal,
                    ForgeMinigameStartPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
