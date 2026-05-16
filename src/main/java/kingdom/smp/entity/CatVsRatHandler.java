package kingdom.smp.entity;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Injects "hunt rats" goals into vanilla cats and ocelots when they enter the world.
 * Cats already chase rabbits via a built-in goal; this adds the same treatment for rats,
 * with Black Rats prioritized.
 *
 * <p>Two goals stacked at different priorities so a Black Rat in sight always wins over a
 * normal one. Both run alongside the cat's existing rabbit-hunting goal.
 */
public final class CatVsRatHandler {
    private CatVsRatHandler() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Cat cat) {
            injectRatTargeting(cat);
        } else if (event.getEntity() instanceof Ocelot ocelot) {
            injectRatTargeting(ocelot);
        }
    }

    private static void injectRatTargeting(net.minecraft.world.entity.PathfinderMob hunter) {
        // Priority 2: Black Rats first
        hunter.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
            hunter, RatEntity.class, 10, true, true,
            (entity, level) -> entity instanceof RatEntity rat && rat.isBlackRat()));
        // Priority 3: any rat
        hunter.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
            hunter, RatEntity.class, true));
    }
}
