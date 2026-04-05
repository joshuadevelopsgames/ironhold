package kingdom.smp.accessory;

import kingdom.smp.ModAttachments;
import kingdom.smp.game.AccessoryTickHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container menu for the Terraria-style accessory / vanity screen.
 * <p>
 * Slot layout:
 * <pre>
 *  0– 4  Accessory slots        (from {@link AccessoryInventory} indices 0–4)
 *  5– 8  Vanity armor slots     (from {@link AccessoryInventory} indices 5–8)
 *  9–12  Real armor display     (player inv 39,38,37,36 = HEAD,CHEST,LEGS,FEET)
 * 13     Offhand                (player inv 40)
 * 14–40  Player inventory       (player inv 9–35)
 * 41–49  Hotbar                 (player inv 0–8)
 * </pre>
 */
public class AccessoryMenu extends AbstractContainerMenu {

    private final AccessoryInventory accessoryInv;

    // ── Slot position constants ───────────────────────────────────────────────

    private static final int ARMOR_X     = 12;
    private static final int VANITY_X    = 52;
    private static final int ACCESSORY_X = 194;
    private static final int SLOT_Y0     = 10;
    private static final int SLOT_DY     = 18;

    private static final int INV_X       = 34;
    private static final int INV_Y       = 112;
    private static final int HOTBAR_Y    = 170;

    // ── Index ranges (for quickMoveStack) ─────────────────────────────────────

    private static final int ACC_START   = 0;
    private static final int ACC_END     = 5;
    private static final int VAN_START   = 5;
    private static final int VAN_END     = 9;
    private static final int ARM_START   = 9;
    private static final int ARM_END     = 13;
    private static final int OFF_SLOT    = 13;
    private static final int INV_START   = 14;
    private static final int INV_END     = 41;
    private static final int HOT_START   = 41;
    private static final int HOT_END     = 50;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Server-side constructor (real inventory). */
    public AccessoryMenu(int containerId, Inventory playerInv, AccessoryInventory accessoryInv) {
        super(AccessoryMenuTypes.ACCESSORY_MENU.get(), containerId);
        this.accessoryInv = accessoryInv;

        // Accessory slots (0–4)
        for (int i = 0; i < AccessoryInventory.ACCESSORY_SLOTS; i++) {
            addSlot(new AccessorySlot(accessoryInv, i,
                    ACCESSORY_X, SLOT_Y0 + i * SLOT_DY));
        }

        // Vanity armor slots (5–8): HEAD, CHEST, LEGS, FEET
        EquipmentSlot[] vanityOrder = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (int i = 0; i < AccessoryInventory.VANITY_SLOTS; i++) {
            addSlot(new VanitySlot(accessoryInv,
                    AccessoryInventory.ACCESSORY_SLOTS + i,
                    VANITY_X, SLOT_Y0 + i * SLOT_DY, vanityOrder[i]));
        }

        // Real armor display (9–12): HEAD → FEET
        for (int i = 0; i < 4; i++) {
            int invIdx = 39 - i; // 39=HEAD, 38=CHEST, 37=LEGS, 36=FEET
            addSlot(new Slot(playerInv, invIdx, ARMOR_X, SLOT_Y0 + i * SLOT_DY));
        }

        // Offhand (13)
        addSlot(new Slot(playerInv, 40, ARMOR_X, SLOT_Y0 + 4 * SLOT_DY + 4));

        // Player inventory 3×9 (14–40)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, 9 + row * 9 + col,
                        INV_X + col * 18, INV_Y + row * 18));
            }
        }

        // Hotbar (41–49)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, INV_X + col * 18, HOTBAR_Y));
        }
    }

    /** Client-side factory (empty accessory inventory, populated by vanilla slot sync). */
    public AccessoryMenu(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new AccessoryInventory());
    }

    // ── Shift-click logic ─────────────────────────────────────────────────────

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack current  = slot.getItem();
        ItemStack original = current.copy();

        if (index < ACC_END) {
            // Accessory → player inv
            if (!moveItemStackTo(current, INV_START, HOT_END, false)) return ItemStack.EMPTY;
        } else if (index < VAN_END) {
            // Vanity → player inv
            if (!moveItemStackTo(current, INV_START, HOT_END, false)) return ItemStack.EMPTY;
        } else if (index <= OFF_SLOT) {
            // Armor / offhand → player inv
            if (!moveItemStackTo(current, INV_START, HOT_END, false)) return ItemStack.EMPTY;
        } else {
            // Player inv / hotbar → try accessory, then vanity, then swap inv↔hotbar
            if (current.getItem() instanceof AccessoryItem) {
                if (!moveItemStackTo(current, ACC_START, ACC_END, false)) return ItemStack.EMPTY;
            } else {
                boolean moved = false;
                for (int i = VAN_START; i < VAN_END; i++) {
                    Slot vs = slots.get(i);
                    if (vs.mayPlace(current) && !vs.hasItem()) {
                        vs.setByPlayer(current.split(1));
                        moved = true;
                        break;
                    }
                }
                if (!moved) {
                    if (index < INV_END) {
                        if (!moveItemStackTo(current, HOT_START, HOT_END, false)) return ItemStack.EMPTY;
                    } else {
                        if (!moveItemStackTo(current, INV_START, INV_END, false)) return ItemStack.EMPTY;
                    }
                }
            }
        }

        if (current.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (current.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, current);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide() && player instanceof ServerPlayer sp) {
            // Persist accessory data and broadcast vanity to all tracking players
            sp.setData(ModAttachments.ACCESSORY_INV.get(), accessoryInv);
            AccessoryTickHandler.broadcastVanity(sp);
        }
    }

    public AccessoryInventory getAccessoryInventory() {
        return accessoryInv;
    }
}
