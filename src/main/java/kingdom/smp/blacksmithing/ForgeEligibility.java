package kingdom.smp.blacksmithing;

import kingdom.smp.gear.QualityScope;
import net.minecraft.world.item.ItemStack;

/**
 * Shared (client + server) predicate for "is this anvil setup a forge repair?"
 * Used by both the server-side result gate ({@code AnvilForgeGateMixin}) and
 * the client hint/hammer button so they agree on exactly when the normal anvil
 * repair is suppressed in favour of the forge minigame.
 */
public final class ForgeEligibility {
    private ForgeEligibility() {}

    /** Reforgeable = quality-eligible AND a damageable tool/weapon/armor (not raw ingots/ores). */
    public static boolean isReforgeable(ItemStack stack) {
        return !stack.isEmpty() && stack.isDamageableItem() && QualityScope.isEligible(stack);
    }

    /**
     * True when slot 0 is damaged reforgeable gear and slot 1 is a valid repair
     * material for it — i.e. exactly the case the forge minigame handles, so the
     * vanilla repair output is suppressed and the player must use the hammer.
     */
    public static boolean isForgeRepair(ItemStack gear, ItemStack material) {
        return isReforgeable(gear)
                && gear.isDamaged()
                && !material.isEmpty()
                && gear.isValidRepairItem(material);
    }
}
