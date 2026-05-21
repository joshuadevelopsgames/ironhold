package kingdom.smp.entity.goal;

import kingdom.smp.entity.FilcherEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;

/**
 * Filchers will target nearby villagers and steal from them when unobserved.
 *
 * <p><b>Steal priority:</b>
 * <ol>
 *   <li>80 % chance — conjure 1–3 emeralds (simulates pickpocketing the
 *       villager's purse; no actual inventory is modified).</li>
 *   <li>20 % chance — copy a random trade-result item from the villager's
 *       offer list (simulates pilfering a rare good).</li>
 * </ol>
 *
 * <p><b>Villager reaction:</b> after being stolen from the villager plays a
 * startled sound and navigates back to their registered home (bed). If no home
 * is registered they flee away from the filcher.
 *
 * <p>The filcher then equips the stolen item visibly in its main hand and
 * flees using the same LOS-aware hide logic used against players.
 */
public class FilcherVillagerStealGoal extends Goal {

    /** How far a filcher will hunt for a villager target. */
    private static final double SCAN_RANGE      = 16.0;
    /** Squared distance at which the steal attempt fires. */
    private static final double STEAL_RANGE_SQ  = 2.0 * 2.0;
    /** Probability of going for emeralds over a trade item. */
    private static final double EMERALD_CHANCE  = 0.80;
    /** Ticks spent fleeing after a successful steal. */
    private static final int    FLEE_DURATION   = 80;
    /** How often (ticks) to recalculate the hide destination while fleeing. */
    private static final int    RECALC_INTERVAL = 12;

    private final FilcherEntity filcher;
    private final double        speed;
    private Villager            target;
    private boolean             stolen;
    private int                 fleeTicks;

    public FilcherVillagerStealGoal(FilcherEntity filcher, double speed) {
        this.filcher = filcher;
        this.speed   = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

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
        if (filcher.isKing()) return false;
        if (filcher.getScatterTicks() > 0) return false;
        net.minecraft.core.BlockPos den = filcher.getDenPos();
        if (den != null && net.minecraft.world.phys.Vec3.atCenterOf(den)
                .distanceToSqr(filcher.position()) > 75.0 * 75.0) return false;
        // Only engage when the filcher doesn't already have loot —
        // a filcher with something in hand should be trading or hiding.
        if (!filcher.getMainHandItem().isEmpty()) return false;
        target = findNearestVillager();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (stolen && fleeTicks > 0) return true;
        if (target == null || !target.isAlive() || target.isSleeping()) return false;
        return filcher.distanceToSqr(target) <= SCAN_RANGE * SCAN_RANGE * 2;
    }

    @Override
    public void start() {
        stolen    = false;
        fleeTicks = 0;
    }

    @Override
    public void stop() {
        filcher.getNavigation().stop();
        target    = null;
        stolen    = false;
        fleeTicks = 0;
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) return;

        filcher.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (stolen) {
            fleeTicks--;
            if (fleeTicks % RECALC_INTERVAL == 0) {
                Vec3 hidePos = findHidePosition();
                filcher.getNavigation().moveTo(hidePos.x, hidePos.y, hidePos.z, speed * 1.1);
            }
            if (fleeTicks <= 0) {
                filcher.setTarget(null);
            }
            return;
        }

