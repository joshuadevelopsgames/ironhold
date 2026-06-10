package kingdom.smp.alcohol;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** A bottled tavern drink that applies a small benefit and raises intoxication. */
public class AlcoholicDrinkItem extends Item {

    public enum Drink {
        SMALL_ALE("small_ale", 0xC88726, 20),
        HONEY_MEAD("honey_mead", 0xE6A92B, 28),
        APPLE_CIDER("apple_cider", 0xD46B28, 14),
        BERRY_WINE("berry_wine", 0x8A2347, 24);

        private final String id;
        private final int color;
        private final int dose;

        Drink(String id, int color, int dose) {
            this.id = id;
            this.color = color;
            this.dose = dose;
        }

        public String id() {
            return id;
        }

        public int color() {
            return color;
        }

        public int dose() {
            return dose;
        }
    }

    private final Drink drink;

    public AlcoholicDrinkItem(Properties properties, Drink drink) {
        super(properties.stacksTo(16));
        this.drink = drink;
    }

    public Drink drink() {
        return drink;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            applyDrinkBenefit(player, drink);
            AlcoholService.drink(player, drink);
            level.playSound(null, player.blockPosition(),
                SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 0.8F, 0.9F);
        }

        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (stack.isEmpty()) {
                return bottle;
            }
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        }
        return stack;
    }

    public static void applyDrinkBenefit(ServerPlayer player, Drink drink) {
        switch (drink) {
            case SMALL_ALE ->
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 20, 0));
            case HONEY_MEAD ->
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 30 * 20, 1));
            case APPLE_CIDER -> {
                player.getFoodData().eat(2, 0.4F);
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 20, 0));
            }
            case BERRY_WINE ->
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 60 * 20, 0));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.ironhold.alcoholic")
            .withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("tooltip.ironhold.alcohol_dose", drink.dose())
            .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
