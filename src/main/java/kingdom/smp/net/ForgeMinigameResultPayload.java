package kingdom.smp.net;

import kingdom.smp.Ironhold;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → Server: result of the blacksmithing forge minigame. The server
 * maps the strike tally onto a quality tier for the held gear item
 * (see {@code BlacksmithingMinigameManager.resolve}).
 *
 * @param success         true if the forge was completed (enough good
 *                        strikes); false if it was botched (too many flaws
 *                        or the metal cooled / the player gave up)
 * @param perfectStrikes  strikes that landed in the inner perfect zone —
 *                        drives how high the quality is raised on success
 * @param goodStrikes     strikes that landed in the outer sweet zone but
 *                        outside the perfect zone (kept for feedback/tuning)
 */
public record ForgeMinigameResultPayload(boolean success, int perfectStrikes, int goodStrikes)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ForgeMinigameResultPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Ironhold.MODID, "forge_minigame_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ForgeMinigameResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, ForgeMinigameResultPayload::success,
                    ByteBufCodecs.VAR_INT, ForgeMinigameResultPayload::perfectStrikes,
                    ByteBufCodecs.VAR_INT, ForgeMinigameResultPayload::goodStrikes,
                    ForgeMinigameResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