        double distSq = filcher.distanceToSqr(target);
        if (distSq > STEAL_RANGE_SQ) {
            // Approach — normal stalk speed, no need to circle behind for villagers
            filcher.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), speed);
        } else {
            filcher.getNavigation().stop();
            attemptSteal();
        }
    }

    // ── Steal logic ───────────────────────────────────────────────────────────

    private void attemptSteal() {
        if (filcher.level().isClientSide()) return;

        ItemStack loot = ItemStack.EMPTY;
        int stolenValue = 0;

        // 80 % — try emeralds first; fall through to trade item if the villager
        // has none (e.g. they haven't been traded with yet).
        if (filcher.getRandom().nextDouble() < EMERALD_CHANCE) {
            loot = tryStealEmeralds();
            if (!loot.isEmpty()) stolenValue = loot.getCount();   // 1–3 emeralds
        }
        if (loot.isEmpty()) {
            loot = tryStealTradeItem();
            if (!loot.isEmpty()) stolenValue = 3;   // trade goods worth ~3 emeralds
        }
        if (loot.isEmpty()) {
            // Nothing worth taking — abort goal so the filcher moves on
            stop();
            return;
        }

        // Equip in main hand — held visibly, drops on death
        filcher.setItemSlot(EquipmentSlot.MAINHAND, loot);
        target.hurt(target.damageSources().mobAttack(filcher), 2.0F);

        // Distinctive low squeak — slightly lower pitch than the player-steal squeak
        filcher.level().playSound(
            null, filcher.blockPosition(),
            SoundEvents.BAT_AMBIENT, SoundSource.HOSTILE,
            0.9F, 0.50F
        );

        // hurt() above triggers vanilla PANIC
        raisePrices(target, stolenValue);

        this.stolen    = true;
        this.fleeTicks = FLEE_DURATION;
    }

    /**
     * Tracks cumulative stolen value on the villager via persistent NBT and
     * applies incremental price increases to all their trade offers.
     *
     * <p>Every 5 emerald-equivalent stolen → +1 to all offer prices, capped at +5.
     * Stored under {@code "filcher_stolen_value"} / {@code "filcher_price_bump"}
     * in the villager's persistent data so it survives restarts.
     */
    private void raisePrices(Villager villager, int stolenValue) {
        var nbt = villager.getPersistentData();

        int totalStolen = nbt.getInt("filcher_stolen_value").orElse(0) + stolenValue;
        nbt.putInt("filcher_stolen_value", totalStolen);

        // target bump tier: 1 per 5 emeralds stolen, capped at 5
        int targetBump = Math.min(totalStolen / 5, 5);
        int prevBump   = nbt.getInt("filcher_price_bump").orElse(0);
        int delta      = targetBump - prevBump;

        if (delta > 0) {
            nbt.putInt("filcher_price_bump", targetBump);
            for (var offer : villager.getOffers()) {
                offer.addToSpecialPriceDiff(delta);
            }
        }
    }

    /**
     * Spawns 1–3 emeralds from thin air — simulating a pickpocket of the
     * villager's coin purse.  Villagers don't hold visible items, so we
     * generate the loot rather than trying to drain their container.
     */
    private ItemStack tryStealEmeralds() {
        int amount = 1 + filcher.getRandom().nextInt(3);   // 1–3 emeralds
        return new ItemStack(Items.EMERALD, amount);
    }

    /**
     * Copies the result item from a random trade offer (what the villager sells).
     * Returns a single-item stack, or {@link ItemStack#EMPTY} if the villager
     * has no offers.
     */
    private ItemStack tryStealTradeItem() {
        var offers = target.getOffers();
        if (offers.isEmpty()) return ItemStack.EMPTY;

        var offer = offers.get(filcher.getRandom().nextInt(offers.size()));
        ItemStack result = offer.getResult().copy();
        result.setCount(1);
        return result;
    }

    // ── LOS-aware flee (mirrors FilcherStealGoal) ─────────────────────────────

    /**
     * Searches for a position that breaks the villager's line of sight.
     * Tries eight candidate directions starting from directly-away.
     */
    private Vec3 findHidePosition() {
        Vec3 away      = filcher.position().subtract(target.position()).normalize();
        double base    = Math.atan2(away.z, away.x);
        double[] steps = { 0, Math.PI/4, -Math.PI/4, Math.PI/2, -Math.PI/2,
                           3*Math.PI/4, -3*Math.PI/4, Math.PI };

        for (double step : steps) {
            double angle = base + step;
            double dist  = 10 + filcher.getRandom().nextInt(4);
            Vec3 candidate = filcher.position().add(
                Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            if (!villagerHasLOSTo(candidate)) return candidate;
        }
        return filcher.position().add(away.scale(10));
    }

    private boolean villagerHasLOSTo(Vec3 pos) {
        Vec3 eyePos     = pos.add(0, filcher.getEyeHeight(), 0);
        Vec3 villagerEye = target.getEyePosition();
        ClipContext ctx = new ClipContext(
            villagerEye, eyePos,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target);
        BlockHitResult hit = filcher.level().clip(ctx);
        return hit.getType() == HitResult.Type.MISS;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Villager findNearestVillager() {
        return filcher.level().getEntitiesOfClass(
            Villager.class,
            filcher.getBoundingBox().inflate(SCAN_RANGE),
            v -> v.isAlive() && !v.isSleeping()
        ).stream()
            .min(Comparator.comparingDouble(v -> v.distanceToSqr(filcher)))
            .orElse(null);
    }
}
