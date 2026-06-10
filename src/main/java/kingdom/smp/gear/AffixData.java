package kingdom.smp.gear;

import java.util.List;

import net.minecraft.world.item.ItemStack;

/** Read/write helpers for the {@link GearComponents#AFFIXES} component + the quality→slot-count rule. */
public final class AffixData {
    private AffixData() {}

    public static List<AffixInstance> get(ItemStack stack) {
        return stack.getOrDefault(GearComponents.AFFIXES.get(), List.of());
    }

    public static void set(ItemStack stack, List<AffixInstance> list) {
        if (list.isEmpty()) {
            stack.remove(GearComponents.AFFIXES.get());
        } else {
            stack.set(GearComponents.AFFIXES.get(), List.copyOf(list));
        }
    }

    public static boolean has(ItemStack stack, Affix affix) {
        for (AffixInstance ai : get(stack)) {
            if (affix.id().equals(ai.id())) return true;
        }
        return false;
    }

    public static float rollOf(ItemStack stack, Affix affix) {
        for (AffixInstance ai : get(stack)) {
            if (affix.id().equals(ai.id())) return ai.roll();
        }
        return 0f;
    }

    /** Number of affix slots this item can hold = its {@link ItemQuality} tier. */
    public static int capacity(ItemStack stack) {
        return switch (GearComponents.getQuality(stack)) {
            case POOR -> 0;
            case FINE -> 1;
            case GOOD -> 2;
            case MINT -> 3;
        };
    }
}
