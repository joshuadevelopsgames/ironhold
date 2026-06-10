package kingdom.smp.gear;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Rolls affixes onto a gear stack: count = {@link ItemQuality} tier, pulled from the pool matching the
 * item's {@link GearClass} (weapon → offensive/on-hit, armor → defensive/utility, tool → utility).
 * Each affix rolls a value within its range. Spec: {@code specs/fantasia-ports/07-gear-affixes.md}.
 */
public final class AffixRoller {
    private AffixRoller() {}

    public enum GearClass { WEAPON, ARMOR, TOOL, NONE }

    private static final Random RAND = new Random();

    /** Replace this item's affixes with a fresh roll appropriate to its quality + gear class. */
    public static void roll(ItemStack stack) {
        if (!QualityScope.isEligible(stack)) {
            AffixData.set(stack, List.of());
            return;
        }
        GearClass gc = gearClass(stack);
        int cap = AffixData.capacity(stack);
        if (gc == GearClass.NONE || cap <= 0) {
            AffixData.set(stack, List.of());
            return;
        }
        List<Affix> pool = new ArrayList<>(poolFor(gc));
        Collections.shuffle(pool, RAND);

        List<AffixInstance> result = new ArrayList<>();
        for (int i = 0; i < cap && i < pool.size(); i++) {
            Affix a = pool.get(i);
            float roll = a.min() + RAND.nextFloat() * (a.max() - a.min());
            result.add(new AffixInstance(a.id(), roll));
        }
        AffixData.set(stack, result);
    }

    public static List<Affix> poolFor(GearClass gc) {
        return switch (gc) {
            case WEAPON -> Affix.forCategories(AffixCategory.OFFENSIVE, AffixCategory.ON_HIT);
            case ARMOR -> Affix.forCategories(AffixCategory.DEFENSIVE, AffixCategory.UTILITY);
            case TOOL -> Affix.forCategories(AffixCategory.UTILITY);
            case NONE -> List.of();
        };
    }

    public static GearClass gearClass(ItemStack stack) {
        var eq = stack.get(DataComponents.EQUIPPABLE);
        if (eq != null) {
            EquipmentSlot s = eq.slot();
            if (s == EquipmentSlot.HEAD || s == EquipmentSlot.CHEST
                    || s == EquipmentSlot.LEGS || s == EquipmentSlot.FEET) {
                return GearClass.ARMOR;
            }
        }
        if (stack.has(DataComponents.TOOL)) {
            return GearClass.TOOL;
        }
        if (hasAttackDamage(stack)) {
            return GearClass.WEAPON;
        }
        return GearClass.NONE;
    }

    private static boolean hasAttackDamage(ItemStack stack) {
        ItemAttributeModifiers mods = stack.getAttributeModifiers();
        for (ItemAttributeModifiers.Entry e : mods.modifiers()) {
            if (e.attribute().equals(Attributes.ATTACK_DAMAGE)) {
                return true;
            }
        }
        return false;
    }
}
