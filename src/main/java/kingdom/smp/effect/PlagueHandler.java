package kingdom.smp.effect;

import kingdom.smp.Ironhold;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/**
 * Handles two parts of the Plague system that live outside the effect class:
 *
 * <ul>
 *   <li>Milk-bucket cure restriction — vanilla milk removes <em>all</em> effects, but Plague
 *       past Stage 0 should require the Plague Tonic. We cancel the Remove event for plague
 *       at Stage 1+, unless our own tonic-use code has set the bypass flag.</li>
 *   <li>Plague bubo drop — when a plagued cow or player dies (any cause, but with the effect
 *       active at Stage 1+), always drop a {@code plague_bubo}.</li>
 *   <li>Plague bubo pickup — touching a bubo infects an uninfected player with fresh Stage 0
 *       plague. Already-plagued players are unaffected (so the bubo can't be used to reset
 *       a late-stage infection back to Stage 0).</li>
 * </ul>
 */
public final class PlagueHandler {
    private PlagueHandler() {}

    /** Set true within {@code PlagueTonicItem} so the Remove event lets the cure proceed. */
    private static final ThreadLocal<Boolean> BYPASS_REMOVE = ThreadLocal.withInitial(() -> false);

    public static void runWithBypass(Runnable action) {
        BYPASS_REMOVE.set(true);
        try {
            action.run();
        } finally {
            BYPASS_REMOVE.set(false);
        }
    }

    @SubscribeEvent
    public static void onEffectRemove(MobEffectEvent.Remove event) {
        Holder<MobEffect> plague = kingdom.smp.ModEffects.PLAGUE_EFFECT;
        if (!event.getEffect().is(plague.getKey())) return;
        if (BYPASS_REMOVE.get()) return;

        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null) return;
        int stage = PlagueEffect.stageOf(instance);
        if (stage >= 1) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;

        Holder<MobEffect> plague = kingdom.smp.ModEffects.PLAGUE_EFFECT;
        MobEffectInstance instance = victim.getEffect(plague);
        if (instance == null) return;
        int stage = PlagueEffect.stageOf(instance);
        if (stage < 1) return;

        if (!(victim instanceof Player) && !(victim instanceof AbstractCow)) return;

        ItemStack bubo = new ItemStack(kingdom.smp.ModItems.PLAGUE_BUBO.get());
        victim.spawnAtLocation((net.minecraft.server.level.ServerLevel) victim.level(), bubo);
    }

    @SubscribeEvent
    public static void onBuboPickup(ItemEntityPickupEvent.Post event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide()) return;
        if (!event.getOriginalStack().is(kingdom.smp.ModItems.PLAGUE_BUBO.get())) return;

        Holder<MobEffect> plague = kingdom.smp.ModEffects.PLAGUE_EFFECT;
        if (player.hasEffect(plague)) return;

        player.addEffect(new MobEffectInstance(
            plague, PlagueEffect.TOTAL_DURATION_TICKS, 0, false, false, true));
    }
}
