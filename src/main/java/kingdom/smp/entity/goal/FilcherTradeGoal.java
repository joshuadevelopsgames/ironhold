package kingdom.smp.entity.goal;

import kingdom.smp.entity.FilcherEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Greedy filcher-to-filcher interaction goal.
 *
 * <p>A filcher carrying a stolen item will seek out other filchers that are
 * holding a <em>more valuable</em> item and attempt to steal it from them.
 * During the approach and negotiation phase both parties emit low bat squeaks.
 * The outcome is probabilistic — greedier thieves don't always win.
 *
 * <p>Value is judged by item rarity first, then stack count. A filcher with
 * a common item will always approach one holding a rare item; a filcher with
 * a rare item largely ignores its peers unless their item is epic.
 *
 * <p>This goal only fires when the filcher is <em>not</em> actively stalking
 * or fleeing from a player (the steal goal holds the MOVE flag in those
 * phases, blocking this goal via the flag system), <em>and</em> when no
 * richer targets exist nearby — specifically no players with hotbar items,
 * no villagers, and no endermen carrying a block. Filcher-on-filcher theft
 * is purely a last resort.
 */
public class FilcherTradeGoal extends Goal {

    /** How close two filchers must be before negotiation can start. */
    private static final double APPROACH_RANGE    = 14.0;
    /** Squared distance at which the "handoff" happens. */
    private static final double HANDOFF_RANGE_SQ  = 2.5 * 2.5;
    /** How often (ticks) to emit a squeak during approach/negotiation. */
    private static final int    SQUEAK_INTERVAL   = 18;
    /** How many ticks the filcher tries to reach the partner before giving up. */
    private static final int    PATIENCE_TICKS    = 100;

    private final FilcherEntity filcher;
    private FilcherEntity       partner;
    private int                 ticksActive;

