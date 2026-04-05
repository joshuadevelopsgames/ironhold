package kingdom.smp.game;

import kingdom.smp.Ironhold;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.neoforged.neoforge.attachment.AttachmentType;

/** Stub carry-weight: tag-based stack weight + class max; applies transient move-speed penalty. */
public final class EncumbranceHandler {
    /** Multiplies final move speed: value v => factor (1 + v). */
    private static final Identifier ENCUMBRANCE_MUL =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "encumbrance_mul");
    /** Flat subtract from move speed after multipliers. */
    private static final Identifier ENCUMBRANCE_ADD =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "encumbrance_add");

    private EncumbranceHandler() {}

    public static int weightFor(ServerPlayer player) {
        return computeWeight(player);
    }

    /** Client-safe: works on any Player (local or server). */
    public static int weightForAnyPlayer(Player player) {
        return computeWeight(player);
    }

    public static void tick(ServerPlayer player, AttachmentType<PlayerKingdomRpgData> rpgKey) {
        PlayerKingdomRpgData rpg = player.getData(rpgKey);
        int max = rpg.playerClass().maxCarryWeight();
        int weight = computeWeight(player);
        var move = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move == null) {
            return;
        }
        move.removeModifier(ENCUMBRANCE_MUL);
        move.removeModifier(ENCUMBRANCE_ADD);
        double ratio = max <= 0 ? 0.0 : (double) weight / (double) max;
        if (ratio > 1.0) {
            double over = ratio - 1.0;
            // Old curve was ~invisible just above cap; keep a floor slowdown so it’s obvious.
            double multPenalty = Mth.clamp(0.18 + over * 0.75, 0.18, 0.72);
            move.addTransientModifier(
                new AttributeModifier(ENCUMBRANCE_MUL, -multPenalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            double flatPenalty = Mth.clamp(0.018 + over * 0.05, 0.018, 0.09);
            if (flatPenalty > 0.0) {
                move.addTransientModifier(
                    new AttributeModifier(ENCUMBRANCE_ADD, -flatPenalty, AttributeModifier.Operation.ADD_VALUE));
            }
            int slowAmp = over >= 1.5 ? 2 : over >= 0.5 ? 1 : 0;
            player.addEffect(
                new MobEffectInstance(MobEffects.SLOWNESS, 45, slowAmp, false, false, true));
        }
        // Under cap: we stop applying; Slowness from this mod expires within a few seconds.
    }

    private static int computeWeight(Player player) {
        int w = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            w += stackWeight(stack);
        }
        w += stackWeight(player.getItemBySlot(EquipmentSlot.OFFHAND));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmor() || slot == EquipmentSlot.BODY) {
                ItemStack stack = player.getItemBySlot(slot);
                w += stackWeight(stack);
            }
        }
        return w;
    }

    private static int stackWeight(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int perItem;
        if (isArmor(stack)) {
            perItem = 25;
        } else if (isToolWeapon(stack)) {
            perItem = 15;
        } else if (stack.getItem() instanceof BlockItem) {
            perItem = 2;
        } else {
            perItem = 3;
        }
        return perItem * stack.getCount();
    }

    private static boolean isArmor(ItemStack stack) {
        return stack.is(ItemTags.HEAD_ARMOR)
            || stack.is(ItemTags.CHEST_ARMOR)
            || stack.is(ItemTags.LEG_ARMOR)
            || stack.is(ItemTags.FOOT_ARMOR);
    }

    private static boolean isToolWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS)
            || stack.is(ItemTags.AXES)
            || stack.is(ItemTags.PICKAXES)
            || stack.is(ItemTags.SHOVELS)
            || stack.is(ItemTags.HOES);
    }
}
