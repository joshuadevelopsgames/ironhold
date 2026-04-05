package kingdom.smp.accessory;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Holds 9 item stacks: 5 accessory slots (indices 0–4) and 4 vanity armor
 * slots (indices 5–8, mapped to HEAD / CHEST / LEGS / FEET).
 * <p>
 * Persisted via {@link #CODEC} as a NeoForge attachment on the player entity
 * and synchronised to clients via {@link #STREAM_CODEC}.
 */
public class AccessoryInventory implements Container {

    public static final int ACCESSORY_SLOTS = 5;
    public static final int VANITY_SLOTS    = 4;
    public static final int TOTAL_SLOTS     = ACCESSORY_SLOTS + VANITY_SLOTS; // 9

    /** Vanity-slot indices inside this container. */
    public static final int VANITY_HEAD  = 5;
    public static final int VANITY_CHEST = 6;
    public static final int VANITY_LEGS  = 7;
    public static final int VANITY_FEET  = 8;

    private final NonNullList<ItemStack> items =
            NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    /** Transient flag — set when a vanity slot changes, cleared after broadcast. */
    private transient boolean vanityDirty = false;

    public AccessoryInventory() {}

    public boolean isVanityDirty()   { return vanityDirty; }
    public void clearVanityDirty()   { vanityDirty = false; }

    /** Factory used by the attachment default-value supplier. */
    public static AccessoryInventory defaultData() {
        return new AccessoryInventory();
    }

    // ── Codecs ────────────────────────────────────────────────────────────────

    /**
     * Lenient: {@code items} may be absent or short (older saves). Extra/missing list entries
     * are padded/truncated so attachment load cannot fail login entirely.
     */
    public static final MapCodec<AccessoryInventory> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            ItemStack.OPTIONAL_CODEC
                .listOf()
                .optionalFieldOf("items", List.of())
                .forGetter(inv -> {
                    List<ItemStack> list = new ArrayList<>(TOTAL_SLOTS);
                    for (int i = 0; i < TOTAL_SLOTS; i++) {
                        list.add(inv.items.get(i));
                    }
                    return list;
                }))
            .apply(instance, list -> {
                AccessoryInventory inv = new AccessoryInventory();
                for (int i = 0; i < TOTAL_SLOTS; i++) {
                    inv.items.set(i, i < list.size() ? list.get(i) : ItemStack.EMPTY);
                }
                return inv;
            }));

    public static final Codec<AccessoryInventory> CODEC =
            ItemStack.OPTIONAL_CODEC.listOf().xmap(
                    list -> {
                        AccessoryInventory inv = new AccessoryInventory();
                        for (int i = 0; i < Math.min(list.size(), TOTAL_SLOTS); i++) {
                            inv.items.set(i, list.get(i));
                        }
                        return inv;
                    },
                    inv -> {
                        List<ItemStack> list = new ArrayList<>(TOTAL_SLOTS);
                        for (int i = 0; i < TOTAL_SLOTS; i++) {
                            list.add(inv.items.get(i));
                        }
                        return list;
                    });

    public static final StreamCodec<RegistryFriendlyByteBuf, AccessoryInventory> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public AccessoryInventory decode(RegistryFriendlyByteBuf buf) {
                    AccessoryInventory inv = new AccessoryInventory();
                    for (int i = 0; i < TOTAL_SLOTS; i++) {
                        inv.items.set(i, ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                    }
                    return inv;
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, AccessoryInventory inv) {
                    for (int i = 0; i < TOTAL_SLOTS; i++) {
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, inv.items.get(i));
                    }
                }
            };

    // ── Container implementation ──────────────────────────────────────────────

    @Override public int getContainerSize()           { return TOTAL_SLOTS; }
    @Override public boolean stillValid(Player player) { return true; }
    @Override public void setChanged()                 { /* no-op */ }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < TOTAL_SLOTS ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(items, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < TOTAL_SLOTS) {
            items.set(slot, stack);
            if (slot >= ACCESSORY_SLOTS) {
                vanityDirty = true;
            }
        }
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    // ── Vanity helpers ────────────────────────────────────────────────────────

    /** Copy all items from another inventory. */
    public void copyFrom(AccessoryInventory other) {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            items.set(i, other.getItem(i).copy());
        }
    }

    /** Get the vanity {@link ItemStack} for the given {@link EquipmentSlot}. */
    public ItemStack getVanityForSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD  -> items.get(VANITY_HEAD);
            case CHEST -> items.get(VANITY_CHEST);
            case LEGS  -> items.get(VANITY_LEGS);
            case FEET  -> items.get(VANITY_FEET);
            default    -> ItemStack.EMPTY;
        };
    }
}
