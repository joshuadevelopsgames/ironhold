package kingdom.smp.entity.goal;

import kingdom.smp.entity.FilcherEntity;
import kingdom.smp.entity.FilcherRole;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Makes a {@link FilcherEntity} steal from a player's hotbar.
 *
 * <h3>Two modes depending on assigned role</h3>
 * <ul>
 *   <li><b>THIEF (active)</b> — stalks the player from behind, flanks around
 *       to the blind side, proactively signals a distractor, and makes the steal
 *       attempt on contact. Requires {@code boldness > 0.6}.</li>
 *   <li><b>All other roles (opportunistic)</b> — never moves toward the player
 *       on purpose. Fires only when the filcher already happens to be within
 *       {@link #OPPORTUNIST_RANGE_SQ} blocks AND the player isn't watching.
 *       No flanking, no distraction setup — just grabs and flees.</li>
 * </ul>
 *
 * <p>If the player is actively looking at the filcher this goal suspends itself
 * so the filcher can fall through to other goals instead.
 */
public class FilcherStealGoal extends Goal {

    /** Squared distance at which the steal attempt is made. */
    private static final double STEAL_RANGE_SQ = 2.2 * 2.2;
    /**
     * Opportunistic steal range: non-THIEF filchers only steal if they are
     * already this close to a player without actively pursuing them.
     */
    private static final double OPPORTUNIST_RANGE_SQ = 4.0 * 4.0;
    /**
     * Dot-product threshold for "behind the player".
     * dot(playerLook, toMob) < threshold → mob is more than ~91° from look dir.
     * Relaxed from -0.2 so the steal fires quickly once the filcher rounds the hip.
     */
    private static final double BEHIND_THRESHOLD = -0.05;
    /**
     * Dot-product threshold for "player is watching the filcher".
     * dot(playerLook, toFilcher) > threshold → filcher is within ~60° of look dir.
     */
    private static final double WATCHING_THRESHOLD = 0.5;
    /** How many ticks to flee after a successful steal before giving up. */
    private static final int FLEE_DURATION = 80;
    /** How often (ticks) to recalculate the hide destination while fleeing. */
    private static final int RECALC_INTERVAL = 10;
    /** Sprint speed used only during the post-steal getaway. */
    private static final double FLEE_SPEED = 1.75;

    private final FilcherEntity filcher;
    private final double speed;
    private Player target;
    private boolean stolen = false;
    private int fleeTicks = 0;
    private int frozenTicks = 0;
    private boolean distractionSent = false;

    public FilcherStealGoal(FilcherEntity filcher, double speed) {
        this.filcher = filcher;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public boolean canUse() {
        if (filcher.isKing()) return false;
        if (filcher.getStealCooldownTicks() > 0) return false;
        if (!(filcher.getTarget() instanceof Player p)) return false;
        if (p.isCreative() || p.isSpectator()) return false;
        this.target = p;
        if (isPlayerWatching()) return false;

        if (filcher.getRole() == FilcherRole.THIEF) {
            // Active mode: stalk and create the opportunity — requires boldness
            return filcher.getBoldness() > 0.6f;
        }
        // Opportunistic mode: only fire if already within arm's reach while doing
        // another job. No chasing, no recruiting — just grab and run.
        return filcher.distanceToSqr(p) <= OPPORTUNIST_RANGE_SQ;
    }

    @Override
    public boolean canContinueToUse() {
        // Already got the goods — keep fleeing regardless of where the player looks.
        if (stolen && fleeTicks > 0) return true;
        if (target == null || !target.isAlive()) return false;
        if (target.isCreative() || target.isSpectator()) return false;
        // During the freeze window, stay alive even if the player is watching
        if (isPlayerWatching() && frozenTicks <= 0) return false;
        return true;
    }

    @Override
    public void start() {
        stolen = false;
        fleeTicks = 0;
        frozenTicks = 0;
        distractionSent = false;
    }

    @Override
    public void stop() {
        if (!stolen) filcher.setStealCooldownTicks(100);
        target = null;
        stolen = false;
        fleeTicks = 0;
        frozenTicks = 0;
        distractionSent = false;
        filcher.getNavigation().stop();
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) return;

        // ── Freeze-when-spotted ───────────────────────────────────────────────
        if (!stolen && isPlayerWatching()) {
            if (frozenTicks == 0) {
                // Just noticed — start freeze window (1–2 s)
                frozenTicks = 20 + filcher.getRandom().nextInt(20);
            }
            frozenTicks--;
            filcher.getNavigation().stop();
            return;   // hold completely still
        } else if (!stolen) {
            frozenTicks = 0;   // player looked away — resume
        }

        if (stolen) {
            // Post-steal: look away and sprint for cover
            fleeTicks--;
            if (fleeTicks % RECALC_INTERVAL == 0) {
                Vec3 hidePos = findHidePosition();
                filcher.getNavigation().moveTo(hidePos.x, hidePos.y, hidePos.z, FLEE_SPEED);
            }
            if (fleeTicks <= 0) {
                filcher.setTarget(null);
            }
            return;
        }

        Vec3 behindPos = getBehindPosition();
        double distSq  = filcher.distanceToSqr(target);

        // ── Opportunistic mode (non-THIEF roles) ─────────────────────────────
        // Don't chase — only steal if already in striking range and behind.
        // No flanking, no distraction recruitment.
        if (filcher.getRole() != FilcherRole.THIEF) {
            if (distSq <= STEAL_RANGE_SQ && isBehindPlayer()) {
                attemptSteal();
            }
            return;
        }

        // ── Active mode (THIEF) ───────────────────────────────────────────────
        // Stalk phase — path to just behind the player.
        // Don't look directly at the target while sneaking; staring at someone's
        // back is a social tell. The model's stalk animation handles orientation.

        // ── Proactive distraction — signal one pack member to the player's front ──
        if (!distractionSent && !stolen) {
            double distSq2 = filcher.distanceToSqr(target);
            if (distSq2 <= 10.0 * 10.0) {
                attemptSetupDistraction();
                distractionSent = true;
            }
        }

        if (distSq > STEAL_RANGE_SQ) {
            double boldSpeed = speed * (0.9 + filcher.getBoldness() * 0.2);
            if (!isBehindPlayer() && distSq > 6.0 * 6.0) {
                // Far out and not behind yet — flank around to the blind side
                Vec3 flankPos = getFlankPosition();
                filcher.getNavigation().moveTo(flankPos.x, flankPos.y, flankPos.z, boldSpeed);
            } else {
                // In rear arc or close enough — move to steal position
                filcher.getNavigation().moveTo(behindPos.x, behindPos.y, behindPos.z, boldSpeed);
            }
        } else {
            filcher.getNavigation().stop();
            if (isBehindPlayer()) {
                attemptSteal();
            } else {
                // Close but not behind — circle quickly to the blind spot
                filcher.getNavigation().moveTo(behindPos.x, behindPos.y, behindPos.z,
                    speed * (0.9 + filcher.getBoldness() * 0.2) * 1.3);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns a flanking waypoint perpendicular to the player's look direction
     * on the side closer to this filcher. Navigating here first makes the
     * approach arc around the player rather than charge straight at their back.
     */
    private Vec3 getFlankPosition() {
        net.minecraft.world.phys.Vec3 look    = target.getLookAngle();
        net.minecraft.world.phys.Vec3 playerPos = target.position();
        // Horizontal perpendicular to look direction
        net.minecraft.world.phys.Vec3 perp = new net.minecraft.world.phys.Vec3(-look.z, 0, look.x).normalize();
        // Which side is the filcher on?
        double side = filcher.position().subtract(playerPos).dot(perp) >= 0 ? 1.0 : -1.0;
        // 4 blocks to that side + 2 blocks behind the player
        return playerPos.add(perp.scale(side * 4.0)).add(look.scale(-2.0));
    }

    /**
     * Sends one nearby non-bold, empty-handed filcher to the player's front
     * as a distraction while this filcher circles to steal from behind.
     */
    private void attemptSetupDistraction() {
        java.util.List<FilcherEntity> candidates = filcher.level().getEntitiesOfClass(
            FilcherEntity.class,
            filcher.getBoundingBox().inflate(16.0),
            f -> f != filcher
                && f.isAlive()
                && f.getMainHandItem().isEmpty()
                && f.getBoldness() <= 0.6f
                && f.getSwarmTicks() == 0
                && f.getTarget() == null
        );
        if (candidates.isEmpty()) return;
        FilcherEntity distractor = candidates.get(filcher.getRandom().nextInt(candidates.size()));
        distractor.setSwarmTarget(target, 60);
    }

    /** Returns a position 1.5 blocks directly behind the player's look direction. */
    private Vec3 getBehindPosition() {
        return target.position().add(target.getLookAngle().scale(-1.5));
    }

    /** Returns true when this filcher is roughly behind the player (outside ~105° FOV). */
    private boolean isBehindPlayer() {
        Vec3 playerLook = target.getLookAngle();
        Vec3 toFilcher  = filcher.position().subtract(target.position()).normalize();
        return playerLook.dot(toFilcher) < BEHIND_THRESHOLD;
    }

    /**
     * Returns true when the player is actively looking toward the filcher
     * (within ~60° of their view direction). When this is the case the filcher
     * abandons the steal attempt and lets a different goal take over.
     */
    private boolean isPlayerWatching() {
        if (target == null) return false;
        Vec3 playerLook = target.getLookAngle();
        Vec3 toFilcher  = filcher.position().subtract(target.getEyePosition()).normalize();
        // Bolder filchers steal even when partially in the player's view cone
        double threshold = WATCHING_THRESHOLD - filcher.getBoldness() * 0.25;
        return playerLook.dot(toFilcher) > threshold;
    }

    /**
     * Searches for a nearby position that breaks the player's line of sight.
     * Tries eight candidate directions starting from directly-away, spiralling
     * outward. Falls back to running straight away if no cover is found.
     */
    private Vec3 findHidePosition() {
        Vec3 away = filcher.position().subtract(target.position()).normalize();
        double baseAngle = Math.atan2(away.z, away.x);

        // Offsets: prefer straight away, then slight angles, then perpendicular
        double[] offsets = { 0, Math.PI / 4, -Math.PI / 4,
                             Math.PI / 2, -Math.PI / 2,
                             3 * Math.PI / 4, -3 * Math.PI / 4,
                             Math.PI };

        for (double offset : offsets) {
            double angle = baseAngle + offset;
            double dist  = 12 + filcher.getRandom().nextInt(4);   // 12–15 blocks
            Vec3 candidate = filcher.position().add(
                Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            if (!playerHasLOSToPos(candidate)) {
                return candidate;
            }
        }

        // No cover found — just run straight away
        return filcher.position().add(away.scale(12));
    }

    /**
     * Returns true if the player has an unobstructed line of sight to
     * {@code pos} (i.e. the position is NOT good cover for the filcher).
     */
    private boolean playerHasLOSToPos(Vec3 pos) {
        Vec3 eyePos    = pos.add(0, filcher.getEyeHeight(), 0);
        Vec3 playerEye = target.getEyePosition();
        ClipContext ctx = new ClipContext(
            playerEye, eyePos,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target);
        BlockHitResult hit = filcher.level().clip(ctx);
        return hit.getType() == HitResult.Type.MISS;
    }

    private void attemptSteal() {
        if (filcher.level().isClientSide()) return;
        var inventory = target.getInventory();

        // First pass: always grab Fool's Gold if present
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && FilcherEntity.isFoolsGold(stack)) {
                executeSteal(inventory, i, stack);
                return;
            }
        }
        // Normal pass: first non-empty hotbar slot
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
        int amount = Math.min(stack.getCount(), 1 + filcher.getRandom().nextInt(3));
        ItemStack stolen = stack.copyWithCount(amount);
        int remaining = stack.getCount() - amount;
        inventory.setItem(slot, remaining > 0 ? stack.copyWithCount(remaining) : ItemStack.EMPTY);

        filcher.setItemSlot(EquipmentSlot.MAINHAND, stolen);
        target.hurt(target.level().damageSources().mobAttack(filcher), 2.0F);
        filcher.playStealSuccess();
        filcher.setStealCooldownTicks(400);

        if (target instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetActionBarTextPacket(
                Component.literal("§eA Filcher stole ")
                    .append(Component.literal("" + amount + "\u00d7 "))
                    .append(stolen.getHoverName())
                    .append("§e! Hunt it down to get it back!")
            ));
        }

        this.stolen    = true;
        this.fleeTicks = FLEE_DURATION;
    }
}
