package kingdom.smp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Backing state for {@link ChaliceBlock} — stores the single liquid currently held
 * (its id, surface tint, opacity and whether it glows) so {@code ChaliceRenderer}
 * can draw the surface inside the cup. An empty {@link #liquidId} means the chalice
 * is empty. State persists across save/load and is synced to the client.
 */
public class ChaliceBlockEntity extends BlockEntity {

    private String liquidId = "";
    private int rgb = 0;
    private float alpha = 1f;
    private boolean emissive = false;
    /** Full brew when the held liquid is a potion, so drinking applies its real effects. */
    @Nullable private PotionContents potion = null;

    public ChaliceBlockEntity(BlockPos pos, BlockState state) {
        super(kingdom.smp.ModBlocks.CHALICE_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isEmpty()    { return liquidId.isEmpty(); }
    public String  liquidId()   { return liquidId; }
    public int     rgb()        { return rgb; }
    public float   alpha()      { return alpha; }
    public boolean emissive()   { return emissive; }
    @Nullable public PotionContents potion() { return potion; }

    public void fill(ChaliceLiquids.Fill fill) {
        this.liquidId = fill.id();
        this.rgb = fill.rgb();
        this.alpha = fill.alpha();
        this.emissive = fill.emissive();
        this.potion = fill.potion();
        markUpdate();
    }

    public void empty() {
        this.liquidId = "";
        this.rgb = 0;
        this.alpha = 1f;
        this.emissive = false;
        this.potion = null;
        markUpdate();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!liquidId.isEmpty()) {
            out.putString("LiquidId", liquidId);
            out.putInt("Rgb", rgb);
            out.putFloat("Alpha", alpha);
            out.putBoolean("Emissive", emissive);
            if (potion != null) {
                out.store("Potion", PotionContents.CODEC, potion);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        liquidId = in.getStringOr("LiquidId", "");
        rgb = in.getIntOr("Rgb", 0);
        alpha = in.getFloatOr("Alpha", 1f);
        emissive = in.getBooleanOr("Emissive", false);
        potion = in.read("Potion", PotionContents.CODEC).orElse(null);
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
