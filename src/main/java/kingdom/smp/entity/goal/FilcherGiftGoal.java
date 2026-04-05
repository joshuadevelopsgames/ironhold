package kingdom.smp.entity.goal;

import kingdom.smp.entity.FilcherEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.List;

/**
 * Altruistic community goal: a filcher that has accumulated multiple items
 * may donate its lowest-value piece to a nearby peer that has nothing.
 *
 * <p>Only fires when this filcher's mainhand is empty (it isn't currently
 * fleeing with stolen goods) and it has at least one item in its personal
 * stash. The probability of gifting is modulated by sociability and
 * inversely by greed — a very greedy, unsociable filcher never gifts.
 */
public class FilcherGiftGoal extends Goal {

    private static final double SCAN_RANGE     = 12.0;
    private static final double GIFT_RANGE_SQ  = 2.5 * 2.5;
    private static final int    PATIENCE_TICKS = 80;

    private final FilcherEntity filcher;
    private FilcherEntity recipient;
    private int ticksActive;

    public FilcherGiftGoal(FilcherEntity filcher) {
        this.filcher = filcher;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Must not be in active carry/flee (CarryFleeGoal would block via MOVE anyway,
        // but guard here so we don't burn on the random check unnecessarily)
        if (!filcher.getMainHandItem().isEmpty()) return false;
        // Need something in stash to give away
        if (filcher.getLowestValueItem().isEmpty()) return false;
        // Sociable but not greedy filchers gift most freely
        float giftChance = filcher.getSociability() * (1.0F - filcher.getGreed());
        if (filcher.getRandom().nextFloat() > giftChance) return false;

        recipient = findEmptyHandedFilcher();
        return recipient != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (recipient == null || !recipient.isAlive()) return false;
        if (filcher.getLowestValueItem().isEmpty()) return false;
        if (ticksActive >= PATIENCE_TICKS) return false;
        return filcher.distanceToSqr(recipient) <= SCAN_RANGE * SCAN_RANGE * 1.5;
    }

    @Override
    public void start() {
        ticksActive = 0;
    }

    @Override
    public void stop() {
        filcher.getNavigation().stop();
        recipient = null;
        ticksActive = 0;
    }

    @Override
    public void tick() {
        if (recipient == null) return;
        ticksActive++;

        double distSq = filcher.distanceToSqr(recipient);
        if (distSq > GIFT_RANGE_SQ) {
            filcher.getNavigation().moveTo(
                recipient.getX(), recipient.getY(), recipient.getZ(), 1.0);
        } else {
            filcher.getNavigation().stop();
            deliverGift();
        }
    }

    private FilcherEntity findEmptyHandedFilcher() {
        List<FilcherEntity> nearby = filcher.level().getEntitiesOfClass(
            FilcherEntity.class,
            filcher.getBoundingBox().inflate(SCAN_RANGE),
            f -> f != filcher && f.isAlive()
                && f.getMainHandItem().isEmpty()
                && f.getFilcherInventory().isEmpty()
        );
        if (nearby.isEmpty()) return null;
        return nearby.get(filcher.getRandom().nextInt(nearby.size()));
    }

    private void deliverGift() {
        if (filcher.level().isClientSide()) return;
        if (recipient == null || !recipient.isAlive()) return;

        ItemStack gift = filcher.removeLowestValueItem();
        if (!gift.isEmpty()) {
            // Toss the item in a gentle arc toward the recipient
            net.minecraft.world.phys.Vec3 from = filcher.getEyePosition();
            net.minecraft.world.phys.Vec3 toRecip =
                recipient.position().add(0, 0.4, 0).subtract(from).normalize();
            net.minecraft.world.entity.item.ItemEntity thrown =
                new net.minecraft.world.entity.item.ItemEntity(
                    filcher.level(), from.x, from.y, from.z, gift);
            thrown.setPickUpDelay(10);
            thrown.setDeltaMovement(toRecip.x * 0.35, toRecip.y * 0.35 + 0.18, toRecip.z * 0.35);
            filcher.level().addFreshEntity(thrown);

            filcher.playGift();
            recipient.playChatter();
        }
        stop();
    }
}
