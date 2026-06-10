package kingdom.smp.food;

import java.util.HashMap;
import java.util.Map;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Reward-only diet (Phase 5 ⑪): eating from varied food groups grants a scaling "Well Fed" buff; a poor
 * diet just means no bonus (never a penalty). Registered to the game bus in {@code Ironhold}.
 *
 * <p>Cooking tie-in (cooked meals stronger + Cooking-rank scaling) is the planned refinement.
 * Spec: {@code specs/fantasia-ports/10-diet.md}.
 */
public final class DietHandler {
    private DietHandler() {}

    private static final long GROUP_DURATION = 6000L; // a group stays satisfied ~5 min after eating from it
    private static final int CHECK_INTERVAL = 40;     // re-evaluate the buff every 2s

    private static final String[] GROUP_NAMES = { "protein", "grain", "vegetable", "fruit", "sweet" };
    private static final TagKey<Item>[] GROUP_TAGS = makeTags();

    @SuppressWarnings("unchecked")
    private static TagKey<Item>[] makeTags() {
        TagKey<Item>[] tags = new TagKey[GROUP_NAMES.length];
        for (int i = 0; i < GROUP_NAMES.length; i++) {
            tags[i] = TagKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "diet/" + GROUP_NAMES[i]));
        }
        return tags;
    }

    @SubscribeEvent
    public static void onFinishEating(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItem();
        long now = player.level().getGameTime();
        DietState state = player.getData(ModAttachments.DIET.get());
        Map<String, Long> next = new HashMap<>(state.until());
        boolean changed = false;
        for (int g = 0; g < GROUP_TAGS.length; g++) {
            if (stack.is(GROUP_TAGS[g])) {
                next.put(GROUP_NAMES[g], now + GROUP_DURATION);
                changed = true;
            }
        }
        if (changed) {
            player.setData(ModAttachments.DIET.get(), new DietState(Map.copyOf(next)));
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
                || player.level().isClientSide()) {
            return;
        }
        if (player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }
        long now = player.level().getGameTime();
        int satisfied = player.getData(ModAttachments.DIET.get()).satisfiedCount(now);
        if (satisfied >= 3) {
            int amp = Math.min(satisfied - 3, 2); // 3 groups → I, 4 → II, 5 → III
            player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 60, amp, true, false, true));
            if (satisfied >= 5) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, false));
            }
        }
    }
}
