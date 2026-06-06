package kingdom.smp.music;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Tracks a back-and-forth duel between the local player and one opponent player,
 * entirely client-side, to drive {@link MusicTrigger#PVP_SKIRMISH} and
 * {@link MusicTrigger#PVP_CLIMAX}.
 *
 * <p>Blows are observed from both directions:
 * <ul>
 *   <li><b>Outgoing</b> — {@link #recordBlow}({@link Side#SELF}, …) is called from an
 *       {@code AttackEntityEvent} handler when the local player hits a player.</li>
 *   <li><b>Incoming</b> — {@link #tick} watches the local player's {@code hurtTime}
 *       rising edge and reads {@code getLastDamageSource()} to detect being hit by a
 *       player. (Server-side damage events don't fire on the client, so we infer from
 *       synced state, the same way ReactiveMusic does.)</li>
 * </ul>
 *
 * <p>A "turn" is a back-and-forth: it only counts when the hitting side <i>alternates</i>,
 * so a one-sided beatdown never escalates to the climax. After more than four turns the
 * tier becomes {@link Tier#CLIMAX}. The duel decays after {@link #DUEL_TIMEOUT_TICKS} with
 * no blows, or when the opponent dies or leaves {@link #DUEL_RANGE} blocks.
 */
public final class PvpEscalation {
    public static final PvpEscalation INSTANCE = new PvpEscalation();

    public enum Side { SELF, OPPONENT }

    public enum Tier { NONE, SKIRMISH, CLIMAX }

    /** Ticks of combat silence before the duel is forgotten (10s). */
    private static final long DUEL_TIMEOUT_TICKS = 200L;
    /** Beyond this distance (blocks) the duel is dropped. */
    private static final double DUEL_RANGE = 48.0;
    /** Alternating turns required to reach the climax ("more than 4"). */
    private static final int CLIMAX_TURNS = 4;

    private Player opponent;
    private Side lastSide;
    private int turns;
    private int blows;
    private long lastBlowTick;

    private int prevHurtTime;

    private PvpEscalation() {}

    /** A blow landed in the duel. {@code side} is who dealt it. */
    public void recordBlow(Side side, Player opp, long now) {
        if (opp == null) return;
        if (opponent != opp) {
            // new opponent — start a fresh duel
            opponent = opp;
            lastSide = null;
            turns = 0;
            blows = 0;
        }
        blows++;
        if (lastSide != null && lastSide != side) {
            turns++; // a genuine back-and-forth exchange
        }
        lastSide = side;
        lastBlowTick = now;
    }

    /** Per-tick: detect incoming hits and decay a stale duel. */
    public void tick(LocalPlayer player, long now) {
        if (player == null) {
            reset();
            return;
        }

        // Incoming-hit detection: rising edge of hurtTime means a fresh hit this tick.
        int hurt = player.hurtTime;
        if (hurt > prevHurtTime) {
            DamageSource src = player.getLastDamageSource();
            if (src != null && src.getEntity() instanceof Player attacker && attacker != player) {
                recordBlow(Side.OPPONENT, attacker, now);
            }
        }
        prevHurtTime = hurt;

        // Decay.
        if (opponent != null) {
            boolean gone = !opponent.isAlive() || opponent.isRemoved();
            boolean tooFar = player.distanceToSqr(opponent) > DUEL_RANGE * DUEL_RANGE;
            boolean stale = now - lastBlowTick > DUEL_TIMEOUT_TICKS;
            if (gone || tooFar || stale) {
                reset();
            }
        }
    }

    public Tier tier() {
        if (opponent == null || blows == 0) return Tier.NONE;
        return turns > CLIMAX_TURNS ? Tier.CLIMAX : Tier.SKIRMISH;
    }

    public void reset() {
        opponent = null;
        lastSide = null;
        turns = 0;
        blows = 0;
        lastBlowTick = 0L;
    }

    /** True when the entity is the local player's current duel opponent or any player target. */
    public static boolean isPlayer(Entity e) {
        return e instanceof Player;
    }
}