    public FilcherTradeGoal(FilcherEntity filcher) {
        this.filcher = filcher;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public boolean canUse() {
        // This filcher must be holding something to have trade leverage.
        if (filcher.getMainHandItem().isEmpty()) return false;
        // Only resort to filcher-on-filcher theft when nothing better is around.
        if (hasNearbyStealableTargets()) return false;

        partner = findCovetablePartner();
        return partner != null;
    }

    /**
     * Returns {@code true} if there are nearby mobs the filcher would prefer
     * to steal from over another filcher:
     * <ul>
     *   <li>Non-creative players with at least one hotbar item</li>
     *   <li>Any living, awake villager</li>
     *   <li>Any enderman currently carrying a block</li>
     * </ul>
     * The scan radius matches the filcher's maximum follow range (20 blocks).
     */
    private boolean hasNearbyStealableTargets() {
        AABB box = filcher.getBoundingBox().inflate(20.0);

        // Players with stealable hotbar items?
        boolean hasPlayer = !filcher.level().getEntitiesOfClass(
            Player.class, box,
            p -> !p.isCreative() && !p.isSpectator() && hasHotbarItems(p)
        ).isEmpty();
        if (hasPlayer) return true;

        // Living, conscious villagers?
        boolean hasVillager = !filcher.level().getEntitiesOfClass(
            Villager.class, box,
            v -> v.isAlive() && !v.isSleeping()
        ).isEmpty();
        if (hasVillager) return true;

        // Endermen holding a block?
        return !filcher.level().getEntitiesOfClass(
            EnderMan.class, box,
            e -> e.isAlive() && e.getCarriedBlock() != null
        ).isEmpty();
    }

    /** Returns true if the player has at least one item in their hotbar (slots 0–8). */
    private static boolean hasHotbarItems(Player p) {
        for (int i = 0; i < 9; i++) {
            if (!p.getInventory().getItem(i).isEmpty()) return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (partner == null || !partner.isAlive()) return false;
        if (ticksActive >= PATIENCE_TICKS) return false;
        // Give up if the target moved far away
        return filcher.distanceToSqr(partner) <= (APPROACH_RANGE * APPROACH_RANGE * 1.5);
    }

    @Override
    public void start() {
        ticksActive = 0;
    }

    @Override
    public void stop() {
        filcher.getNavigation().stop();
        partner    = null;
        ticksActive = 0;
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        if (partner == null) return;
        ticksActive++;

        // Emit mutual squeaks — the filcher chatter of negotiation
        if (ticksActive % SQUEAK_INTERVAL == 0) {
            playSqueak(filcher);
            playSqueak(partner);
        }

        double distSq = filcher.distanceToSqr(partner);

        if (distSq > HANDOFF_RANGE_SQ) {
            // Walk toward partner
            filcher.getNavigation().moveTo(
                partner.getX(), partner.getY(), partner.getZ(), 1.0);
        } else {
            // Close enough — attempt the snatch
            filcher.getNavigation().stop();
            attemptSnatch();
            stop();   // goal ends regardless of outcome
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Scans nearby filchers and returns the one holding the most valuable item
     * that is strictly worth more than what this filcher currently holds.
     * Returns {@code null} if no such partner exists.
     */
    private FilcherEntity findCovetablePartner() {
        int myValue = itemValue(filcher.getMainHandItem());

        List<FilcherEntity> nearby = filcher.level().getEntitiesOfClass(
            FilcherEntity.class,
            filcher.getBoundingBox().inflate(APPROACH_RANGE),
            e -> e != filcher
              && !e.getMainHandItem().isEmpty()
              && itemValue(e.getMainHandItem()) > myValue
        );

        if (nearby.isEmpty()) return null;

        // Pick the one with the highest value (greediest target)
        return nearby.stream()
            .max((a, b) -> itemValue(a.getMainHandItem()) - itemValue(b.getMainHandItem()))
            .orElse(null);
    }

    /**
     * Attempts to take the partner's item. The snatch succeeds with probability
     * proportional to the value gap — the bigger the gap, the more eager the
     * thief and the more they succeed. Partners with a small value lead resist
     * more successfully.
     *
     * <p>On success both filchers squeak; the items are swapped so neither
     * walks away empty-handed (filchers are thieves, not muggers).
     */
    private void attemptSnatch() {
        if (filcher.level().isClientSide()) return;
        if (partner == null || !partner.isAlive()) return;

        ItemStack myItem      = filcher.getMainHandItem();
        ItemStack partnerItem = partner.getMainHandItem();
        if (myItem.isEmpty() || partnerItem.isEmpty()) return;

        int myVal      = itemValue(myItem);
        int partnerVal = itemValue(partnerItem);
        int gap        = partnerVal - myVal;   // always > 0 by canUse guard

        // Base success chance: 40% + up to 40% more for large value gaps
        // Max gap across rarity tiers is ~30, so cap contribution at 30
        double successChance = 0.40 + Math.min(gap, 30) / 30.0 * 0.40;

        if (filcher.getRandom().nextDouble() < successChance) {
            // Swap — both keep something, filcher upgrades
            filcher.setItemSlot(EquipmentSlot.MAINHAND, partnerItem.copy());
            partner.setItemSlot(EquipmentSlot.MAINHAND, myItem.copy());

            // Celebratory squeak from the winner, annoyed squeak from the loser
            playSqueak(filcher);
            playSqueak(partner);
        }
        // On failure: both walk away with what they had — just play one squeak
        else {
            playSqueak(filcher);
        }
    }

    /** Bat ambient sound at a slightly lower pitch — the filcher's private chatter. */
    private static void playSqueak(FilcherEntity f) {
        f.level().playSound(
            null, f.blockPosition(),
            SoundEvents.BAT_AMBIENT, SoundSource.NEUTRAL,
            0.6F, 0.60F
        );
    }

    /**
     * Item value heuristic. Rarity contributes heavily (Epic > Rare > Uncommon > Common);
     * stack size is a tiebreaker. This mirrors how a greedy thief thinks: a single
     * diamond is worth far more than a stack of dirt.
     */
    private static int itemValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        int rarityScore = switch (stack.getRarity()) {
            case COMMON   -> 0;
            case UNCOMMON -> 10;
            case RARE     -> 30;
            case EPIC     -> 60;
        };
        return rarityScore + stack.getCount();
    }
}
