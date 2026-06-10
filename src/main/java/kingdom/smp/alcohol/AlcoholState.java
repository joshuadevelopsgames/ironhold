package kingdom.smp.alcohol;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persistent server-side intoxication and blackout progress for one player. */
public record AlcoholState(
        int load,
        long lastDrinkTick,
        long blackoutCooldownUntil,
        int blackoutTicksRemaining,
        int blackoutStage,
        String recap) {

    public static final AlcoholState SOBER = new AlcoholState(0, 0L, 0L, 0, -1, "");

    public static final MapCodec<AlcoholState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.INT.optionalFieldOf("load", 0).forGetter(AlcoholState::load),
        Codec.LONG.optionalFieldOf("last_drink_tick", 0L).forGetter(AlcoholState::lastDrinkTick),
        Codec.LONG.optionalFieldOf("blackout_cooldown_until", 0L).forGetter(AlcoholState::blackoutCooldownUntil),
        Codec.INT.optionalFieldOf("blackout_ticks_remaining", 0).forGetter(AlcoholState::blackoutTicksRemaining),
        Codec.INT.optionalFieldOf("blackout_stage", -1).forGetter(AlcoholState::blackoutStage),
        Codec.STRING.optionalFieldOf("recap", "").forGetter(AlcoholState::recap)
    ).apply(i, AlcoholState::new));

    public boolean blackedOut() {
        return blackoutTicksRemaining > 0;
    }

    public AlcoholState withLoad(int value, long tick) {
        return new AlcoholState(value, tick, blackoutCooldownUntil,
            blackoutTicksRemaining, blackoutStage, recap);
    }

    public AlcoholState startBlackout(int ticks, long cooldownUntil) {
        return new AlcoholState(load, lastDrinkTick, cooldownUntil, ticks, -1, "");
    }

    public AlcoholState advanceBlackout(int ticksRemaining, int stage, String nextRecap) {
        return new AlcoholState(load, lastDrinkTick, blackoutCooldownUntil,
            ticksRemaining, stage, nextRecap);
    }

    public AlcoholState finishBlackout(int remainingLoad) {
        return new AlcoholState(remainingLoad, lastDrinkTick, blackoutCooldownUntil, 0, -1, "");
    }
}
