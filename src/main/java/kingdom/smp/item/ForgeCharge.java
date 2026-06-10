package kingdom.smp.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Forge-power state stored on a {@link BattleHammerItem} stack as a data component.
 *
 * <p>The Battle Hammer charges by landing consecutive critical hits: each crit bumps
 * {@link #level} (capped at {@link BattleHammerItem#MAX_FORGE_CHARGE}). The combo never
 * times out and survives plain non-crit hits — it only resets to 0 when a swing fully
 * misses (whiffs into the air) or when a ground slam spends the charge. {@link #lastCritTick}
 * records the game time of the most recent crit. The level drives both the inner-ring glow
 * stage (rendered client-side from this synced component) and the ground-slam power on
 * release.</p>
 */
public record ForgeCharge(int level, long lastCritTick) {

    public static final ForgeCharge NONE = new ForgeCharge(0, 0L);

    public static final Codec<ForgeCharge> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.INT.fieldOf("level").forGetter(ForgeCharge::level),
                    Codec.LONG.fieldOf("last_crit_tick").forGetter(ForgeCharge::lastCritTick))
            .apply(i, ForgeCharge::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ForgeCharge> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ForgeCharge::level,
            ByteBufCodecs.LONG, ForgeCharge::lastCritTick,
            ForgeCharge::new);
}
