package kingdom.smp.rpg.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.UUID;

/**
 * Active Guardian's Vow link on the protected ally. The Knight-side reference is implicit:
 * a single redirect is keyed by the ally entity (this attachment).
 *
 * @param knightUUID  the casting Knight's UUID; redirect target
 * @param expiryTick  world tick at which the link breaks naturally
 * @param pvpMode     true if the link was established between two players (lower redirect %)
 */
public record GuardianVowData(UUID knightUUID, long expiryTick, boolean pvpMode) {

    public static final GuardianVowData EMPTY = new GuardianVowData(new UUID(0L, 0L), 0L, false);

    public static final MapCodec<GuardianVowData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        net.minecraft.core.UUIDUtil.CODEC.fieldOf("knight").forGetter(GuardianVowData::knightUUID),
        Codec.LONG.fieldOf("expiry").forGetter(GuardianVowData::expiryTick),
        Codec.BOOL.fieldOf("pvp").forGetter(GuardianVowData::pvpMode)
    ).apply(i, GuardianVowData::new));

    public boolean isActive(long now) {
        return expiryTick > now;
    }
}
