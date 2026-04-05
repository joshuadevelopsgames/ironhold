package kingdom.smp.accessory;

import kingdom.smp.ModAttachments;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * {@link Container} view that always reads/writes the player's {@link AccessoryInventory}
 * attachment via {@code getData}, so gameplay code and the survival {@link net.minecraft.world.inventory.InventoryMenu}
 * always share the same backing store (avoids orphan instances from ctor-time {@code put}).
 */
public final class AccessoryInventoryAttachmentContainer implements Container {

    private final Player player;

    public AccessoryInventoryAttachmentContainer(Player player) {
        this.player = player;
    }

    private AccessoryInventory inv() {
        return player.getData(ModAttachments.ACCESSORY_INV.get());
    }

    @Override
    public int getContainerSize() {
        return inv().getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inv().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inv().getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return inv().removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return inv().removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inv().setItem(slot, stack);
    }

    @Override
    public void setChanged() {
        inv().setChanged();
    }

    @Override
    public boolean stillValid(Player p) {
        return inv().stillValid(p);
    }

    @Override
    public void clearContent() {
        inv().clearContent();
    }
}
