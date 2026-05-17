package kingdom.smp.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Items;

/**
 * Crusader shield wall: face the threat and hold block continuously in melee — no periodic {@link LivingEntity#stopUsingItem()}
 * pulses that reset {@code BLOCKS_ATTACKS} delay ticks (those made blocking effectively never register).
 * {@link net.minecraft.world.entity.LivingEntity#applyItemBlocking} keys off {@link LivingEntity#getYHeadRot()} vs damage origin;
 * slow {@link net.minecraft.world.entity.ai.control.LookControl} rotation often reads as a flank — snap yaw toward target while blocking.
 */
public final class KnightCrusaderShieldGoal extends Goal {

    private final KnightCrusaderEntity knight;

    public KnightCrusaderShieldGoal(KnightCrusaderEntity knight) {
        this.knight = knight;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        return knight.getTarget() != null
            && knight.getOffhandItem().is(Items.SHIELD)
            && !knight.isShieldDisabled();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        LivingEntity target = knight.getTarget();
        if (target == null || !target.isAlive()) {
            knight.stopUsingItem();
            return;
        }

        knight.getLookControl().setLookAt(target, 60.0F, 30.0F);

        double distance = knight.distanceTo(target);
        if (distance > 11.0) {
            knight.stopUsingItem();
            return;
        }

        if (!knight.isUsingItem()) {
            knight.startUsingItem(InteractionHand.OFF_HAND);
        }

        knight.getNavigation().stop();

        double dx = target.getX() - knight.getX();
        double dz = target.getZ() - knight.getZ();
        if (dx * dx + dz * dz > 1.0E-6) {
            float yaw = (float) Mth.wrapDegrees(Mth.atan2(dz, dx) * Mth.RAD_TO_DEG - 90.0);
            knight.setYHeadRot(yaw);
            knight.setYBodyRot(yaw);
        }
    }

    @Override
    public void stop() {
        knight.stopUsingItem();
    }
}
