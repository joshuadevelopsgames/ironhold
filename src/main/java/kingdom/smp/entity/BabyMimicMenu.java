package kingdom.smp.entity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 5-slot chest menu for the baby mimic's inventory.
 * Laid out as a single row of 5 slots centered above the player inventory.
 */
public class BabyMimicMenu extends AbstractContainerMenu {

    public static final int MIMIC_SLOTS = 5;
    private final Container mimicInv;

    /** Server-side constructor — real inventory. */
    public BabyMimicMenu(int containerId, Inventory playerInv, Container mimicInv) {
        super(BabyMimicMenuTypes.BABY_MIMIC_MENU.get(), containerId);
        this.mimicInv = mimicInv;
        mimicInv.startOpen(playerInv.player);

        // 5 mimic slots in a row, centered (starts at x=44 for centering in 176-wide GUI)
        for (int i = 0; i < MIMIC_SLOTS; i++) {
            addSlot(new Slot(mimicInv, i, 44 + i * 18, 20));
        }

        // Player inventory (3×9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, 9 + row * 9 + col, 8 + col * 18, 51 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 109));
        }
    }

    /** Client-side factory — empty container, synced by vanilla. */
    public BabyMimicMenu(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(MIMIC_SLOTS));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack current = slot.getItem();
        ItemStack original = current.copy();

        if (index < MIMIC_SLOTS) {
            // Mimic → player
            if (!moveItemStackTo(current, MIMIC_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // Player → mimic
            if (!moveItemStackTo(current, 0, MIMIC_SLOTS, false)) return ItemStack.EMPTY;
        }

        if (current.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return mimicInv.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        mimicInv.stopOpen(player);
    }
}
