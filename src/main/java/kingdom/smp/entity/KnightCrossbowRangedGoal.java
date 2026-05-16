package kingdom.smp.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Crossbow combat goal based on vanilla logic, with fixes:
 * <ul>
 *   <li>{@code seeTime} resets on lost LOS instead of counting negative — otherwise {@code seeTime < N} keeps {@code needsToMove}
 *       true for ages and the mob never charges.</li>
 *   <li>Survival/adventure players within ~7 blocks trigger short flee path segments so spacing
 *       is visible (unlike tiny delta knockback eaten by friction + conflicting navigation).</li>
 *   <li>{@link #stop()} clears the main-hand crossbow stack after {@link LivingEntity#stopUsingItem()} — not {@link LivingEntity#getUseItem()}
 *       post-stop (often empty / wrong).</li>
 * </ul>
 */
public final class KnightCrossbowRangedGoal extends Goal {

    public static final UniformInt PATHFINDING_DELAY_RANGE = TimeUtil.rangeOfSeconds(1, 2);

    private static final int LINE_OF_SIGHT_TICKS_TO_HOLD = 1;

    private static final double PLAYER_RETREAT_DISTANCE_SQR = 7.0 * 7.0;

    private final KnightCrossbowmanEntity mob;
    private CrossbowState crossbowState = CrossbowState.UNCHARGED;
    private final double speedModifier;
    private final float attackRadiusSqr;
    private int seeTime;
    private int attackDelay;
    private int updatePathDelay;

    public KnightCrossbowRangedGoal(KnightCrossbowmanEntity mob, double speedModifier, float attackRadius) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.isValidTarget() && this.isHoldingCrossbow();
    }

    private boolean isHoldingCrossbow() {
        return this.mob.isHolding(is -> is.getItem() instanceof CrossbowItem);
    }

    @Override
    public boolean canContinueToUse() {
        return this.isValidTarget() && (this.canUse() || !this.mob.getNavigation().isDone()) && this.isHoldingCrossbow();
    }

    private boolean isValidTarget() {
        return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
    }

    @Override
    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.seeTime = 0;
        if (this.mob.isUsingItem()) {
            this.mob.stopUsingItem();
        }
        this.mob.setChargingCrossbow(false);
        ItemStack main = this.mob.getMainHandItem();
        if (main.is(Items.CROSSBOW)) {
            main.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }

        boolean hasLineOfSight = this.mob.getSensing().hasLineOfSight(target);
        if (hasLineOfSight) {
            this.seeTime = Math.min(this.seeTime + 1, 40);
        } else {
            this.seeTime = 0;
        }

        double distanceToSqr = this.mob.distanceToSqr(target);

        Player kitePlayer = target instanceof Player p ? p : null;
        boolean kiteSurvivalPlayer =
            kitePlayer != null
                && kitePlayer.isAlive()
                && !kitePlayer.isCreative()
                && !kitePlayer.isSpectator()
                && distanceToSqr < PLAYER_RETREAT_DISTANCE_SQR;

        boolean approachTarget;
        if (kiteSurvivalPlayer) {
            approachTarget = false;
            Vec3 away = this.mob.position().subtract(kitePlayer.position());
            Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
            if (horizontal.lengthSqr() > 1.0E-6) {
                horizontal = horizontal.normalize();
                Vec3 strafe = new Vec3(-horizontal.z, 0.0, horizontal.x).scale((this.mob.getRandom().nextDouble() - 0.5) * 2.5);
                Vec3 fleePoint = this.mob.position().add(horizontal.scale(10.0)).add(strafe);
                this.updatePathDelay--;
                if (this.updatePathDelay <= 0) {
                    this.mob.getNavigation().moveTo(fleePoint.x, fleePoint.y, fleePoint.z, this.speedModifier * 1.28);
                    this.updatePathDelay = PATHFINDING_DELAY_RANGE.sample(this.mob.getRandom());
                }
            }
        } else {
            approachTarget =
                (distanceToSqr > this.attackRadiusSqr || this.seeTime < LINE_OF_SIGHT_TICKS_TO_HOLD) && this.attackDelay == 0;

            if (approachTarget) {
                this.updatePathDelay--;
                if (this.updatePathDelay <= 0) {
                    this.mob.getNavigation().moveTo(target, this.canRun() ? this.speedModifier : this.speedModifier * 0.5);
                    this.updatePathDelay = PATHFINDING_DELAY_RANGE.sample(this.mob.getRandom());
                }
            } else {
                this.updatePathDelay = 0;
                this.mob.getNavigation().stop();
            }
        }

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (this.crossbowState == CrossbowState.UNCHARGED) {
            if (!approachTarget) {
                this.mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.mob, item -> item instanceof CrossbowItem));
                this.crossbowState = CrossbowState.CHARGING;
                this.mob.setChargingCrossbow(true);
            }
        } else if (this.crossbowState == CrossbowState.CHARGING) {
            if (!this.mob.isUsingItem()) {
                this.crossbowState = CrossbowState.UNCHARGED;
            }

            int pullTime = this.mob.getTicksUsingItem();
            ItemStack useItem = this.mob.getUseItem();
            if (pullTime >= CrossbowItem.getChargeDuration(useItem, this.mob)) {
                this.mob.releaseUsingItem();
                this.crossbowState = CrossbowState.CHARGED;
                this.attackDelay = 20 + this.mob.getRandom().nextInt(20);
                this.mob.setChargingCrossbow(false);
            }
        } else if (this.crossbowState == CrossbowState.CHARGED) {
            this.attackDelay--;
            if (this.attackDelay == 0) {
                this.crossbowState = CrossbowState.READY_TO_ATTACK;
            }
        } else if (this.crossbowState == CrossbowState.READY_TO_ATTACK && hasLineOfSight) {
            this.mob.performRangedAttack(target, 1.0F);
            this.crossbowState = CrossbowState.UNCHARGED;
        }
    }

    private boolean canRun() {
        return this.crossbowState == CrossbowState.UNCHARGED;
    }

    private enum CrossbowState {
        UNCHARGED,
        CHARGING,
        CHARGED,
        READY_TO_ATTACK
    }
}
