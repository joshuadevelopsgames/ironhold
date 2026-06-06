package kingdom.smp.entity.goal;

import kingdom.smp.entity.FilcherEntity;
// FilcherRole import retained-removed: role-based branching has been deleted in favour of a
// single "steal when the player isn't looking" path. All filchers behave the same; the king
// just has buffed HP + cooldown bypass (see FilcherEntity#applyKingStats).
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
 * <p>Single behavior path for every filcher: when a target player is in range and
 * NOT looking, stalk to a position just behind them and lift one hotbar item.
 * After a successful steal, sprint to cover. If spotted mid-approach, freeze for
 * 1–2 seconds; if still spotted after that, abandon and let other goals take over.
 *
 * <p>Kings bypass the post-failure cooldown so they're more aggressive than the
 * rest of the pack.
 */
public class FilcherStealGoal extends Goal {

    /** Squared distance at which the steal attempt is made. */
    private static final double STEAL_RANGE_SQ = 2.2 * 2.2;
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

    public FilcherStealGoal(FilcherEntity filcher, double speed) {
        this.filcher = filcher;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public boolean canUse() {
        // Kings bypass the cooldown — they're the buffed thieves of the pack.
        if (!filcher.isKing() && filcher.getStealCooldownTicks() > 0) return false;
        if (!(filcher.getTarget() instanceof Player p)) return false;
        if (p.isCreative() || p.isSpectator()) return false;
        this.target = p;
        if (isPlayerWatching()) return false;
        // Single behavior path: if a player is within targeting range and not looking,
        // every filcher (king or not) stalks them and tries to steal from behind.
        return true;
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
    }

    @Override
    public void stop() {
        // Kings never go on cooldown — they're relentless. Regular filchers cool down
        // after a failed attempt so they don't immediately re-target the same player.
        if (!stolen && !filcher.isKing()) filcher.setStealCooldownTicks(100);
        target = null;
        stolen = false;
        fleeTicks = 0;
        frozenTicks = 0;
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

        // Single behavior path: stalk to a position just behind the player and steal.
        // Don't look directly at the target while sneaking; staring at someone's back
        // is a social tell. The model's stalk animation handles orientation.

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

        // First pass: always grab Fool's Gold if present (check entire main inventory, not just hotbar)
        for (int i = 0; i < 36; i++) {
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
