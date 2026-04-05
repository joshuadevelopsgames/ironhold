package kingdom.smp.entity.goal;

import kingdom.smp.entity.FilcherEntity;
import kingdom.smp.entity.FilcherRole;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Coordinated heist goal: when a filcher's solo steal is blocked because
 * the player is actively watching, it rallies nearby idle filchers to swarm
 * the player as a distraction while it circles around for a backstab steal.
 *
 * <h3>Sequence</h3>
 * <ol>
 *   <li><b>Recruit</b> — plays a recruit-call sound; all nearby empty-handed
 *       filchers have their target set to the player so they rush toward it.</li>
 *   <li><b>Wait</b> — the mastermind hangs back for {@link #SWARM_DELAY}
 *       ticks while the distractors close in.</li>
 *   <li><b>Strike</b> — circles to behind the player and steals once in
 *       range and positioned correctly.</li>
 *   <li><b>Flee</b> — sprints away for {@link #FLEE_DURATION} ticks.</li>
 * </ol>
 */
public class FilcherRecruitGoal extends Goal {

    private static final double STEAL_RANGE_SQ    = 1.8 * 1.8;
    private static final double BEHIND_THRESHOLD  = -0.2;
    private static final double WATCHING_THRESHOLD = 0.5;
    private static final int    RECRUIT_RANGE      = 16;
    /** Ticks to hang back while the swarm engages the player. */
    private static final int    SWARM_DELAY        = 60;
    private static final int    FLEE_DURATION      = 100;

    private final FilcherEntity filcher;
    private Player target;
    private int ticksActive;
    private boolean stolen;
    private int fleeTicks;

    public FilcherRecruitGoal(FilcherEntity filcher) {
        this.filcher = filcher;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (filcher.isKing()) return false;
        // Only the assigned thief orchestrates coordinated heists
        if (filcher.getRole() != FilcherRole.THIEF) return false;
        if (!(filcher.getTarget() instanceof Player p)) return false;
        if (p.isCreative() || p.isSpectator()) return false;
        // Solo steal can't fire because player is watching — this is our fallback
        if (!isPlayerWatching(p)) return false;
        // Can't recruit while already holding loot
        if (!filcher.getMainHandItem().isEmpty()) return false;
        // Need at least one idle filcher nearby to act as distractor
        if (findRecruits().isEmpty()) return false;

        this.target = p;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (stolen && fleeTicks > 0) return true;
        if (target == null || !target.isAlive()) return false;
        if (target.isCreative() || target.isSpectator()) return false;
        return ticksActive < SWARM_DELAY + 120;
    }

    @Override
    public void start() {
        ticksActive = 0;
        stolen = false;
        fleeTicks = 0;

        // Send recruits swarming at the player as a distraction
        for (FilcherEntity recruit : findRecruits()) {
            recruit.setSwarmTarget(target, 80);
        }
        filcher.playRecruitCall();
    }

    @Override
    public void stop() {
        filcher.getNavigation().stop();
        target = null;
        ticksActive = 0;
        stolen = false;
        fleeTicks = 0;
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) return;
        ticksActive++;

        if (stolen) {
            fleeTicks--;
            if (fleeTicks % 12 == 0) {
                Vec3 away = filcher.position().subtract(target.position()).normalize();
                Vec3 flee  = filcher.position().add(away.scale(12));
                filcher.getNavigation().moveTo(flee.x, flee.y, flee.z, 1.4);
            }
            if (fleeTicks <= 0) filcher.setTarget(null);
            return;
        }

        if (ticksActive < SWARM_DELAY) {
            // Hang back while distractors engage
            filcher.getNavigation().stop();
            filcher.getLookControl().setLookAt(target, 30.0F, 30.0F);
            return;
        }

        // Strike phase — circle to the player's blind spot
        filcher.getLookControl().setLookAt(target, 30.0F, 30.0F);
        Vec3 behindPos = target.position().add(target.getLookAngle().scale(-1.5));
        double distSq  = filcher.distanceToSqr(target);

        if (distSq > STEAL_RANGE_SQ) {
            filcher.getNavigation().moveTo(behindPos.x, behindPos.y, behindPos.z, 1.5);
        } else if (isBehindPlayer()) {
            attemptSteal();
        } else {
            // Not behind yet — keep circling
            filcher.getNavigation().moveTo(behindPos.x, behindPos.y, behindPos.z, 1.6);
        }
    }

    private boolean isPlayerWatching(Player p) {
        Vec3 look      = p.getLookAngle();
        Vec3 toFilcher = filcher.position().subtract(p.getEyePosition()).normalize();
        return look.dot(toFilcher) > WATCHING_THRESHOLD;
    }

    private boolean isBehindPlayer() {
        Vec3 look      = target.getLookAngle();
        Vec3 toFilcher = filcher.position().subtract(target.position()).normalize();
        return look.dot(toFilcher) < BEHIND_THRESHOLD;
    }

    private List<FilcherEntity> findRecruits() {
        return filcher.level().getEntitiesOfClass(
            FilcherEntity.class,
            filcher.getBoundingBox().inflate(RECRUIT_RANGE),
            f -> f != filcher && f.isAlive() && f.getMainHandItem().isEmpty()
        );
    }

    private void attemptSteal() {
        if (filcher.level().isClientSide()) return;
        var inventory = target.getInventory();

        // Prioritize fool's gold
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && kingdom.smp.entity.FilcherEntity.isFoolsGold(stack)) {
                executeSteal(inventory, i, stack);
                return;
            }
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                executeSteal(inventory, i, stack);
                return;
            }
        }
    }

    private void executeSteal(net.minecraft.world.entity.player.Inventory inventory,
                               int slot, ItemStack stack) {
        // Coordinated steal takes slightly more (2–4)
        int amount = Math.min(stack.getCount(), 2 + filcher.getRandom().nextInt(3));
        ItemStack stolen = stack.copyWithCount(amount);
        int remaining = stack.getCount() - amount;
        inventory.setItem(slot, remaining > 0 ? stack.copyWithCount(remaining) : ItemStack.EMPTY);
        filcher.setItemSlot(EquipmentSlot.MAINHAND, stolen);
        filcher.playStealSuccess();
        this.stolen    = true;
        this.fleeTicks = FLEE_DURATION;
    }
}
