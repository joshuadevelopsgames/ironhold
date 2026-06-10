package kingdom.smp.quest;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container menu for the Quest Board GUI.
 *
 * <p>Only the player's inventory + hotbar are real {@link Slot}s. Quest task
 * icons and reward zones are display/click-only, handled by
 * {@link kingdom.smp.client.screen.QuestBoardScreen}.
 *
 * <p><strong>Slot layout is per-slot — every one of the 36 player-inventory
 * slots is positioned individually</strong> via the {@code SLOT_POSITIONS}
 * 36-element array. Index 0–8 is the hotbar; 9–35 are the three main rows
 * left-to-right, top-to-bottom (matching {@link Inventory}'s internal
 * indexing). Tweak any single position to nudge a slot — they don't have to
 * be a uniform grid.
 */
public class QuestBoardMenu extends AbstractContainerMenu {

    /**
     * Per-slot (x, y) for all 36 player-inventory slots, in the order
     * AbstractContainerScreen expects them registered:
     *
     * <pre>
     *   indices 0–26  : main inventory (rows 1, 2, 3 left→right)  ← Inventory[ 9..35 ]
     *   indices 27–35 : hotbar                                     ← Inventory[ 0..8  ]
     * </pre>
     *
     * <p>Currently a uniform grid that approximates the painted slot strip.
     * Replace any individual entry with a custom (x, y) to break the grid.
     */
    private static final int[][] SLOT_POSITIONS = buildDefaultLayout();

    private static int[][] buildDefaultLayout() {
        // Derived by scanning quest_board.png for #242323 (painted slot interior),
        // computed for the 232×240 GUI size (multiples of 8 — keeps GUI scale stable).
        // Cols at 40, 57, 75, 91, 108, 125, 142, 159, 176 (pitch ~17)
        // Rows at y=153, 169, 184 (pitch ~16); hotbar at 203.
        final int[] COL_X = { 40, 57, 75, 91, 108, 125, 142, 159, 176 };
        final int ROW_Y_0  = 153;
        final int ROW_Y_1  = 169;
        final int ROW_Y_2  = 184;
        final int HOTBAR_Y = 203;

        int[][] pos = new int[36][2];
        for (int r = 0; r < 3; r++) {
            int y = (r == 0 ? ROW_Y_0 : r == 1 ? ROW_Y_1 : ROW_Y_2);
            for (int c = 0; c < 9; c++) {
                pos[r * 9 + c] = new int[]{ COL_X[c], y };
            }
        }
        for (int c = 0; c < 9; c++) {
            pos[27 + c] = new int[]{ COL_X[c], HOTBAR_Y };
        }
        return pos;
    }

    private QuestData questData;

    /** Server-side: real player inventory + injected quest data. */
    public QuestBoardMenu(int containerId, Inventory playerInv, QuestData quest) {
        super(QuestBoardMenuTypes.QUEST_BOARD_MENU.get(), containerId);
        this.questData = quest != null ? quest : QuestData.EMPTY;
        addPlayerInventory(playerInv);
    }

    /** Fallback ctor (no extra data) — real opens go through the buffer-decoding menu factory. */
    public QuestBoardMenu(int containerId, Inventory playerInv) {
        this(containerId, playerInv, QuestData.EMPTY);
    }

    private void addPlayerInventory(Inventory playerInv) {
        // Main inventory: index 0–26 in the menu's slot list, backed by
        // Inventory[9..35] (vanilla's main-inventory indices).
        for (int i = 0; i < 27; i++) {
            int[] xy = SLOT_POSITIONS[i];
            addSlot(new Slot(playerInv, i + 9, xy[0], xy[1]));
        }
        // Hotbar: menu indices 27–35, backed by Inventory[0..8].
        for (int i = 0; i < 9; i++) {
            int[] xy = SLOT_POSITIONS[27 + i];
            addSlot(new Slot(playerInv, i, xy[0], xy[1]));
        }
    }

    public QuestData questData() { return questData; }

    public void setQuestData(QuestData q) { this.questData = q; }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        // 0–26 = main inv, 27–35 = hotbar — shift moves between them.
        if (index < 27) {
            if (!moveItemStackTo(stack, 27, 36, false)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, 27, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
