package kingdom.smp.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Client-side cache of vanity armor items for all known players.
 * Populated by {@link kingdom.smp.net.SyncVanityPayload}; read by the
 * vanity-rendering mixin so other players' vanity overrides are visible.
 */
public final class VanityCache {
    private VanityCache() {}

    private static final Map<UUID, VanityData> CACHE = new ConcurrentHashMap<>();

    public record VanityData(ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet) {
        public ItemStack forSlot(EquipmentSlot slot) {
            return switch (slot) {
                case HEAD  -> head;
                case CHEST -> chest;
                case LEGS  -> legs;
                case FEET  -> feet;
                default    -> ItemStack.EMPTY;
            };
        }
    }

    public static void update(UUID uuid, ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet) {
        CACHE.put(uuid, new VanityData(head, chest, legs, feet));
    }

    public static ItemStack getVanity(UUID uuid, EquipmentSlot slot) {
        VanityData data = CACHE.get(uuid);
        return data != null ? data.forSlot(slot) : ItemStack.EMPTY;
    }

    public static void remove(UUID uuid) {
        CACHE.remove(uuid);
    }

    public static void clear() {
        CACHE.clear();
    }
}
