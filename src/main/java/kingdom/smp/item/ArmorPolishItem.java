package kingdom.smp.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Armor Polish — a consumable that permanently increases the max durability
 * of the armor piece in the player's other hand by 20%.
 * Can be applied up to 3 times per item (tracked via custom damage component).
 */
public class ArmorPolishItem extends Item {

    private static final int MAX_APPLICATIONS = 3;
    private static final float DURABILITY_BOOST = 0.20F;

    public ArmorPolishItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // Polish is used on the item in the OTHER hand
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND
            ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack target = player.getItemInHand(otherHand);

        if (target.isEmpty() || target.getMaxDamage() <= 0) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal("Hold the armor you want to polish in your other hand.")
                    .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }

        // Check if it's armor
        boolean isArmor = target.is(net.minecraft.tags.ItemTags.HEAD_ARMOR)
            || target.is(net.minecraft.tags.ItemTags.CHEST_ARMOR)
            || target.is(net.minecraft.tags.ItemTags.LEG_ARMOR)
            || target.is(net.minecraft.tags.ItemTags.FOOT_ARMOR);
        if (!isArmor) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal("Armor Polish can only be used on armor pieces.")
                    .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }

        // Check application count (stored in custom data)
        int applications = getPolishCount(target);
        if (applications >= MAX_APPLICATIONS) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal("This armor has already been polished to its limit!")
                    .withStyle(ChatFormatting.GOLD));
            }
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            // Increase max durability by 20%
            int currentMax = target.getMaxDamage();
            int newMax = currentMax + (int) (currentMax * DURABILITY_BOOST);
            target.set(DataComponents.MAX_DAMAGE, newMax);

            // Track applications
            setPolishCount(target, applications + 1);

            // Consume the polish
            ItemStack polishStack = player.getItemInHand(hand);
            if (!player.getAbilities().instabuild) {
                polishStack.shrink(1);
            }

            level.playSound(null, player.blockPosition(),
                SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.6F, 1.4F);

            player.sendSystemMessage(Component.literal("Armor polished! (" + (applications + 1) + "/" + MAX_APPLICATIONS + ")")
                .withStyle(ChatFormatting.GREEN));
        }

        return InteractionResult.SUCCESS;
    }

    private static int getPolishCount(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0;
        return customData.copyTag().getIntOr("ArmorPolish", 0);
    }

    private static void setPolishCount(ItemStack stack, int count) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putInt("ArmorPolish", count);
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("Consumable").withStyle(ChatFormatting.YELLOW));
        tooltip.accept(Component.literal("Hold armor in off-hand, use to polish")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.accept(Component.literal("+20% durability per use (max 3)")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
