package kingdom.smp.dynlight;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Held-item → luminance lookup. Two sources, in order:
 *
 * <ol>
 *   <li>Explicit entries loaded from {@code assets/<ns>/dynamiclights/item/*.json}.
 *   <li>{@link BlockItem}s whose default block emits light (torch, lantern, glowstone…).
 *       This is the "light source" half of the user-facing scope rule.
 * </ol>
 *
 * <p>Items that fall through both paths emit no light, no matter who holds them.
 */
public final class ItemLightRegistry {
    private ItemLightRegistry() {}

    /** Replaced atomically by {@link DynamicLightLoader} on resource reload. */
    private static volatile Map<Identifier, Integer> registered = Map.of();

    static void setRegistered(Map<Identifier, Integer> next) {
        registered = next;
    }

    public static int luminanceOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        Item item = stack.getItem();
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        Integer explicit = id == null ? null : registered.get(id);
        if (explicit != null && explicit > 0) return Math.min(15, explicit);
        if (item instanceof BlockItem block) {
            int e = block.getBlock().defaultBlockState().getLightEmission();
            if (e > 0) return Math.min(15, e);
        }
        return 0;
    }

    public static int registeredCount() { return registered.size(); }
}
