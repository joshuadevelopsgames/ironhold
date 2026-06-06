package kingdom.smp.effect;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

public final class BleedingHandler {
    private BleedingHandler() {}

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
        Holder<MobEffect> bleeding = kingdom.smp.ModEffects.BLEEDING_EFFECT;
        if (bleeding == null || !event.getEffect().is(bleeding.getKey())) return;
        if (BYPASS_REMOVE.get()) return;
        
        // Prevent removal by milk or other default curative means.
        // It must be cured by Bandages or Golden Apples.
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        ItemStack stack = event.getItem();
        Holder<MobEffect> bleeding = kingdom.smp.ModEffects.BLEEDING_EFFECT;
        Holder<MobEffect> stifled = kingdom.smp.ModEffects.STIFLED_BLEEDING_EFFECT;

        if (bleeding == null || stifled == null) return;

        if (entity.hasEffect(bleeding)) {
            if (stack.is(Items.MILK_BUCKET)) {
                // Milk stifles the bleeding for 5 minutes (6000 ticks)
                entity.addEffect(new MobEffectInstance(stifled, 6000, 0, false, true, true));
                
                if (entity instanceof ServerPlayer player) {
                    player.sendSystemMessage(Component.literal("The milk temporarily stifles the bleeding, but you still need a bandage!").withStyle(ChatFormatting.YELLOW), true);
                }
            } else if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
                // Golden apples completely cure it
                runWithBypass(() -> entity.removeEffect(bleeding));
            }
        }
    }
}
