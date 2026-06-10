package kingdom.smp.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import kingdom.smp.ModAttachments;
import kingdom.smp.accessory.AccessoryInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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

    /**
     * Resolve the vanity item worn in {@code slot} for a player on the client. Returns
     * {@link ItemStack#EMPTY} when nothing is worn.
     *
     * <p>For the <b>local player</b> we prefer the {@link AccessoryInventory} attachment: it's kept
     * current on this client (container sync while the accessory menu is open, owner-sync on close),
     * giving instant, flicker-free preview of your own changes. For <b>other players</b> we use the
     * {@link #getVanity synced cache} only — their attachment is sent just once, when we start
     * tracking them, so it goes stale on later equip/unequip. (That staleness is exactly why vanity
     * used to refresh on other screens only after the wearer respawned, which re-tracks the entity.)
     * {@link kingdom.smp.net.SyncVanityPayload} keeps the cache live on every change.
     */
    public static ItemStack resolve(LivingEntity entity, EquipmentSlot slot) {
        if (entity == Minecraft.getInstance().player) {
            AccessoryInventory inv = entity.getExistingDataOrNull(ModAttachments.ACCESSORY_INV.get());
            if (inv != null) {
                ItemStack vanity = inv.getVanityForSlot(slot);
                if (!vanity.isEmpty()) return vanity;
            }
        }
        return getVanity(entity.getUUID(), slot);
    }

    public static void remove(UUID uuid) {
        CACHE.remove(uuid);
    }

    public static void clear() {
        CACHE.clear();
    }
}
