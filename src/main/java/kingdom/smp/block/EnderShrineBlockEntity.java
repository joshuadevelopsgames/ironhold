package kingdom.smp.block;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * State for an {@link EnderShrineBlock}: stocked revive {@code charges} and the binding {@code owner}.
 * Charges are added by using an Ender Totem on the block and consumed by a death-rescue.
 */
public class EnderShrineBlockEntity extends BlockEntity {

    public static final int MAX_CHARGES = 5;

    private int charges = 0;
    private UUID owner;

    public EnderShrineBlockEntity(BlockPos pos, BlockState state) {
        super(kingdom.smp.ModBlocks.ENDER_SHRINE_BLOCK_ENTITY.get(), pos, state);
    }

    public int getCharges() {
        return charges;
    }

    /** @return true if a charge was added; false if already at {@link #MAX_CHARGES}. */
    public boolean addCharge() {
        if (charges >= MAX_CHARGES) {
            return false;
        }
        charges++;
        setChanged();
        return true;
    }

    /** @return true if a charge was consumed; false if empty. */
    public boolean consumeCharge() {
        if (charges <= 0) {
            return false;
        }
        charges--;
        setChanged();
        return true;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("Charges", charges);
        if (owner != null) {
            out.store("Owner", UUIDUtil.CODEC, owner);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        charges = in.getIntOr("Charges", 0);
        owner = in.read("Owner", UUIDUtil.CODEC).orElse(null);
    }
}
