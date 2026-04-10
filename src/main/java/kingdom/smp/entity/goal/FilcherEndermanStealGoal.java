package kingdom.smp.entity.goal;

import kingdom.smp.entity.FilcherEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;

/**
 * Makes a {@link FilcherEntity} target nearby endermen that are carrying a
 * block and steal it from them.
 *
 * <p>This is the filcher's way of opportunistically looting passive endermen —
 * an enderman absorbed a block from the world and the filcher noticed. Unlike
 * the player-steal goal the filcher doesn't need to sneak up from behind; it
 * simply closes in and snatches the block.
 *
 * <p>The enderman reacts by immediately targeting the filcher (it has been
 * provoked), so the filcher flees using the same LOS-aware hide logic used
 * against players and villagers.
 *
 * <p>This goal only fires when the filcher's main hand is empty — a filcher
 * already carrying loot has no reason to grab a dirt block from an enderman.
 */
public class FilcherEndermanStealGoal extends Goal {

    /** How far the filcher will search for a block-carrying enderman. */
    private static final double SCAN_RANGE     = 16.0;
    /** Squared distance at which the steal attempt fires. */
    private static final double STEAL_RANGE_SQ = 2.0 * 2.0;
    /** Ticks spent fleeing after a successful steal. */
    private static final int    FLEE_DURATION  = 80;
    /** How often (ticks) to recalculate the hide destination while fleeing. */
    private static final int    RECALC_INTERVAL = 12;

    private final FilcherEntity filcher;
    private final double        speed;
    private EnderMan            target;
    private boolean             stolen;
    private int                 fleeTicks;

    public FilcherEndermanStealGoal(FilcherEntity filcher, double speed) {
        this.filcher = filcher;
        this.speed   = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public boolean canUse() {
        if (filcher.isKing()) return false;
        net.minecraft.core.BlockPos den = filcher.getDenPos();
        if (den != null && net.minecraft.world.phys.Vec3.atCenterOf(den)
                .distanceToSqr(filcher.position()) > 75.0 * 75.0) return false;
        // A filcher with loot already has no need for a dirt block.
        if (!filcher.getMainHandItem().isEmpty()) return false;
        target = findEndermanWithBlock();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (stolen && fleeTicks > 0) return true;
        if (target == null || !target.isAlive()) return false;
        // Give up if the enderman set down or lost its block before we arrived.
        if (!stolen && target.getCarriedBlock() == null) return false;
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
            filcher.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), speed);
        } else {
            filcher.getNavigation().stop();
            attemptSteal();
        }
    }

    // ── Steal logic ───────────────────────────────────────────────────────────

    private void attemptSteal() {
        if (filcher.level().isClientSide()) return;

        BlockState carried = target.getCarriedBlock();
        if (carried == null) {
            // Enderman set the block down between ticks — nothing to take.
            stop();
            return;
        }

        // Lift the block off the enderman and pocket it.
        ItemStack loot = new ItemStack(carried.getBlock());
        target.setCarriedBlock(null);

        filcher.setItemSlot(EquipmentSlot.MAINHAND, loot);

        // Quiet grab — no damage, so the enderman doesn't aggro and kill the filcher.
        filcher.level().playSound(
            null, filcher.blockPosition(),
            SoundEvents.BAT_AMBIENT, SoundSource.HOSTILE,
            1.0F, 0.45F
        );

        this.stolen    = true;
        this.fleeTicks = FLEE_DURATION;
    }

    // ── Target search ─────────────────────────────────────────────────────────

    private EnderMan findEndermanWithBlock() {
        return filcher.level().getEntitiesOfClass(
            EnderMan.class,
            filcher.getBoundingBox().inflate(SCAN_RANGE),
            e -> e.isAlive() && e.getCarriedBlock() != null
        ).stream()
            .min(Comparator.comparingDouble(e -> e.distanceToSqr(filcher)))
            .orElse(null);
    }

    // ── LOS-aware flee ────────────────────────────────────────────────────────

    /**
     * Searches for a nearby position that breaks the enderman's line of sight.
     * Tries eight candidate directions starting from directly-away.
     */
    private Vec3 findHidePosition() {
        Vec3 away   = filcher.position().subtract(target.position()).normalize();
        double base = Math.atan2(away.z, away.x);
        double[] steps = { 0, Math.PI/4, -Math.PI/4, Math.PI/2, -Math.PI/2,
                           3*Math.PI/4, -3*Math.PI/4, Math.PI };

        for (double step : steps) {
            double angle = base + step;
            double dist  = 10 + filcher.getRandom().nextInt(4);
            Vec3 candidate = filcher.position().add(
                Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            if (!endermanHasLOSTo(candidate)) return candidate;
        }
        return filcher.position().add(away.scale(10));
    }

    private boolean endermanHasLOSTo(Vec3 pos) {
        Vec3 eyePos      = pos.add(0, filcher.getEyeHeight(), 0);
        Vec3 endermanEye = target.getEyePosition();
        ClipContext ctx  = new ClipContext(
            endermanEye, eyePos,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target);
        BlockHitResult hit = filcher.level().clip(ctx);
        return hit.getType() == HitResult.Type.MISS;
    }
}
