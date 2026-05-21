package kingdom.smp.entity.goal;

import kingdom.smp.entity.FilcherEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Priority-1 goal: whenever the filcher is carrying stolen loot it makes a
 * beeline for its den (or the nearest dark spot if no den exists), using
 * obstacles to break the player's line of sight along the way.
 *
 * <h3>Behaviour loop</h3>
 * <ol>
 *   <li><b>Navigate to den</b> — if a den position is known, the filcher heads
 *       straight there, zig-zagging behind cover if a player is nearby.
 *       Den-seeking overrides threat-fleeing: the filcher never runs AWAY from
 *       the den just because a player is nearby.</li>
 *   <li><b>Seek darkness</b> — if no den exists, probe randomly for a dark
 *       spot, biased downward (caves/underground).  When threatened, also
 *       weave behind solid blocks.</li>
 *   <li><b>Stash</b> — once in a dark position and unthreatened for
 *       {@link #HIDE_DURATION} consecutive ticks, the carried item is moved
 *       into the filcher's personal stash inventory.</li>
 * </ol>
 */
public class FilcherCarryFleeGoal extends Goal {

    private static final double THREAT_RANGE    = 20.0;
    private static final int    SAFE_LIGHT      = 3;
    private static final int    HIDE_DURATION   = 60;   // ticks before stashing
    private static final int    RECALC_TICKS    = 12;
    private static final int    PROBE_ATTEMPTS  = 24;
    private static final int    SEARCH_RADIUS   = 24;
    private static final double FLEE_SPEED      = 1.65;  // threatened
    private static final double SEEK_SPEED      = 1.35;  // safe path to den

    private final FilcherEntity filcher;
    private int ticksActive;
    private int ticksHiding;

    public FilcherCarryFleeGoal(FilcherEntity filcher) {
        this.filcher = filcher;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    // Goal-evaluation throttle: canUse() does an AABB entity scan, too costly to
    // run every tick. Cap to once per 15 ticks per filcher, staggered by entity id.
    private static final int CAN_USE_INTERVAL = 15;
    private boolean cachedCanUse = false;
    private int lastCanUseTick = -1;

    @Override
    public boolean canUse() {
        int tick = filcher.tickCount;
        int stagger = filcher.getId() % CAN_USE_INTERVAL;
        if (tick - lastCanUseTick < CAN_USE_INTERVAL && ((tick + stagger) % CAN_USE_INTERVAL) != 0) {
            return cachedCanUse;
        }
        lastCanUseTick = tick;
        cachedCanUse = evaluateCanUse();
        return cachedCanUse;
    }

    private boolean evaluateCanUse() {
        return !filcher.getMainHandItem().isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        return !filcher.getMainHandItem().isEmpty();
    }

    @Override
    public void start() {
        ticksActive = 0;
        ticksHiding = 0;
    }

    @Override
    public void stop() {
        filcher.getNavigation().stop();
        ticksActive = 0;
        ticksHiding = 0;
    }

    @Override
    public void tick() {
        ticksActive++;

        boolean inDark    = isInDarkness(filcher.blockPosition());
        List<Player> threats = getThreats();
        boolean threatened  = !threats.isEmpty();

        if (inDark && !threatened) {
            filcher.getNavigation().stop();
            ticksHiding++;
            if (ticksHiding >= HIDE_DURATION) stashItem();
            return;
        }

        ticksHiding = 0;

        if (ticksActive % RECALC_TICKS == 0) {
            BlockPos den = filcher.getDenPos();
            Vec3 dest;
            double speed;

            if (den != null) {
                // Always move toward the den — use cover if threatened
                dest  = threatened ? findCoveredPathToDen(den, threats)
                                   : Vec3.atCenterOf(den);
                speed = threatened ? FLEE_SPEED : SEEK_SPEED;
            } else {
                // No den — flee + probe for darkness
                dest  = threatened ? findFleeDest(threats) : findDarkSpot();
                speed = threatened ? FLEE_SPEED : SEEK_SPEED;
            }

            if (dest != null) {
                filcher.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
            }
        }
    }

    // ── Stash ─────────────────────────────────────────────────────────────────

    private void stashItem() {
        if (filcher.level().isClientSide()) return;

        ItemStack item = filcher.getMainHandItem().copy();
        filcher.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        ItemStack leftover = filcher.getFilcherInventory().addItem(item);
        if (!leftover.isEmpty()) {
            filcher.setItemSlot(EquipmentSlot.MAINHAND, leftover);
        } else {
            filcher.triggerShowOff();
        }
    }

    // ── Threat detection ──────────────────────────────────────────────────────

    private List<Player> getThreats() {
        return filcher.level().getEntitiesOfClass(
            Player.class,
            filcher.getBoundingBox().inflate(THREAT_RANGE),
            p -> !p.isCreative() && !p.isSpectator()
        );
    }

    // ── Cover-aware path to den ───────────────────────────────────────────────

    /**
     * Finds a waypoint on the way to the den that breaks line of sight with
     * threatening players. Tries a straight step first, then lateral offsets
     * perpendicular to the den direction.
     */
    private Vec3 findCoveredPathToDen(BlockPos denPos, List<Player> threats) {
        Vec3 here    = filcher.position();
        Vec3 denVec  = Vec3.atCenterOf(denPos);
        Vec3 toDen   = denVec.subtract(here).normalize();
        double dist  = Math.min(10.0, here.distanceTo(denVec) * 0.7);

        Vec3 straight = here.add(toDen.scale(dist));
        if (!anyThreatHasLoS(straight, threats)) return straight;

        // Perpendicular axis for lateral offsets
        Vec3 perp = new Vec3(-toDen.z, 0, toDen.x);
        for (double side : new double[]{ 5, 7, 9, -5, -7, -9 }) {
            Vec3 candidate = straight.add(perp.scale(side));
            if (!anyThreatHasLoS(candidate, threats)) return candidate;
        }
        return straight;   // couldn't find cover — head straight toward den
    }

    /** Returns true if ANY threatening player has LoS to {@code pos}. */
    private boolean anyThreatHasLoS(Vec3 pos, List<Player> threats) {
        Vec3 eyePos = pos.add(0, filcher.getEyeHeight(), 0);
        for (Player p : threats) {
            ClipContext ctx = new ClipContext(
                p.getEyePosition(), eyePos,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p);
            BlockHitResult hit = filcher.level().clip(ctx);
            if (hit.getType() == HitResult.Type.MISS) return true;
        }
        return false;
    }

    // ── Fallback navigation (no den) ─────────────────────────────────────────

    private Vec3 findFleeDest(List<Player> threats) {
        if (threats.isEmpty()) return findDarkSpot();

        Vec3 away = Vec3.ZERO;
        for (Player p : threats) {
            away = away.add(filcher.position().subtract(p.position()).normalize());
        }
        away = away.normalize();

        for (int dist = 6; dist <= SEARCH_RADIUS; dist += 4) {
            Vec3 candidate  = filcher.position().add(away.scale(dist));
            BlockPos candPos = BlockPos.containing(candidate);
            if (isInDarkness(candPos) && filcher.level().getBlockState(candPos).isAir()
                    && !anyThreatHasLoS(candidate, threats)) {
                return candidate;
            }
        }
        return filcher.position().add(away.scale(SEARCH_RADIUS / 2.0));
    }

    private Vec3 findDarkSpot() {
        BlockPos base = filcher.blockPosition();
        var rand = filcher.getRandom();
        for (int i = 0; i < PROBE_ATTEMPTS; i++) {
            int dx = rand.nextIntBetweenInclusive(-SEARCH_RADIUS, SEARCH_RADIUS);
            int dy = rand.nextIntBetweenInclusive(-8, 1);
            int dz = rand.nextIntBetweenInclusive(-SEARCH_RADIUS, SEARCH_RADIUS);
            BlockPos candidate = base.offset(dx, dy, dz);
            if (isInDarkness(candidate) && filcher.level().getBlockState(candidate).isAir()) {
                return Vec3.atCenterOf(candidate);
            }
        }
        return null;
    }

    private boolean isInDarkness(BlockPos pos) {
        return filcher.level().getMaxLocalRawBrightness(pos) <= SAFE_LIGHT;
    }
}
