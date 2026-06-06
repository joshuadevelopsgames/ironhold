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
 * {@link #level} (capped at {@link BattleHammerItem#MAX_FORGE_CHARGE}); if too long
 * passes since the last crit ({@link BattleHammerItem#COMBO_TIMEOUT_TICKS}) the combo
 * resets. {@link #lastCritTick} is the game time of the most recent crit, used to detect
 * an expired combo. The level drives both the inner-ring glow stage (rendered client-side
 * from this synced component) and the ground-slam power on release, and is reset to 0 when
 * a slam is performed (the charge is "spent").</p>
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
