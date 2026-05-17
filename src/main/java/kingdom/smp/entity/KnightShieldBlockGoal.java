package kingdom.smp.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Items;

/**
 * Holds shield raised during melee. {@link net.minecraft.world.entity.LivingEntity#getItemBlockingWith()} requires an
 * uninterrupted {@link net.minecraft.world.item.component.BlocksAttacks#blockDelayTicks()} window — stopping each melee swing kept blocking reset forever.
 */
public final class KnightShieldBlockGoal extends Goal {

    private final KnightEntity knight;

    public KnightShieldBlockGoal(KnightEntity knight) {
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
        if (distance > 10.5) {
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
