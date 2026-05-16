package kingdom.smp.rpg.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.UUID;

/**
 * Per-mob mark from a Knight's Iron Word taunt.
 *
 * @param tauntPlayer    UUID of the casting Knight (so we can credit/exempt them on damage events)
 * @param kingdom        Kingdom index of the casting Knight (only same-kingdom allies get the mark bonus)
 * @param markExpiry     World tick at which the +15% Marked debuff expires
 * @param forcedExpiry   World tick at which the forced-target re-assertion stops
 */
public record TauntMarkData(UUID tauntPlayer, int kingdom, long markExpiry, long forcedExpiry) {

    public static final TauntMarkData EMPTY = new TauntMarkData(new UUID(0L, 0L), -1, 0L, 0L);

    public static final MapCodec<TauntMarkData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        net.minecraft.core.UUIDUtil.CODEC.fieldOf("taunt_player").forGetter(TauntMarkData::tauntPlayer),
        Codec.INT.fieldOf("kingdom").forGetter(TauntMarkData::kingdom),
        Codec.LONG.fieldOf("mark_expiry").forGetter(TauntMarkData::markExpiry),
        Codec.LONG.fieldOf("forced_expiry").forGetter(TauntMarkData::forcedExpiry)
    ).apply(i, TauntMarkData::new));

    public boolean isMarked(long now) {
        return markExpiry > now;
    }

    public boolean isForcedTarget(long now) {
        return forcedExpiry > now;
    }
}
