package kingdom.smp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Backing state for a vanilla tripwire hook acting as a tool/item rack.
 *
 * <p>Every tripwire hook gains one of these (the {@code EntityBlock} interface is
 * grafted onto {@link net.minecraft.world.level.block.TripWireHookBlock} by
 * {@code TripWireHookEntityBlockMixin}) — but it stays empty and inert until a
 * player right-clicks an item onto it. It does not tick.
 *
 * <p>Holds a single displayed {@link ItemStack} plus an {@code orientation}
 * (0-3) the renderer reads to hang the item one of four ways: down, left, up,
 * right. Sneak-right-click cycles the orientation; plain right-click hangs /
 * retrieves the item (see {@code TripwireRackHandler}). The item drops when the
 * hook is broken via {@code BlockDropsEvent}.
 */
public class TripwireRackBlockEntity extends BlockEntity {

    /** Number of hanging orientations the item cycles through: down → left → up → right. */
    public static final int ORIENTATION_COUNT = 4;

    private ItemStack item = ItemStack.EMPTY;
    private int orientation = 0;

    public TripwireRackBlockEntity(BlockPos pos, BlockState state) {
        super(kingdom.smp.ModBlocks.TRIPWIRE_RACK_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStack getItem() {
        return item;
    }

    public int getOrientation() {
        return orientation;
    }

    /** Hang an item on the rack. Pass a single-count stack. */
    public void setItem(ItemStack stack) {
        this.item = stack == null ? ItemStack.EMPTY : stack;
        markUpdate();
    }

    /** Take the displayed item off, returning what was there (possibly empty). */
    public ItemStack removeItem() {
        ItemStack prev = this.item;
        this.item = ItemStack.EMPTY;
        markUpdate();
        return prev;
    }

    /** Advance the hanging direction one step: down → left → up → right → down. */
    public void cycleOrientation() {
        this.orientation = (this.orientation + 1) % ORIENTATION_COUNT;
        markUpdate();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("Orientation", orientation);
        // ItemStack.CODEC rejects empty stacks — only store when something hangs here.
        if (!item.isEmpty()) {
            out.store("Item", ItemStack.CODEC, item);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        orientation = in.getIntOr("Orientation", 0);
        item = in.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void markUpdate() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
}
